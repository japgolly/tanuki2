package golly.tanuki2;

import golly.tanuki2.core.Engine;
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

			display= new Display();
			engine= new Engine();
			new AppWindow(display, engine).show();

		} finally {
			if (display != null)
				display.dispose();
		}
	}
}
