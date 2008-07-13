package golly.tanuki2.support;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @author Golly
 * @since 17/02/2007
 */
public class Log {
	private static class CustomFormatter extends Formatter {
		private final SimpleDateFormat datefmt= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS"); //$NON-NLS-1$

		public String format(LogRecord rec) {
			StringBuffer buf= new StringBuffer(1000);
			buf.append(rec.getLevel().getName().charAt(0));
			buf.append(" ["); //$NON-NLS-1$
			buf.append(datefmt.format(new Date(rec.getMillis())));
			buf.append("] "); //$NON-NLS-1$
			buf.append(formatMessage(rec));
			buf.append('\n');
			return buf.toString();
		}
	}

	private static class CustomLevel extends Level {
		private static final long serialVersionUID= 5002365420004545691L;

		public CustomLevel(String name, int value) {
			super(name, value);
		}
	}

	public static final Level FATAL= new CustomLevel("FATAL", 901); //$NON-NLS-1$
	public static final Level ERROR= new CustomLevel("ERROR", 701); //$NON-NLS-1$
	public static final Level WARN= new CustomLevel("WARN", 501); //$NON-NLS-1$
	public static final Level INFO= new CustomLevel("INFO", 301); //$NON-NLS-1$
	public static final Level DEBUG= new CustomLevel("DEBUG", 101); //$NON-NLS-1$

	private static Logger log= null;
	private static final String FILENAME= "log.txt"; //$NON-NLS-1$
	private static final String FULL_FILENAME= Helpers.addPathElements(OSSpecific.getTanukiSettingsDirectory(), FILENAME);
	private static final String STARTUP_LOG_ENTRY= "==================== STARTING ===================="; //$NON-NLS-1$

	private Log() {
		// Private constructor
	}
	
	public static void init() throws SecurityException, IOException {
		log= Logger.getLogger(""); //$NON-NLS-1$
		for (Handler h : log.getHandlers())
			log.removeHandler(h);
		
		FileHandler fh= new FileHandler(FULL_FILENAME, true);
		fh.setEncoding("UTF-8"); //$NON-NLS-1$
		fh.setLevel(Level.ALL);
		fh.setFormatter(new CustomFormatter());
		
		log.addHandler(fh);
		log.setLevel(Level.ALL);
	}

	public static Logger get() {
		return log;
	}

	public static void logStartup() {
		log(INFO, STARTUP_LOG_ENTRY);
	}

	public static void log(Level level, String msg) {
		get().log(level, msg);
	}
}
