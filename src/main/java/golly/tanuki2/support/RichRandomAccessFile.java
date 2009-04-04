package golly.tanuki2.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Golly
 * @since 20/07/2007
 */
public class RichRandomAccessFile extends RandomAccessFile {
	public final byte[] buffer= new byte[65536];

	public RichRandomAccessFile(String name, String mode) throws FileNotFoundException {
		super(name, mode);
	}

	public RichRandomAccessFile(File file, String mode) throws FileNotFoundException {
		super(file, mode);
	}

	public RichRandomAccessFile seekTo(long bytes) throws IOException {
		if (bytes >= 0) {
			super.seek(bytes);
		} else {
			super.seek(length() - bytes);
		}
		return this;
	}

	public byte[] readFully(int len) throws IOException {
		assert (len <= buffer.length);
		readFully(buffer, 0, len);
		return buffer;
	}

	public byte[] readFully(long len) throws IOException {
		return readFully((int) len);
	}
}
