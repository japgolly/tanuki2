package golly.tanuki2.ui;

import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.OSSpecific;
import golly.tanuki2.support.TanukiImage;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * @author Golly
 * @since 12/03/2007
 */
abstract class AbstractFileView implements IFileView {
	protected final SharedUIResources sharedUIResources;
	protected Menu contextMenu= null;
	protected Set<MenuItem> selectionRequiredMenuItems= null;
	protected Set<MenuItem> singleSelectionMenuItems= null;
	protected Set<MenuItem> singleAudioSelectionMenuItems= null;
	protected Set<MenuItem> singleFileSelectionMenuItems= null;
	protected Set<MenuItem> singleSelectionWithDirMenuItems= null;

	public AbstractFileView(SharedUIResources sharedUIResources) {
		this.sharedUIResources= sharedUIResources;
	}

	protected void addCommonFileViewListeners(Control w) {
		w.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				onDoubleClick();
			}
		});
		w.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.NONE) {
					// DEL
					if (e.keyCode == SWT.DEL) {
						onDelete();
						e.doit= false;
					}
					// F5
					else if (e.keyCode == SWT.F5) {
						sharedUIResources.appUIShared.refreshFiles(true);
						e.doit= false;
					}
				} else if (e.stateMask == SWT.CTRL) {
					// CTRL-A
					if (e.character == 1) {
						selectAll();
						e.doit= false;
					}
					// CTRL-L
					else if (e.character == 'L' - 'A' + 1) {
						onLaunchFile();
						e.doit= false;
					}
				}
			}
		});
	}

	protected void createMenu(Control w) {
		selectionRequiredMenuItems= new HashSet<MenuItem>();
		singleSelectionMenuItems= new HashSet<MenuItem>();
		singleAudioSelectionMenuItems= new HashSet<MenuItem>();
		singleFileSelectionMenuItems= new HashSet<MenuItem>();
		singleSelectionWithDirMenuItems= new HashSet<MenuItem>();
		contextMenu= new Menu(w);
		w.setMenu(contextMenu);

		// mi: edit album
		final MenuItem miEditAlbum= new MenuItem(contextMenu, SWT.PUSH);
		miEditAlbum.setImage(TanukiImage.EDITOR.get());
		miEditAlbum.setText(I18n.l("main_contextMenu_editAlbum")); //$NON-NLS-1$
		miEditAlbum.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onEdit();
			}
		});
		// mi: edit artist
		final MenuItem miEditArtist= new MenuItem(contextMenu, SWT.PUSH);
		miEditArtist.setImage(TanukiImage.EDITOR.get());
		miEditArtist.setText(I18n.l("main_contextMenu_editArtist")); //$NON-NLS-1$
		miEditArtist.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onEditArtist();
			}
		});
		// mi: launch file
		MenuItem miLaunchFile= new MenuItem(contextMenu, SWT.PUSH);
		miLaunchFile.setText(I18n.l("main_contextMenu_launchFile") + "\tCtrl+L"); //$NON-NLS-1$ //$NON-NLS-2$
		singleFileSelectionMenuItems.add(miLaunchFile);
		miLaunchFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onLaunchFile();
			}
		});
		// mi: open folder
		final MenuItem miOpenFolder= new MenuItem(contextMenu, SWT.PUSH);
		miOpenFolder.setImage(TanukiImage.EXPLORER.get());
		miOpenFolder.setText(I18n.l("main_contextMenu_openFolder")); //$NON-NLS-1$
		singleSelectionWithDirMenuItems.add(miOpenFolder);
		miOpenFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onOpenFolder();
			}
		});
		// mi: open prompt
		final MenuItem miOpenPrompt= new MenuItem(contextMenu, SWT.PUSH);
		miOpenPrompt.setImage(TanukiImage.TERMINAL.get());
		miOpenPrompt.setText(I18n.l("main_contextMenu_openPrompt")); //$NON-NLS-1$
		singleSelectionWithDirMenuItems.add(miOpenPrompt);
		miOpenPrompt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onOpenPrompt();
			}
		});
		// mi: remove items
		MenuItem miRemoveItems= new MenuItem(contextMenu, SWT.PUSH);
		miRemoveItems.setImage(TanukiImage.REMOVE.get());
		miRemoveItems.setText(I18n.l("main_contextMenu_removeItems") + "\tDel"); //$NON-NLS-1$ //$NON-NLS-2$
		selectionRequiredMenuItems.add(miRemoveItems);
		miRemoveItems.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onDelete();
			}
		});

		// add listener
		contextMenu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				final boolean selection= getSelectionCount() > 0;
				final boolean single= isSingleSelection();
				final FileData fd= single ? getSelectedFileData() : null;
				final boolean singleFile= (fd != null);
				final boolean singleAudio= (fd != null) && fd.isAudio() && !fd.isMarkedForDeletion();
				final boolean singleWithDir= single && getSelectedDir() != null;
				// Generic cases
				for (MenuItem mi : selectionRequiredMenuItems)
					mi.setEnabled(selection);
				for (MenuItem mi : singleSelectionMenuItems)
					mi.setEnabled(single);
				for (MenuItem mi : singleFileSelectionMenuItems)
					mi.setEnabled(singleFile);
				for (MenuItem mi : singleAudioSelectionMenuItems)
					mi.setEnabled(singleAudio);
				for (MenuItem mi : singleSelectionWithDirMenuItems)
					mi.setEnabled(singleWithDir);
				// Special cases
				miEditAlbum.setEnabled(onEdit_getDirData() != null);
				miEditArtist.setEnabled(!getAllSelectedDirDataWithAudio(true).isEmpty());
			}
		});
	}

	// =============================================================================================== //
	// = Selection
	// =============================================================================================== //

	protected abstract Set<DirData> getAllSelectedDirData();

	protected Set<DirData> getAllSelectedDirDataWithAudio(boolean andNotMarkedForDeletion) {
		final Set<DirData> all= getAllSelectedDirData();
		final Set<DirData> hasAudio= new HashSet<DirData>();
		for (DirData dd : all)
			if (dd.hasAudioContent(andNotMarkedForDeletion))
				hasAudio.add(dd);
		return hasAudio;
	}

	protected abstract int getSelectionCount();

	protected abstract String getSelectedDir();

	protected abstract FileData getSelectedFileData();

	protected abstract String getSelectedFullFilename();

	protected abstract boolean isFileSelected();

	protected final boolean isSingleSelection() {
		return getSelectionCount() == 1;
	}

	// =============================================================================================== //
	// = Events + related
	// =============================================================================================== //

	protected abstract void onDelete();

	protected void onDoubleClick() {
		if (isSingleSelection()) {
			final FileData fd= getSelectedFileData();
			if (fd != null && !fd.isAudio())
				onLaunchFile();
			else
				onEdit();
		}
	}

	protected final void onEdit() {
		if (isSingleSelection()) {
			final DirData dd= onEdit_getDirData();
			if (dd != null)
				sharedUIResources.appUIShared.openAlbumEditor(dd, getWidget().getShell());
		}
	}

	protected abstract DirData onEdit_getDirData();

	protected void onEditArtist() {
		if (ArtistEditor.open(getWidget().getShell(), getAllSelectedDirDataWithAudio(true)))
			sharedUIResources.appUIShared.onDataUpdated_RefreshNow();
	}

	protected void onLaunchFile() {
		if (isSingleSelection() && isFileSelected())
			sharedUIResources.appUIShared.launch(getSelectedFullFilename());
	}

	protected void onOpenFolder() {
		if (isSingleSelection()) {
			final String dir= getSelectedDir();
			if (dir != null)
				OSSpecific.openFolder(dir);
		}
	}

	protected void onOpenPrompt() {
		if (isSingleSelection()) {
			final String dir= getSelectedDir();
			if (dir != null)
				OSSpecific.openPrompt(dir);
		}
	}

	// =============================================================================================== //
	// = Other
	// =============================================================================================== //

	protected abstract void selectAll();
}
