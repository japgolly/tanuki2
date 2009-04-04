package golly.tanuki2.support;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Golly
 * @since 24/07/2007
 */
public class RichRandomAccessFileCache {

	// ====== STATIC ======

	private static final RichRandomAccessFileCache singleton= new RichRandomAccessFileCache();

	public static RichRandomAccessFileCache getInstance() {
		return singleton;
	}

	// ====== INSTANCE ======

	private final Map<String, RichRandomAccessFile> cache;

	private RichRandomAccessFileCache() {
		cache= new HashMap<String, RichRandomAccessFile>();
	}

	public RichRandomAccessFile get(String filename) throws IOException {
		return get(new File(filename));
	}

	public RichRandomAccessFile get(File file) throws IOException {
		final String filename= file.getCanonicalPath();
		RichRandomAccessFile r= cache.get(filename);
		if (r == null) {
			r= new RichRandomAccessFile(file, "r"); //$NON-NLS-1$
			cache.put(filename, r);
		}
		return r;
	}

	public void clear() {
		for (final RichRandomAccessFile f : cache.values()) {
			try {
				f.close();
			} catch (IOException e) {
				// Ignore
			}
		}
		cache.clear();
	}
}
