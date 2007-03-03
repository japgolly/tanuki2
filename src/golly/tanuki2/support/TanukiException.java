package golly.tanuki2.support;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Golly
 * @since 17/02/2007
 */
public class TanukiException extends Exception {
	private static final long serialVersionUID= -9194151115829165719L;
	private final String code;

	public TanukiException(String code, String logMessage) {
		this.code= code;
		Log.log(Log.ERROR, code + ": " + logMessage); //$NON-NLS-1$
	}

	public TanukiException(Throwable t) {
		t.printStackTrace(System.err);
		this.code= t.getClass().getSimpleName();
		try {
			StringWriter sw= new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			Log.log(Log.ERROR, MessageFormat.format("{0}: {1}\n{2}", code, t.getMessage(), sw.toString()).replaceAll("[\n\r ]+$", "").replace("\n", "\n\t")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		} catch (Throwable t2) {
		}
	}

	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return I18n.l("general_error_msg", getCode()); //$NON-NLS-1$
	}

	/**
	 * @see #showErrorDialog(String)
	 */
	public void showErrorDialog() {
		showErrorDialog(getMessage());
	}

	/**
	 * @see #showErrorDialog(Shell, String)
	 */
	public void showErrorDialog(Shell shell) {
		showErrorDialog(shell, getMessage());
	}

	/**
	 * Displays an error dialog showing the error to the user.
	 */
	public static void showErrorDialog(final String message) {
		showErrorDialog(null, message);
	}

	/**
	 * Displays an error dialog showing the error to the user. <br>
	 * If an error occurs opening up a dialog box, then the error message is instead sent to <em>stderr</em>.
	 */
	public static void showErrorDialog(Shell shell, final String message) {
		try {
			UIHelpers.showMessageBox(shell, SWT.ICON_ERROR, I18n.l("general_error_title"), message); //$NON-NLS-1$
		} catch (Throwable t) {
			System.err.println(message);
		}
	}
}
