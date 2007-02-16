package golly.tanuki2.core;

import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;

import java.io.File;
import java.util.HashMap;

/**
 * @author Golly
 * @since 16/02/2007
 */
public class Engine {
	public final HashMap<String, DirData> dirs= new HashMap<String, DirData>();
	public final HashMap<String, FileData> files= new HashMap<String, FileData>();

	public void addFolder(String sourceFolderName) {
		addFolder(new File(sourceFolderName));
	}

	private void addFolder(File sourceFolder) {
		final String sourceFolderName= sourceFolder.getAbsolutePath();
		DirData dd= dirs.get(sourceFolderName);
		if (dd == null)
			dirs.put(sourceFolderName, dd= new DirData());

		for (File f : sourceFolder.listFiles())
			if (f.isDirectory())
				addFolder(f);
			else
				addFile(dd, f);
	}

	private void addFile(DirData dd, File f) {
		final String fullFilename= f.getAbsolutePath();
		FileData fd= files.get(fullFilename);
		if (fd == null)
			files.put(fullFilename, fd= new FileData());
		dd.files.put(f.getName(), fd);
	}
}
