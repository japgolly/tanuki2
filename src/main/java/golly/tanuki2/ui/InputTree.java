package golly.tanuki2.ui;

import golly.tanuki2.core.Engine;
import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.Helpers.OptimisibleDirTreeNode;
import golly.tanuki2.support.UIHelpers.TwoColours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author Golly
 * @since 17/02/2007
 */
public class InputTree extends AbstractTreeBasedFileView {
	private static final String albumInfoFmt= "%s / %s / %s"; //$NON-NLS-1$
	private static final String trackInfoFmt= "   %2s / %s"; //$NON-NLS-1$
	private static final boolean dirsWithNoFilesHaveCheck= false;

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
		while (i-- > 0) {
			if (args[i] == null) {
				args[i]= I18n.l("inputTree_txt_nullInfoValue"); //$NON-NLS-1$
			} else {
				foundNonNull= true;
			}
		}
		return foundNonNull ? String.format(fmt, args) : ""; //$NON-NLS-1$
	}

	private final Set<String> collapsedDirs= new HashSet<String>();
	private Map<String, DirData> dirs= null;

	private final Engine engine;

	public InputTree(Composite parent, SharedUIResources sharedUIResources, Engine engine) {
		super(parent, sharedUIResources, SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK);
		this.engine= engine;

		new TreeColumn(tree, SWT.LEFT);
		new TreeColumn(tree, SWT.LEFT);

		tree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.detail == SWT.CHECK) {
					onCheck(e);
				}
			}
		});
	}

	// =============================================================================================== //
	// = Required due to interface / superclass
	// =============================================================================================== //

	@Override
	protected void addFilesToTree(TreeItem parent, String path) {
		final DirData dd= dirs.get(path);
		if (dd == null || dd.files.isEmpty()) {
			parent.setChecked(dirsWithNoFilesHaveCheck);
		}
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
				if (dd.hasAudioContent(false)) {
					updateAlbumDirItem(parent, dd);
				}
			}
		}
	}

	@Override
	protected void getAllSelectedDirData(final Set<DirData> r, TreeItem ti) {
		if (ti.getData() instanceof FileData) {
			r.add(((FileData) ti.getData()).getDirData());
		} else {
			r.add(engine.dirs.get(ti.getData()));
			for (TreeItem c : ti.getItems()) {
				getAllSelectedDirData(r, c);
			}
		}
	}

	@Override
	protected Object getDataForDirTreeItem(final String fullDir) {
		return fullDir;
	}

	@Override
	protected String getSelectedDir() {
		if (isFileSelected()) {
			return getSelectedFileData().getDirData().dir;
		} else {
			return (String) getSelected().getData();
		}
	}

	@Override
	protected FileData getSelectedFileData() {
		final TreeItem ti= getSelected();
		return ti.getData() instanceof FileData ? (FileData) ti.getData() : null;
	}

	@Override
	protected String getSelectedFullFilename() {
		return getFullFilename(getSelected());
	}

	@Override
	protected void onDelete() {
		String[] files= new String[tree.getSelectionCount()];
		int i= 0;
		for (TreeItem ti : tree.getSelection()) {
			files[i++]= getFullFilename(ti);
		}
		sharedUIResources.appUIShared.removeFiles(files);
	}

	@Override
	protected DirData onEdit_getDirData() {
		// If selected item is a file
		FileData fd= getSelectedFileData();
		if (fd != null && fd.isAudio() && !fd.isMarkedForDeletion()) {
			return fd.getDirData();
		} else {
			// Or if directory, get the first audio child-item, and use its DirData
			for (TreeItem i : getSelected().getItems()) {
				fd= i.getData() instanceof FileData ? (FileData) i.getData() : null;
				if (fd != null && fd.isAudio() && !fd.isMarkedForDeletion()) {
					return fd.getDirData();
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void refreshFiles(Map<String, DirData> dirs) {
		this.dirs= dirs;

		// Remember which dirs are collapsed
		collapsedDirs.clear();
		for (TreeItem i : tree.getItems()) {
			recordCollapsedTreeItems(i);
		}

		// Remember current selection
		int i= tree.getSelectionCount();
		String[] selected= new String[i];
		TreeItem[] currentlySelectedTreeItems= tree.getSelection();
		while (i-- > 0) {
			selected[i]= getFullFilename(currentlySelectedTreeItems[i]);
		}

		// Create a virtual representation of the tree
		final Map<String, OptimisibleDirTreeNode> unoptimisedDirTree= new HashMap<String, OptimisibleDirTreeNode>();
		for (String dir : dirs.keySet()) {
			Helpers.addDirToUnoptimisedDirTree(unoptimisedDirTree, dir, !dirs.get(dir).files.isEmpty());
		}
		final Map<String, Map> optimisedDirTree= Helpers.optimiseDirTree(unoptimisedDirTree);

		// Populate the tree
		tree.removeAll();
		populateTree(optimisedDirTree);

		// Expand items and re-select previously selected
		List<TreeItem> newSelectedTreeItems= new ArrayList<TreeItem>();
		for (TreeItem ti : tree.getItems()) {
			restorePreviousTreeItemState(ti, selected, newSelectedTreeItems);
		}
		if (tree.getItemCount() > 0) {
			tree.showItem(tree.getItem(0));
		}
		if (!newSelectedTreeItems.isEmpty()) {
			tree.setSelection(newSelectedTreeItems.toArray(new TreeItem[newSelectedTreeItems.size()]));
			tree.showSelection();
		}
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private String getFullFilename(TreeItem ti) {
		if (ti.getData() instanceof FileData) {
			return Helpers.addPathElements(((FileData) ti.getData()).getDirData().dir, ti.getText());
		} else {
			return (String) ti.getData();
		}
	}

	private void onCheck(SelectionEvent e) {
		final TreeItem ti= (TreeItem) e.item;
		// Update file
		if (ti.getData() instanceof FileData) {
			final FileData fd= (FileData) ti.getData();
			fd.setMarkedForDeletion(!ti.getChecked());
			setFileItemInfoText(ti, fd);
			setFileItemColor(ti, fd);
			updateAlbumDirItem(ti.getParentItem(), fd.getDirData());
			sharedUIResources.appUIShared.onDataUpdated(true);
		}
		// Update dir and children
		else {
			DirData dd= engine.dirs.get(getFullFilename(ti));
			if (dd == null || dd.files.isEmpty()) {
				ti.setChecked(dirsWithNoFilesHaveCheck);
				e.doit= false;
			} else {
				final boolean delete= !ti.getChecked();
				for (TreeItem i : ti.getItems()) {
					if (i.getData() instanceof FileData) {
						FileData fd= (FileData) i.getData();
						i.setChecked(!delete);
						fd.setMarkedForDeletion(delete);
						setFileItemInfoText(i, fd);
						setFileItemColor(i, fd);
					}
				}
				updateAlbumDirItem(ti, dd);
			}
		}
	}

	private void recordCollapsedTreeItems(TreeItem ti) {
		if (ti.getItemCount() > 0) {
			if (!ti.getExpanded()) {
				collapsedDirs.add((String) ti.getData());
			}
			for (TreeItem i : ti.getItems()) {
				recordCollapsedTreeItems(i);
			}
		}
	}

	private void restorePreviousTreeItemState(TreeItem ti, String[] filenamesToSelect, List<TreeItem> newSelectedTreeItems) {
		// Find new TIs that should be selected
		for (String s : filenamesToSelect) {
			if (s.equals(getFullFilename(ti))) {
				newSelectedTreeItems.add(ti);
			}
		}
		// Expand/collapse tree
		if (ti.getItemCount() > 0) {
			ti.setExpanded(!collapsedDirs.contains(ti.getData()));
			for (TreeItem i : ti.getItems()) {
				restorePreviousTreeItemState(i, filenamesToSelect, newSelectedTreeItems);
			}
		}
	}

	private void setFileItemInfoText(TreeItem ti, final FileData fd) {
		if (fd.isMarkedForDeletion()) {
			ti.setText(1, I18n.l("inputTree_txt_markedForDeletion")); //$NON-NLS-1$
		} else if (fd.isAudio()) {
			ti.setText(1, formatInfo(trackInfoFmt, fd.getTn(), fd.getTrack()));
		} else {
			ti.setText(1, I18n.l("general_txt_sizeInBytes", fd.getSize())); //$NON-NLS-1$
		}
	}

	private void updateAlbumDirItem(TreeItem parent, DirData dd) {
		// Update text and color
		final TwoColours c;
		final String txt;
		// If contains audio content not marked for deletion
		if (dd.hasAudioContent(true)) {
			final Set<AlbumData> albumDataSet= new HashSet<AlbumData>();
			for (FileData fd : dd.files.values()) {
				if (!fd.isMarkedForDeletion() && fd.isAudio()) {
					albumDataSet.add(fd.getAlbumData());
				}
			}
			// Album data not in sync
			if (albumDataSet.size() > 1) {
				txt= I18n.l("inputTree_txt_multiAlbumInfos"); //$NON-NLS-1$
				c= sharedUIResources.itemIncompleteColours;
			}
			// Album data in sync or empty
			else {
				AlbumData ad= albumDataSet.isEmpty() ? null : albumDataSet.iterator().next();
				if (ad == null) {
					ad= new AlbumData();
				}
				txt= formatInfo(albumInfoFmt, ad.getArtist(), ad.getYear(), ad.getAlbum());
				c= ad.isComplete() ? sharedUIResources.itemCompleteColours : sharedUIResources.itemIncompleteColours;
			}
		}
		// Either no files or all marked for deletion
		else {
			txt= ""; //$NON-NLS-1$
			c= sharedUIResources.itemCompleteColours;
		}
		parent.setText(1, txt);
		parent.setBackground(c.background);
		parent.setForeground(c.foreground);

		// Update check
		boolean nonDelete= false;
		for (FileData fd : dd.files.values()) {
			if (!fd.isMarkedForDeletion()) {
				nonDelete= true;
				break;
			}
		}
		parent.setChecked(nonDelete);
	}
}
