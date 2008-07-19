package golly.tanuki2;

import golly.tanuki2.support.Version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Golly
 * @since 16/07/2007
 */
@SuppressWarnings("nls")
public final class StaticConfig {

	public static final String COPYRIGHT;
	public static final String VERSION;
	public static final Version VERSION_VALUE;
	public static final String URL_TANUKI_HOMEPAGE;
	public static final String URL_LATEST_VERSION;
	public static final String URL_LASTFM= "http://www.last.fm/user/japgolly";

	static {
		final Properties p= loadProperties();
		COPYRIGHT= p.getProperty("copyright").replace("(c)", "\u00A9");
		VERSION= p.getProperty("version.name");
		VERSION_VALUE= new Version(p.getProperty("version.value"));
		URL_TANUKI_HOMEPAGE= p.getProperty("url.main");
		URL_LATEST_VERSION= p.getProperty("url.latest_version");
	}

	private static Properties loadProperties() {
		final String filename= "app.properties";
		
		// Open the properties file
		InputStream fin= StaticConfig.class.getResourceAsStream("/" + filename);
		if (fin == null)
			throw new IllegalStateException("File not found: " + filename);

		// Read and return
		final Properties p= new Properties();
		try {
			p.load(fin);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
		return p;
	}
}
