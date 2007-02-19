package golly.tanuki2.res;

import golly.tanuki2.support.UIResourceManager;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * @author Golly
 * @since 19/02/2007
 */
@SuppressWarnings("nls")
public enum TanukiImage {
	MIME_AUDIO("audio-x-generic.png"), //
	MIME_IMAGE("image-x-generic.png"), //
	MIME_TEXT("text-x-generic.png"), //
	FOLDER("folder.png"), //
	;

	private final String filename;
	private Image img= null;

	private TanukiImage(String filename) {
		this.filename= filename;
	}

	public Image get() {
		if (img == null) {
			img= new Image(Display.getCurrent(), TanukiImage.class.getResourceAsStream(filename));
			UIResourceManager.add(TanukiImage.class.getCanonicalName() + filename, img);
		}
		return img;
	}
}
