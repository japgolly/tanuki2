package golly.tanuki2.ui;

import golly.tanuki2.core.Engine;
import golly.tanuki2.core.Engine.ProcessingCommands;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.Helpers.OptimisibleDirTreeNode;
import golly.tanuki2.ui.OutputTree.TreeItemInfo.Type;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author Golly
 * @since 14/03/2007
 */
public class OutputTree extends AbstractTreeBasedFileView {
	private final Map<String, TreeItemInfo> content= new HashMap<String, TreeItemInfo>();
	private final Engine engine;
	private Map<String, ProcessingCommands> processingList= null;
	private final Set<String> addedFiles= new HashSet<String>();
	private final Set<String> incompleteDirs= new HashSet<String>();

	public OutputTree(Composite parent, SharedUIResources sharedUIResources, Engine engine) {
		super(parent, sharedUIResources, SWT.FULL_SELECTION | SWT.MULTI);
		this.engine= engine;
	}

	// =============================================================================================== //
	// = TreeItemInfo
	// =============================================================================================== //

	public static final class TreeItemInfo {
		public static enum Type {
			DELETION, INCOMPLETE, MOVED
		};

		public final FileData fd;
		public final ProcessingCommands pc;
		public final String sourceFilename;
		public final Type type;

		public TreeItemInfo(Type type, FileData fd, String srcFilename) {
			this(type, null, fd, srcFilename);
		}

		public TreeItemInfo(Type type, ProcessingCommands pc) {
			this(type, pc, null, null);
		}

		private TreeItemInfo(Type type, ProcessingCommands pc, FileData fd, String srcFilename) {
			this.type= type;
			this.pc= pc;
			this.fd= fd;
			this.sourceFilename= srcFilename;
		}

		public String toString() {
			return Helpers.inspect(this, false);
		}
	}

	// =============================================================================================== //
	// = Required due to interface / superclass
	// =============================================================================================== //

	@Override
	protected void addFilesToTree(TreeItem parent, String path) {
		final TreeItemInfo info= content.get(path);
		if (info != null) {

			// Moved files
			if (info.type == Type.MOVED) {
				final Map<String, String> moves= info.pc.moves;
				if (!moves.isEmpty()) {
					final Map<String, FileData> srcDirFiles= engine.dirs.get(info.pc.sourceDirectory).files;
					for (final String targetFilename : Helpers.sort(moves.values())) {
						String sourceFilename= null;
						for (String s : moves.keySet())
							if (moves.get(s).equals(targetFilename)) {
								sourceFilename= s;
								break;
							}
						final FileData fd= srcDirFiles.get(sourceFilename);
						createFileTreeItem(parent, targetFilename, new TreeItemInfo(Type.MOVED, fd, sourceFilename));
						addedFiles.add(Helpers.addPathElements(info.pc.sourceDirectory, sourceFilename));
					}
				}
			}

			// Deletions
			else if (info.type == Type.DELETION) {
				final Set<String> deletions= info.pc.deletions;
				if (!deletions.isEmpty()) {
					final Map<String, FileData> srcDirFiles= engine.dirs.get(info.pc.sourceDirectory).files;
					for (final String filename : Helpers.sort(deletions)) {
						final FileData fd= srcDirFiles.get(filename);
						createFileTreeItem(parent, filename, new TreeItemInfo(Type.DELETION, fd, filename));
						addedFiles.add(Helpers.addPathElements(info.pc.sourceDirectory, filename));
					}
				}
			}

			// Incomplete
			else if (info.type == Type.INCOMPLETE) {
				final Map<String, FileData> files= engine.dirs.get(info.pc.sourceDirectory).files;
				for (final String filename : Helpers.sort(files.keySet()))
					createFileTreeItem(parent, filename, new TreeItemInfo(Type.INCOMPLETE, files.get(filename), filename));
			}

		}
	}

	@Override
	protected void getAllSelectedDirData(Set<DirData> r, TreeItem ti) {
		final TreeItemInfo info= (TreeItemInfo) ti.getData();
		if (info != null && info.type != Type.DELETION) {
			if (info.fd != null)
				r.add(info.fd.getDirData());
			else {
				r.add(engine.dirs.get(info.pc.sourceDirectory));
				for (TreeItem c : ti.getItems())
					getAllSelectedDirData(r, c);
			}
		}
	}

	@Override
	protected Object getDataForDirTreeItem(final String fullDir) {
		return content.get(fullDir);
	}

	@Override
	protected String getSelectedDir() {
		final TreeItemInfo info= getSelectedInfo();
		// Root element
		if (info == null)
			return null;
		// Directory
		else if (info.fd == null)
			return info.pc.sourceDirectory;
		// File
		else
			return info.fd.getDirData().dir;
	}

	@Override
	protected FileData getSelectedFileData() {
		final TreeItemInfo info= getSelectedInfo();
		return (info == null) ? null : info.fd;
	}

	@Override
	protected String getSelectedFullFilename() {
		final TreeItemInfo info= getSelectedInfo();
		return Helpers.addPathElements(info.fd.getDirData().dir, info.sourceFilename);
	}

	@Override
	protected void onDelete() {
		final Set<String> files= new HashSet<String>();
		for (TreeItem ti : tree.getSelection())
			collectFullFilenames(ti, files);
		sharedUIResources.appUIShared.removeFiles(files.toArray(new String[files.size()]));
	}

	@Override
	protected DirData onEdit_getDirData() {
		final TreeItemInfo info= getSelectedInfo();
		if (info != null) {
			// If selected item is a file
			FileData fd= info.fd;
			if (fd != null && fd.isAudio() && !fd.isMarkedForDeletion())
				return fd.getDirData();
			else
				// Or if directory, get the first audio child-item, and use its DirData
				for (TreeItem i : getSelected().getItems()) {
					fd= ((TreeItemInfo) i.getData()).fd;
					if (fd != null && fd.isAudio() && !fd.isMarkedForDeletion())
						return fd.getDirData();
				}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void refreshFiles(Map<String, DirData> dirs) {
		// TODO OutputTree doesn't yet handle incomplete content
		processingList= engine.createProcessingList(null);
		content.clear();
		addedFiles.clear();
		incompleteDirs.clear();

		// Add all content that will be processed
		final Map<String, OptimisibleDirTreeNode> unoptimisedDirTree= new HashMap<String, OptimisibleDirTreeNode>();
		final String rootTxtDeletion= I18n.l("outputTree_txt_rootDeletion"); //$NON-NLS-1$
		final String rootTxtTargetDir= I18n.l("outputTree_txt_rootTargetDir"); //$NON-NLS-1$
		for (final ProcessingCommands pc : processingList.values()) {
			if (!pc.moves.isEmpty()) {
				final String localPath= Helpers.addPathElements(rootTxtTargetDir, pc.targetDirectory);
				Helpers.addDirToUnoptimisedDirTree(unoptimisedDirTree, localPath, true);
				content.put(localPath, new TreeItemInfo(Type.MOVED, pc));
			}
			if (!pc.deletions.isEmpty()) {
				final String localPath= Helpers.addPathElements(rootTxtDeletion, pc.sourceDirectory);
				Helpers.addDirToUnoptimisedDirTree(unoptimisedDirTree, localPath, true);
				content.put(localPath, new TreeItemInfo(Type.DELETION, pc));
			}
		}
		for (String rootElement : unoptimisedDirTree.keySet())
			Helpers.addDirToUnoptimisedDirTree(unoptimisedDirTree, rootElement, true);
		final Map<String, Map> optimisedDirTree= Helpers.optimiseDirTree(unoptimisedDirTree);
		tree.removeAll();
		populateTree(optimisedDirTree);

		// Add incomplete content
		final Map<String, OptimisibleDirTreeNode> unoptimisedDirTree2= new HashMap<String, OptimisibleDirTreeNode>();
		final String rootTxtIncomplete= I18n.l("outputTree_txt_rootIncomplete"); //$NON-NLS-1$
		for (String filename : engine.files.keySet())
			if (!addedFiles.contains(filename)) {
				final String dir= engine.files.get(filename).getDirData().dir;
				if (!incompleteDirs.contains(dir)) {
					final String localPath= Helpers.addPathElements(rootTxtIncomplete, dir);
					Helpers.addDirToUnoptimisedDirTree(unoptimisedDirTree2, localPath, true);
					final ProcessingCommands pc= new ProcessingCommands();
					pc.sourceDirectory= dir;
					content.put(localPath, new TreeItemInfo(Type.INCOMPLETE, pc));
				}
			}
		for (String rootElement : unoptimisedDirTree2.keySet())
			Helpers.addDirToUnoptimisedDirTree(unoptimisedDirTree2, rootElement, true);
		final Map<String, Map> optimisedDirTree2= Helpers.optimiseDirTree(unoptimisedDirTree2);
		populateTree(optimisedDirTree2);

		// Update style of root notes
		for (TreeItem ti : tree.getItems())
			if (rootTxtDeletion.equals(ti.getText()))
				setTreeItemColor(ti, sharedUIResources.deletionColours);
			else if (rootTxtIncomplete.equals(ti.getText()))
				setTreeItemColor(ti, sharedUIResources.itemIncompleteColours);

		// TODO Should I bother recording and restoring collapsed items?
		setExpandedAll(true);
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private void collectFullFilenames(TreeItem ti, Set<String> files) {
		final TreeItemInfo info= (TreeItemInfo) ti.getData();
		// Root elements + directories 
		if (info == null || info.fd == null)
			for (TreeItem i : ti.getItems())
				collectFullFilenames(i, files);
		// File
		else
			files.add(Helpers.addPathElements(info.fd.getDirData().dir, info.sourceFilename));
	}

	private void createFileTreeItem(TreeItem parent, final String targetFilename, final TreeItemInfo treeItemInfo) {
		TreeItem ti= new TreeItem(parent, SWT.NONE);
		ti.setData(treeItemInfo);
		ti.setImage(treeItemInfo.fd.getImage());
		ti.setText(0, targetFilename);
		setFileItemColor(ti, treeItemInfo.fd);
	}

	private TreeItemInfo getSelectedInfo() {
		return (TreeItemInfo) getSelected().getData();
	}
}
