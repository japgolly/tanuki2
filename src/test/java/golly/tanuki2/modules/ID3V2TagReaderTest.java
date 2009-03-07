package golly.tanuki2.modules;

import golly.tanuki2.TestHelper;
import golly.tanuki2.support.RichRandomAccessFileCache;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Golly
 * @since 23/07/2007
 */
@SuppressWarnings("nls")
public class ID3V2TagReaderTest extends TestHelper {
	private String DATA_DIR= getTestResourcePath("tags");
	private ID3V2TagReader r= null;

	@Before
	public void setup() {
		r= new ID3V2TagReader();
	}

	@After
	public void teardown() {
		RichRandomAccessFileCache.getInstance().clear();
	}

	@Test
	public void id3v24InHeader() throws URISyntaxException {
		subtestParse(r, DATA_DIR + "/id3v2.4_header.mp3", "monkey", 1996, "ＩＤ３", "3", "id3v2 example");
		subtestParse(r, DATA_DIR + "/id3v2.4_header.mp3", "monkey", 1996, "ＩＤ３", "3", "id3v2 example");
	}

	@Test
	public void id3v23InHeader() throws URISyntaxException {
		subtestParse(r, DATA_DIR + "/id3v2.3_header.mp3", "一石二鳥", 2006, "Score: 20th Anniversary World Tour", "2", "Vacant");
		subtestParse(r, DATA_DIR + "/id3v2.3_header.mp3", "一石二鳥", 2006, "Score: 20th Anniversary World Tour", "2", "Vacant");
	}

	@Test
	public void id3v22InHeader() throws URISyntaxException {
		subtestParse(r, DATA_DIR + "/id3v2.2_header.mp3", "Cemetery Of Scream", 2005, "The Event Horizon", "10", "Where Next?");
		subtestParse(r, DATA_DIR + "/id3v2.2_header.mp3", "Cemetery Of Scream", 2005, "The Event Horizon", "10", "Where Next?");
	}
}
