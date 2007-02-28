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
	public final Clipboard clipboard;
	public final Color deletionBkgColor, deletionFgColor;
	public final Color incompleteBkgColor, incompleteFgColor;
	public final Color nonAudioBkgColor, nonAudioFgColor;

	@SuppressWarnings("nls")
	public SharedUIResources(Display display, AppWindow appWindow) {
		this.appWindow= appWindow;
		clipboard= new Clipboard(display);
		deletionBkgColor= UIResourceManager.getColor("shared_deletion_bkg", 255, 240, 240);
		deletionFgColor= UIResourceManager.getColor("shared_deletion_fg", 255, 0, 0);
		incompleteBkgColor= UIResourceManager.getColor("shared_incomplete_bkg", 220, 220, 255);
		incompleteFgColor= UIResourceManager.getColor("shared_incomplete_fg", 0, 0, 255);
		nonAudioBkgColor= UIResourceManager.getColorGrey("shared_nonAudio_bkg", 240);
		nonAudioFgColor= UIResourceManager.getColorGrey("shared_nonAudio_fg", 0);
	}
}
