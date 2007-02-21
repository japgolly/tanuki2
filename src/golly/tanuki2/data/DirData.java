package golly.tanuki2.data;

import golly.tanuki2.support.Helpers;

import java.util.HashMap;

/**
 * @author Golly
 * @since 16/02/2007
 */
public class DirData {
	public final String dir;
	public final HashMap<String, FileData> files;

	private String toString= null;

	public DirData(final String dir) {
		this.dir= dir;
		this.files= new HashMap<String, FileData>();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof DirData))
			return false;
		DirData dd2= (DirData) obj;
		return dir.equals(dd2.dir);
	}

	@Override
	public String toString() {
		if (toString == null)
			try {
				toString= Helpers.inspect(this, true, "dir"); //$NON-NLS-1$
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		return toString;
	}
}
