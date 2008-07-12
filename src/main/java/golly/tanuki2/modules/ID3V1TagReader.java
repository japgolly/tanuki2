package golly.tanuki2.modules;

import golly.tanuki2.data.TrackPropertyMap;
import golly.tanuki2.data.TrackPropertyType;
import golly.tanuki2.support.AbstractTagReader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Golly
 * @since 20/07/2007
 */
public class ID3V1TagReader extends AbstractTagReader {
	private static final Charset ID3V1_CHARSET= Charset.forName("ASCII"); //$NON-NLS-1$
	private static final Pattern ID3V1_RTRIM= Pattern.compile("[\0 ]+$"); //$NON-NLS-1$

	protected void readTags(final List<TrackPropertyMap> results) throws IOException {
		fin.seekTo(128).readFully(128);
		if (compare(buf, 0, 'T', 'A', 'G')) {
			final TrackPropertyMap tpm= new TrackPropertyMap();
			tpm.put(TrackPropertyType.TRACK, readValue(buf, 3, 32));
			tpm.put(TrackPropertyType.ARTIST, readValue(buf, 33, 62));
			tpm.put(TrackPropertyType.ALBUM, readValue(buf, 63, 92));
			tpm.put(TrackPropertyType.YEAR, readValue(buf, 93, 96));
			if (buf[125] == 0)
				tpm.put(TrackPropertyType.TN, String.valueOf((int) buf[126]));
			results.add(tpm);
		}
	}

	private static String readValue(byte[] buf, int from, int to) {
		return ID3V1_RTRIM.matcher(newString(buf, from, to - from + 1, ID3V1_CHARSET)).replaceFirst(""); //$NON-NLS-1$
	}
}
