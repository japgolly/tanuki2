package golly.tanuki2.ui;

import golly.tanuki2.data.FileData;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.TanukiException;

import java.io.File;
import java.io.IOException;
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
	protected Set<MenuItem> singleSelectionMenuItems= null;
	protected Set<MenuItem> singleAudioSelectionMenuItems= null;

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
		singleSelectionMenuItems= new HashSet<MenuItem>();
		singleAudioSelectionMenuItems= new HashSet<MenuItem>();
		contextMenu= new Menu(w);
		w.setMenu(contextMenu);

		// mi: edit album
		MenuItem miEditAlbum= new MenuItem(contextMenu, SWT.PUSH);
		miEditAlbum.setImage(TanukiImage.EDITOR.get());
		miEditAlbum.setText(I18n.l("main_contextMenu_editAlbum")); //$NON-NLS-1$
		singleAudioSelectionMenuItems.add(miEditAlbum);
		miEditAlbum.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onEdit();
			}
		});
		// mi: launch file
		MenuItem miLaunchFile= new MenuItem(contextMenu, SWT.PUSH);
		miLaunchFile.setText(I18n.l("main_contextMenu_launchFile") + "\tCtrl+L"); //$NON-NLS-1$ //$NON-NLS-2$
		singleSelectionMenuItems.add(miLaunchFile);
		miLaunchFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onLaunchFile();
			}
		});
		// mi: open folder
		MenuItem miOpenFolder= new MenuItem(contextMenu, SWT.PUSH);
		miOpenFolder.setImage(TanukiImage.EXPLORER.get());
		miOpenFolder.setText(I18n.l("main_contextMenu_openFolder")); //$NON-NLS-1$
		singleSelectionMenuItems.add(miOpenFolder);
		miOpenFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onOpenFolder();
			}
		});
		// mi: open prompt
		MenuItem miOpenPrompt= new MenuItem(contextMenu, SWT.PUSH);
		miOpenPrompt.setImage(TanukiImage.TERMINAL.get());
		miOpenPrompt.setText(I18n.l("main_contextMenu_openPrompt")); //$NON-NLS-1$
		singleSelectionMenuItems.add(miOpenPrompt);
		miOpenPrompt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onOpenPrompt();
			}
		});
		// mi: remove items
		MenuItem miRemoveItems= new MenuItem(contextMenu, SWT.PUSH);
		miRemoveItems.setImage(TanukiImage.REMOVE.get());
		miRemoveItems.setText(I18n.l("main_contextMenu_removeItems") + "\tDel"); //$NON-NLS-1$ //$NON-NLS-2$
		miRemoveItems.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onDelete();
			}
		});

		// add listener
		contextMenu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				final boolean single= isSingleSelection();
				final FileData fd= single ? getSelectedFileData() : null;
				final boolean singleAudio= (fd != null) && fd.isAudio() && !fd.isMarkedForDeletion();
				for (MenuItem mi : singleSelectionMenuItems)
					mi.setEnabled(single);
				for (MenuItem mi : singleAudioSelectionMenuItems)
					mi.setEnabled(singleAudio);
			}
		});
	}

	// =============================================================================================== //
	// = Selection
	// =============================================================================================== //

	protected abstract int getSelectionCount();

	protected abstract String getSelectedDir();

	protected abstract FileData getSelectedFileData();

	protected abstract String getSelectedFullFilename();

	protected abstract boolean isFileSelected();

	protected final boolean isSingleSelection() {
		return getSelectionCount() == 1;
	}

	// =============================================================================================== //
	// = Events
	// =============================================================================================== //

	protected abstract void onDelete();

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

	protected void onOpenFolder() {
		if (isSingleSelection())
			// TODO win32
			try {
				Runtime.getRuntime().exec("explorer.exe .", null, new File(getSelectedDir())); //$NON-NLS-1$
			} catch (IOException e) {
				new TanukiException(e).showErrorDialog();
			}
	}

	protected void onOpenPrompt() {
		if (isSingleSelection())
			// TODO win32
			try {
				Runtime.getRuntime().exec("cmd.exe /C start cmd.exe", null, new File(getSelectedDir())); //$NON-NLS-1$
			} catch (IOException e) {
				new TanukiException(e).showErrorDialog();
			}
	}

	protected abstract void selectAll();
}
