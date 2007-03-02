package golly.tanuki2.ui;

import golly.tanuki2.support.UIResourceManager;
import golly.tanuki2.support.UIHelpers.TwoColours;
import golly.tanuki2.ui.AppWindow.AppUIShared;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;

/**
 * @author Golly
 * @since 19/02/2007
 */
public class SharedUIResources {
	public final AppUIShared appUIShared;
	public final Clipboard clipboard;
	public final TwoColours deletionColours, itemCompleteColours, itemIncompleteColours, nonAudioFileColours;

	@SuppressWarnings("nls")
	public SharedUIResources(Display display, AppUIShared appUIShared) {
		this.appUIShared= appUIShared;
		clipboard= new Clipboard(display);
		deletionColours= new TwoColours(UIResourceManager.getColor("shared_deletion_bkg", 255, 237, 237), UIResourceManager.getColor("shared_deletion_fg", 250, 0, 0));
		itemCompleteColours= new TwoColours(UIResourceManager.getColorGrey("shared_complete_bkg", 255), UIResourceManager.getColorGrey("shared_complete_fg", 1));
		itemIncompleteColours= new TwoColours(UIResourceManager.getColor("shared_incomplete_bkg", 220, 220, 255), UIResourceManager.getColor("shared_incomplete_fg", 0, 0, 250));
		nonAudioFileColours= new TwoColours(UIResourceManager.getColorGrey("shared_nonAudio_bkg", 240), UIResourceManager.getColorGrey("shared_nonAudio_fg", 0x50));
	}
}
