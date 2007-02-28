package golly.tanuki2;

import java.util.Locale;

import golly.tanuki2.core.Engine;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIResourceManager;
import golly.tanuki2.ui.AppWindow;

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

			I18n.setLocale(Locale.ENGLISH);
			display= new Display();
			engine= new Engine();
			engine.addFolder("X:\\music\\1. Fresh\\IN FLAMES Discografia (www.heavytorrents.org)");// DELME
			new AppWindow(display, engine).show();

		} finally {
			UIResourceManager.disposeAll();
			if (display != null)
				display.dispose();
		}
	}
}
