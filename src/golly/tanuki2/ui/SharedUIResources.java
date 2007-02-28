package golly.tanuki2.ui;

import golly.tanuki2.support.UIResourceManager;
import golly.tanuki2.support.UIHelpers.TwoColours;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;

/**
 * @author Golly
 * @since 19/02/2007
 */
public class SharedUIResources {
	public final AppWindow appWindow;
	public final Clipboard clipboard;
	public final TwoColours deletionColours, incompleteFileColours, nonAudioFileColours;

	@SuppressWarnings("nls")
	public SharedUIResources(Display display, AppWindow appWindow) {
		this.appWindow= appWindow;
		clipboard= new Clipboard(display);
		deletionColours= new TwoColours(UIResourceManager.getColor("shared_deletion_bkg", 255, 237, 237), UIResourceManager.getColor("shared_deletion_fg", 250, 0, 0));
		incompleteFileColours= new TwoColours(UIResourceManager.getColor("shared_incomplete_bkg", 220, 220, 255), UIResourceManager.getColor("shared_incomplete_fg", 0, 0, 250));
		nonAudioFileColours= new TwoColours(UIResourceManager.getColorGrey("shared_nonAudio_bkg", 240), UIResourceManager.getColorGrey("shared_nonAudio_fg", 0x50));
	}
}
