package golly.tanuki2.tasks;

import golly.tanuki2.StaticConfig;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.OSSpecific;
import golly.tanuki2.support.UIHelpers;
import golly.tanuki2.support.Version;

import java.net.URL;

import org.eclipse.swt.SWT;

/**
 * @author Golly
 * @since 02/08/2007
 */
public class CheckForUpdatesTask implements Runnable {
	private final Thread thread;

	public CheckForUpdatesTask() {
		thread= new Thread(this);
	}

	public synchronized void start() {
		thread.start();
	}

	public synchronized void stop() {
		try {
			thread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void run() {
		try {
			byte[] buf= new byte[1024];
			new URL(StaticConfig.URL_LATEST_VERSION).openStream().read(buf);
			Version v= new Version(new String(buf, "ASCII")); //$NON-NLS-1$
			if (v.compareTo(StaticConfig.VERSION_VALUE) == 1)
				onNewVersionFound(v);

		} catch (Throwable e) {
			// Ignore
		}
	}

	private void onNewVersionFound(Version v) {
		// Wait a little (so it looks like the main screen loads up first)
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// Alert user
		if (UIHelpers.showYesNoBox(null, SWT.ICON_INFORMATION, I18n.l("autoUpdate_title_newVersionAvailable"), I18n.l("autoUpdate_txt_newVersionAvailable", StaticConfig.VERSION_VALUE.toString(), v.toString()))) { //$NON-NLS-1$ //$NON-NLS-2$
			// Open website
			OSSpecific.openBrowser(StaticConfig.URL_TANUKI_HOMEPAGE);
		}
	}
}
