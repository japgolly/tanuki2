package golly.tanuki2.support;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author Golly
 * @since 17/02/2007
 */
public class I18n {
	private static final String BUNDLE_ID= "golly.tanuki2.res.strings"; //$NON-NLS-1$
	private static ResourceBundle res= null;

	public static void setLocale(Locale locale) {
		Locale.setDefault(locale);
		res= ResourceBundle.getBundle(BUNDLE_ID, locale);
	}

	/**
	 * Returns a localised string.
	 */
	public static String l(String key) {
		return res.getString(key);
	}

	/**
	 * Returns a localised string and formats with specified args.
	 */
	public static String l(String key, Object... args) {
		return args.length == 0 ? l(key) : MessageFormat.format(l(key), args);
	}

	/**
	 * Returns a localised string, or a fallback value if the given key doesn't exist in the bundle.
	 */
	public static String ltry(String key, String fallback) {
		try {
			return l(key);
		} catch (MissingResourceException e) {
			return fallback;
		}
	}
}
