package golly.tanuki2.ui;

import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIHelpers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Golly
 * @since 04/03/2007
 */
public class YesNoToAllBox {

	// =============================================================================================== //
	// = Public
	// =============================================================================================== //

	public static enum Value {
		YES, NO, YES_TO_ALL, NO_TO_ALL
	}

	public static Value show(Shell parent, String message) {
		return show(parent, I18n.l("general_app_title"), message); //$NON-NLS-1$
	}

	public static Value show(Shell parent, String message, Value defaultButton) {
		return show(parent, I18n.l("general_app_title"), message, defaultButton); //$NON-NLS-1$
	}

	public static Value show(Shell parent, String title, String message) {
		return new YesNoToAllBox(parent, title, message, Value.YES).show();
	}

	public static Value show(Shell parent, String title, String message, Value defaultButton) {
		return new YesNoToAllBox(parent, title, message, defaultButton).show();
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private final Display display;
	private final Shell shell, parent;
	private final Value defaultButtonValue;
	private Button defaultButton= null;
	private Value ret= null;

	private YesNoToAllBox(Shell parent, String title, String message, Value defaultButtonValue) {
		this.defaultButtonValue= defaultButtonValue;
		this.display= parent.getDisplay();
		this.parent= parent;
		this.shell= new Shell(parent, (SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL) & ~SWT.CLOSE);
		shell.setText(title);
		shell.setLayout(UIHelpers.makeGridLayout(2, false, 12, 24));

		Label lImg= new Label(shell, SWT.NONE);
		lImg.setLayoutData(UIHelpers.makeGridData(1, false, SWT.RIGHT, 1, false, SWT.CENTER));
		lImg.setImage(display.getSystemImage(SWT.ICON_QUESTION));

		Label lTxt= new Label(shell, SWT.NONE);
		lTxt.setLayoutData(UIHelpers.makeGridData(1, true, SWT.LEFT, 1, false, SWT.CENTER));
		lTxt.setText(message);

		// Add buttons
		Composite composite= new Composite(shell, SWT.NONE);
		composite.setLayoutData(UIHelpers.makeGridData(2, true, SWT.CENTER));
		composite.setLayout(UIHelpers.makeFillLayout(SWT.HORIZONTAL, 0, 16));
		createButton(composite, "general_btn_yes", Value.YES); //$NON-NLS-1$
		createButton(composite, "general_btn_yesToAll", Value.YES_TO_ALL); //$NON-NLS-1$
		createButton(composite, "general_btn_no", Value.NO); //$NON-NLS-1$
		createButton(composite, "general_btn_noToAll", Value.NO_TO_ALL); //$NON-NLS-1$

		// Add listener to block pre-mature close
		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				if (ret == null)
					event.doit= false;
			}
		});
	}

	public Value show() {
		shell.pack();
		UIHelpers.centerInFrontOfParent(display, shell, parent.getBounds());

		shell.open();

		if (defaultButton != null) {
			shell.setDefaultButton(defaultButton);
			defaultButton.setFocus();
		}

		UIHelpers.passControlToUiUntilShellClosed(shell);
		return ret;
	}

	private void createButton(Composite composite, String label, Value value) {
		final Button b= new Button(composite, SWT.PUSH);
		UIHelpers.setButtonText(b, label);
		b.setData(value);
		b.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				ret= (Value) event.widget.getData();
				shell.close();
			}
		});
		if (defaultButtonValue == value)
			defaultButton= b;
	}
}
