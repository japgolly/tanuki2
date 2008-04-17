package golly.tanuki2.ui;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.support.AutoResizeColumnsListener;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.TanukiImage;
import golly.tanuki2.support.UIHelpers;
import golly.tanuki2.support.UIHelpers.TwoColours;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author Golly
 * @since 14/03/2007
 */
public abstract class AbstractTreeBasedFileView extends AbstractFileView {
	protected final AutoResizeColumnsListener autoColumnResizer;
	protected final Tree tree;

	public AbstractTreeBasedFileView(Composite parent, SharedUIResources sharedUIResources, int treeStyle) {
		super(sharedUIResources);
		tree= new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | treeStyle);

		createMenu(tree);

		tree.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL) {
					// CTRL +, CTRL -
					if (e.character == '+' || e.character == '-') {
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

		this.autoColumnResizer= UIHelpers.createAutoResizeColumnsListener(tree);
		tree.addListener(SWT.Resize, autoColumnResizer);

		addCommonFileViewListeners(tree);
	}

	@SuppressWarnings("unchecked")
	protected void addDirToTree(TreeItem parent, Map<String, Map> children, String path) {
		if (children != null)
			for (String dir : Helpers.sort(children.keySet())) {
				final String fullDir= Helpers.addPathElements(path, dir);
				TreeItem ti= new TreeItem(parent, SWT.NONE);
				ti.setChecked(true);
				ti.setData(getDataForDirTreeItem(fullDir));
				ti.setImage(TanukiImage.FOLDER.get());
				ti.setText(0, dir);
				addDirToTree(ti, children.get(dir), fullDir);
			}
		addFilesToTree(parent, path);
	}

	protected abstract void addFilesToTree(TreeItem parent, String path);

	protected Set<DirData> getAllSelectedDirData() {
		final Set<DirData> r= new HashSet<DirData>();
		for (TreeItem ti : tree.getSelection())
			getAllSelectedDirData(r, ti);
		r.remove(null);
		return r;
	}

	protected abstract void getAllSelectedDirData(final Set<DirData> r, TreeItem ti);

	public AutoResizeColumnsListener getAutoResizeColumnsListener() {
		return autoColumnResizer;
	}

	protected abstract Object getDataForDirTreeItem(final String fullDir);

	protected TreeItem getSelected() {
		return tree.getSelection()[0];
	}

	protected int getSelectionCount() {
		return tree.getSelectionCount();
	}

	public Tree getWidget() {
		return tree;
	}

	protected boolean isFileSelected() {
		return getSelectedFileData() != null;
	}

	@SuppressWarnings("unchecked")
	protected void populateTree(final Map<String, Map> optimisedDirTree) {
		for (String dir : Helpers.sort(optimisedDirTree.keySet())) {
			TreeItem ti= new TreeItem(tree, SWT.NONE);
			ti.setData(getDataForDirTreeItem(dir));
			ti.setText(dir);
			ti.setImage(TanukiImage.FOLDER.get());
			addDirToTree(ti, optimisedDirTree.get(dir), dir);
		}
	}

	protected void selectAll() {
		tree.selectAll();
	}

	protected void setExpanded(TreeItem ti, boolean expanded) {
		ti.setExpanded(expanded);
		for (TreeItem i : ti.getItems())
			setExpanded(i, expanded);
	}

	protected void setExpandedAll(boolean expanded) {
		for (TreeItem i : tree.getItems())
			setExpanded(i, expanded);
		tree.showSelection();
	}

	protected void setFileItemColor(final TreeItem ti, final FileData fd) {
		setTreeItemColor(ti, sharedUIResources.appUIShared.getFileItemColours(fd, false));
	}

	protected void setTreeItemColor(final TreeItem ti, final TwoColours c) {
		if (c != null) {
			ti.setBackground(c.background);
			ti.setForeground(c.foreground);
		}
	}
}
