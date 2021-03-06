package golly.tanuki2.ui;

import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.RuntimeConfig;
import golly.tanuki2.support.TanukiImage;
import golly.tanuki2.support.UIHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Golly
 * @since 19/03/2007
 */
public class ConfigDialog {
	private static final Pattern pPreprocessFormatString1= Pattern.compile("^[\\\\/" + Helpers.whitespaceChars + "]+|[\\\\/" + Helpers.whitespaceChars + "]+$"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final Pattern pTagInFormatString= Pattern.compile("\\[:[a-z]+:\\]", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private static final String[] supportedTags= Helpers.map(new String[] {"artist", "year", "album", "tn", "track"}, "[:", ":]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

	private final RuntimeConfig cfg;

	private final Shell shell;
	private final Color tagInFormatStringForegroundColour;
	private final Color tagInFormatStringBackgroundColour;
	private boolean firstWidget= true, updated= false;
	private final LineStyleListener lineStyleListener;
	private final Button btnAutoTitleCase, btnIntelligentTitleCase;
	private final Button btnCheckVersionOnStartup;
	private final Button btnOk;

	private final StyledText iwTargetDirFormat, iwTargetAudioFileFormat;
	private final Text iwAlbumBlacklist;

	public ConfigDialog(Shell parent, RuntimeConfig cfg) {
		this.cfg= cfg;

		shell= new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setLayout(UIHelpers.makeGridLayout(1, false, 4, 16));
		shell.setText(I18n.l("config_title_window")); //$NON-NLS-1$
		shell.setImage(TanukiImage.TANUKI.get());

		// GROUP: Text
		Group g= new Group(shell, SWT.SHADOW_ETCHED_IN);
		g.setText(I18n.l("config_grp_text")); //$NON-NLS-1$
		g.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		g.setLayout(UIHelpers.makeGridLayout(1, false, 4, 8));
		// Auto title case button
		btnAutoTitleCase= new Button(g, SWT.CHECK);
		btnAutoTitleCase.setText(I18n.l("config_btn_autoTitleCase")); //$NON-NLS-1$
		btnAutoTitleCase.setSelection(cfg.autoTitleCase);
		// Intelligent title case button
		// TODO Add a tooltip describing what intelligent title case is
		btnIntelligentTitleCase= new Button(g, SWT.CHECK);
		btnIntelligentTitleCase.setText(I18n.l("config_btn_intelligentTitleCase")); //$NON-NLS-1$
		btnIntelligentTitleCase.setSelection(cfg.intelligentTitleCase);
		// Artist blacklist
		// TODO Add a tooltip for artist blacklist
		firstWidget= false;
		iwAlbumBlacklist= createLabelAndText(g, "config_txt_artistBlacklist", cfg.artistBlacklist); //$NON-NLS-1$

		// GROUP: Output
		g= new Group(shell, SWT.SHADOW_ETCHED_IN);
		g.setText(I18n.l("config_grp_output")); //$NON-NLS-1$
		g.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		g.setLayout(UIHelpers.makeGridLayout(1, false, 4, 2));
		firstWidget= true;
		// Add output format widgets
		tagInFormatStringForegroundColour= shell.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		tagInFormatStringBackgroundColour= shell.getDisplay().getSystemColor(SWT.COLOR_GRAY);
		lineStyleListener= new LineStyleListener() {
			public void lineGetStyle(LineStyleEvent event) {
				final List<StyleRange> styles= new ArrayList<StyleRange>();
				final Matcher m= pTagInFormatString.matcher(event.lineText);
				while (m.find()) {
					final StyleRange sr= new StyleRange();
					sr.start= m.start();
					sr.length= m.end() - m.start();
					sr.foreground= tagInFormatStringForegroundColour;
					sr.background= tagInFormatStringBackgroundColour;
					styles.add(sr);
				}
				event.styles= styles.toArray(new StyleRange[styles.size()]);
			}
		};
		iwTargetDirFormat= createLabelAndStyledText(g, "config_txt_targetDirFormat", cfg.targetDirFormat); //$NON-NLS-1$
		iwTargetAudioFileFormat= createLabelAndStyledText(g, "config_txt_targetAudioFileFormat", cfg.targetAudioFileFormat); //$NON-NLS-1$

		// GROUP: Text
		g= new Group(shell, SWT.SHADOW_ETCHED_IN);
		g.setText(I18n.l("config_grp_other")); //$NON-NLS-1$
		g.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		g.setLayout(UIHelpers.makeGridLayout(1, false, 4, 8));
		// Check version on startup
		btnCheckVersionOnStartup= new Button(g, SWT.CHECK);
		btnCheckVersionOnStartup.setText(I18n.l("config_btn_checkVersionOnStartup")); //$NON-NLS-1$
		btnCheckVersionOnStartup.setSelection(cfg.checkVersionOnStartup);

		// Ok and Cancel buttons
		Composite composite= new Composite(shell, SWT.NONE);
		GridData gd= UIHelpers.makeGridData(1, true, SWT.CENTER);
		composite.setLayoutData(gd);
		composite.setLayout(UIHelpers.makeGridLayout(2, true, 0, 24));
		// Button: ok
		btnOk= new Button(composite, SWT.PUSH);
		UIHelpers.setButtonText(btnOk, "general_btn_ok"); //$NON-NLS-1$
		btnOk.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));
		shell.setDefaultButton(btnOk);
		btnOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onOk();
			}
		});
		// Button: cancel
		Button btnCancel= new Button(composite, SWT.PUSH);
		UIHelpers.setButtonText(btnCancel, "general_btn_cancel"); //$NON-NLS-1$
		btnCancel.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});

		shell.pack();
		UIHelpers.setWidth(shell, 420);
		UIHelpers.centerInFrontOfParent(shell.getDisplay(), shell, parent.getBounds());
	}

	public boolean show() {
		shell.open();
		UIHelpers.passControlToUiUntilShellClosed(shell);
		return updated;
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private void createLabel(Composite parent, String label) {
		// Create label
		Label l= new Label(parent, SWT.NONE);
		GridData gd= UIHelpers.makeGridData(1, true, SWT.FILL);
		if (firstWidget) {
			firstWidget= false;
		} else {
			gd.verticalIndent= 10;
		}
		l.setLayoutData(gd);
		l.setText(I18n.l(label));
	}

	private StyledText createLabelAndStyledText(Composite parent, String label, String value) {
		// Create label
		createLabel(parent, label);

		// Create styled text widget
		StyledText t= new StyledText(parent, SWT.BORDER | SWT.SINGLE);
		t.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		if (value != null) {
			t.setText(value);
		}
		t.addLineStyleListener(lineStyleListener);
		return t;
	}

	private Text createLabelAndText(Composite parent, String label, String value) {
		// Create label
		createLabel(parent, label);

		// Create text widget
		Text t= new Text(parent, SWT.BORDER | SWT.SINGLE);
		t.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		if (value != null) {
			t.setText(value);
		}
		return t;
	}

	private void onOk() {
		// Validate dir and file format strings
		final String targetAudioFileFormat= pPreprocessFormatString1.matcher(iwTargetAudioFileFormat.getText()).replaceAll(""); //$NON-NLS-1$
		final String targetDirFormat= pPreprocessFormatString1.matcher(iwTargetDirFormat.getText()).replaceAll(""); //$NON-NLS-1$
		if (targetDirFormat.contains("[:tn:]") || targetDirFormat.contains("[:track:]")) { //$NON-NLS-1$ //$NON-NLS-2$
			UIHelpers.showTanukiError(shell, "config_err_targetDirFormat_containsTrackTags"); //$NON-NLS-1$
			iwTargetDirFormat.setFocus();
			iwTargetDirFormat.selectAll();
			return;
		}
		if (!validateTagsInFormatString(targetAudioFileFormat, iwTargetAudioFileFormat)) {
			return;
		}
		if (!validateTagsInFormatString(targetDirFormat, iwTargetDirFormat)) {
			return;
		}
		if (!targetAudioFileFormat.contains("[:tn:]") && !targetAudioFileFormat.contains("[:track:]")) { //$NON-NLS-1$ //$NON-NLS-2$
			UIHelpers.showTanukiError(shell, "config_err_targetFileFormat_doesntContainTrackTags"); //$NON-NLS-1$
			iwTargetAudioFileFormat.setFocus();
			iwTargetAudioFileFormat.selectAll();
			return;
		}

		// Validate artist blacklist
		String artistBlacklist= iwAlbumBlacklist.getText().trim();
		if (artistBlacklist.length() == 0) {
			artistBlacklist= null;
		} else {
			try {
				Pattern.compile(artistBlacklist);
			} catch (PatternSyntaxException e) {
				UIHelpers.showTanukiError(shell, "config_err_artistBlacklist_badRegex", e.getDescription()); //$NON-NLS-1$
				iwAlbumBlacklist.setFocus();
				iwAlbumBlacklist.selectAll();
				return;
			}
		}

		// Update config
		cfg.autoTitleCase= btnAutoTitleCase.getSelection();
		cfg.intelligentTitleCase= btnIntelligentTitleCase.getSelection();
		cfg.targetAudioFileFormat= targetAudioFileFormat;
		cfg.targetDirFormat= targetDirFormat;
		cfg.checkVersionOnStartup= btnCheckVersionOnStartup.getSelection();
		cfg.artistBlacklist= artistBlacklist;

		// Finished
		updated= true;
		shell.close();
	}

	private boolean validateTagsInFormatString(final String fmtStr, final StyledText widget) {
		Matcher m= pTagInFormatString.matcher(fmtStr);
		while (m.find()) {
			boolean ok= false;
			for (String t : supportedTags) {
				if (t.equals(m.group())) {
					ok= true;
					break;
				}
			}
			if (!ok) {
				UIHelpers.showTanukiError(shell, "config_err_targetFormat_containsInvalidTag", m.group(), Helpers.join(Helpers.map(supportedTags, "  ", ""), "\n")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				widget.setFocus();
				widget.setSelection(m.start(), m.end());
				return false;
			}
		}
		return true;
	}

	/**
	 * Indicates whether or not the given {@link RuntimeConfig} was successfully updated.
	 */
	public boolean isUpdated() {
		return updated;
	}
}
