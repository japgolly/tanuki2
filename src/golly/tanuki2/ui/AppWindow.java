package golly.tanuki2.ui;

import golly.tanuki2.core.Engine;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.support.I18n;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

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
	private final IFileView[] fileViews;
	private final SharedUIResources sharedUIResources;
	private final Shell shell;
	private final TabFolder tabFolder;
	private final Set<IFileView> fileViewsUptodate= new HashSet<IFileView>();
	private IFileView currentFileView= null;

	public AppWindow(Display display_, Engine engine_) {
		appUIShared= new AppUIShared();
		display= display_;
		engine= engine_;
		sharedUIResources= new SharedUIResources(display, appUIShared);
		fileTransfer= FileTransfer.getInstance();

		// Create shell
		shell= new Shell();
		shell.setSize(1600, 800);
		shell.setText(I18n.l("general_app_title")); //$NON-NLS-1$
		Display.setAppName(shell.getText());
		shell.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				shell.setRedraw(false);
				resizeWidgets();
				shell.setRedraw(true);
			}
		});

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
		// All tabs
		fileViews= new IFileView[] {inputTree, flatList};
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
		composite.setLayout(UIHelpers.makeGridLayout(4, true, 4, 32));
		composite.setBackground(shell.getBackground());

		Button b;
		b= new Button(composite, SWT.PUSH);
		b.setText("ah");
		b.setLayoutData(UIHelpers.makeGridData(1, false, SWT.CENTER));
		b= new Button(composite, SWT.PUSH);
		b.setText("ah");
		b.setLayoutData(UIHelpers.makeGridData(1, false, SWT.CENTER));

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
					boolean added= false;
					for (String f : files)
						if (new File(f).isDirectory()) {
							engine.addFolder(f);
							added= true;
						} else
							; // TODO Handle adding on non-directories
					if (added)
						appUIShared.onDataUpdated_RefreshNow();
				}
			}
		});
	}

	protected void onFileViewChanged(IFileView fileView) {
		currentFileView= fileView;
		appUIShared.refreshFiles(false);
		currentFileView.getWidget().setFocus();
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

		public void onDataUpdated(boolean isCurrentViewUptodate) {
			fileViewsUptodate.clear();
			if (isCurrentViewUptodate)
				fileViewsUptodate.add(currentFileView);
		}

		public void onDataUpdated_RefreshNow() {
			onDataUpdated(false);
			refreshFiles(false);
		}

		public void onFilesRemoved() {
			engine.removeEmptyDirs();
			onDataUpdated_RefreshNow();
		}

		public boolean openAlbumEditor(DirData dd, Shell shell) {
			AlbumEditor ae= new AlbumEditor(shell, dd);
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
						currentFileView.getWidget().setRedraw(false);
						currentFileView.refreshFiles(engine.dirs);
						currentFileView.getWidget().setRedraw(true);
					}
				});
			}
		}

		public void remove(String item) {
			engine.remove(item);
		}
	}
}
