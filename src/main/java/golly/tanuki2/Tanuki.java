package golly.tanuki2;

import golly.tanuki2.core.Engine;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.Log;
import golly.tanuki2.support.OSSpecific;
import golly.tanuki2.support.RuntimeConfig;
import golly.tanuki2.support.TanukiException;
import golly.tanuki2.support.TanukiImage;
import golly.tanuki2.support.UIResourceManager;
import golly.tanuki2.tasks.CheckForUpdatesTask;
import golly.tanuki2.ui.AppWindow;

import java.util.Locale;

import org.eclipse.swt.widgets.Display;

/**
 * @author Golly
 * @since 16/02/2007
 */
public class Tanuki {

	public static void main(String[] args) {
		new Tanuki().run();
	}

	public void run() {
		Display display= null;
		Engine engine= null;
		CheckForUpdatesTask checkForUpdatesTask= null;
		try {

			// Init
			Helpers.mkdir(OSSpecific.getTanukiSettingsDirectory());
			Log.init();
			Log.logStartup();
			I18n.setLocale(Locale.ENGLISH);
			RuntimeConfig.load();
			display= new Display();
			TanukiImage.setDisplay(display);
			engine= new Engine();

			// Check for new version
			if (RuntimeConfig.getInstance().checkVersionOnStartup)
				(checkForUpdatesTask= new CheckForUpdatesTask()).start();

			// Start app
			new AppWindow(display, engine).show();

		} catch (Throwable t) {
			new TanukiException(t).showErrorDialog();

		} finally {
			RuntimeConfig.tryToSave();
			if (checkForUpdatesTask != null)
				checkForUpdatesTask.stop();
			UIResourceManager.disposeAll();
			if (display != null)
				display.dispose();
		}
	}
}
