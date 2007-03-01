package golly.tanuki2.ui;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.RankedObject;
import golly.tanuki2.data.RankedObjectCollection;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIHelpers;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Golly
 * @since 20/02/2007
 */
public class AlbumEditor {
	private static final Pattern patNumeric= Pattern.compile("^\\d+$"); //$NON-NLS-1$

	private final Shell shell;
	private final DirData dd;
	private final Combo iwArtist, iwYear, iwAlbum;
	private final Map<String, Text> iwTnMap, iwTrackMap;
	private final ScrolledComposite trackInfoComposite;
	private boolean updated= false;

	public AlbumEditor(Shell parent, DirData dd_) {
		this.dd= dd_;
		final RankedObjectCollection<AlbumData> allAlbumData= new RankedObjectCollection<AlbumData>();

		// Shell
		shell= new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		shell.setLayout(UIHelpers.makeGridLayout(1, true, 4, 4));
		shell.setText(I18n.l("albumEditor_txt_windowTitle")); //$NON-NLS-1$

		// Album info
		Composite composite= new Composite(shell, SWT.NONE);
		composite.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		composite.setLayout(UIHelpers.makeGridLayout(2, false, 0, 6));
		iwArtist= addAlbumInfoField(composite, "general_field_artist", SWT.FILL); //$NON-NLS-1$
		iwYear= addAlbumInfoField(composite, "general_field_year", SWT.LEFT); //$NON-NLS-1$
		iwAlbum= addAlbumInfoField(composite, "general_field_album", SWT.FILL); //$NON-NLS-1$

		// Track info
		trackInfoComposite= new ScrolledComposite(shell, SWT.FLAT | SWT.BORDER | SWT.V_SCROLL);
		trackInfoComposite.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL, 1, true, SWT.FILL));
		composite= new Composite(trackInfoComposite, SWT.NONE);
		composite.setLayout(UIHelpers.makeGridLayout(2, false, 0, 2));
		iwTnMap= new HashMap<String, Text>();
		iwTrackMap= new HashMap<String, Text>();
		for (String f : Helpers.sort(dd.files.keySet())) {
			final FileData fd= dd.files.get(f);
			if (fd.isAudio() && !fd.isMarkedForDeletion()) {
				// Create track widgets
				// label
				Label l= new Label(composite, SWT.LEFT);
				l.setText(f);
				GridData ld= UIHelpers.makeGridData(2, true, SWT.LEFT);
				ld.verticalIndent= 6;
				l.setLayoutData(ld);
				// tn widget
				Text t= new Text(composite, SWT.BORDER);
				iwTnMap.put(f, t);
				ld= UIHelpers.makeGridData(1, false, SWT.LEFT);
				ld.minimumWidth= ld.widthHint= 24;
				t.setLayoutData(ld);
				if (fd.getTn() != null)
					t.setText(fd.getTn().toString());
				// track widget
				t= new Text(composite, SWT.BORDER);
				iwTrackMap.put(f, t);
				t.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
				if (fd.getTrack() != null)
					t.setText(fd.getTrack());

				// Rank album data
				if (fd.getAlbumData() != null)
					allAlbumData.increaseRank(fd.getAlbumData(), 1);
			}
		}
		addListenersToTrackInfoWidgets();
		trackInfoComposite.setAlwaysShowScrollBars(true);
		trackInfoComposite.setContent(composite);
		composite.pack();
		trackInfoComposite.setMinSize(16, composite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		trackInfoComposite.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				UIHelpers.setWidth(trackInfoComposite.getContent(), trackInfoComposite.getClientArea().width);
			}
		});

		// Buttons
		composite= new Composite(shell, SWT.NONE);
		composite.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));
		composite.setLayout(UIHelpers.makeGridLayout(2, true, 0, 24));
		// Button: ok
		Button btnOk= new Button(composite, SWT.PUSH);
		UIHelpers.setButtonText(btnOk, "albumEditor_btn_ok"); //$NON-NLS-1$
		btnOk.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));
		shell.setDefaultButton(btnOk);
		btnOk.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onOk();
			}
		});
		// Button: cancel
		Button btnCancel= new Button(composite, SWT.PUSH);
		UIHelpers.setButtonText(btnCancel, "albumEditor_btn_cancel"); //$NON-NLS-1$
		btnCancel.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));
		btnCancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onCancel();
			}
		});

		// Populate
		for (RankedObject<AlbumData> adr : allAlbumData) {
			final AlbumData ad= adr.data;
			addToCombo(iwArtist, ad.getArtist());
			addToCombo(iwYear, ad.getYear());
			addToCombo(iwAlbum, ad.getAlbum());
		}

		// Resize and position window
		shell.pack();
		UIHelpers.setWidth(shell, 380);
		Rectangle dca= Display.getCurrent().getClientArea();
		Rectangle shellBounds= shell.getBounds();
		if (shellBounds.height > dca.height)
			UIHelpers.setHeight(shell, dca.height);
		UIHelpers.centerInFrontOfParent(Display.getCurrent(), shell, parent.getBounds());
	}

	public boolean didUpdate() {
		return updated;
	}

	public void show() {
		shell.open();

		Display display= Display.getCurrent();
		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
	}

	// =============================================================================================== //
	// = Events
	// =============================================================================================== //

	protected void onCancel() {
		shell.close();
	}

	protected void onOk() {
		// Check all numeric fields are valid numbers
		if (!checkNumber(iwYear, iwYear.getText()))
			return;
		for (Text t : iwTnMap.values())
			if (!checkNumber(t, t.getText()))
				return;

		// Create new AlbumData
		AlbumData ad= new AlbumData();
		ad.setArtist(processWidgetText(iwArtist.getText()));
		ad.setAlbum(processWidgetText(iwAlbum.getText()));
		ad.setYear(processWidgetText(iwYear.getText()));

		// Update each track
		for (String f : Helpers.sort(dd.files.keySet())) {
			final FileData fd= dd.files.get(f);
			if (fd.isAudio() && !fd.isMarkedForDeletion()) {
				fd.setAlbumData(ad);
				fd.setTn(processWidgetText(iwTnMap.get(f).getText()));
				fd.setTrack(processWidgetText(iwTrackMap.get(f).getText()));
			}
		}

		this.updated= true;
		shell.close();
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

	private void addListenersToTrackInfoWidgets() {
		final Listener makeSureWidgetIsVisible= new Listener() {
			public void handleEvent(Event e) {
				Control child= (Control) e.widget;
				Rectangle bounds= child.getBounds();
				Rectangle area= trackInfoComposite.getClientArea();
				Point origin= trackInfoComposite.getOrigin();
				if (origin.x > bounds.x)
					origin.x= Math.max(0, bounds.x);
				if (origin.y > bounds.y)
					origin.y= Math.max(0, bounds.y);
				if (origin.x + area.width < bounds.x + bounds.width)
					origin.x= Math.max(0, bounds.x + bounds.width - area.width);
				if (origin.y + area.height < bounds.y + bounds.height)
					origin.y= Math.max(0, bounds.y + bounds.height - area.height);
				trackInfoComposite.setOrigin(origin);
			}
		};
		for (Text t : iwTnMap.values())
			t.addListener(SWT.Activate, makeSureWidgetIsVisible);
		for (Text t : iwTrackMap.values())
			t.addListener(SWT.Activate, makeSureWidgetIsVisible);
	}

	private void addToCombo(Combo combo, Integer i) {
		if (i == null)
			return;
		addToCombo(combo, i.toString());
	}

	private void addToCombo(Combo combo, String str) {
		if (str == null)
			return;
		UIHelpers.addUnlessExists(combo, str);
		if (combo.getText().length() == 0)
			combo.setText(str);
	}

	private boolean checkNumber(Control widget, String origText) {
		String text= processWidgetText(origText);
		if (text == null)
			return true;
		if (patNumeric.matcher(text).matches())
			return true;
		else {
			UIHelpers.showTanukiError(shell, "albumEditor_err_invalidNumber", origText); //$NON-NLS-1$
			widget.setFocus();
			return false;
		}
	}

	private static String processWidgetText(String text) {
		if (text == null)
			return null;
		text= Helpers.unicodeTrim(text);
		return (text.length() == 0) ? null : text;
	}
}
