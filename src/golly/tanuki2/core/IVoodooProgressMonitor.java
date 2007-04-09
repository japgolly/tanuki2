package golly.tanuki2.core;

import java.io.File;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

/**
 * @author Golly
 * @since 07/03/2007
 */
public interface IVoodooProgressMonitor {

	public abstract Shell getShell();

	public abstract boolean isCancelled();
	
	public abstract void starting(int dirCount, int totalFiles);

	public abstract void nextDir(String srcDir, String targetDir, int fileCount);

	public abstract void nextFile();

	public abstract void deleting(File file);

	public abstract void moving(File source, File target);

	public abstract void fileOperationComplete(boolean result);

	public abstract void rmdirs(List<File> removedDirs);

	public abstract void finished(boolean aborted);
}