package golly.tanuki2.support;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Golly
 * @since 17/02/2007
 */
public class UIHelpers {
	public static void addUnlessExists(Combo combo, String name) {
		for (String s : combo.getItems())
			if (name.equals(s))
				return;
		combo.add(name);
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

	public static void showGmtError(Shell shell, String i18nStringKey, Object... args) {
		showMessageBox(shell, SWT.ICON_ERROR, I18n.l("general_error_title"), I18n.l(i18nStringKey, args)); //$NON-NLS-1$
	}

	public static void showGmtWarning(Shell shell, String i18nStringKey, Object... args) {
		showMessageBox(shell, SWT.ICON_WARNING, I18n.l("general_error_title"), I18n.l(i18nStringKey, args)); //$NON-NLS-1$
	}

	public static boolean disableShowMessageBox= false;

	public static void showMessageBox(Shell shell, int iconType, String title, String message) {
		if (disableShowMessageBox)
			return;
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
				showMessageBox(shell, iconType, title, message);
			} finally {
				if (disposeDisplay)
					display.dispose();
			}
		} else {
			// Show
			MessageBox m= new MessageBox(shell, iconType);
			m.setText(title);
			m.setMessage(message);
			m.open();
		}
	}
}
