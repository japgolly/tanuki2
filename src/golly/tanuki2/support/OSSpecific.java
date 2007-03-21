package golly.tanuki2.support;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;

/**
 * @author Golly
 * @since 19/03/2007
 */
public final class OSSpecific {

	// =============================================================================================== //
	// = Current OS
	// =============================================================================================== //

	public static enum OS {
		WIN32, LINUX, MAC, OTHER
	}

	private static final OS os;

	static {
		final String osStr= SWT.getPlatform();
		if ("win32".equals(osStr)) //$NON-NLS-1$
			os= OS.WIN32;
		else if ("gtk".equals(osStr)) //$NON-NLS-1$
			os= OS.LINUX;
		else if ("carbon".equals(osStr)) //$NON-NLS-1$
			os= OS.MAC;
		else
			os= OS.OTHER;
	}

	// =============================================================================================== //
	// = Fields
	// =============================================================================================== //

	private static final Pattern pMakeFilenameSafe_naughtyChars= Pattern.compile("[\\\\/:*?<>|]"); //$NON-NLS-1$

	// =============================================================================================== //
	// = Methods
	// =============================================================================================== //

	private static void attemptsFailed() {
		UIHelpers.showTanukiWarning(null, "general_err_OSFunctionFailed_attemptsFailed"); //$NON-NLS-1$		
	}

	private static boolean exec(String cmd) {
		return exec(cmd, null);
	}

	private static boolean exec(String cmd, String dir) {
		if (cmd == null)
			return false;
		try {
			Runtime.getRuntime().exec(cmd, null, dir == null ? null : new File(dir));
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static String getEOL() {
		switch (os) {
		case MAC:
			return "\r"; //$NON-NLS-1$
		case WIN32:
			return "\r\n"; //$NON-NLS-1$
		default:
			return "\n"; //$NON-NLS-1$
		}
	}

	public static void launch(String fullFilename) {
		Program.launch(fullFilename);
	}

	/**
	 * Replaces all file-system-unsafe characters with safe alternatives.
	 */
	public static String makeFilenameSafe(String filename) {
		filename= filename.replace("\"", "''"); //$NON-NLS-1$ //$NON-NLS-2$
		return pMakeFilenameSafe_naughtyChars.matcher(filename).replaceAll("_"); //$NON-NLS-1$
	}

	public static void openBrowser(String url) {
		switch (os) {
		case WIN32:
			if (!Program.launch(url))
				attemptsFailed();
			break;
		default:
			Program p= Program.findProgram("html"); //$NON-NLS-1$
			if (p == null || !p.execute(url))
				if (!exec("firefox " + url)) //$NON-NLS-1$
					if (!exec("mozilla " + url)) //$NON-NLS-1$
						if (!exec("netscape " + url)) //$NON-NLS-1$
							attemptsFailed();
		}
	}

	public static void openFolder(String dir) {
		switch (os) {
		case LINUX:
			if (!exec("nautilus " + dir)) //$NON-NLS-1$
				if (!exec("gnome-open " + dir)) //$NON-NLS-1$
					if (!exec("kfm file:" + dir)) //$NON-NLS-1$
						if (!exec("konqueror file:" + dir)) //$NON-NLS-1$
							if (!exec("xfe " + dir)) //$NON-NLS-1$
								attemptsFailed();
			break;
		case WIN32:
			if (!exec("explorer.exe .", dir)) //$NON-NLS-1$
				attemptsFailed();
			break;
		default:
			unsupported();
		}
	}

	public static void openPrompt(String dir) {
		switch (os) {
		case LINUX:
			if (!exec("kde-terminal", dir)) //$NON-NLS-1$
				if (!exec("gnome-terminal", dir)) //$NON-NLS-1$
					if (!exec("konsole", dir)) //$NON-NLS-1$
						if (!exec("xterm", dir)) //$NON-NLS-1$
							attemptsFailed();
			break;
		case WIN32:
			if (!exec("cmd.exe /C start cmd.exe", dir)) //$NON-NLS-1$
				attemptsFailed();
			break;
		default:
			unsupported();
		}
	}

	private static void unsupported() {
		UIHelpers.showTanukiWarning(null, "general_err_OSFunctionFailed_unsupported"); //$NON-NLS-1$		
	}
}
