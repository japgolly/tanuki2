package golly.tanuki2.ui;

import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIHelpers;

import java.io.File;

import org.eclipse.swt.SWT;
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
	private final Display display;
	private final Shell shell;
	private final Label lblDir1, lblDir2, lblDirP;
	private final ProgressBar pbDirs;
	private final Label lblFile1, lblFile2, lblFileP;
	private final ProgressBar pbFiles;
	private final Label lblOverall1, lblOverallP;
	private final ProgressBar pbOverall;
	private final Button btnClose;

	private boolean running= false;
	private int dirCount, fileCount;
	private int totalFiles;
	private int currentDirNumber, currentFileNumber, overallFileNumber;

	public VoodooProgressDialog(Shell parent) {
		this.display= parent.getDisplay();
		this.shell= new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		shell.setLayout(UIHelpers.makeFillLayout(SWT.VERTICAL, 10, 20));
		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				if (running)
					event.doit= false;
			}
		});
		Composite composite;

		// Dir progress
		composite= new Composite(shell, SWT.NONE);
		composite.setLayout(UIHelpers.makeGridLayout(2, false, 0, 4));
		lblDir1= new Label(composite, SWT.LEFT);
		lblDir1.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		lblDirP= new Label(composite, SWT.RIGHT);
		lblDirP.setLayoutData(UIHelpers.makeGridData(1, false, SWT.RIGHT, 2, true, SWT.BOTTOM));
		lblDir2= new Label(composite, SWT.LEFT);
		lblDir2.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		pbDirs= new ProgressBar(composite, SWT.HORIZONTAL | SWT.SMOOTH);
		pbDirs.setLayoutData(UIHelpers.makeGridData(2, true, SWT.FILL));

		// File progress
		composite= new Composite(shell, SWT.NONE);
		composite.setLayout(UIHelpers.makeGridLayout(2, false, 0, 4));
		lblFile1= new Label(composite, SWT.LEFT);
		lblFile1.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		lblFileP= new Label(composite, SWT.RIGHT);
		lblFileP.setLayoutData(UIHelpers.makeGridData(1, false, SWT.RIGHT, 2, true, SWT.BOTTOM));
		lblFile2= new Label(composite, SWT.LEFT);
		lblFile2.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		pbFiles= new ProgressBar(composite, SWT.HORIZONTAL | SWT.SMOOTH);
		pbFiles.setLayoutData(UIHelpers.makeGridData(2, true, SWT.FILL));

		// Overall progress
		composite= new Composite(shell, SWT.NONE);
		composite.setLayout(UIHelpers.makeGridLayout(2, false, 0, 4));
		lblOverall1= new Label(composite, SWT.LEFT);
		lblOverall1.setLayoutData(UIHelpers.makeGridData(1, true, SWT.FILL));
		lblOverall1.setText(I18n.l("voodoo_txt_overall")); //$NON-NLS-1$
		lblOverallP= new Label(composite, SWT.RIGHT);
		lblOverallP.setLayoutData(UIHelpers.makeGridData(1, false, SWT.RIGHT));
		pbOverall= new ProgressBar(composite, SWT.HORIZONTAL | SWT.SMOOTH);
		pbOverall.setLayoutData(UIHelpers.makeGridData(2, true, SWT.FILL));

		// Close button
		composite= new Composite(shell, SWT.NONE);
		composite.setLayout(UIHelpers.makeGridLayout(1, false, 0, 4));
		btnClose= new Button(composite, SWT.PUSH);
		btnClose.setLayoutData(UIHelpers.makeGridData(1, true, SWT.CENTER));
		btnClose.setText(I18n.l("voodoo_btn_close")); //$NON-NLS-1$
		btnClose.setEnabled(false);
		btnClose.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				shell.close();
			}
		});

		lblFileP.setText("xxxx/xxxx"); //$NON-NLS-1$
		lblDirP.setText("xxxx/xxxx"); //$NON-NLS-1$
		lblOverallP.setText("xxxxx/xxxxx"); //$NON-NLS-1$
		shell.pack();
		UIHelpers.setWidth(shell, (int) Math.min(1100, display.getClientArea().width * .975));
		UIHelpers.centerInFrontOfParent(display, shell, display.getClientArea());
		lblFileP.setText(""); //$NON-NLS-1$
		lblDirP.setText(""); //$NON-NLS-1$
		lblOverallP.setText(""); //$NON-NLS-1$
	}

	public Shell getShell() {
		return shell;
	}

	public void starting(int dirCount, int totalFiles) {
		running= true;
		this.dirCount= dirCount;
		this.totalFiles= totalFiles;
		currentDirNumber= overallFileNumber= 0;
		shell.open();
		UIHelpers.configureProgressBar(pbDirs, 0, dirCount, 0);
		UIHelpers.configureProgressBar(pbOverall, 0, totalFiles, 0);
	}

	public void nextDir(String srcDir, String targetDir, int fileCount) {
		this.fileCount= fileCount;
		UIHelpers.configureProgressBar(pbFiles, 0, fileCount, 0);
		currentFileNumber= 0;
		lblFile1.setText(""); //$NON-NLS-1$
		lblFile2.setText(""); //$NON-NLS-1$
		lblFileP.setText(""); //$NON-NLS-1$

		pbDirs.setSelection(currentDirNumber++);
		if (targetDir == null) {
			lblDir1.setText(""); //$NON-NLS-1$
			lblDir2.setText(I18n.l("voodoo_txt_dirSource", srcDir)); //$NON-NLS-1$
		} else {
			lblDir1.setText(I18n.l("voodoo_txt_dirSource", srcDir)); //$NON-NLS-1$
			lblDir2.setText(I18n.l("voodoo_txt_dirTarget", targetDir)); //$NON-NLS-1$
		}
		lblDirP.setText(I18n.l("voodoo_txt_progress", currentDirNumber, dirCount)); //$NON-NLS-1$
	}

	public void nextFile() {
		pbFiles.setSelection(currentFileNumber++);
		pbOverall.setSelection(overallFileNumber++);
		lblFileP.setText(I18n.l("voodoo_txt_progress", currentFileNumber, fileCount)); //$NON-NLS-1$
		lblOverallP.setText(I18n.l("voodoo_txt_progress", overallFileNumber, totalFiles)); //$NON-NLS-1$
		lblFile1.setText(""); //$NON-NLS-1$
		lblFile2.setText(""); //$NON-NLS-1$
	}

	public void deleting(File file) {
		lblFile1.setText(""); //$NON-NLS-1$
		lblFile2.setText(I18n.l("voodoo_txt_fileDeleting", file.getName())); //$NON-NLS-1$
		sleep();
	}

	public void moving(File source, File target) {
		if (source.getName().equals(target.getName())) {
			lblFile1.setText(""); //$NON-NLS-1$
			lblFile2.setText(I18n.l("voodoo_txt_fileMovingA", source.getName())); //$NON-NLS-1$
		} else {
			lblFile1.setText(I18n.l("voodoo_txt_fileMovingA", source.getName())); //$NON-NLS-1$
			lblFile2.setText(I18n.l("voodoo_txt_fileMovingB", target.getName())); //$NON-NLS-1$
		}
		sleep();
	}

	public void finished() {
		pbFiles.setSelection(currentFileNumber);
		pbDirs.setSelection(currentDirNumber);
		pbOverall.setSelection(overallFileNumber);
		btnClose.setEnabled(true);
		btnClose.setFocus();
		shell.setDefaultButton(btnClose);

		while (!shell.isDisposed())
			if (!display.readAndDispatch()) {
				running= false;
				display.sleep();
			}
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
