package golly.tanuki2.ui;

import golly.tanuki2.core.Engine;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.AutoResizeColumnsListener;
import golly.tanuki2.support.Config;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.TanukiException;
import golly.tanuki2.support.UIHelpers;
import golly.tanuki2.support.UIHelpers.TwoColours;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

/**
 * @author Golly
 * @since 16/02/2007
 */
public class AppWindow {
	private final static int MARGIN= 0;
	private final static int SPACING= 4;

	private final AppUIShared appUIShared;
	private final Display display;
	private final Engine engine;
	private final ExpandBar expandBar;
	private final FileTransfer fileTransfer;
	private final IFileView inputTree, flatList;
	private final SharedUIResources sharedUIResources;
	private final Shell shell;
	private final TabFolder tabFolder;
	private final Text iwTargetDir;
	private final Button btnTargetDirBrowse;
	private final Set<IFileView> fileViewsUptodate= new HashSet<IFileView>();
	private IFileView currentFileView= null;

	public AppWindow(Display display_, Engine engine_) {
		appUIShared= new AppUIShared();
		display= display_;
		engine= engine_;
		sharedUIResources= new SharedUIResources(display, appUIShared);
		fileTransfer= FileTransfer.getInstance();

		// Create shell
		shell= new Shell(display, SWT.SHELL_TRIM);
		shell.setImage(TanukiImage.TANUKI.get());
		shell.setText(I18n.l("general_app_title")); //$NON-NLS-1$
		Display.setAppName(shell.getText());
		if (Config.appwndHeight > 64 && Config.appwndWidth > 64 && Config.appwndX >= 0 && Config.appwndY >= 0) {
			shell.setSize(Config.appwndWidth, Config.appwndHeight);
			shell.setLocation(Config.appwndX, Config.appwndY);
		} else {
			Rectangle ca= display.getClientArea();
			shell.setSize((int) (ca.width * .8), ((int) (ca.height * .8)));
			shell.setLocation(ca.x + (int) (ca.width * .1), ca.y + ((int) (ca.height * .1)));
		}
		shell.setMaximized(Config.appwndMaximised);
		shell.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				shell.setRedraw(false);
				resizeWidgets();
				shell.setRedraw(true);
			}
		});
		shell.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				updateConfig();
			}
		});

		// TODO: Add a menu with at least exit, preferences, about

		// Create tab folder
		tabFolder= new TabFolder(shell, SWT.NONE);
		// Create tab: input tree
		TabItem ti= new TabItem(tabFolder, SWT.NONE);
		inputTree= new InputTree(tabFolder, sharedUIResources);
		ti.setControl(inputTree.getWidget());
		ti.setData(inputTree);
		ti.setText(I18n.l("main_tab_inputTree")); //$NON-NLS-1$
		// Create tab: flat list
		ti= new TabItem(tabFolder, SWT.NONE);
		flatList= new FlatList(tabFolder, sharedUIResources);
		ti.setControl(flatList.getWidget());
		ti.setData(flatList);
		ti.setText(I18n.l("main_tab_flatList")); //$NON-NLS-1$
		// Tab folder again
		makeDropTarget(tabFolder);
		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onFileViewChanged((IFileView) e.item.getData());
			}
		});
		tabFolder.setSelection(0);
		onFileViewChanged(inputTree);

		// Create expandBar
		expandBar= new ExpandBar(shell, SWT.NONE);
		expandBar.addExpandListener(new ExpandListener() {
			public void itemCollapsed(ExpandEvent e) {
				resize((ExpandItem) e.item, false);
			}

			public void itemExpanded(ExpandEvent e) {
				resize((ExpandItem) e.item, true);
			}

			private void resize(ExpandItem ei, boolean expanded) {
				expandBar.setRedraw(false);
				if (ei.getExpanded() != expanded) {
					ei.setExpanded(expanded);
					resizeWidgets();
					ei.setExpanded(!expanded);
				} else
					resizeWidgets();
				expandBar.setRedraw(true);
			}
		});

		// Create controls area
		Composite composite= new Composite(expandBar, SWT.NONE);
		composite.setLayout(UIHelpers.makeGridLayout(1, true, 0, 6));
		composite.setBackground(shell.getBackground());

		// Controls row
		Composite c2= new Composite(composite, SWT.NONE);
		c2.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		c2.setLayout(UIHelpers.makeRowLayout(0, 24, true, true, false));
		// btn: add folder
		Button btnAddFolder= new Button(c2, SWT.PUSH);
		btnAddFolder.setImage(TanukiImage.ADD_FOLDER.get());
		UIHelpers.setButtonText(btnAddFolder, "main_btn_addFolder"); //$NON-NLS-1$
		btnAddFolder.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				onAddFolder();
			}
		});
		// btn: add files
		Button btnAddFiles= new Button(c2, SWT.PUSH);
		btnAddFiles.setImage(TanukiImage.ADD_FILE.get());
		UIHelpers.setButtonText(btnAddFiles, "main_btn_addFiles"); //$NON-NLS-1$
		btnAddFiles.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				onAddFiles();
			}
		});

		// Controls row
		c2= new Composite(composite, SWT.NONE);
		c2.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		c2.setLayout(UIHelpers.makeGridLayout(2, false, 0, 22));
		// group: output
		Group g= new Group(c2, SWT.NONE);
		g.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		g.setLayout(UIHelpers.makeGridLayout(2, false, 0, 8));
		g.setText(I18n.l("main_group_targetDir")); //$NON-NLS-1$
		// target dir
		iwTargetDir= new Text(g, SWT.BORDER | SWT.SINGLE);
		iwTargetDir.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		iwTargetDir.setText(Config.targetDir);
		// target dir browse
		btnTargetDirBrowse= new Button(g, SWT.PUSH);
		btnTargetDirBrowse.setLayoutData(UIHelpers.makeGridData(1, false, SWT.RIGHT));
		UIHelpers.setButtonText(btnTargetDirBrowse, "main_btn_targetDirBrowse"); //$NON-NLS-1$
		btnTargetDirBrowse.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				onTargetDirBrowse();
			}
		});
		// voodoo button
		Button b= new Button(c2, SWT.PUSH);
		b.setLayoutData(UIHelpers.makeGridData(1, false, SWT.RIGHT, 1, false, SWT.FILL));
		b.setImage(TanukiImage.VOODOO.get());
		UIHelpers.setButtonText(b, "main_btn_voodoo"); //$NON-NLS-1$
		b.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				onVoodoo();
			}
		});
		// add to expand bar
		composite.pack();
		ExpandItem expandItem= new ExpandItem(expandBar, SWT.NONE, 0);
		expandItem.setText(I18n.l("main_sectionHeader_controls")); //$NON-NLS-1$
		expandItem.setHeight(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		expandItem.setControl(composite);
		expandItem.setExpanded(true);
	}

	// =============================================================================================== //
	// = Public
	// =============================================================================================== //

	public void show() {
		shell.open();
		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private String getCleanedUpTargetDir() {
		return Helpers.unicodeTrim(Helpers.ensureCorrectDirSeperators(iwTargetDir.getText()));
	}

	private void makeDropTarget(Control widget) {
		DropTarget target= new DropTarget(widget, DND.DROP_COPY | DND.DROP_DEFAULT);
		target.setTransfer(new Transfer[] {fileTransfer});
		target.addDropListener(new DropTargetAdapter() {
			public void dragEnter(DropTargetEvent event) {
				dragOperationChanged(event);
			}

			public void dragOperationChanged(DropTargetEvent event) {
				if (event.detail != DND.DROP_DEFAULT)
					return;
				if ((event.operations & DND.DROP_COPY) != 0)
					event.detail= DND.DROP_COPY;
				else
					event.detail= DND.DROP_NONE;
			}

			public void drop(DropTargetEvent event) {
				if (fileTransfer.isSupportedType(event.currentDataType)) {
					String[] files= (String[]) event.data;
					engine.add(files);
					appUIShared.onDataUpdated_RefreshNow();
				}
			}
		});
	}

	protected void onAddFiles() {
		FileDialog dlg= new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		if (Config.lastAddedDir != null)
			dlg.setFilterPath(Config.lastAddedDir);
		String file= dlg.open();
		if (file != null) {
			File f= new File(file);
			String dir= f.isDirectory() ? f.toString() : f.getParent();
			Config.lastAddedDir= dir;
			engine.add(Helpers.map(dlg.getFileNames(), Helpers.addPathElements(dir, ""), "")); //$NON-NLS-1$ //$NON-NLS-2$
			appUIShared.onDataUpdated_RefreshNow();
		}
	}

	protected void onAddFolder() {
		DirectoryDialog dlg= new DirectoryDialog(shell);
		if (Config.lastAddedDir != null)
			dlg.setFilterPath(Config.lastAddedDir);
		dlg.setMessage(I18n.l("main_txt_selectFolderToAddMsg")); //$NON-NLS-1$
		String dir= dlg.open();
		if (dir != null) {
			Config.lastAddedDir= dir;
			engine.add(dir);
			appUIShared.onDataUpdated_RefreshNow();
		}
	}

	protected void onFileViewChanged(IFileView fileView) {
		currentFileView= fileView;
		appUIShared.refreshFiles(false);
		currentFileView.getWidget().setFocus();
	}

	protected void onTargetDirBrowse() {
		DirectoryDialog dirDlg= new DirectoryDialog(shell);
		dirDlg.setFilterPath(getCleanedUpTargetDir());
		dirDlg.setMessage(I18n.l("main_txt_selectTargetDirMsg")); //$NON-NLS-1$
		String dir= dirDlg.open();
		if (dir != null) {
			iwTargetDir.setText(dir);
			iwTargetDir.setFocus();
		}
	}

	protected void onVoodoo() {
		String targetDir= getCleanedUpTargetDir();
		// Is target dir empty
		if (targetDir.length() == 0) {
			UIHelpers.showTanukiError(shell, "main_err_targetDir_empty"); //$NON-NLS-1$
			btnTargetDirBrowse.setFocus();
		}
		// Is target dir invalid
		else if (!new File(targetDir).isAbsolute()) {
			UIHelpers.showTanukiError(shell, "main_err_targetDir_invalid"); //$NON-NLS-1$
			iwTargetDir.setFocus();
			iwTargetDir.selectAll();
		}
		// Voodoo time
		else {
			try {
				engine.doYaVoodoo(targetDir, new VoodooProgressDialog(shell), null);
			} catch (Throwable t) {
				TanukiException e= (t instanceof TanukiException) ? (TanukiException) t : new TanukiException(t);
				e.showErrorDialog(shell);
			}
			appUIShared.onDataUpdated_RefreshNow();
		}
	}

	protected void resizeWidgets() {
		Rectangle ca= shell.getClientArea();
		ca.width-= (MARGIN << 1);
		ca.height-= (MARGIN << 1);
		ca.x+= MARGIN;
		ca.y+= MARGIN;
		// Resize expandBar
		final int expandBarSize= expandBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		expandBar.setBounds(ca.x, ca.y + ca.height - expandBarSize, ca.width, expandBarSize);
		// Resize input view
		tabFolder.setBounds(ca.x, ca.y, ca.width, ca.height - expandBarSize - SPACING);
	}

	protected void updateConfig() {
		Config.appwndMaximised= shell.getMaximized();
		Rectangle b= shell.getBounds();
		Config.appwndX= b.x;
		Config.appwndY= b.y;
		Config.appwndHeight= b.height;
		Config.appwndWidth= b.width;

		Config.targetDir= iwTargetDir.getText();
	}

	// =============================================================================================== //
	// = UI package only
	// =============================================================================================== //

	/**
	 * This contains functions for other UI classes to call. They are not publicly accessible.
	 */
	final class AppUIShared {

		public TwoColours getFileItemColours(final FileData fd, boolean checkAlbumDataToo) {
			if (fd.isMarkedForDeletion())
				return sharedUIResources.deletionColours;
			else if (!fd.isAudio())
				return sharedUIResources.nonAudioFileColours;
			else if (!fd.isComplete(checkAlbumDataToo))
				return sharedUIResources.itemIncompleteColours;
			else
				return sharedUIResources.itemCompleteColours;
		}
		
		public void launch(String fullFilename) {
			// TODO win32 only? test on other platforms
			Program.launch(fullFilename);
		}

		public void onDataUpdated(boolean isCurrentViewUptodate) {
			fileViewsUptodate.clear();
			if (isCurrentViewUptodate)
				fileViewsUptodate.add(currentFileView);
		}

		public void onDataUpdated_RefreshNow() {
			onDataUpdated(false);
			refreshFiles(false);
		}

		public boolean openAlbumEditor(DirData dd, Shell shell) {
			AlbumEditor ae= new AlbumEditor(shell, dd, engine, sharedUIResources);
			ae.show();
			if (ae.didUpdate()) {
				onDataUpdated_RefreshNow();
				return true;
			} else
				return false;
		}

		public void refreshFiles(boolean force) {
			final IFileView currentFileView= AppWindow.this.currentFileView;
			if (force || !fileViewsUptodate.contains(currentFileView)) {
				fileViewsUptodate.add(currentFileView);
				display.asyncExec(new Runnable() {
					public void run() {
						// Disable redraw and auto-resize-column-listener
						final Control c= currentFileView.getWidget();
						final AutoResizeColumnsListener arcl= currentFileView.getAutoResizeColumnsListener();
						arcl.enabled= arcl.disableRedraw= false;
						c.setRedraw(false);

						// Refresh files
						currentFileView.refreshFiles(engine.dirs);

						// Re-enable redraw and auto-resize-column-listener
						arcl.resizeColumns();
						c.setRedraw(true);
						arcl.enabled= arcl.disableRedraw= true;
					}
				});
			}
		}

		public boolean removeFiles(String[] files) {
			final MessageBox mb= new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO | SWT.APPLICATION_MODAL);
			mb.setText(I18n.l("general_app_title")); //$NON-NLS-1$
			mb.setMessage(I18n.l("main_txt_removeSelectedConfirmationMsg")); //$NON-NLS-1$
			if (mb.open() != SWT.YES)
				return false;

			for (String f : files)
				engine.remove(f);
			engine.removeEmptyDirs();
			onDataUpdated_RefreshNow();
			return true;
		}

	}
}
