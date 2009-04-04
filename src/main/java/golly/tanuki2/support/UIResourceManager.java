package golly.tanuki2.support;

import java.util.HashMap;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Display;

/**
 * @author Golly
 * @since 19/01/2007
 */
public final class UIResourceManager {
	private static final HashMap<String, Resource> resources= new HashMap<String, Resource>();

	public static void add(String id, Resource r) {
		resources.put(id, r);
	}

	public static Resource get(String id) {
		return resources.get(id);
	}

	public static Color getColor(String id, int r, int g, int b) {
		Color c= (Color) resources.get(id);
		if (c == null) {
			add(id, c= new Color(Display.getCurrent(), r, g, b));
		}
		return c;
	}

	public static Color getColorGrey(String id, int i) {
		return getColor(id, i, i, i);
	}

	public static Font getFont(String id, Font baseFont, int style) {
		Font f= (Font) get(id);
		if (f == null) {
			f= UIHelpers.createFont(baseFont, style);
			add(id, f);
		}
		return f;
	}

	public static synchronized void disposeAll() {
		for (Resource r : resources.values()) {
			r.dispose();
		}
		resources.clear();
	}
}
