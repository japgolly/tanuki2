package golly.tanuki2.ui;

import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIHelpers;
import golly.tanuki2.support.UIResourceManager;
import golly.tanuki2.support.UIHelpers.TwoColours;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Golly
 * @since 06/03/07
 */
public class VoodooProgressDialog {
	private static final String CONSOLE_INDENT= "  "; //$NON-NLS-1$

	private final Display display;
	private final Shell shell;
	private final StyledText console;
	private final Label lblOverall1, lblOverallP;
	private final ProgressBar pbOverall;
	private final Button btnClose;
	private final TwoColours clrDelete, clrMoveSource, clrMoveTarget, clrDirSource, clrDirTarget;

	private boolean running= false;
	private int totalFiles, currentFileNumber;
	private int consoleIndex= 0, consoleLines= 0;

	public VoodooProgressDialog(Shell parent) {
		this.display= parent.getDisplay();
		this.shell= new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE | SWT.MAX);
		shell.setLayout(UIHelpers.makeGridLayout(1, true, 4, 16));
		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				if (running)
					event.doit= false;
			}
		});

		// Colours
		clrDelete= new TwoColours(null, UIResourceManager.getColor("voodooProgessDlg_clrDelete_fg", 224, 0, 0)); //$NON-NLS-1$
		clrMoveSource= new TwoColours(null, UIResourceManager.getColor("voodooProgessDlg_clrMoveSource_fg", 48, 48, 192)); //$NON-NLS-1$
		clrMoveTarget= new TwoColours(null, UIResourceManager.getColor("voodooProgessDlg_clrMoveTarget_fg", 0, 128, 0)); //$NON-NLS-1$
		clrDirSource= new TwoColours(UIResourceManager.getColor("voodooProgessDlg_clrDirSource_bg", 255, 254, 192), UIResourceManager.getColorGrey("voodooProgessDlg_clrDirSource_fg", 0)); //$NON-NLS-1$ //$NON-NLS-2$
		clrDirTarget= new TwoColours(UIResourceManager.getColor("voodooProgessDlg_clrDirTarget_bg", 214, 255, 214), UIResourceManager.getColorGrey("voodooProgessDlg_clrDirTarget_fg", 0)); //$NON-NLS-1$ //$NON-NLS-2$

		// Console
		console= new StyledText(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		console.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL, 1, true, SWT.FILL));
		console.setIndent(2);

		// Overall progress
		Composite composite= new Composite(shell, SWT.NONE);
		composite.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		composite.setLayout(UIHelpers.makeGridLayout(2, false, 0, 4));
		lblOverall1= new Label(composite, SWT.LEFT);
		lblOverall1.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		lblOverall1.setText(I18n.l("voodoo_txt_overall")); //$NON-NLS-1$
		lblOverallP= new Label(composite, SWT.RIGHT);
		lblOverallP.setLayoutData(UIHelpers.makeGridData(1, false, SWT.RIGHT));
		pbOverall= new ProgressBar(composite, SWT.HORIZONTAL | SWT.SMOOTH);
		pbOverall.setLayoutData(UIHelpers.makeGridData(2, true, SWT.FILL));

		// Close button
		btnClose= new Button(shell, SWT.PUSH);
		btnClose.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));
		UIHelpers.setButtonText(btnClose, "voodoo_btn_close"); //$NON-NLS-1$
		btnClose.setEnabled(false);
		btnClose.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				shell.close();
			}
		});

		lblOverallP.setText("xxxxx/xxxxx"); //$NON-NLS-1$
		shell.pack();
		final Rectangle dca= display.getClientArea();
		UIHelpers.setWidth(shell, (int) Math.min(1000, dca.width * .975));
		UIHelpers.setHeight(shell, (int) Math.min(652, dca.height * .975));
		UIHelpers.centerInFrontOfParent(display, shell, dca);
		lblOverallP.setText(""); //$NON-NLS-1$
		shell.setFocus();
	}

	// =============================================================================================== //
	// = Public
	// =============================================================================================== //

	public Shell getShell() {
		return shell;
	}

	public void starting(int dirCount, int totalFiles) {
		running= true;
		this.totalFiles= totalFiles;
		currentFileNumber= 0;
		UIHelpers.configureProgressBar(pbOverall, 0, totalFiles, 0);
		shell.open();
	}

	public void nextDir(String srcDir, String targetDir, int fileCount) {
		if (consoleLines != 0)
			consoleWriteLn();
		consoleWriteLn(false, "voodoo_consoletxt_dirSource", srcDir, clrDirSource, true); //$NON-NLS-1$
		if (targetDir != null)
			consoleWriteLn(true, "voodoo_consoletxt_dirTarget", targetDir, clrDirTarget, true); //$NON-NLS-1$
		console.setTopIndex(consoleLines);
	}

	public void nextFile() {
		pbOverall.setSelection(currentFileNumber++);
		lblOverallP.setText(I18n.l("voodoo_txt_progress", currentFileNumber, totalFiles)); //$NON-NLS-1$
	}

	public void deleting(File file) {
		consoleWriteLn(true, "voodoo_consoletxt_fileDeleting", file.getName(), clrDelete, false); //$NON-NLS-1$
		console.setTopIndex(consoleLines);
		sleep();
	}

	public void moving(File source, File target) {
		consoleWriteLn(true, "voodoo_consoletxt_fileMoving", source.getName(), clrMoveSource, false, target.getName(), clrMoveTarget, false); //$NON-NLS-1$
		console.setTopIndex(consoleLines);
		sleep();
	}

	public void finished() {
		pbOverall.setSelection(currentFileNumber);
		btnClose.setEnabled(true);
		btnClose.setFocus();
		shell.setDefaultButton(btnClose);
		btnClose.setFocus();

		while (!shell.isDisposed())
			if (!display.readAndDispatch()) {
				running= false;
				display.sleep();
			}
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private void consoleWriteLn() {
		consoleWriteLn(""); //$NON-NLS-1$
	}

	private void consoleWriteLn(String txt) {
		console.append(txt + "\n"); //$NON-NLS-1$
		consoleIndex+= txt.length() + 1;
		consoleLines++;
	}

	private void consoleWriteLn(boolean indent, String i18nKey, String arg1, TwoColours colours1, boolean bold1) {
		final String txt= (indent ? CONSOLE_INDENT : "") + I18n.l(i18nKey, arg1); //$NON-NLS-1$
		console.append(txt + "\n"); //$NON-NLS-1$
		applyConsoleStyle(txt, arg1, colours1, bold1);
		consoleIndex+= txt.length() + 1;
		consoleLines++;
	}

	private void consoleWriteLn(boolean indent, String i18nKey, String arg1, TwoColours colours1, boolean bold1, String arg2, TwoColours colours2, boolean bold2) {
		final String txt= (indent ? CONSOLE_INDENT : "") + I18n.l(i18nKey, arg1, arg2); //$NON-NLS-1$
		final String txt1= (indent ? CONSOLE_INDENT : "") + I18n.l(i18nKey, arg1, makeFakeString(arg2)); //$NON-NLS-1$
		final String txt2= (indent ? CONSOLE_INDENT : "") + I18n.l(i18nKey, makeFakeString(arg1), arg2); //$NON-NLS-1$
		console.append(txt + "\n"); //$NON-NLS-1$
		applyConsoleStyle(txt1, arg1, colours1, bold1);
		applyConsoleStyle(txt2, arg2, colours2, bold2);
		consoleIndex+= txt.length() + 1;
		consoleLines++;
	}

	private void applyConsoleStyle(final String txt, String selection, TwoColours colours, boolean bold) {
		final StyleRange style= new StyleRange();
		style.start= consoleIndex + txt.indexOf(selection);
		style.length= selection.length();
		if (bold)
			style.fontStyle= SWT.BOLD;
		if (colours != null) {
			if (colours.background != null)
				style.background= colours.background;
			if (colours.foreground != null)
				style.foreground= colours.foreground;
		}
		console.setStyleRange(style);
	}

	private String makeFakeString(String arg) {
		int i= arg.length();
		char[] tmp= new char[i];
		while (i-- > 0)
			tmp[i]= '_';
		return new String(tmp);
	}

	// DELME
	private void sleep() {
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
