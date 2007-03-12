package golly.tanuki2.ui;

import golly.tanuki2.data.FileData;

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Control;

/**
 * @author Golly
 * @since 12/03/2007
 */
abstract class AbstractFileView {
	protected final SharedUIResources sharedUIResources;

	public AbstractFileView(SharedUIResources sharedUIResources) {
		this.sharedUIResources= sharedUIResources;
	}

	protected void addCommonFileViewListeners(Control w) {
		w.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				onDoubleClick();
			}
		});
	}

	// =============================================================================================== //
	// = Selection
	// =============================================================================================== //

	protected abstract int getSelectionCount();

	protected abstract FileData getSelectedFileData();

	protected abstract String getSelectedFullFilename();

	protected abstract boolean isFileSelected();

	protected final boolean isSingleSelection() {
		return getSelectionCount() == 1;
	}

	// =============================================================================================== //
	// = Events
	// =============================================================================================== //

	protected void onDoubleClick() {
		final FileData fd= getSelectedFileData();
		if (fd != null && !fd.isAudio())
			onLaunchFile();
		else
			onEdit();
	}

	protected abstract void onEdit();

	protected void onLaunchFile() {
		if (isSingleSelection() && isFileSelected())
			sharedUIResources.appUIShared.launch(getSelectedFullFilename());
	}
}
