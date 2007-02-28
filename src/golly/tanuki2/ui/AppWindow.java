package golly.tanuki2.ui;

import golly.tanuki2.core.Engine;
import golly.tanuki2.data.FileData;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIHelpers;
import golly.tanuki2.support.UIHelpers.TwoColours;

import java.io.File;

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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
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

	private final Display display;
	private final Engine engine;
	private final ExpandBar expandBar;
	private final FileTransfer fileTransfer;
	private final IFileView inputTree, flatList;
	private final IFileView[] fileViews;
	private final SharedUIResources sharedUIResources;
	private final Shell shell;
	private final TabFolder tabFolder;

	public AppWindow(Display display_, Engine engine_) {
		display= display_;
		engine= engine_;
		sharedUIResources= new SharedUIResources(display, this);
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
		makeDropTarget(tabFolder);
		// Create tab: input tree
		TabItem ti= new TabItem(tabFolder, SWT.NONE);
		inputTree= new InputTree(tabFolder, sharedUIResources);
		ti.setControl(inputTree.getWidget());
		ti.setText(I18n.l("main_tab_inputTree")); //$NON-NLS-1$
		// Create tab: flat list
		ti= new TabItem(tabFolder, SWT.NONE);
		flatList= new FlatList(tabFolder, sharedUIResources);
		ti.setControl(flatList.getWidget());
		ti.setText(I18n.l("main_tab_flatList")); //$NON-NLS-1$
		fileViews= new IFileView[] {inputTree, flatList};

		// Create expandBar
		expandBar= new ExpandBar(shell, SWT.NONE);
		expandBar.addExpandListener(new ExpandListener() {
			public void itemCollapsed(ExpandEvent e) {
				resize(false);
			}

			public void itemExpanded(ExpandEvent e) {
				resize(true);
			}

			private void resize(boolean expanded) {
				expandBar.setRedraw(false);
				if (expandBar.getItem(0).getExpanded() != expanded) {
					expandBar.getItem(0).setExpanded(expanded);
					resizeWidgets();
					expandBar.getItem(0).setExpanded(!expanded);
				} else
					resizeWidgets();
				expandBar.setRedraw(true);
			}
		});

		// Create stats area
		Composite composite= new Composite(expandBar, SWT.BORDER);
		composite.setLayout(UIHelpers.makeGridLayout(4, true, 0, 2));
		new Label(composite, SWT.LEFT).setText("qwe");
		new Label(composite, SWT.LEFT).setText("qwe");
		new Label(composite, SWT.LEFT).setText("qwe");
		new Label(composite, SWT.LEFT).setText("qwe");
		new Label(composite, SWT.LEFT).setText("qwe");
		new Label(composite, SWT.LEFT).setText("qwe");
		new Label(composite, SWT.LEFT).setText("qwe");
		new Label(composite, SWT.LEFT).setText("qwe");
		ExpandItem expandItem= new ExpandItem(expandBar, SWT.NONE, 0);
		expandItem.setText(I18n.l("stats_txt_sectionHeader")); //$NON-NLS-1$
		expandItem.setHeight(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		expandItem.setControl(composite);
		expandItem.setExpanded(true);

		// DELME
		engine.addFolder("X:\\music\\1. Fresh\\IN FLAMES Discografia (www.heavytorrents.org)");
		refreshFiles();
		tabFolder.setSelection(0);
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
					boolean added= false;
					for (String f : files)
						if (new File(f).isDirectory()) {
							engine.addFolder(f);
							added= true;
						} else
							; // TODO Handle adding on non-directories
					if (added)
						refreshFiles();
				}
			}
		});
	}

	public void onFilesRemoved() {
		engine.removeEmptyDirs();
		refreshFiles();
	}

	public void refreshFiles() {
		display.asyncExec(new Runnable() {
			public void run() {
				for (IFileView fv : fileViews)
					fv.getWidget().setRedraw(false);
				for (IFileView fv : fileViews)
					fv.refreshFiles(engine.dirs);
				for (IFileView fv : fileViews)
					fv.getWidget().setRedraw(true);
			}
		});
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

	public void show() {
		shell.open();
		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
	}

	public void remove(String item) {
		engine.remove(item);
	}

	public TwoColours getFileItemColours(final FileData fd, boolean checkAlbumDataToo) {
		if (fd.isMarkedForDeletion())
			return sharedUIResources.deletionColours;
		else if (!fd.isAudio())
			return sharedUIResources.nonAudioFileColours;
		else if (!fd.isComplete(checkAlbumDataToo))
			return sharedUIResources.incompleteFileColours;
		return null;
	}
}
