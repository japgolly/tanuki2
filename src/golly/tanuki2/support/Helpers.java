package golly.tanuki2.support;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

/**
 * @author Golly
 * @since 16/02/2007
 */
public final class Helpers {

	public static String addPathElement(final String path, final String name) {
		return path.length() == 0 ? name : path + File.separator + name;
	}

	public static String[] sort(final Set<String> data) {
		String[] r= data.toArray(new String[data.size()]);
		Arrays.sort(r);
		return r;
	}
}
