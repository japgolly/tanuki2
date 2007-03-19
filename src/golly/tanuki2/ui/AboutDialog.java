package golly.tanuki2.ui;

import golly.tanuki2.Tanuki;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.UIHelpers;
import golly.tanuki2.support.WebBrowser;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Golly
 * @since 16/03/2007
 */
public class AboutDialog {
	public static final String WEBSITE_LASTFM= "http://www.last.fm/user/japgolly"; //$NON-NLS-1$
	public static final String WEBSITE_TANUKI= "http://software.davidbarri.com"; //$NON-NLS-1$

	private final Shell shell;

	public AboutDialog(Shell parent) {
		shell= new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setLayout(UIHelpers.makeGridLayout(2, false, 14, 10));
		shell.setText(I18n.l("general_app_title")); //$NON-NLS-1$
		shell.setImage(TanukiImage.TANUKI.get());

		Label l= new Label(shell, SWT.NONE);
		l.setLayoutData(UIHelpers.makeGridData(1, false, SWT.LEFT));
		l.setImage(TanukiImage.TANUKI.get());

		l= new Label(shell, SWT.NONE);
		l.setLayoutData(UIHelpers.makeGridData(1, true, SWT.LEFT));
		l.setText(I18n.l("about_txt_appWithVersion", Tanuki.VERSION)); //$NON-NLS-1$

		l= new Label(shell, SWT.NONE);
		l.setLayoutData(UIHelpers.makeGridData(2, true, SWT.LEFT));
		l.setText(I18n.l("about_txt_copyright")); //$NON-NLS-1$

		Link link= new Link(shell, SWT.NONE);
		link.setLayoutData(UIHelpers.makeGridData(2, true, SWT.LEFT));
		link.setText(I18n.l("about_txt_websiteTanuki", "<a>" + WEBSITE_TANUKI + "</a>")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		link.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				WebBrowser.open(event.text);
			}
		});

		link= new Link(shell, SWT.NONE);
		link.setLayoutData(UIHelpers.makeGridData(2, true, SWT.LEFT));
		link.setText(I18n.l("about_txt_websiteLastfm", "<a>" + WEBSITE_LASTFM + "</a>")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		link.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				WebBrowser.open(event.text);
			}
		});

		Button btn= new Button(shell, SWT.PUSH);
		GridData gd= UIHelpers.makeGridData(2, true, SWT.CENTER);
		gd.verticalIndent= 14;
		btn.setLayoutData(gd);
		UIHelpers.setButtonText(btn, "general_btn_ok"); //$NON-NLS-1$
		shell.setDefaultButton(btn);
		btn.setFocus();
		btn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				shell.close();
			}
		});

		shell.pack();
		UIHelpers.centerInFrontOfParent(shell.getDisplay(), shell, parent.getBounds());
	}

	public void show() {
		shell.open();
		UIHelpers.passControlToUiUntilShellClosed(shell);
	}
}
