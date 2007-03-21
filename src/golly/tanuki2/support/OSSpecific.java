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

	private static boolean exec(String cmd, String... args) {
		return execInDir(null, cmd, args);
	}

	private static boolean execInDir(String dir, String cmd, String... args) {
		if (cmd == null)
			return false;

		// Make cmdarray
		final String[] cmdarray;
		if (args.length > 0) {
			int i= args.length;
			cmdarray= new String[i + 1];
			while (i-- > 0)
				cmdarray[i + 1]= args[i];
			cmdarray[0]= cmd;
		} else
			cmdarray= new String[] {cmd};

		// Execute
		try {
			Runtime.getRuntime().exec(cmdarray, null, dir == null ? null : new File(dir));
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

	@SuppressWarnings("nls")
	public static void openBrowser(String url) {
		switch (os) {
		case MAC:
			if (!exec("/usr/bin/open", url))
				attemptsFailed();
			break;
		case WIN32:
			if (!Program.launch(url))
				attemptsFailed();
			break;
		default:
			Program p= Program.findProgram("html");
			if (p == null || !p.execute(url))
				if (!exec("firefox", url))
					if (!exec("mozilla", url))
						if (!exec("netscape", url))
							attemptsFailed();
		}
	}

	@SuppressWarnings("nls")
	public static void openFolder(String dir) {
		switch (os) {
		case LINUX:
			if (!exec("nautilus", dir))
				if (!exec("gnome-open", dir))
					if (!exec("kfm", "file:" + dir))
						if (!exec("konqueror", "file:" + dir))
							if (!exec("xfe", dir))
								attemptsFailed();
			break;
		case MAC:
			if (!exec("/usr/bin/open", dir))
				attemptsFailed();
			break;
		case WIN32:
			if (!execInDir(dir, "explorer.exe", "."))
				attemptsFailed();
			break;
		default:
			unsupported();
		}
	}

	@SuppressWarnings("nls")
	public static void openPrompt(String dir) {
		switch (os) {
		case LINUX:
			if (!execInDir(dir, "kde-terminal"))
				if (!execInDir(dir, "gnome-terminal"))
					if (!execInDir(dir, "konsole"))
						if (!execInDir(dir, "xterm"))
							attemptsFailed();
			break;
		case MAC:
			if (!execInDir(dir, "/Applications/Utilities/Terminal.app/Contents/MacOS/Terminal"))
				attemptsFailed();
			break;
		case WIN32:
			if (!execInDir(dir, "cmd.exe", "/C start cmd.exe"))
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
