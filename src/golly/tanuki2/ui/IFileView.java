package golly.tanuki2.ui;

import golly.tanuki2.data.DirData;

import java.util.HashMap;

import org.eclipse.swt.widgets.Control;

/**
 * @author Golly
 * @since 19/02/2007
 */
public interface IFileView {

	public abstract Control getWidget();

	public abstract void refreshFiles(HashMap<String, DirData> dirs);

}