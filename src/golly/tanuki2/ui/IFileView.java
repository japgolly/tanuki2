package golly.tanuki2.ui;

import golly.tanuki2.data.DirData;
import golly.tanuki2.support.AutoResizeColumnsListener;

import java.util.Map;

import org.eclipse.swt.widgets.Control;

/**
 * @author Golly
 * @since 19/02/2007
 */
interface IFileView {

	public abstract AutoResizeColumnsListener getAutoResizeColumnsListener();

	public abstract Control getWidget();

	public abstract void refreshFiles(Map<String, DirData> dirs);
}