package golly.tanuki2.core;

import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.RankedObjectCollection;
import golly.tanuki2.data.TrackPropertyMap;
import golly.tanuki2.data.TrackPropertyType;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.LevenshteinDistance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;

/**
 * @author Golly
 * @since 10/03/2007
 */
public class ClipboardParser {
	private static final Pattern pCrapBeforeTnAndText= Pattern.compile("^\\D*\\s+?(\\d{1,3}[^\\p{javaLetterOrDigit}])"); //$NON-NLS-1$
	private static final Pattern pTnAndText= Pattern.compile("^(\\d{1,3})[^\\p{javaLetterOrDigit}](.+)$"); //$NON-NLS-1$
	private static final Pattern pQuotedText= Pattern.compile("^\"(.+)\"$"); //$NON-NLS-1$
	private static final Pattern pFindNumber= Pattern.compile("(?<!\\p{javaLetterOrDigit})0*?([1-9]\\d*)(?!\\p{javaLetterOrDigit})"); //$NON-NLS-1$

	public String getClipboardText(Clipboard cb) {
		final TextTransfer transfer= TextTransfer.getInstance();
		return (String) cb.getContents(transfer);
	}

	/**
	 * Parses text and returns a track_number-to-track_name map.
	 */
	@SuppressWarnings("nls")
	public Map<Integer, String> parse(String txt) {
		final Map<Integer, String> values= new HashMap<Integer, String>();

		// Extract tn+txt from each line
		for (String line : txt.split("[\r\n]+")) {
			line= pCrapBeforeTnAndText.matcher(line).replaceFirst("$1");
			Matcher m= pTnAndText.matcher(line);
			if (m.matches())
				values.put(Integer.parseInt(m.group(1)), m.group(2));
		}

		// Remove crap from beginnings and ends
		removeCommonCrap(values, true);
		removeCommonCrap(values, false);

		// Unquote
		if (values.size() > 1) {
			boolean allMatch= true;
			for (String v : values.values())
				if (!pQuotedText.matcher(v).matches()) {
					allMatch= false;
					break;
				}
			if (allMatch)
				for (Integer k : values.keySet())
					values.put(k, pQuotedText.matcher(values.get(k)).replaceFirst("$1"));
		}

		// Clean up
		for (Integer k : values.keySet())
			values.put(k, Helpers.unicodeTrim(values.get(k)));

		// Done
		return values;
	}

	/**
	 * Parses text and attempts to match the results to the files in a <code>DirData</code>.
	 * 
	 * @return a map of filenames to track properties.
	 */
	public Map<String, TrackPropertyMap> parseAndMatch(DirData dd, String txt) {
		final Map<String, TrackPropertyMap> completeMatches= new HashMap<String, TrackPropertyMap>();
		final Map<Integer, String> clipboardResults= parse(txt);

		matchClipboardResultsUsingTrackName(dd.files, clipboardResults, completeMatches);
		matchClipboardResultsUsingTN(dd.files, clipboardResults, completeMatches);

		return completeMatches;
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private void assignBestResultMatches(final Map<Integer, String> clipboardResults, final Map<String, TrackPropertyMap> completeMatches, final Map<Integer, RankedObjectCollection<String>> rankedMatchesPerResult) {
		while (!rankedMatchesPerResult.isEmpty()) {
			// Find highest ranking matches
			final Set<Integer> keysWithoutMatches= new HashSet<Integer>();
			final RankedObjectCollection<Integer> highestRanks= new RankedObjectCollection<Integer>();
			for (Integer tn : rankedMatchesPerResult.keySet()) {
				final RankedObjectCollection<String> ranks= rankedMatchesPerResult.get(tn);
				if (ranks.isEmpty())
					keysWithoutMatches.add(tn);
				else
					highestRanks.add(tn, ranks.getWinningRank() - (ranks.getWinnerCount() > 1 ? 0.2 : 0));
			}

			// Remove empty matches
			for (Integer i : keysWithoutMatches)
				rankedMatchesPerResult.remove(i);

			// Assign
			if (!highestRanks.isEmpty()) {
				final Integer tn= highestRanks.getWinner();
				TrackPropertyMap tp= new TrackPropertyMap();
				tp.put(TrackPropertyType.TN, tn.toString());
				tp.put(TrackPropertyType.TRACK, clipboardResults.get(tn));
				final String filename= rankedMatchesPerResult.get(tn).getWinner();
				completeMatches.put(filename, tp);

				// Remove
				rankedMatchesPerResult.remove(tn);
				for (RankedObjectCollection<String> r : rankedMatchesPerResult.values())
					r.remove(filename);
			}
		}
	}

	private void matchClipboardResultsUsingTrackName(Map<String, FileData> ddFiles, final Map<Integer, String> clipboardResults, final Map<String, TrackPropertyMap> completeMatches) {
		final Map<Integer, RankedObjectCollection<String>> rankedMatchesPerResult= new HashMap<Integer, RankedObjectCollection<String>>();
		for (Integer tn : clipboardResults.keySet()) {
			final String nTrack= Helpers.normalizeText(clipboardResults.get(tn));
			final RankedObjectCollection<String> rankedMatches= new RankedObjectCollection<String>();
			for (String filename : ddFiles.keySet())
				if (ddFiles.get(filename).isAudio()) {
					// Compare to filename
					final String nFilename= Helpers.normalizeText(Helpers.removeFilenameExtension(filename));
					double rank= -LevenshteinDistance.calculateDistance2(nFilename, nTrack);
					// Compare to FileData.track
					final FileData fd= ddFiles.get(filename);
					if (fd.getTrack() != null)
						rank= Math.max(rank, -LevenshteinDistance.calculateDistance2(Helpers.normalizeText(fd.getTrack()), nTrack));
					// Store
					rankedMatches.add(filename, rank);
				}
			rankedMatchesPerResult.put(tn, rankedMatches);
		}
		assignBestResultMatches(clipboardResults, completeMatches, rankedMatchesPerResult);
	}

	private void matchClipboardResultsUsingTN(Map<String, FileData> ddFiles, final Map<Integer, String> clipboardResults, final Map<String, TrackPropertyMap> completeMatches) {
		final Map<Integer, RankedObjectCollection<String>> rankedMatchesPerResult= new HashMap<Integer, RankedObjectCollection<String>>();
		for (String filename : ddFiles.keySet())
			if (ddFiles.get(filename).isAudio()) {
				final String filenameNoExt= Helpers.removeFilenameExtension(filename);
				// Find numbers in filename
				final Matcher m= pFindNumber.matcher(filenameNoExt);
				final List<Integer> numbersFound= new ArrayList<Integer>();
				while (m.find())
					numbersFound.add(Integer.parseInt(m.group(1)));
				// Register this filename as potential match for each number found
				final double numberCount= numbersFound.size();
				for (Integer i : numbersFound)
					if (clipboardResults.containsKey(i)) {
						RankedObjectCollection<String> x= rankedMatchesPerResult.get(i);
						if (x == null)
							rankedMatchesPerResult.put(i, x= new RankedObjectCollection<String>());
						x.add(filename, 1.0 / numberCount);
					}
			}
		assignBestResultMatches(clipboardResults, completeMatches, rankedMatchesPerResult);
	}

	@SuppressWarnings("nls")
	private static final <K> void removeCommonCrap(Map<K, String> map, boolean beginning) {
		if (map.size() > 1) {
			final String whitespace= Helpers.whitespaceChars;
			String regex= beginning ? "^[" + whitespace + "]*" : "[" + whitespace + "]*$";
			Matcher m;
			boolean loop= true;
			while (loop) {
				final Pattern pFind= Pattern.compile(beginning ? (regex + "([^" + whitespace + "]+)[" + whitespace + "].+") : (".+[" + whitespace + "]([^" + whitespace + "]+)" + regex));
				String thisPassRegex= null;
				for (String v : map.values())
					if ((m= pFind.matcher(v)).matches()) {
						String found= Pattern.quote(m.group(1));
						found= found.replaceAll("\\d+(?:[:,.]\\d+)*", "\\\\E\\\\d+(?:[:,.]\\\\d+)*\\\\Q");
						found= found.replace("\\Q\\E", "");
						if (thisPassRegex == null)
							thisPassRegex= found;
						else if (!found.equals(thisPassRegex)) {
							loop= false;
							break;
						}
					} else {
						loop= false;
						break;
					}

				if (loop)
					regex= beginning ? (regex + thisPassRegex + "[" + whitespace + "]+") : ("[" + whitespace + "]+" + thisPassRegex + regex);
			}

			// Remove crap
			final Pattern pCrap= Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			for (K k : map.keySet())
				map.put(k, pCrap.matcher(map.get(k)).replaceFirst(""));
		}
	}
}