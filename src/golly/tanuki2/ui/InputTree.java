package golly.tanuki2.ui;

import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.Helpers;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author Golly
 * @since 17/02/2007
 */
public class InputTree implements IFileView {
	private static final Pattern pathSeperatorPattern= Pattern.compile("[\\/\\\\]"); //$NON-NLS-1$
	private final Tree tree;
	private HashMap<String, DirData> dirs= null;

	public InputTree(Composite parent) {
		tree= new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
	}

	public Tree getWidget() {
		return tree;
	}

	@SuppressWarnings("unchecked")
	public void refreshFiles(HashMap<String, DirData> dirs) {
		this.dirs= dirs;
		tree.setRedraw(false);
		tree.clearAll(true);

		HashMap<String, HashMap> dirTree= new HashMap<String, HashMap>();
		for (String dir : dirs.keySet()) {
			HashMap<String, HashMap> tree= dirTree;
			String[] a= pathSeperatorPattern.split(dir);
			for (String e : a) {
				HashMap<String, HashMap> newtree= tree.get(e);
				if (newtree == null)
					tree.put(e, newtree= new HashMap<String, HashMap>());
				tree= newtree;
			}
		}

		dirTree= removeEmptyNodes(dirTree);

		for (String dir : Helpers.sort(dirTree.keySet())) {
			TreeItem ti= new TreeItem(tree, SWT.NONE);
			ti.setText(dir);
			ti.setImage(TanukiImage.FOLDER.get());
			addChildren(ti, dirTree.get(dir), dir);
		}

		tree.setRedraw(true);
	}

	@SuppressWarnings("unchecked")
	private void addChildren(TreeItem parent, HashMap<String, HashMap> children, String path) {
		if (children != null) {
			// Add directories
			for (String dir : Helpers.sort(children.keySet())) {
				TreeItem ti= new TreeItem(parent, SWT.NONE);
				ti.setText(dir);
				ti.setImage(TanukiImage.FOLDER.get());
				addChildren(ti, children.get(dir), Helpers.addPathElement(path, dir));
			}
		} else {
			// Add files
			final HashMap<String, FileData> files= dirs.get(path).files;
			for (String f : Helpers.sort(files.keySet())) {
				final FileData fd= files.get(f);
				TreeItem ti= new TreeItem(parent, SWT.NONE);
				ti.setText(f);
				ti.setImage(fd.getImage());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private HashMap<String, HashMap> removeEmptyNodes(HashMap<String, HashMap> tree) {
		HashMap<String, HashMap> r= new HashMap<String, HashMap>();
		removeEmptyNodes(tree, "", r); //$NON-NLS-1$
		if (r.size() == 1 && r.keySet().iterator().next().length() == 0)
			r= r.get(""); //$NON-NLS-1$
		return r;
	}

	@SuppressWarnings("unchecked")
	private void removeEmptyNodes(HashMap<String, HashMap> tree, String path, HashMap<String, HashMap> target) {
		switch (tree.size()) {
		case 0: {
			target.put(path, null);
			break;
		}
		case 1: {
			final String name= tree.keySet().iterator().next();
			removeEmptyNodes(tree.get(name), Helpers.addPathElement(path, name), target);
			break;
		}
		default: {
			HashMap<String, HashMap> subTarget= target.get(path);
			if (subTarget == null)
				target.put(path, subTarget= new HashMap<String, HashMap>());
			for (String name : tree.keySet())
				removeEmptyNodes(tree.get(name), name, subTarget);
			break;
		}
		}
	}

}
