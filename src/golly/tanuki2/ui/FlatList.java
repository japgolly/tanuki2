package golly.tanuki2.ui;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.Tanuki2Exception;
import golly.tanuki2.support.UIResourceManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Golly
 * @since 19/02/2007
 */
public class FlatList implements IFileView {
	private static final String EOL= "\r\n"; //$NON-NLS-1$ // TODO win32
	private final Table table;
	private final int INDEX_FILENAME, INDEX_ARTIST, INDEX_YEAR, INDEX_ALBUM, INDEX_TN, INDEX_TRACK;
	private final Color nonAudioBkgColor;
	private final MenuItem[] singleSelectionMenuItems;
	private final Clipboard clipboard;

	public FlatList(Composite parent) {
		// Create resources
		nonAudioBkgColor= UIResourceManager.getColorGrey("flatlist_nonAudio_bkg", 230); //$NON-NLS-1$
		clipboard= new Clipboard(Display.getCurrent());

		// Create table
		table= new Table(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		INDEX_FILENAME= addColumn("general_field_filename", SWT.LEFT); //$NON-NLS-1$
		INDEX_ARTIST= addColumn("general_field_artist", SWT.LEFT); //$NON-NLS-1$
		INDEX_YEAR= addColumn("general_field_year", SWT.CENTER); //$NON-NLS-1$
		INDEX_ALBUM= addColumn("general_field_album", SWT.LEFT); //$NON-NLS-1$
		INDEX_TN= addColumn("general_field_tn", SWT.CENTER); //$NON-NLS-1$
		INDEX_TRACK= addColumn("general_field_track", SWT.LEFT); //$NON-NLS-1$

		// Create popup menu
		Menu popupMenu= new Menu(table);
		table.setMenu(popupMenu);
		// mi: open folder
		MenuItem miCopyFilenames= new MenuItem(popupMenu, SWT.PUSH);
		miCopyFilenames.setText(I18n.l("flatList_menu_copyFilenames") + "\tCtrl+C"); //$NON-NLS-1$ //$NON-NLS-2$
		miCopyFilenames.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onCopyFilenames();
			}
		});
		// mi: open folder
		MenuItem miOpenFolder= new MenuItem(popupMenu, SWT.PUSH);
		miOpenFolder.setText(I18n.l("flatList_menu_openFolder")); //$NON-NLS-1$
		miOpenFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onOpenFolder();
			}
		});
		// mi: open prompt
		MenuItem miOpenPrompt= new MenuItem(popupMenu, SWT.PUSH);
		miOpenPrompt.setText(I18n.l("flatList_menu_openPrompt")); //$NON-NLS-1$
		miOpenPrompt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onOpenPrompt();
			}
		});
		// popup menu other
		singleSelectionMenuItems= new MenuItem[] {miOpenFolder, miOpenPrompt};
		popupMenu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				final boolean single= table.getSelectionCount() == 1;
				for (MenuItem mi : singleSelectionMenuItems)
					mi.setEnabled(single);
			}
		});
		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == 'C' - 'A' + 1) {
					onCopyFilenames();
					e.doit= false;
				}
			}
		});
	}

	// =============================================================================================== //
	// = Public
	// =============================================================================================== //

	public Table getWidget() {
		return table;
	}

	public void refreshFiles(HashMap<String, DirData> dirs) {
		table.setRedraw(false);
		table.clearAll();

		for (String dir : Helpers.sort(dirs.keySet())) {
			final String dir2= dir + File.separator;
			final HashMap<String, FileData> files= dirs.get(dir).files;
			for (String file : Helpers.sort(files.keySet())) {
				final FileData fd= files.get(file);
				final TableItem ti= new TableItem(table, SWT.NONE);
				ti.setData(fd);
				
				ti.setText(INDEX_FILENAME, dir2 + file);
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
				
				ti.setImage(fd.getImage());
				if (!fd.isAudio())
					ti.setBackground(nonAudioBkgColor);
			}
		}
		for (TableColumn tc : table.getColumns())
			tc.pack();

		table.setRedraw(true);
	}

	// =============================================================================================== //
	// = Events
	// =============================================================================================== //

	protected void onCopyFilenames() {
		StringBuilder sb= new StringBuilder();
		for (TableItem ti : table.getSelection()) {
			sb.append(ti.getText());
			sb.append(EOL);
		}
		clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
	}

	protected void onOpenFolder() {
		// TODO win32
		try {
			Runtime.getRuntime().exec("explorer.exe .", null, new File(getSelectedData().getDirData().dir)); //$NON-NLS-1$
		} catch (IOException e) {
			new Tanuki2Exception(e).showErrorDialog();
		}
	}

	protected void onOpenPrompt() {
		// TODO win32
		try {
			Runtime.getRuntime().exec("cmd.exe /C start cmd.exe", null, new File(getSelectedData().getDirData().dir)); //$NON-NLS-1$
		} catch (IOException e) {
			new Tanuki2Exception(e).showErrorDialog();
		}
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private int addColumn(String name, int align) {
		new TableColumn(table, align).setText(I18n.l(name));
		return table.getColumnCount() - 1;
	}

	private FileData getData(TableItem ti) {
		return (FileData) ti.getData();
	}

	private TableItem getSelected() {
		return table.getSelection()[0];
	}

	private FileData getSelectedData() {
		return getData(getSelected());
	}
}
