package golly.tanuki2.ui;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.RankedObject;
import golly.tanuki2.data.RankedObjectCollection;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.UIHelpers;

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Golly
 * @since 12/03/2007
 */
public class ArtistEditor {
	public static boolean open(Shell parent, Set<DirData> dirdataSet) {
		if (dirdataSet.isEmpty()) {
			UIHelpers.showTanukiWarning(parent, "main_err_noAudioSelectedForArtistEditor"); //$NON-NLS-1$
			return false;
		} else
			return new ArtistEditor(parent, dirdataSet).open();
	}

	private final Shell shell;
	private final Combo iwArtist;
	private final Set<DirData> dirdataSet;
	private boolean updated= false;

	private ArtistEditor(Shell parent, Set<DirData> dirdataSet) {
		this.dirdataSet= dirdataSet;
		shell= new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setImage(TanukiImage.EDITOR.get());
		shell.setText(parent.getText());
		shell.setLayout(UIHelpers.makeGridLayout(2, true, 8, 8));

		// Combo
		iwArtist= new Combo(shell, SWT.NONE);
		iwArtist.setLayoutData(UIHelpers.makeGridData(2, true, SWT.FILL));

		// Ok button
		Button btnOk= new Button(shell, SWT.PUSH);
		btnOk.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));
		UIHelpers.setButtonText(btnOk, "general_btn_ok"); //$NON-NLS-1$
		btnOk.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onOk();
			}
		});
		shell.setDefaultButton(btnOk);

		// Cancel button
		Button btnCancel= new Button(shell, SWT.PUSH);
		btnCancel.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));
		UIHelpers.setButtonText(btnCancel, "general_btn_cancel"); //$NON-NLS-1$
		btnCancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});

		iwArtist.setText("123456789012345678901234567890123456789012345678901234567890"); //$NON-NLS-1$
		shell.pack();
		UIHelpers.centerInFrontOfParent(shell.getDisplay(), shell, parent.getBounds());

		// Populate artist combo
		RankedObjectCollection<String> artists= new RankedObjectCollection<String>();
		for (DirData dd : dirdataSet)
			for (FileData fd : dd.files.values())
				if (fd.getAlbumData() != null && !fd.isMarkedForDeletion())
					artists.increaseRank(fd.getAlbumData().getArtist(), 1);
		artists.remove(null);
		for (RankedObject<String> a : artists)
			iwArtist.add(a.data);
		if (artists.getWinner() != null)
			iwArtist.setText(artists.getWinner());
		else
			iwArtist.setText(""); //$NON-NLS-1$
	}

	public boolean open() {
		shell.open();

		final Display display= Display.getCurrent();
		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();

		return updated;
	}

	protected void onOk() {
		final String artist= iwArtist.getText();
		for (DirData dd : dirdataSet)
			for (FileData fd : dd.files.values())
				if (!fd.isMarkedForDeletion()) {
					AlbumData ad= fd.getAlbumData();
					if (ad == null)
						ad= new AlbumData();
					ad.setArtist(artist);
					fd.setAlbumData(ad);
				}
		updated= true;
		shell.close();
	}
}
