package golly.tanuki2.ui;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.AutoResizeColumnsListener;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.OSSpecific;
import golly.tanuki2.support.UIHelpers;
import golly.tanuki2.support.UIHelpers.TwoColours;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Golly
 * @since 19/02/2007
 */
public class FlatList extends AbstractFileView {
	private final Table table;
	private final int INDEX_FILENAME, INDEX_ARTIST, INDEX_YEAR, INDEX_ALBUM, INDEX_TN, INDEX_TRACK;
	private final AutoResizeColumnsListener autoColumnResizer;

	public FlatList(Composite parent, SharedUIResources sharedUIResources_) {
		super(sharedUIResources_);

		// Create table
		table= new Table(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		INDEX_FILENAME= addColumn("general_field_filename", SWT.LEFT); //$NON-NLS-1$
		INDEX_ARTIST= addColumn("general_field_artist", SWT.LEFT); //$NON-NLS-1$
		INDEX_YEAR= addColumn("general_field_year", SWT.CENTER); //$NON-NLS-1$
		INDEX_ALBUM= addColumn("general_field_album", SWT.LEFT); //$NON-NLS-1$
		INDEX_TN= addColumn("general_field_tn", SWT.CENTER); //$NON-NLS-1$
		INDEX_TRACK= addColumn("general_field_track", SWT.LEFT); //$NON-NLS-1$

		// Create popup menu
		// TODO Need to give users access to text processors (title case, remove underscores, etc). Prolly best to put it in a context-menu.
		createMenu(table);
		// mi: copy filenames
		MenuItem miCopyFilenames= new MenuItem(contextMenu, SWT.PUSH);
		miCopyFilenames.setImage(TanukiImage.COPY.get());
		miCopyFilenames.setText(I18n.l("main_contextMenu_copyFilenames") + "\tCtrl+C"); //$NON-NLS-1$ //$NON-NLS-2$
		selectionRequiredMenuItems.add(miCopyFilenames);
		miCopyFilenames.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onCopyFilenames();
			}
		});

		// Add table listeners
		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL) {
					// CTRL-C
					if (e.character == 3) {
						onCopyFilenames();
						e.doit= false;
					}
				}
			}
		});
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (e.detail == SWT.CHECK)
					onCheck(e);
			}
		});
		this.autoColumnResizer= UIHelpers.createAutoResizeColumnsListener(table);
		table.addListener(SWT.Resize, autoColumnResizer);
		addCommonFileViewListeners(table);
	}

	// =============================================================================================== //
	// = Public
	// =============================================================================================== //

	public AutoResizeColumnsListener getAutoResizeColumnsListener() {
		return autoColumnResizer;
	}

	public Table getWidget() {
		return table;
	}

	public void refreshFiles(Map<String, DirData> dirs) {
		table.removeAll();

		for (String dir : Helpers.sort(dirs.keySet())) {
			final String dir2= dir + File.separator;
			final HashMap<String, FileData> files= dirs.get(dir).files;
			for (String file : Helpers.sort(files.keySet())) {
				final FileData fd= files.get(file);
				final TableItem ti= new TableItem(table, SWT.NONE);
				ti.setData(fd);
				ti.setImage(fd.getImage());
				ti.setText(INDEX_FILENAME, dir2 + file);
				if (fd.isAudio()) {
					if (fd.getTn() != null)
						ti.setText(INDEX_TN, fd.getTn().toString());
					if (fd.getTrack() != null)
						ti.setText(INDEX_TRACK, fd.getTrack());
					final AlbumData ad= fd.getAlbumData();
					if (ad != null) {
						if (ad.getAlbum() != null)
							ti.setText(INDEX_ALBUM, ad.getAlbum());
						if (ad.getArtist() != null)
							ti.setText(INDEX_ARTIST, ad.getArtist());
						if (ad.getYear() != null)
							ti.setText(INDEX_YEAR, ad.getYear().toString());
					}
				}
				ti.setChecked(!fd.isMarkedForDeletion());
				setFileItemColor(ti, fd);
			}
		}
	}

	// =============================================================================================== //
	// = Events
	// =============================================================================================== //

	protected void onCheck(SelectionEvent e) {
		final TableItem ti= (TableItem) e.item;
		final FileData fd= (FileData) ti.getData();
		fd.setMarkedForDeletion(!ti.getChecked());
		setFileItemColor(ti, fd);
		sharedUIResources.appUIShared.onDataUpdated(true);
	}

	protected void onCopyFilenames() {
		final String EOL= OSSpecific.getEOL();
		StringBuilder sb= new StringBuilder();
		for (TableItem ti : table.getSelection()) {
			sb.append(ti.getText());
			sb.append(EOL);
		}
		sharedUIResources.clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
	}

	protected void onDelete() {
		String[] files= new String[table.getSelectionCount()];
		int i= 0;
		for (TableItem ti : table.getSelection())
			files[i++]= ti.getText();
		sharedUIResources.appUIShared.removeFiles(files);
	}

	protected DirData onEdit_getDirData() {
		final FileData fd= getSelectedFileData();
		if (fd.isAudio() && !fd.isMarkedForDeletion())
			return fd.getDirData();
		else
			return null;
	}

	protected void selectAll() {
		table.selectAll();
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private int addColumn(String name, int align) {
		new TableColumn(table, align).setText(I18n.l(name));
		return table.getColumnCount() - 1;
	}

	protected Set<DirData> getAllSelectedDirData() {
		final Set<DirData> r= new HashSet<DirData>();
		for (TableItem ti : table.getSelection())
			r.add(getData(ti).getDirData());
		return r;
	}

	private FileData getData(TableItem ti) {
		return (FileData) ti.getData();
	}

	private TableItem getSelected() {
		return table.getSelection()[0];
	}

	protected String getSelectedDir() {
		return getSelectedFileData().getDirData().dir;
	}

	protected FileData getSelectedFileData() {
		return getData(getSelected());
	}

	protected String getSelectedFullFilename() {
		return getSelected().getText();
	}

	protected int getSelectionCount() {
		return table.getSelectionCount();
	}

	protected boolean isFileSelected() {
		return true;
	}

	private void setFileItemColor(final TableItem ti, final FileData fd) {
		final TwoColours c= sharedUIResources.appUIShared.getFileItemColours(fd, true);
		if (c != null) {
			ti.setBackground(c.background);
			ti.setForeground(c.foreground);
		}
	}
}
