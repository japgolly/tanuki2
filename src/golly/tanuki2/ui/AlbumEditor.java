package golly.tanuki2.ui;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.AlbumDataAndRank;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIHelpers;

import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Golly
 * @since 20/02/2007
 */
public class AlbumEditor {
	private final Shell shell;
	private final DirData dd;
	private final Combo iwArtist, iwYear, iwAlbum;

	public AlbumEditor(Shell parent, DirData dd_) {
		this.dd= dd_;
		SortedSet<AlbumDataAndRank> allAlbumData= new TreeSet<AlbumDataAndRank>();

		// Shell
		shell= new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		shell.setLayout(UIHelpers.makeGridLayout(1, true, 4, 4));

		// Album info
		Composite composite= new Composite(shell, SWT.NONE);
		composite.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		composite.setLayout(UIHelpers.makeGridLayout(2, false, 0, 6));
		iwArtist= addAlbumInfoField(composite, "general_field_artist", SWT.FILL); //$NON-NLS-1$
		iwYear= addAlbumInfoField(composite, "general_field_year", SWT.LEFT); //$NON-NLS-1$
		iwAlbum= addAlbumInfoField(composite, "general_field_album", SWT.FILL); //$NON-NLS-1$

		// Track info
		composite= new Composite(shell, SWT.NONE);
		composite.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		composite.setLayout(UIHelpers.makeGridLayout(2, false, 0, 2));
		for (String f : Helpers.sort(dd.files.keySet())) {
			final FileData fd= dd.files.get(f);
			if (fd.isAudio()) {

				// Create track widgets
				Label l= new Label(composite, SWT.LEFT);
				l.setText(f);
				GridData ld= UIHelpers.makeGridData(2, true, SWT.LEFT);
				ld.verticalIndent= 6;
				l.setLayoutData(ld);
				Combo c= new Combo(composite, SWT.NONE);
				ld= UIHelpers.makeGridData(1, false, SWT.LEFT);
				ld.minimumWidth= ld.widthHint= 24;
				c.setLayoutData(ld);
				if (fd.getTn() != null)
					c.setText(fd.getTn().toString());
				c= new Combo(composite, SWT.NONE);
				c.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
				if (fd.getTrack() != null)
					c.setText(fd.getTrack());

				// Record album data
				AlbumDataAndRank.addOneToSet(allAlbumData, fd.getAlbumData());
			}
		}

		// Buttons
		composite= new Composite(shell, SWT.BORDER);
		composite.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));

		// Populate
		boolean select= true;
		for (AlbumDataAndRank adr : allAlbumData) {
			final AlbumData ad= adr.data;
			addToCombo(iwArtist, ad.getArtist(), select);
			addToCombo(iwYear, ad.getYear(), select);
			addToCombo(iwAlbum, ad.getAlbum(), select);
			select= false;
		}

		// Shell again
		shell.pack();
		Rectangle pca= parent.getBounds();
		Point s= shell.getSize();
		s.x= 360;
		shell.setSize(s);
		shell.setLocation(pca.x + (pca.width - s.x) / 2, pca.y + (pca.height - s.y) / 2);
	}

	public void show() {
		shell.open();
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private Combo addAlbumInfoField(Composite composite, String labelKey, int comboStyle) {
		Label l= new Label(composite, SWT.LEFT);
		l.setText(I18n.l("general_field_labelFmt", I18n.l(labelKey))); //$NON-NLS-1$
		l.setLayoutData(UIHelpers.makeGridData(1, false, SWT.LEFT));
		Combo c= new Combo(composite, SWT.NONE);
		c.setLayoutData(UIHelpers.makeGridData(1, true, comboStyle));
		return c;
	}

	private void addToCombo(Combo combo, Integer i, boolean select) {
		if (i == null)
			return;
		addToCombo(combo, i.toString(), select);
	}

	private void addToCombo(Combo combo, String str, boolean select) {
		if (str == null)
			return;
		UIHelpers.addUnlessExists(combo, str);
		if (select)
			combo.setText(str);
	}
}
