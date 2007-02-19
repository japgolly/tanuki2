package golly.tanuki2.data;

import java.util.HashMap;

/**
 * @author Golly
 * @since 16/02/2007
 */
public class DirData {
	public final String dir;
	public final HashMap<String, FileData> files;

	public DirData(final String dir) {
		this.dir= dir;
		this.files= new HashMap<String, FileData>();
	}
}
