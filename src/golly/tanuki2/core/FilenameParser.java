package golly.tanuki2.core;

import golly.tanuki2.data.DirData;
import golly.tanuki2.data.TrackProperties;
import golly.tanuki2.data.TrackPropertyType;
import golly.tanuki2.support.Helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Golly
 * @since 23/02/2007
 */
public class FilenameParser implements ITrackProprtyReader {

	// =============================================================================================== //
	// = SmartPattern
	// =============================================================================================== //

	@SuppressWarnings("nls")
	public static class SmartPattern {

		// ======================================================= //
		// = SmartPattern: Static
		// ======================================================= //

		private static final Map<TrackPropertyType, String> trackPropertyPatterns= new HashMap<TrackPropertyType, String>();
		private static final Map<String, String> macros= new HashMap<String, String>();
		private static final Pattern patTrackPropertyMacro= Pattern.compile("\\[:([a-z]+):\\]");

		private static String possiblyEnclosed(String x) {
			return "(?:[\\[({<])?" + x + "(?:[\\])}>])?";
		}

		static {
			trackPropertyPatterns.put(TrackPropertyType.ARTIST, "([^\\\\/\\[\\](){}.]+?)");
			trackPropertyPatterns.put(TrackPropertyType.YEAR, possiblyEnclosed("((?:1[7-9]|20)\\d{2})"));
			trackPropertyPatterns.put(TrackPropertyType.ALBUM, "([^\\\\/]+?)");
			trackPropertyPatterns.put(TrackPropertyType.TN, possiblyEnclosed("(0*(?:1\\d{2}|\\d{1,2}))"));
			trackPropertyPatterns.put(TrackPropertyType.TRACK, "([^\\\\/]+?)");
			macros.put("sep", " *- *");
			macros.put("sepOrSpace", Helpers.regexOr(macros.get("sep"), " +"));
			macros.put("sepSpaceOrUndsc", Helpers.regexOr(macros.get("sep"), "[ _]+"));
			macros.put("sepSpaceUndscOrDot", Helpers.regexOr(macros.get("sep"), "[ _]+", "\\. *"));
			macros.put("website", possiblyEnclosed("(?:[a-z0-9_-]+\\.)+(?:com|org|net)(?:\\.[a-z]{2,3})?"));
			String bitrate= "(?:96|112|128|160|192|224|256|320|vbr(?: ?(?:hq|[0-9]+))?)" + "(?: *(?:k|kbps))?";
			macros.put("bitrate", possiblyEnclosed(bitrate + "(?:" + macros.get("sepOrSpace") + bitrate + ")?"));
			macros.put("dirCrap", "(?: *" + Helpers.regexOr(macros.get("website"), macros.get("bitrate")) + ")*");
			macros.put("album_andor_artist", Helpers.regexOr("[:artist:](?:<sepOrSpace>[:album:])?", "[:album:](?:<sepOrSpace>[:artist:])?"));

		}

		// ======================================================= //
		// = SmartPattern: Instance
		// ======================================================= //

		public final Pattern pat;
		public final Map<TrackPropertyType, Integer> indexes= new HashMap<TrackPropertyType, Integer>();

		public SmartPattern(String... patternStrings) {
			String patternString= ".*[\\\\/]" + Helpers.join(patternStrings, "<dirCrap>[\\\\/]") + "\\.[^.]+$";

			// Replace macros with regex
			for (String i : macros.keySet()) {
				patternString= patternString.replace("<" + i + ">?", "(?:" + macros.get(i) + ")?");
				patternString= patternString.replace("<" + i + ">", "(?:" + macros.get(i) + ")");
			}

			// Get indexes for track properties and add back-references
			StringBuilder patternStringWithBackReferences= new StringBuilder();
			int charsCopiesFromPatternString= 0;
			Matcher m= patTrackPropertyMacro.matcher(patternString);
			int index= 0;
			while (m.find()) {
				index++;
				final String name= m.group(1);
				TrackPropertyType tp= null;
				for (TrackPropertyType i : TrackPropertyType.values())
					if (i.name.equals(name)) {
						tp= i;
						break;
					}
				if (tp == null)
					throw new RuntimeException("Unrecognised field: " + m.group());
				if (indexes.containsKey(tp)) {
					index--;
					patternStringWithBackReferences.append(patternString.substring(charsCopiesFromPatternString, m.start()));
					patternStringWithBackReferences.append('\\');
					patternStringWithBackReferences.append(indexes.get(tp).toString());
					charsCopiesFromPatternString= m.end();
				} else
					indexes.put(tp, index);
			}
			patternStringWithBackReferences.append(patternString.substring(charsCopiesFromPatternString));
			patternString= patternStringWithBackReferences.toString();

			// Replace track property macros with regex
			for (TrackPropertyType i : TrackPropertyType.values())
				patternString= patternString.replace("[:" + i.name + ":]", trackPropertyPatterns.get(i));

			// Create pattern
			pat= Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
		}

		@Override
		public String toString() {
			return pat.toString();
		}
	}

	// =============================================================================================== //
	// = FilenameParser
	// =============================================================================================== //

	private static final Pattern patCrapSuffix= Pattern.compile("^[^/]+?([ \\-_\\[({\\.][^/]*?)/(?:[^/]+?\\1/)+$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private static final Pattern patDirSep= Pattern.compile("[\\\\/]"); //$NON-NLS-1$

	private final Set<SmartPattern> patterns= new HashSet<SmartPattern>();
	private final Set<SmartPattern> dirPatterns= new HashSet<SmartPattern>();

	@SuppressWarnings("nls")
	public FilenameParser() {
		// No need to check for duplicate artist/album values in the file basename because it is removed automagically in readMultipleTrackProperties()
		String tnAndTrack= "[:tn:]<sepSpaceUndscOrDot>[:track:]";
		String yearAndAlbumWithArtist= "(?:[:artist:]<sepSpaceOrUndsc>)?[:year:]<sepSpaceUndscOrDot>(?:[:artist:]<sepSpaceOrUndsc>)?[:album:](?:<sepSpaceOrUndsc>[:artist:])?";
		String albumWithArtist= "(?:[:artist:]<sepSpaceOrUndsc>)?[:album:](?:<sepSpaceOrUndsc>[:artist:])?";

		addPattern("[:artist:](?:<sepSpaceOrUndsc>Discogra.+?)?", yearAndAlbumWithArtist, tnAndTrack);
		addPattern("[:artist:](?:<sepSpaceOrUndsc>Discogra.+?)?", albumWithArtist, tnAndTrack);
		addPattern("[:artist:]<sepSpaceOrUndsc>[:year:]<sepSpaceOrUndsc>[:album:]", tnAndTrack);
		addPattern("[:artist:]<sep>[:album:](?:<sepSpaceOrUndsc>[:year:])?", tnAndTrack);
		
		addPattern("[:artist:]<sepSpaceUndscOrDot>[:year:](?:<sepSpaceUndscOrDot>[:album:])?<sepSpaceOrUndsc>" + tnAndTrack);
		addPattern("[:artist:](?:<sep>[:album:])?<sepSpaceOrUndsc>" + tnAndTrack);
		addPattern("[:album:](?:<sep>[:artist:])?<sepSpaceOrUndsc>" + tnAndTrack);
	}

	public Map<String, List<TrackProperties>> readMultipleTrackProperties(final DirData dd) {
		final Map<String, List<TrackProperties>> r= new HashMap<String, List<TrackProperties>>();

		// Make a map of processed filenames (audio files only)
		Map<String, String> processedFilenameMap= new HashMap<String, String>();
		String processedDir= dd.dir;
		StringBuilder allFilenames= new StringBuilder();
		for (String shortFilename : dd.files.keySet())
			if (dd.files.get(shortFilename).isAudio()) {
				allFilenames.append(Helpers.removeFilenameExtension(shortFilename));
				allFilenames.append('/');
				processedFilenameMap.put(shortFilename, shortFilename);
			}

		// PRE-PROCESS DIR: If all audio files end in the same string, remove it and try to remove it from the dir too
		if (processedFilenameMap.size() > 1) {
			Matcher m= patCrapSuffix.matcher(allFilenames);
			if (m.matches()) {
				Pattern p= Pattern.compile(Pattern.quote(m.group(1)) + "\\.[^.]+$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
				for (String f : processedFilenameMap.keySet())
					processedFilenameMap.put(f, p.matcher(processedFilenameMap.get(f)).replaceFirst(".x")); //$NON-NLS-1$
				processedDir= Pattern.compile(Pattern.quote(m.group(1)) + "$", Pattern.CASE_INSENSITIVE).matcher(processedDir).replaceFirst(""); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		// PRE-PROCESS DIR: Convert underscores to spaces if no spaces in any audio files
		{
			boolean spacesFound= false, underscoresFound= false;
			for (String processedFilename : processedFilenameMap.values()) {
				if (processedFilename.indexOf(' ') != -1) {
					spacesFound= true;
					break;
				}
				if (!underscoresFound)
					if (processedFilename.indexOf('_') != -1)
						underscoresFound= true;
			}
			if (underscoresFound && !spacesFound) {
				// Convert underscores in filenames
				for (String f : processedFilenameMap.keySet())
					processedFilenameMap.put(f, processedFilenameMap.get(f).replace('_', ' '));
				// Convert underscores in directories
				String[] de= patDirSep.split(processedDir);
				int i= de.length;
				while (i-- > 0) {
					if (de[i].indexOf(' ') == -1)
						de[i]= de[i].replace('_', ' ');
					else
						break;
				}
				processedDir= Helpers.join(de, File.separator);
			}
		}

		// PRE-PROCESS DIR: Remove artist/album from filename if found in all files
		if (processedFilenameMap.size() > 1)
			for (SmartPattern p : dirPatterns) {
				boolean failed= false;
				Matcher m= null;
				for (String processedFilename : processedFilenameMap.values()) {
					String filename= Helpers.addPathElements(processedDir, processedFilename);
					if (!(m= p.pat.matcher(filename)).matches()) {
						failed= true;
						break;
					}
				}
				if (!failed) {
					removeValueFromAllFiles(processedFilenameMap, m.group(p.indexes.get(TrackPropertyType.ALBUM)));
					removeValueFromAllFiles(processedFilenameMap, m.group(p.indexes.get(TrackPropertyType.ARTIST)));
				}
			}

		// Read track properties for each file
		for (String shortFilename : processedFilenameMap.keySet())
			r.put(shortFilename, readTrackProperties(Helpers.addPathElements(processedDir, processedFilenameMap.get(shortFilename))));

		return r;
	}

	public List<TrackProperties> readTrackProperties(final String filename) {
		List<TrackProperties> r= new ArrayList<TrackProperties>();
		Integer i;
		Matcher m;
		for (SmartPattern sp : patterns)
			if ((m= sp.pat.matcher(filename)).matches()) {
				TrackProperties tp= new TrackProperties();
				for (TrackPropertyType k : TrackPropertyType.values())
					if ((i= sp.indexes.get(k)) != null)
						if (m.group(i) != null)
							tp.put(k, Helpers.unicodeTrim(m.group(i)));
				r.add(tp);
			}
		return r;
	}

	// ======================================================= //
	// = FilenameParser: Internal
	// ======================================================= //

	@SuppressWarnings("nls")
	private void addPattern(String... patternStrings) {
		patterns.add(new SmartPattern(patternStrings));
		if (patternStrings.length > 1) {
			// Make sure the filename pattern doesn't include an artist or album tag
			final String filenamePattern= patternStrings[patternStrings.length - 1];
			if (!filenamePattern.contains("[:artist:]") && !filenamePattern.contains("[:album:]")) {
				// Replace the filename pattern and add
				patternStrings[patternStrings.length - 1]= "[^\\\\/]+"; //$NON-NLS-1$
				dirPatterns.add(new SmartPattern(patternStrings));
			}
		}
	}

	private static final String strMakeRegexWithLenientSpacing_sep= "[" + Helpers.whitespaceChars + "_\\.\\-]"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final Pattern patMakeRegexWithLenientSpacing_sep= Pattern.compile(strMakeRegexWithLenientSpacing_sep + "+"); //$NON-NLS-1$

	@SuppressWarnings("nls")
	private String makeRegexWithLenientSpacing(String value) {
		String[] valueWords= patMakeRegexWithLenientSpacing_sep.split(value);
		int i= valueWords.length;
		while (i-- > 0)
			valueWords[i]= Pattern.quote(valueWords[i]);
		return Helpers.join(valueWords, strMakeRegexWithLenientSpacing_sep + "*");
	}

	private static final String removeValueFromAllFiles_sep= SmartPattern.macros.get("sepSpaceUndscOrDot"); //$NON-NLS-1$
	private static final Pattern patRemoveValueFromAllFiles_sepAtBeginning= Pattern.compile("^(?:" + removeValueFromAllFiles_sep + ")?"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final Pattern patRemoveValueFromAllFiles_sepAtEnd= Pattern.compile("(?:" + removeValueFromAllFiles_sep + ")?$"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final Pattern patRemoveValueFromAllFiles_crapAtBeginning= Pattern.compile("^ - "); //$NON-NLS-1$
	private static final Pattern patRemoveValueFromAllFiles_crapAtEnd= Pattern.compile(" - (\\.[^.]+)$"); //$NON-NLS-1$

	@SuppressWarnings("nls")
	private void removeValueFromAllFiles(Map<String, String> processedFilenameMap, String value) {
		if (value != null) {
			final Pattern patValue= Pattern.compile("(?:" + removeValueFromAllFiles_sep + ")?(" + makeRegexWithLenientSpacing(value) + ")(?:" + removeValueFromAllFiles_sep + ")?", Pattern.CASE_INSENSITIVE);
			Matcher m;
			int[] mins= new int[100];
			int[] maxs= new int[100];
			boolean[] valid= new boolean[100];
			int occuranceCount= 0;

			// Find value in files and store ranges of occurance substrings
			for (String processedFilename : processedFilenameMap.values()) {
				m= patValue.matcher(processedFilename);
				while (m.find()) {
					final int a= m.start(1), b= m.end(1);
					boolean added= false;
					int oc= occuranceCount;
					while (oc-- > 0) {
						if (b > mins[oc] && a < maxs[oc]) {
							added= true;
							if (a < mins[oc])
								mins[oc]= a;
							if (b > maxs[oc])
								maxs[oc]= b;
							break;
						}
					}
					if (!added) {
						mins[occuranceCount]= a;
						maxs[occuranceCount]= b;
						valid[occuranceCount++]= true;
					}
				}
			}
			// Check each occurance range and delete unless there is an occurance in range in every filename
			int oc= occuranceCount;
			while (oc-- > 0) {
				boolean foundInAll= true;
				for (String processedFilename : processedFilenameMap.values()) {
					boolean foundInFile= false;
					m= patValue.matcher(processedFilename);
					while (m.find())
						if (m.start(1) >= mins[oc] && m.end(1) <= maxs[oc]) {
							foundInFile= true;
							break;
						}
					if (!foundInFile) {
						foundInAll= false;
						break;
					}
				} // for file
				if (!foundInAll)
					valid[oc]= false;
			}
			// If found, remove it from all files
			oc= occuranceCount;
			while (oc-- > 0)
				if (valid[oc]) {
					final int min= mins[oc], max= maxs[oc];
					for (String shortFilename : processedFilenameMap.keySet()) {
						String f= processedFilenameMap.get(shortFilename);
						m= patValue.matcher(f);
						while (m.find()) {
							final int a= m.start(1), b= m.end(1);
							if (a >= min && b <= max) {
								final String pre= patRemoveValueFromAllFiles_sepAtEnd.matcher(f.substring(0, a)).replaceAll("");
								final String post= patRemoveValueFromAllFiles_sepAtBeginning.matcher(f.substring(b)).replaceAll("");
								f= pre + " - " + post;
								m= patValue.matcher(f);
								break;
							}
						}
						f= patRemoveValueFromAllFiles_crapAtBeginning.matcher(f).replaceAll("");
						f= patRemoveValueFromAllFiles_crapAtEnd.matcher(f).replaceAll("$1");
						processedFilenameMap.put(shortFilename, f);
					}
				}
		}
	}
}
