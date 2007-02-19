package golly.tanuki2.ui;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
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

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	@SuppressWarnings("unchecked")
	private void addChildren(TreeItem parent, HashMap<String, HashMap> children, String path) {
		if (children != null) {
			// Add directories
			for (String dir : Helpers.sort(children.keySet())) {
				TreeItem ti= new TreeItem(parent, SWT.NONE);
				ti.setImage(TanukiImage.FOLDER.get());
				ti.setText(0, dir);
				addChildren(ti, children.get(dir), Helpers.addPathElement(path, dir));
			}
		} else {
			// Add files
			final HashMap<String, FileData> files= dirs.get(path).files;
			final Set<AlbumData> albumDataSet= new HashSet<AlbumData>();
			boolean foundAudio= false;
			for (String f : Helpers.sort(files.keySet())) {
				final FileData fd= files.get(f);
				TreeItem ti= new TreeItem(parent, SWT.NONE);
				ti.setImage(fd.getImage());
				ti.setText(0, f);
				if (fd.isAudio()) {
					ti.setText(1, formatInfo(trackInfoFmt, fd.getTn(), fd.getTrack()));
					foundAudio= true;
					albumDataSet.add(fd.getAlbumData());
					if (!fd.isComplete())
						ti.setBackground(sharedUIResources.incompleteBkgColor);
				} else
					ti.setBackground(sharedUIResources.nonAudioBkgColor);
			}
			// Update parent
			if (foundAudio) {
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
