package golly.tanuki2.support;

import golly.tanuki2.support.AutoResizeColumnsListener.WidgetWithColumns;
import golly.tanuki2.support.AutoResizeColumnsListener.WidgetWithColumns_Table;
import golly.tanuki2.support.AutoResizeColumnsListener.WidgetWithColumns_Tree;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

/**
 * @author Golly
 * @since 17/02/2007
 */
public class UIHelpers {
	public static abstract class RunnableWithShell implements Runnable {
		public Shell shell= null;
		public Object ret= null;

		public final void run() {
			run(shell);
		}

		public abstract void run(Shell shell);
	}

	public static class TwoColours {
		public Color background= null, foreground= null;

		public TwoColours() {
		}

		public TwoColours(Color background, Color foreground) {
			this.background= background;
			this.foreground= foreground;
		}
	}

	public static void addUnlessExists(Combo combo, String name) {
		for (String s : combo.getItems())
			if (name.equals(s))
				return;
		combo.add(name);
	}

	public static AutoResizeColumnsListener createAutoResizeColumnsListener(Table t) {
		return createAutoResizeColumnsListener(new WidgetWithColumns_Table(t), true);
	}

	public static AutoResizeColumnsListener createAutoResizeColumnsListener(Tree t) {
		return createAutoResizeColumnsListener(new WidgetWithColumns_Tree(t), true);
	}

	private static AutoResizeColumnsListener createAutoResizeColumnsListener(WidgetWithColumns wwc, boolean disableRedraw) {
		final AutoResizeColumnsListener listener= new AutoResizeColumnsListener(wwc);
		listener.disableRedraw= disableRedraw;
		return listener;
	}

	/**
	 * Repositions a window in front of its parent. It will align the center points of both windows, and then makes sure
	 * the window is in the display client area.
	 */
	public static void centerInFrontOfParent(Display display, Shell wnd, Rectangle parentBounds) {
		Rectangle dca= display.getClientArea();
		Point s= wnd.getSize();
		Point l= new Point(parentBounds.x + (parentBounds.width - s.x) / 2, parentBounds.y + (parentBounds.height - s.y) / 2);
		int d= (l.y + s.y) - (dca.y + dca.height);
		if (d > 0)
			l.y-= d;
		d= (l.x + s.x) - (dca.x + dca.width);
		if (d > 0)
			l.x-= d;
		if (l.x < dca.x)
			l.x= dca.x;
		if (l.y < dca.y)
			l.y= dca.y;
		wnd.setLocation(l);
	}

	public static void configureProgressBar(ProgressBar bar, int min, int max, int selection) {
		bar.setMinimum(min);
		bar.setMaximum(max);
		bar.setSelection(selection);
	}

	public static Font createFont(Font baseFont, int style) {
		FontData[] fds= baseFont.getFontData();
		for (FontData fd : fds)
			fd.setStyle(style);
		return new Font(Display.getCurrent(), fds);
	}

	public static FillLayout makeFillLayout(int type, int margin, int spacing) {
		final FillLayout layout= new FillLayout(type);
		layout.marginHeight= layout.marginWidth= margin;
		layout.spacing= spacing;
		return layout;
	}

	public static GridLayout makeGridLayout(int cols, boolean colsSameWidth, int margin, int spacing) {
		final GridLayout layout= new GridLayout(cols, colsSameWidth);
		layout.marginTop= layout.marginBottom= layout.marginLeft= layout.marginRight= margin;
		layout.horizontalSpacing= layout.verticalSpacing= spacing;
		return layout;
	}

	public static GridData makeGridData(int hspan, boolean grabhspace, int halign, int vspan, boolean grabvspace, int valign) {
		GridData gd= new GridData();
		gd.horizontalSpan= hspan;
		gd.verticalSpan= vspan;
		gd.grabExcessHorizontalSpace= grabhspace;
		gd.grabExcessVerticalSpace= grabvspace;
		gd.horizontalAlignment= halign;
		gd.verticalAlignment= valign;
		return gd;
	}

	public static GridData makeGridData(int colspan, boolean grabhspace, int halign) {
		return makeGridData(colspan, grabhspace, halign, 1, false, GridData.CENTER);
	}

	public static RowLayout makeRowLayout(int margin, int spacing, boolean pack, boolean wrap, boolean justify) {
		RowLayout layout= new RowLayout();
		layout.marginLeft= layout.marginTop= layout.marginRight= layout.marginBottom= margin;
		layout.spacing= spacing;
		layout.pack= pack;
		layout.wrap= wrap;
		layout.justify= justify;
		return layout;
	}

	public static void passControlToUiUntilShellClosed(Shell shell) {
		final Display display= shell.getDisplay();
		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
	}

	public static void runWithShell(Shell shell, RunnableWithShell runnable) {
		if (shell == null) {
			// Get a display
			final boolean disposeDisplay;
			Display display= Display.getCurrent();
			if (display == null) {
				display= new Display();
				disposeDisplay= true;
			} else
				disposeDisplay= false;
			try {
				// Get a shell
				shell= display.getActiveShell();
				if (shell == null)
					shell= new Shell(display);
				// Call self with shell
				runWithShell(shell, runnable);
			} finally {
				if (disposeDisplay)
					display.dispose();
			}
		} else {
			runnable.shell= shell;
			shell.getDisplay().syncExec(runnable);
		}
	}

	public static void selectItem(List list, String name) {
		int i= list.getItemCount();
		while (i-- > 0)
			if (list.getItem(i).equals(name)) {
				list.select(i);
				return;
			}
	}

	public static void setButtonText(Button button, String i18nStringKey) {
		button.setText("   " + I18n.l(i18nStringKey) + "   "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static void setHeight(Control control, int height) {
		control.setSize(control.getSize().x, height);
	}

	public static void setWidth(Control control, int width) {
		control.setSize(width, control.getSize().y);
	}

	public static int showAbortIgnoreRetryBox(Shell shell, final String title, final String message) {
		final RunnableWithShell r= new RunnableWithShell() {
			public void run(Shell shell) {
				MessageBox m= new MessageBox(shell, SWT.ICON_ERROR | SWT.ABORT | SWT.RETRY | SWT.IGNORE);
				m.setText(title);
				m.setMessage(message);
				ret= (Integer) m.open();
			}
		};
		runWithShell(shell, r);
		return (Integer) r.ret;
	}

	public static boolean showOkCancelBox(Shell shell, final int iconType, final String title, final String message) {
		final RunnableWithShell r= new RunnableWithShell() {
			public void run(Shell shell) {
				MessageBox m= new MessageBox(shell, iconType | SWT.OK | SWT.CANCEL);
				m.setText(title);
				m.setMessage(message);
				ret= (Boolean) (m.open() == SWT.OK);
			}
		};
		runWithShell(shell, r);
		return (Boolean) r.ret;
	}

	public static boolean disableShowMessageBox= false;

	public static void showMessageBox(Shell shell, final int iconType, final String title, final String message) {
		if (disableShowMessageBox)
			return;
		runWithShell(shell, new RunnableWithShell() {
			public void run(Shell shell) {
				MessageBox m= new MessageBox(shell, iconType);
				m.setText(title);
				m.setMessage(message);
				m.open();
			}
		});
	}

	public static void showTanukiError(Shell shell, String i18nStringKey, Object... args) {
		showMessageBox(shell, SWT.ICON_ERROR, I18n.l("general_error_title"), I18n.l(i18nStringKey, args)); //$NON-NLS-1$
	}

	public static void showTanukiWarning(Shell shell, String i18nStringKey, Object... args) {
		showMessageBox(shell, SWT.ICON_WARNING, I18n.l("general_error_title"), I18n.l(i18nStringKey, args)); //$NON-NLS-1$
	}
}
