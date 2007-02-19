package golly.tanuki2.ui;

import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIResourceManager;

import java.io.File;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Golly
 * @since 19/02/2007
 */
public class FlatList implements IFileView {
	private final Table table;
	private final int INDEX_FILENAME, INDEX_ARTIST, INDEX_YEAR, INDEX_ALBUM, INDEX_TN, INDEX_TRACK;
	private final Color nonAudioBkgColor;

	public FlatList(Composite parent) {
		table= new Table(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);

		INDEX_FILENAME= addColumn("general_field_filename", SWT.LEFT); //$NON-NLS-1$
		INDEX_ARTIST= addColumn("general_field_artist", SWT.LEFT); //$NON-NLS-1$
		INDEX_YEAR= addColumn("general_field_year", SWT.CENTER); //$NON-NLS-1$
		INDEX_ALBUM= addColumn("general_field_album", SWT.LEFT); //$NON-NLS-1$
		INDEX_TN= addColumn("general_field_tn", SWT.CENTER); //$NON-NLS-1$
		INDEX_TRACK= addColumn("general_field_track", SWT.LEFT); //$NON-NLS-1$

		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		for (TableColumn tc : table.getColumns())
			tc.setWidth(100);
		
		nonAudioBkgColor= UIResourceManager.getColorGrey("flatlist_nonAudio_bkg", 242); //$NON-NLS-1$
	}

	private int addColumn(String name, int align) {
		new TableColumn(table, align).setText(I18n.l(name));
		return table.getColumnCount() - 1;
	}

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
				ti.setText(INDEX_FILENAME, dir2 + file);
				ti.setImage(fd.getImage());
				if (!fd.isAudio())
					ti.setBackground(nonAudioBkgColor);
			}
		}
		for (TableColumn tc : table.getColumns())
			tc.pack();

		table.setRedraw(true);
	}
}
