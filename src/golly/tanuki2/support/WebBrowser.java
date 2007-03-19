package golly.tanuki2.support;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Golly
 * @since 08/03/2007
 */
public class WebBrowser {

	/**
	 * Returns a URL for a google search.
	 */
	public static String getGoogleSearchUrl(String... searchTerms) {
		try {
			return "http://www.google.com/search?ie=utf-8&oe=utf-8&q=" + URLEncoder.encode(Helpers.join(searchTerms, " "), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Opens a page in the default web browser.
	 */
	public static void open(String url) {
		OSSpecific.openBrowser(url);
	}

}
