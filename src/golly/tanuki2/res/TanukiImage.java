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
	COPY("edit-copy.png"), //
	EDITOR("accessories-text-editor.png"), //
	EXPLORER("system-file-manager.png"), //
	FOLDER("folder.png"), //
	MIME_AUDIO("audio-x-generic.png"), //
	MIME_IMAGE("image-x-generic.png"), //
	MIME_TEXT("text-x-generic.png"), //
	REMOVE("list-remove.png"), //
	TANUKI("tanuki.ico"), //
	TERMINAL("utilities-terminal.png"), //
	VOODOO("voodoo.png"), //
	;

	// ====== STATIC ======

	private static Display display= null;

	public static void setDisplay(Display display_) {
		display= display_;
	}

	// ====== INTERNAL ======

	private final String filename;
	private Image img= null;

	private TanukiImage(String filename) {
		this.filename= filename;
	}

	// ====== PUBLIC ======

	public Image get() {
		if (img == null) {
			img= new Image(display, TanukiImage.class.getResourceAsStream(filename));
			UIResourceManager.add(TanukiImage.class.getCanonicalName() + filename, img);
		}
		return img;
	}
}
