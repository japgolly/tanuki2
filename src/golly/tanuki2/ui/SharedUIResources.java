package golly.tanuki2.ui;

import golly.tanuki2.support.UIResourceManager;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * @author Golly
 * @since 19/02/2007
 */
public class SharedUIResources {
	public final AppWindow appWindow;
	public final Color nonAudioBkgColor;
	public final Color incompleteBkgColor;
	public final Clipboard clipboard;

	public SharedUIResources(Display display, AppWindow appWindow) {
		this.appWindow= appWindow;
		nonAudioBkgColor= UIResourceManager.getColorGrey("shared_nonAudio_bkg", 234); //$NON-NLS-1$
		incompleteBkgColor= UIResourceManager.getColor("shared_incomplete_bkg", 255, 225, 225); //$NON-NLS-1$
		clipboard= new Clipboard(display);
	}
}
