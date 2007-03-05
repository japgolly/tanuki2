package golly.tanuki2.ui;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.AutoResizeColumnsListener;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIHelpers;
import golly.tanuki2.support.Helpers.OptimisibleDirTreeNode;
import golly.tanuki2.support.UIHelpers.TwoColours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author Golly
 * @since 17/02/2007
 */
public class InputTree implements IFileView {
	private static final Pattern pathSeperatorPattern= Pattern.compile("[\\/\\\\]"); //$NON-NLS-1$
	private static final String albumInfoFmt= "%s / %s / %s"; //$NON-NLS-1$
	private static final String trackInfoFmt= "   %2s / %s"; //$NON-NLS-1$

	private final Set<String> collapsedDirs= new HashSet<String>();
	private final SharedUIResources sharedUIResources;
	private final Tree tree;
	private final AutoResizeColumnsListener autoColumnResizer;
	private Map<String, DirData> dirs= null;

	public InputTree(Composite parent, SharedUIResources sharedUIResources_) {
		this.sharedUIResources= sharedUIResources_;
		tree= new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK);
		tree.setHeaderVisible(true);
		new TreeColumn(tree, SWT.LEFT).setWidth(600);
		new TreeColumn(tree, SWT.LEFT).setWidth(600);

		// TODO Add a context menu to InputTree 

		tree.addKeyListener(new KeyAdapter() {
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
					// CTRL A
					if (e.character == 1) {
						tree.selectAll();
						e.doit= false;
					}
					// CTRL +, CTRL -
					else if (e.character == '+' || e.character == '-') {
						autoColumnResizer.enabled= autoColumnResizer.disableRedraw= false;
						tree.setRedraw(false);
						setExpandedAll(e.character == '+');
						tree.setRedraw(true);
						autoColumnResizer.enabled= autoColumnResizer.disableRedraw= true;
						e.doit= false;
					}
				}
			}
		});
		tree.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				onEdit();
			}
		});
		tree.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (e.detail == SWT.CHECK)
					onCheck(e);
			}
		});
		this.autoColumnResizer= UIHelpers.createAutoResizeColumnsListener(tree);
		tree.addListener(SWT.Resize, autoColumnResizer);
	}

	// =============================================================================================== //
	// = Public
	// =============================================================================================== //

	public AutoResizeColumnsListener getAutoResizeColumnsListener() {
		return autoColumnResizer;
	}

	public Tree getWidget() {
		return tree;
	}

	@SuppressWarnings("unchecked")
	public void refreshFiles(Map<String, DirData> dirs) {
		this.dirs= dirs;

		// Remember which dirs are collapsed
		collapsedDirs.clear();
		for (TreeItem i : tree.getItems())
			recordCollapsedTreeItems(i);

		// Remember current selection
		int i= tree.getSelectionCount();
		String[] selected= new String[i];
		TreeItem[] currentlySelectedTreeItems= tree.getSelection();
		while (i-- > 0)
			selected[i]= getFullFilename(currentlySelectedTreeItems[i]);

		// Create a virtual representation of the tree
		final Map<String, OptimisibleDirTreeNode> unoptimisedDirTree= new HashMap<String, OptimisibleDirTreeNode>();
		for (String dir : dirs.keySet()) {
			Map<String, OptimisibleDirTreeNode> t= unoptimisedDirTree;
			OptimisibleDirTreeNode latestNode= null;
			for (String dirElement : pathSeperatorPattern.split(dir)) {
				latestNode= t.get(dirElement);
				if (latestNode == null) {
					latestNode= new OptimisibleDirTreeNode();
					t.put(dirElement, latestNode);
				}
				t= latestNode.children;
			}
			if (latestNode != null && !dirs.get(dir).files.isEmpty())
				latestNode.hasFiles= true;
		}
		final Map<String, Map> optimisedDirTree= Helpers.optimiseDirTree(unoptimisedDirTree);

		// Populate the tree widget
		tree.removeAll();
		for (String dir : Helpers.sort(optimisedDirTree.keySet())) {
			TreeItem ti= new TreeItem(tree, SWT.NONE);
			ti.setChecked(true);
			ti.setData(dir);
			ti.setText(dir);
			ti.setImage(TanukiImage.FOLDER.get());
			addChildren(ti, optimisedDirTree.get(dir), dir);
		}

		// Expand items and re-select previously selected
		List<TreeItem> newSelectedTreeItems= new ArrayList<TreeItem>();
		for (TreeItem ti : tree.getItems())
			restorePreviousTreeItemState(ti, selected, newSelectedTreeItems);
		if (tree.getItemCount() > 0)
			tree.showItem(tree.getItem(0));
		if (!newSelectedTreeItems.isEmpty()) {
			tree.setSelection(newSelectedTreeItems.toArray(new TreeItem[newSelectedTreeItems.size()]));
			tree.showSelection();
		}
	}

	// =============================================================================================== //
	// = Events
	// =============================================================================================== //

	protected void onCheck(SelectionEvent e) {
		final TreeItem ti= (TreeItem) e.item;
		if (ti.getData() instanceof FileData) {
			final FileData fd= (FileData) ti.getData();
			fd.setMarkedForDeletion(!ti.getChecked());
			setFileItemInfoText(ti, fd);
			setFileItemColor(ti, fd);
			updateAlbumDirItem(ti.getParentItem(), fd.getDirData());
			sharedUIResources.appUIShared.onDataUpdated(true);
		} else {
			// TODO onCheck() on a dir should update children
			ti.setChecked(true);
			e.doit= false;
		}
	}

	protected void onDelete() {
		String[] files= new String[tree.getSelectionCount()];
		int i= 0;
		for (TreeItem ti : tree.getSelection())
			files[i++]= getFullFilename(ti);
		sharedUIResources.appUIShared.removeFiles(files);
	}

	protected void onEdit() {
		if (tree.getSelectionCount() != 1)
			return;

		final TreeItem ti= tree.getSelection()[0];
		DirData dd= null;
		// If selected item is a file
		FileData fd= ti.getData() instanceof FileData ? (FileData) ti.getData() : null;
		if (fd != null && fd.isAudio() && !fd.isMarkedForDeletion())
			dd= fd.getDirData();
		else
			// Or if directory, get the first audio child-item, and use its DirData
			for (TreeItem i : ti.getItems()) {
				fd= i.getData() instanceof FileData ? (FileData) i.getData() : null;
				if (fd != null && fd.isAudio() && !fd.isMarkedForDeletion()) {
					dd= fd.getDirData();
					break;
				}
			}

		// Show album editor
		if (dd != null)
			sharedUIResources.appUIShared.openAlbumEditor(dd, tree.getShell());
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	@SuppressWarnings("unchecked")
	private void addChildren(TreeItem parent, Map<String, Map> children, String path) {
		// Add directories
		if (children != null)
			for (String dir : Helpers.sort(children.keySet())) {
				final String fullDir= Helpers.addPathElements(path, dir);
				TreeItem ti= new TreeItem(parent, SWT.NONE);
				ti.setChecked(true);
				ti.setData(fullDir);
				ti.setImage(TanukiImage.FOLDER.get());
				ti.setText(0, dir);
				addChildren(ti, children.get(dir), fullDir);
			}

		// Add files
		final DirData dd= dirs.get(path);
		if (dd != null) {
			final Map<String, FileData> files= dd.files;
			if (!files.isEmpty()) {
				for (String f : Helpers.sort(files.keySet())) {
					final FileData fd= files.get(f);
					TreeItem ti= new TreeItem(parent, SWT.NONE);
					ti.setChecked(!fd.isMarkedForDeletion());
					ti.setData(fd);
					ti.setImage(fd.getImage());
					ti.setText(0, f);
					setFileItemInfoText(ti, fd);
					setFileItemColor(ti, fd);
				}
				// Update parent
				if (dd.hasAudioContent())
					updateAlbumDirItem(parent, dd);
			}
		}
	}

	/**
	 * Creates a string that contains info about album or track data. The resulting string will appear beside the
	 * file/dir in the tree.
	 * 
	 * @param fmt The i18n key of the format string.
	 * @param args The info fields to use as args.
	 */
	private static String formatInfo(String fmt, Object... args) {
		boolean foundNonNull= false;
		int i= args.length;
		while (i-- > 0)
			if (args[i] == null)
				args[i]= I18n.l("inputTree_txt_nullInfoValue"); //$NON-NLS-1$
			else
				foundNonNull= true;
		return foundNonNull ? String.format(fmt, args) : ""; //$NON-NLS-1$
	}

	private String getFullFilename(TreeItem ti) {
		if (ti.getData() instanceof FileData)
			return Helpers.addPathElements(((FileData) ti.getData()).getDirData().dir, ti.getText());
		else
			return (String) ti.getData();
	}

	private void recordCollapsedTreeItems(TreeItem ti) {
		if (ti.getItemCount() > 0) {
			if (!ti.getExpanded())
				collapsedDirs.add((String) ti.getData());
			for (TreeItem i : ti.getItems())
				recordCollapsedTreeItems(i);
		}
	}

	private void restorePreviousTreeItemState(TreeItem ti, String[] filenamesToSelect, List<TreeItem> newSelectedTreeItems) {
		// Find new TIs that should be selected
		for (String s : filenamesToSelect)
			if (s.equals(getFullFilename(ti)))
				newSelectedTreeItems.add(ti);
		// Expand/collapse tree
		if (ti.getItemCount() > 0) {
			ti.setExpanded(!collapsedDirs.contains((String) ti.getData()));
			for (TreeItem i : ti.getItems())
				restorePreviousTreeItemState(i, filenamesToSelect, newSelectedTreeItems);
		}
	}

	private void setExpanded(TreeItem ti, boolean expanded) {
		ti.setExpanded(expanded);
		for (TreeItem i : ti.getItems())
			setExpanded(i, expanded);
	}

	private void setExpandedAll(boolean expanded) {
		for (TreeItem i : tree.getItems())
			setExpanded(i, expanded);
		tree.showSelection();
	}

	private void setFileItemColor(final TreeItem ti, final FileData fd) {
		final TwoColours c= sharedUIResources.appUIShared.getFileItemColours(fd, false);
		if (c != null) {
			ti.setBackground(c.background);
			ti.setForeground(c.foreground);
		}
	}

	private void setFileItemInfoText(TreeItem ti, final FileData fd) {
		if (fd.isMarkedForDeletion())
			ti.setText(1, I18n.l("inputTree_txt_markedForDeletion")); //$NON-NLS-1$
		else if (fd.isAudio())
			ti.setText(1, formatInfo(trackInfoFmt, fd.getTn(), fd.getTrack()));
	}

	private void updateAlbumDirItem(TreeItem parent, DirData dd) {
		// Collect data about files
		final Set<AlbumData> albumDataSet= new HashSet<AlbumData>();
		boolean allMarkedForDeletion= true;
		if (dd.files.isEmpty())
			allMarkedForDeletion= false;
		else
			for (FileData fd : dd.files.values()) {
				if (!fd.isMarkedForDeletion()) {
					allMarkedForDeletion= false;
					if (fd.isAudio())
						if (fd.getAlbumData() != null)
							albumDataSet.add(fd.getAlbumData());
				}
			}

		// Set text + check if album data complete
		boolean isAlbumDataIncomplete= false;
		if (allMarkedForDeletion)
			parent.setText(1, I18n.l("inputTree_txt_markedForDeletion")); //$NON-NLS-1$
		else if (!dd.hasAudioContent() || albumDataSet.isEmpty())
			parent.setText(1, ""); //$NON-NLS-1$
		else if (albumDataSet.size() == 1) {
			AlbumData ad= albumDataSet.iterator().next();
			parent.setText(1, formatInfo(albumInfoFmt, ad.getArtist(), ad.getYear(), ad.getAlbum()));
			isAlbumDataIncomplete= !ad.isComplete();
		} else {
			parent.setText(1, I18n.l("inputTree_txt_multiAlbumInfos")); //$NON-NLS-1$
			isAlbumDataIncomplete= true;
		}

		// Update item
		final TwoColours c;
		if (allMarkedForDeletion)
			c= sharedUIResources.deletionColours;
		else if (isAlbumDataIncomplete)
			c= sharedUIResources.itemIncompleteColours;
		else
			c= sharedUIResources.itemCompleteColours;
		parent.setChecked(!allMarkedForDeletion);
		parent.setBackground(c.background);
		parent.setForeground(c.foreground);

	}
}
