package golly.tanuki2;

import golly.tanuki2.core.Engine;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.Config;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.Log;
import golly.tanuki2.support.TanukiException;
import golly.tanuki2.support.UIResourceManager;
import golly.tanuki2.ui.AppWindow;

import java.util.Locale;

import org.eclipse.swt.widgets.Display;

/**
 * @author Golly
 * @since 16/02/2007
 */
public class Startup {
	public static void main(String[] args) {
		new Startup().run();
	}

	public void run() {
		Display display= null;
		Engine engine= null;
		try {

			Log.init();
			Log.logStartup();
			I18n.setLocale(Locale.ENGLISH);
			Config.load();
			display= new Display();
			TanukiImage.setDisplay(display);
			engine= new Engine();
			engine.add("X:\\music\\1. Fresh\\IN FLAMES Discografia (www.heavytorrents.org)");// DELME
			Engine.PRETEND_MODE= true;
			new AppWindow(display, engine).show();

		} catch (Throwable t) {
			new TanukiException(t).showErrorDialog();

		} finally {
			Config.tryToSave();
			UIResourceManager.disposeAll();
			if (display != null)
				display.dispose();
		}
	}
}
