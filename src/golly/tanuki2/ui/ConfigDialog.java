package golly.tanuki2.ui;

import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.Config;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Golly
 * @since 19/03/2007
 */
public class ConfigDialog {
	private static final Pattern pPreprocessFormatString1= Pattern.compile("^[\\\\/" + Helpers.whitespaceChars + "]+|[\\\\/" + Helpers.whitespaceChars + "]+$"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final Pattern pTagInFormatString= Pattern.compile("\\[:[a-z]+:\\]", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private static final String[] supportedTags= Helpers.map(new String[] {"artist", "year", "album", "tn", "track"}, "[:", ":]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

	private final Shell shell;
	private final StyledText iwTargetDirFormat, iwTargetAudioFileFormat;
	private final Color tagInFormatStringColour;
	private boolean firstWidget= true, updated= false;
	private final LineStyleListener lineStyleListener;

	public ConfigDialog(Shell parent) {
		shell= new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setLayout(UIHelpers.makeGridLayout(1, false, 7, 3));
		shell.setText(I18n.l("config_title_window")); //$NON-NLS-1$
		shell.setImage(TanukiImage.TANUKI.get());

		// Add output format widgets
		tagInFormatStringColour= shell.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		lineStyleListener= new LineStyleListener() {
			public void lineGetStyle(LineStyleEvent event) {
				final List<StyleRange> styles= new ArrayList<StyleRange>();
				final Matcher m= pTagInFormatString.matcher(event.lineText);
				while (m.find()) {
					final StyleRange sr= new StyleRange();
					sr.start= m.start();
					sr.length= m.end() - m.start();
					sr.foreground= tagInFormatStringColour;
					styles.add(sr);
				}
				event.styles= (StyleRange[]) styles.toArray(new StyleRange[styles.size()]);
			}
		};
		iwTargetDirFormat= addStyledText("config_txt_targetDirFormat", Config.targetDirFormat); //$NON-NLS-1$
		iwTargetAudioFileFormat= addStyledText("config_txt_targetAudioFileFormat", Config.targetAudioFileFormat); //$NON-NLS-1$

		// Ok and Cancel buttons
		Composite composite= new Composite(shell, SWT.NONE);
		GridData gd= UIHelpers.makeGridData(1, true, SWT.CENTER);
		gd.verticalIndent= 12;
		composite.setLayoutData(gd);
		composite.setLayout(UIHelpers.makeGridLayout(2, true, 0, 24));
		// Button: ok
		Button btnOk= new Button(composite, SWT.PUSH);
		UIHelpers.setButtonText(btnOk, "general_btn_ok"); //$NON-NLS-1$
		btnOk.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));
		shell.setDefaultButton(btnOk);
		btnOk.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onOk();
			}
		});
		// Button: cancel
		Button btnCancel= new Button(composite, SWT.PUSH);
		UIHelpers.setButtonText(btnCancel, "general_btn_cancel"); //$NON-NLS-1$
		btnCancel.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));
		btnCancel.addSelectionListener(new SelectionAdapter() {
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

	private StyledText addStyledText(String label, String value) {
		Label l= new Label(shell, SWT.NONE);
		GridData gd= UIHelpers.makeGridData(1, true, SWT.FILL);
		if (firstWidget)
			firstWidget= false;
		else
			gd.verticalIndent= 10;
		l.setLayoutData(gd);
		l.setText(I18n.l(label));

		StyledText t= new StyledText(shell, SWT.BORDER | SWT.SINGLE);
		t.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		t.setText(value);
		t.addLineStyleListener(lineStyleListener);
		return t;
	}

	private void onOk() {
		final String targetAudioFileFormat= pPreprocessFormatString1.matcher(iwTargetAudioFileFormat.getText()).replaceAll(""); //$NON-NLS-1$
		final String targetDirFormat= pPreprocessFormatString1.matcher(iwTargetDirFormat.getText()).replaceAll(""); //$NON-NLS-1$

		// Validate
		if (targetDirFormat.contains("[:tn:]") || targetDirFormat.contains("[:track:]")) { //$NON-NLS-1$ //$NON-NLS-2$
			UIHelpers.showTanukiError(shell, "config_err_targetDirFormat_containsTrackTags"); //$NON-NLS-1$
			iwTargetDirFormat.setFocus();
			iwTargetDirFormat.selectAll();
			return;
		}
		if (!validateTagsInFormatString(targetAudioFileFormat, iwTargetAudioFileFormat))
			return;
		if (!validateTagsInFormatString(targetDirFormat, iwTargetDirFormat))
			return;
		if (!targetAudioFileFormat.contains("[:tn:]") && !targetAudioFileFormat.contains("[:track:]")) { //$NON-NLS-1$ //$NON-NLS-2$
			UIHelpers.showTanukiError(shell, "config_err_targetFileFormat_doesntContainTrackTags"); //$NON-NLS-1$
			iwTargetAudioFileFormat.setFocus();
			iwTargetAudioFileFormat.selectAll();
			return;
		}

		// Update config
		Config.targetAudioFileFormat= targetAudioFileFormat;
		Config.targetDirFormat= targetDirFormat;

		// Finished
		updated= true;
		shell.close();
	}

	private boolean validateTagsInFormatString(final String fmtStr, final StyledText widget) {
		Matcher m= pTagInFormatString.matcher(fmtStr);
		while (m.find()) {
			boolean ok= false;
			for (String t : supportedTags)
				if (t.equals(m.group())) {
					ok= true;
					break;
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
}
