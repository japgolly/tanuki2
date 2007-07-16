package golly.tanuki2.ui;

import golly.tanuki2.core.IVoodooProgressMonitor;
import golly.tanuki2.support.RuntimeConfig;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIHelpers;
import golly.tanuki2.support.UIResourceManager;
import golly.tanuki2.support.UIHelpers.TwoColours;

import java.io.File;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
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
public class VoodooProgressDialog implements IVoodooProgressMonitor {
	private static final String CONSOLE_INDENT= "    "; //$NON-NLS-1$

	private final Display display;
	private final Shell shell;
	private final StyledText console;
	private final Label lblOverall1, lblOverallP;
	private final ProgressBar pbOverall;
	private final Button btnClose;
	private final TwoColours clrDelete, clrMoveSource, clrMoveTarget, clrDirSource, clrDirTarget, clrRmdir, clrFailed;

	private boolean allowClose= false, cancelled= false;
	private int totalFiles, currentFileNumber;
	private int consoleIndex= 0, consoleLines= 0;

	public VoodooProgressDialog(Shell parent) {
		this.display= parent.getDisplay();
		this.shell= new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE | SWT.MAX);
		shell.setLayout(UIHelpers.makeGridLayout(1, true, 4, 16));
		shell.setImage(parent.getImage());
		shell.setText(parent.getText());
		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				if (allowClose) {
					final Point sz= shell.getSize();
					RuntimeConfig.voodooWndWidth= sz.x;
					RuntimeConfig.voodooWndHeight= sz.y;
				} else
					event.doit= false;
			}
		});

		// Colours
		clrDelete= new TwoColours(null, UIResourceManager.getColor("voodooProgessDlg_clrDelete_fg", 224, 0, 0)); //$NON-NLS-1$
		clrMoveSource= new TwoColours(null, UIResourceManager.getColor("voodooProgessDlg_clrMoveSource_fg", 48, 48, 192)); //$NON-NLS-1$
		clrMoveTarget= new TwoColours(null, UIResourceManager.getColor("voodooProgessDlg_clrMoveTarget_fg", 0, 128, 0)); //$NON-NLS-1$
		clrDirSource= new TwoColours(UIResourceManager.getColor("voodooProgessDlg_clrDirSource_bg", 255, 254, 192), UIResourceManager.getColorGrey("voodooProgessDlg_clrDirSource_fg", 0)); //$NON-NLS-1$ //$NON-NLS-2$
		clrDirTarget= new TwoColours(UIResourceManager.getColor("voodooProgessDlg_clrDirTarget_bg", 214, 255, 214), UIResourceManager.getColorGrey("voodooProgessDlg_clrDirTarget_fg", 0)); //$NON-NLS-1$ //$NON-NLS-2$
		clrRmdir= new TwoColours(UIResourceManager.getColor("voodooProgessDlg_clrRmdir_bg", 255, 232, 232), UIResourceManager.getColorGrey("voodooProgessDlg_clrRmdir_fg", 0)); //$NON-NLS-1$ //$NON-NLS-2$
		clrFailed= new TwoColours(null, UIResourceManager.getColor("voodooProgessDlg_clrFailed_fg", 212, 0, 0)); //$NON-NLS-1$

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
		int w= RuntimeConfig.voodooWndWidth;
		int h= RuntimeConfig.voodooWndHeight;
		if (w < 16)
			w= (int) Math.min(1000, dca.width * .975);
		else if (w > dca.width)
			w= dca.width;
		if (h < 16)
			h= (int) Math.min(652, dca.height * .975);
		else if (h > dca.height)
			h= dca.height;
		shell.setSize(w, h);
		UIHelpers.centerInFrontOfParent(display, shell, parent.getBounds());
		lblOverallP.setText(""); //$NON-NLS-1$
		shell.setFocus();
	}

	// =============================================================================================== //
	// = Public
	// =============================================================================================== //

	public void open() {
		shell.open();
		UIHelpers.passControlToUiUntilShellClosed(shell);
	}

	public Shell getShell() {
		return shell;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void starting(final int dirCount, final int totalFiles_) {
		this.totalFiles= totalFiles_;
		if (totalFiles == 0)
			UIHelpers.showTanukiError(shell, "voodoo_err_nothingToProcess"); //$NON-NLS-1$
		else
			display.syncExec(new Runnable() {
				public void run() {
					currentFileNumber= 0;
					UIHelpers.configureProgressBar(pbOverall, 0, totalFiles, 0);
					consoleWriteLn(I18n.l("voodoo_consoletxt_started", dirCount, totalFiles)); //$NON-NLS-1$
					if (!UIHelpers.showOkCancelBox(shell, SWT.ICON_INFORMATION, I18n.l("general_app_title"), I18n.l("voodoo_txt_confirmMsg", dirCount, totalFiles))) { //$NON-NLS-1$ //$NON-NLS-2$
						consoleWriteLn();
						consoleWriteLn(I18n.l("voodoo_consoletxt_cancelled")); //$NON-NLS-1$
						cancelled= true;
					}
				}
			});
	}

	public void nextDir(final String srcDir, final String targetDir, final int fileCount) {
		display.syncExec(new Runnable() {
			public void run() {
				consoleWriteLn();
				consoleWriteLn(false, "voodoo_consoletxt_dirSource", srcDir, clrDirSource, true); //$NON-NLS-1$
				if (targetDir != null)
					consoleWriteLn(true, "voodoo_consoletxt_dirTarget", targetDir, clrDirTarget, true); //$NON-NLS-1$
				console.setTopIndex(consoleLines);
			}
		});
	}

	public void nextFile() {
		display.syncExec(new Runnable() {
			public void run() {
				pbOverall.setSelection(currentFileNumber++);
				lblOverallP.setText(I18n.l("voodoo_txt_progress", currentFileNumber, totalFiles)); //$NON-NLS-1$
			}
		});
	}

	public void deleting(final File file) {
		display.syncExec(new Runnable() {
			public void run() {
				consoleWrite(true, "voodoo_consoletxt_fileDeleting", file.getName(), clrDelete, false); //$NON-NLS-1$
				consoleWrite(I18n.l("voodoo_consoletxt_fileCompleted_prefix")); //$NON-NLS-1$
			}
		});
	}

	public void moving(final File source, final File target) {
		display.syncExec(new Runnable() {
			public void run() {
				consoleWrite(true, "voodoo_consoletxt_fileMoving", source.getName(), clrMoveSource, false, target.getName(), clrMoveTarget, false); //$NON-NLS-1$
				consoleWrite(I18n.l("voodoo_consoletxt_fileCompleted_prefix")); //$NON-NLS-1$
			}
		});
	}

	public void fileOperationComplete(final int status) {
		display.syncExec(new Runnable() {
			public void run() {
				switch (status) {
				case SUCCEEDED:
					consoleWriteLn(I18n.l("voodoo_consoletxt_fileCompleted_ok")); //$NON-NLS-1$
					break;
				case FAILED:
					consoleWriteLn(I18n.l("voodoo_consoletxt_fileCompleted_failed"), clrFailed, true); //$NON-NLS-1$
					break;
				case SKIPPED:
					consoleWriteLn(I18n.l("voodoo_consoletxt_fileCompleted_skipped")); //$NON-NLS-1$
					break;
				}
				console.setTopIndex(consoleLines);
			}
		});
	}

	public void rmdirs(final List<File> removedDirs) {
		display.syncExec(new Runnable() {
			public void run() {
				for (File dir : removedDirs)
					consoleWriteLn(true, "voodoo_consoletxt_rmdir", dir.toString(), clrRmdir, false); //$NON-NLS-1$
				console.setTopIndex(consoleLines);
			}
		});
	}

	public void finished(final boolean aborted) {
		display.syncExec(new Runnable() {
			public void run() {
				if (totalFiles > 0) {
					pbOverall.setSelection(currentFileNumber);
					btnClose.setEnabled(true);
					btnClose.setFocus();
					shell.setDefaultButton(btnClose);
					btnClose.setFocus();
					consoleWriteLn();
					if (aborted)
						consoleWriteLn(I18n.l("voodoo_consoletxt_aborted")); //$NON-NLS-1$
					else
						consoleWriteLn(I18n.l("voodoo_consoletxt_finished")); //$NON-NLS-1$
					console.setTopIndex(consoleLines);
				}
				allowClose= true;
				if (totalFiles == 0)
					shell.close();
			}
		});
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private void consoleWrite(String txt) {
		console.append(txt);
		consoleIndex+= txt.length();
	}

	private void consoleWrite(String txt, TwoColours colours, boolean bold) {
		console.append(txt);
		applyConsoleStyle(txt, txt, colours, bold);
		consoleIndex+= txt.length();
	}

	private void consoleWrite(boolean indent, String i18nKey, String arg1, TwoColours colours1, boolean bold1) {
		final String txt= (indent ? CONSOLE_INDENT : "") + I18n.l(i18nKey, arg1); //$NON-NLS-1$
		console.append(txt);
		applyConsoleStyle(txt, arg1, colours1, bold1);
		consoleIndex+= txt.length();
	}

	private void consoleWrite(boolean indent, String i18nKey, String arg1, TwoColours colours1, boolean bold1, String arg2, TwoColours colours2, boolean bold2) {
		final String txt= (indent ? CONSOLE_INDENT : "") + I18n.l(i18nKey, arg1, arg2); //$NON-NLS-1$
		final String txt1= (indent ? CONSOLE_INDENT : "") + I18n.l(i18nKey, arg1, makeFakeString(arg2)); //$NON-NLS-1$
		final String txt2= (indent ? CONSOLE_INDENT : "") + I18n.l(i18nKey, makeFakeString(arg1), arg2); //$NON-NLS-1$
		console.append(txt);
		applyConsoleStyle(txt1, arg1, colours1, bold1);
		applyConsoleStyle(txt2, arg2, colours2, bold2);
		consoleIndex+= txt.length();
	}

	private void consoleWriteLn() {
		consoleWriteLn(""); //$NON-NLS-1$
	}

	private void consoleWriteLn(String txt) {
		console.append(txt + "\n"); //$NON-NLS-1$
		consoleIndex+= txt.length() + 1;
		consoleLines++;
	}

	private void consoleWriteLn(String txt, TwoColours colours, boolean bold) {
		consoleWrite(txt, colours, bold);
		consoleWriteLn();
	}

	private void consoleWriteLn(boolean indent, String i18nKey, String arg1, TwoColours colours1, boolean bold1) {
		consoleWrite(indent, i18nKey, arg1, colours1, bold1);
		consoleWriteLn();
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
}
