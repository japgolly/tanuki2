package golly.tanuki2;

import golly.tanuki2.core.ClipboardParserTest;
import golly.tanuki2.core.EngineTest;
import golly.tanuki2.data.DataTest;
import golly.tanuki2.modules.FilenameParserTest;
import golly.tanuki2.modules.ID3V2TagReaderTest;
import golly.tanuki2.support.HelperTest;
import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author Golly
 * @since 23/02/2007
 */
@RunWith(Suite.class)
@SuiteClasses({//
		HelperTest.class, //
		DataTest.class, //
		EngineTest.class, //
		ClipboardParserTest.class, //
		FilenameParserTest.class, //
		ID3V2TagReaderTest.class //
})
public class AllTests {
	public static Test suite() {
		return new JUnit4TestAdapter(AllTests.class);
	}
}