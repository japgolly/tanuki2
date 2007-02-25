package golly.tanuki2.ui;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.Helpers.OptimisibleDirTreeNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
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

	private final SharedUIResources sharedUIResources;
	private final Tree tree;
	private HashMap<String, DirData> dirs= null;

	public InputTree(Composite parent, SharedUIResources sharedUIResources) {
		this.sharedUIResources= sharedUIResources;
		tree= new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		tree.setHeaderVisible(true);
		new TreeColumn(tree, SWT.LEFT).setWidth(600);
		new TreeColumn(tree, SWT.LEFT).setWidth(600);

		tree.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL)
					if (e.character == '+' || e.character == '-') {
						tree.setRedraw(false);
						setExpandedAll(e.character == '+');
						tree.setRedraw(true);
						e.doit= false;
					}
			}
		});
		tree.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				onEdit();
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
		this.dirs= dirs;
		tree.setRedraw(false);
		tree.removeAll();

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
		for (String dir : Helpers.sort(optimisedDirTree.keySet())) {
			TreeItem ti= new TreeItem(tree, SWT.NONE);
			ti.setText(dir);
			ti.setImage(TanukiImage.FOLDER.get());
			addChildren(ti, optimisedDirTree.get(dir), dir);
		}

		tree.setRedraw(true);
	}

	// =============================================================================================== //
	// = Events
	// =============================================================================================== //

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

		if (dd != null) {
			new AlbumEditor(tree.getShell(), dd).show();
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
				TreeItem ti= new TreeItem(parent, SWT.NONE);
				ti.setImage(TanukiImage.FOLDER.get());
				ti.setText(0, dir);
				addChildren(ti, children.get(dir), Helpers.addPathElement(path, dir));
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
					ti.setData(fd);
					ti.setImage(fd.getImage());
					ti.setText(0, f);
					if (fd.isAudio()) {
						ti.setText(1, formatInfo(trackInfoFmt, fd.getTn(), fd.getTrack()));
						albumDataSet.add(fd.getAlbumData());
						if (!fd.isComplete())
							ti.setBackground(sharedUIResources.incompleteBkgColor);
					} else
						ti.setBackground(sharedUIResources.nonAudioBkgColor);
				}
				// Update parent
				if (dd.hasAudioContent()) {
					boolean complete= false;
					if (albumDataSet.size() > 1)
						parent.setText(1, I18n.l("inputTree_txt_multiAlbumInfos")); //$NON-NLS-1$
					else {
						AlbumData ad= albumDataSet.iterator().next();
						if (ad != null) {
							parent.setText(1, formatInfo(albumInfoFmt, ad.getArtist(), ad.getYear(), ad.getAlbum()));
							complete= ad.isComplete();
						}
					}
					if (!complete)
						parent.setBackground(sharedUIResources.incompleteBkgColor);
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
}
