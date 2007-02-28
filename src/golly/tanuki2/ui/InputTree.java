package golly.tanuki2.ui;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.Helpers.OptimisibleDirTreeNode;

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
	private Map<String, DirData> dirs= null;

	public InputTree(Composite parent, SharedUIResources sharedUIResources_) {
		this.sharedUIResources= sharedUIResources_;
		tree= new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK);
		tree.setHeaderVisible(true);
		new TreeColumn(tree, SWT.LEFT).setWidth(600);
		new TreeColumn(tree, SWT.LEFT).setWidth(600);

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
						sharedUIResources.appWindow.refreshFiles();
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
						tree.setRedraw(false);
						setExpandedAll(e.character == '+');
						tree.setRedraw(true);
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
				final TreeItem ti= (TreeItem) e.item;
				if (ti.getData() instanceof FileData) {
					final FileData fd= (FileData) ti.getData();
					fd.setMarkedForDeletion(!ti.getChecked());
					setFileItemColor(ti, fd);
				} else {
					// TODO This should update children
					ti.setChecked(true);
					e.doit= false;
				}
			}
		});
	}

	// =============================================================================================== //
	// = Public
	// =============================================================================================== //

	public Tree getWidget() {
		return tree;
	}

	@SuppressWarnings("unchecked")
	public void refreshFiles(HashMap<String, DirData> dirs) {
		// init
		this.dirs= dirs;
		tree.setRedraw(false);

		// Remember which dirs are collapsed
		collapsedDirs.clear();
		for (TreeItem i : tree.getItems())
			recordCollapsedTreeItems(i);

		// Remember current selection
		int i= tree.getSelectionCount();
		Object[] selected= new Object[i];
		TreeItem[] currentlySelectedTreeItems= tree.getSelection();
		while (i-- > 0)
			selected[i]= currentlySelectedTreeItems[i].getData();

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

		tree.setRedraw(true);
	}

	// =============================================================================================== //
	// = Events
	// =============================================================================================== //

	protected void onDelete() {
		for (TreeItem ti : tree.getSelection())
			if (ti.getData() instanceof FileData) {
				// Delete file
				final FileData fd= (FileData) ti.getData();
				sharedUIResources.appWindow.remove(Helpers.addPathElement(fd.getDirData().dir, ti.getText()));
			} else {
				// Delete dir
				final String dir= (String) ti.getData();
				sharedUIResources.appWindow.remove(dir);
			}
		sharedUIResources.appWindow.refreshFiles();
	}

	protected void onEdit() {
		if (tree.getSelectionCount() != 1)
			return;

		final TreeItem ti= tree.getSelection()[0];
		DirData dd= null;
		// If selected item is a file
		if (ti.getData() instanceof FileData && ((FileData) ti.getData()).isAudio())
			dd= ((FileData) ti.getData()).getDirData();
		else
			// Or if directory, get the first audio child-item, and use its DirData
			for (TreeItem i : ti.getItems())
				if (i.getData() instanceof FileData && ((FileData) i.getData()).isAudio()) {
					dd= ((FileData) i.getData()).getDirData();
					break;
				}

		// Show album editor
		if (dd != null) {
			AlbumEditor ae= new AlbumEditor(tree.getShell(), dd);
			ae.show();
			if (ae.didUpdate())
				sharedUIResources.appWindow.refreshFiles();
		}
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	@SuppressWarnings("unchecked")
	private void addChildren(TreeItem parent, Map<String, Map> children, String path) {
		// Add directories
		if (children != null)
			for (String dir : Helpers.sort(children.keySet())) {
				final String fullDir= Helpers.addPathElement(path, dir);
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
				final Set<AlbumData> albumDataSet= new HashSet<AlbumData>();
				for (String f : Helpers.sort(files.keySet())) {
					final FileData fd= files.get(f);
					TreeItem ti= new TreeItem(parent, SWT.NONE);
					ti.setChecked(!fd.isMarkedForDeletion());
					ti.setData(fd);
					ti.setImage(fd.getImage());
					ti.setText(0, f);
					if (fd.isAudio()) {
						ti.setText(1, formatInfo(trackInfoFmt, fd.getTn(), fd.getTrack()));
						if (fd.getAlbumData() != null)
							albumDataSet.add(fd.getAlbumData());
					}
					setFileItemColor(ti, fd);
				}
				// Update parent
				if (dd.hasAudioContent()) {
					boolean isAlbumDataComplete= false;
					if (albumDataSet.size() == 1) {
						AlbumData ad= albumDataSet.iterator().next();
						parent.setText(1, formatInfo(albumInfoFmt, ad.getArtist(), ad.getYear(), ad.getAlbum()));
						isAlbumDataComplete= ad.isComplete();
					} else if (albumDataSet.size() > 1)
						parent.setText(1, I18n.l("inputTree_txt_multiAlbumInfos")); //$NON-NLS-1$
					if (!isAlbumDataComplete) {
						parent.setBackground(sharedUIResources.incompleteBkgColor);
						parent.setForeground(sharedUIResources.incompleteFgColor);
					}
				}
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

	private void recordCollapsedTreeItems(TreeItem ti) {
		if (ti.getItemCount() > 0) {
			if (!ti.getExpanded())
				collapsedDirs.add((String) ti.getData());
			for (TreeItem i : ti.getItems())
				recordCollapsedTreeItems(i);
		}
	}

	private void restorePreviousTreeItemState(TreeItem ti, Object[] previouslySelected, List<TreeItem> newSelectedTreeItems) {
		// Find new TIs that should be selected
		for (Object s : previouslySelected)
			if (s.equals(ti.getData()))
				newSelectedTreeItems.add(ti);
		// Expand/collapse tree
		if (ti.getItemCount() > 0) {
			ti.setExpanded(!collapsedDirs.contains((String) ti.getData()));
			for (TreeItem i : ti.getItems())
				restorePreviousTreeItemState(i, previouslySelected, newSelectedTreeItems);
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

	private void setFileItemColor(TreeItem ti, final FileData fd) {
		if (fd.isMarkedForDeletion()) {
			ti.setBackground(sharedUIResources.deletionBkgColor);
			ti.setForeground(sharedUIResources.deletionFgColor);
		} else if (!fd.isAudio()) {
			ti.setBackground(sharedUIResources.nonAudioBkgColor);
			ti.setForeground(sharedUIResources.nonAudioFgColor);
		} else if (!fd.isComplete(false)) {
			ti.setBackground(sharedUIResources.incompleteBkgColor);
			ti.setForeground(sharedUIResources.incompleteFgColor);
		}
	}
}
