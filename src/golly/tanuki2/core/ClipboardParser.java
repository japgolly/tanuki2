package golly.tanuki2.core;

import golly.tanuki2.support.Helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Golly
 * @since 10/03/2007
 */
@SuppressWarnings("nls")
public class ClipboardParser {

	private static final Pattern pCrapBeforeTnAndText= Pattern.compile("^\\D*\\s+?(\\d{1,3}[^\\p{javaLetterOrDigit}])");
	private static final Pattern pTnAndText= Pattern.compile("^(\\d{1,3})[^\\p{javaLetterOrDigit}](.+)$");
	private static final Pattern pQuotedText= Pattern.compile("^\"(.+)\"$");

	public Map<Integer, String> readTracks(String txt) {
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