package golly.tanuki2.ui;

import golly.tanuki2.core.Engine;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Golly
 * @since 16/02/2007
 */
public class AppWindow {
	private final Display display;
	private final Shell shell;
	private final Engine engine;
	private final InputFileTree inputTreeView;

	public AppWindow(Display display_, Engine engine_) {
		display= display_;
		engine= engine_;
		shell= new Shell();
		FillLayout fl= new FillLayout();
		fl.marginHeight= fl.marginWidth= 4;
		shell.setLayout(fl);
		shell.setSize(600, 500);
		shell.setText("Tanuki 2");

		inputTreeView= new InputFileTree(shell);

		// DELME
		engine.addFolder("X:\\music\\1. Fresh\\Meshuggah - Discografia [heavytorrents.org]");
		engine.addFolder("X:\\music\\4. Done\\Unexpect");
		engine.addFolder("C:\\2\\Nevermore\\2004 Enemies of Reality");

		inputTreeView.refreshFiles(engine.dirs);
	}

	public void show() {
		shell.open();
		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
	}
}
