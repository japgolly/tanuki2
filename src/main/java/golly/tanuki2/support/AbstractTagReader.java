package golly.tanuki2.support;

import golly.tanuki2.core.ITrackPropertyReader;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.TrackPropertyMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Golly
 * @since 24/07/2007
 */
public abstract class AbstractTagReader implements ITrackPropertyReader {

	protected RichRandomAccessFile fin= null;
	protected byte[] buf= null;

	public Map<String, List<TrackPropertyMap>> readMultipleTrackProperties(DirData dd) {
		final Map<String, List<TrackPropertyMap>> r= new HashMap<String, List<TrackPropertyMap>>();
		for (Map.Entry<String, FileData> e : dd.files.entrySet()) {
			if (e.getValue().isAudio()) {
				r.put(e.getKey(), readTrackProperties(Helpers.addPathElements(dd.dir, e.getKey())));
			}
		}
		return r;
	}

	public List<TrackPropertyMap> readTrackProperties(String filename) {
		final List<TrackPropertyMap> results= new ArrayList<TrackPropertyMap>();

		// Open file
		try {
			fin= RichRandomAccessFileCache.getInstance().get(filename);
		} catch (IOException e) {
			// Ignore
		}

		// Read tag
		if (fin != null) {
			try {
				buf= fin.buffer;
				readTags(results);
			} catch (Throwable e) {
				// Ignore
			} finally {
				fin= null;
				buf= null;
			}
		}

		return results;
	}

	protected abstract void readTags(final List<TrackPropertyMap> results) throws IOException;

	protected boolean compare(byte[] buffer, int offset, byte... expected) {
		int i= expected.length;
		while (i-- > 0) {
			if (buffer[offset + i] != expected[i]) {
				return false;
			}
		}
		return true;
	}

	protected boolean compare(byte[] buffer, int offset, char... expected) {
		int i= expected.length;
		while (i-- > 0) {
			if (buffer[offset + i] != ((byte) expected[i])) {
				return false;
			}
		}
		return true;
	}

	protected static String newString(byte[] bytes, Charset charset) {
		try {
			return new String(bytes, charset.name());
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	protected static String newString(byte[] bytes, int offset, int length, Charset charset) {
		try {
			return new String(bytes, offset, length, charset.name());
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}
