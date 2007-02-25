package golly.tanuki2.core;

import golly.tanuki2.data.DirData;
import golly.tanuki2.data.TrackProperties;
import golly.tanuki2.data.TrackPropertyType;
import golly.tanuki2.support.Helpers;

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
			trackPropertyPatterns.put(TrackPropertyType.YEAR, possiblyEnclosed("(\\d{4})"));
			trackPropertyPatterns.put(TrackPropertyType.ALBUM, "([^\\\\/]+?)");
			trackPropertyPatterns.put(TrackPropertyType.TN, possiblyEnclosed("(\\d{1,3})"));
			trackPropertyPatterns.put(TrackPropertyType.TRACK, "([^\\\\/]+?)");
			macros.put("sep", " *- *");
			macros.put("sepOrSpace", Helpers.regexOr(macros.get("sep"), " +"));
			macros.put("sepSpaceOrUndsc", Helpers.regexOr(macros.get("sep"), "[ _]+"));
			macros.put("sepSpaceUndscOrDot", Helpers.regexOr(macros.get("sep"), "[ _]+", "\\. *"));
			macros.put("website", possiblyEnclosed("(?:[a-z0-9_-]+\\.)+(?:com|org|net)(?:\\.[a-z]{2,3})?"));
			String bitrate= "(?:96|112|128|160|192|224|256|320|vbr(?: ?(?:hq|[0-9]+))?)" + "(?: ?(?:k|kbps))?";
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
	private static final Pattern patRemoveExt= Pattern.compile("\\.[^.]+$"); //$NON-NLS-1$

	private final Set<SmartPattern> patterns= new HashSet<SmartPattern>();

	@SuppressWarnings("nls")
	public FilenameParser() {
		String yearAndAlbumWithArtist= "(?:[:artist:]<sepSpaceOrUndsc>)?[:year:]<sepSpaceUndscOrDot>(?:[:artist:]<sepSpaceOrUndsc>)?[:album:](?:<sepSpaceOrUndsc>[:artist:])?";
		String albumWithArtist= "(?:[:artist:]<sepSpaceOrUndsc>)?[:album:](?:<sepSpaceOrUndsc>[:artist:])?";
		String tnAndTrackWithAA= "(?:<album_andor_artist><sepSpaceOrUndsc>)?[:tn:]<sepSpaceUndscOrDot>(?:<album_andor_artist><sepSpaceOrUndsc>)?[:track:](?:<sepSpaceOrUndsc><album_andor_artist>)?";
		
		patterns.add(new SmartPattern("[:artist:](?:<sepSpaceOrUndsc>Discogra.+?)?", yearAndAlbumWithArtist, tnAndTrackWithAA));
		patterns.add(new SmartPattern("[:artist:](?:<sepSpaceOrUndsc>Discogra.+?)?", albumWithArtist, tnAndTrackWithAA));
		patterns.add(new SmartPattern("[:artist:]<sepSpaceOrUndsc>[:year:]<sepSpaceOrUndsc>[:album:]", tnAndTrackWithAA));
		patterns.add(new SmartPattern("[:artist:]<sep>[:album:](?:<sepSpaceOrUndsc>[:year:])?", tnAndTrackWithAA));
	}

	public Map<String, List<TrackProperties>> readMultipleTrackProperties(final DirData dd) {
		final Map<String, List<TrackProperties>> r= new HashMap<String, List<TrackProperties>>();

		// Make a map of processed filenames
		Map<String, String> processedFilenameMap= new HashMap<String, String>();
		String processedDir= dd.dir;
		StringBuilder allFilenames= new StringBuilder();
		for (String shortFilename : dd.files.keySet())
			if (dd.files.get(shortFilename).isAudio()) {
				allFilenames.append(patRemoveExt.matcher(shortFilename).replaceFirst("")); //$NON-NLS-1$
				allFilenames.append('/');
				processedFilenameMap.put(shortFilename, shortFilename);
			}

		// If all audio files end in the same string, remove it and try to remove it from the dir too
		if (processedFilenameMap.size() > 1) {
			Matcher m= patCrapSuffix.matcher(allFilenames);
			if (m.matches()) {
				Pattern p= Pattern.compile(Pattern.quote(m.group(1)) + "\\.[^.]+$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
				for (String f : processedFilenameMap.keySet())
					processedFilenameMap.put(f, p.matcher(processedFilenameMap.get(f)).replaceFirst(".x")); //$NON-NLS-1$
				processedDir= Pattern.compile(Pattern.quote(m.group(1)) + "$", Pattern.CASE_INSENSITIVE).matcher(processedDir).replaceFirst(""); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		// Read track properties for each file
		for (String shortFilename : processedFilenameMap.keySet())
			r.put(shortFilename, readTrackProperties(Helpers.addPathElement(processedDir, processedFilenameMap.get(shortFilename))));

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
}
