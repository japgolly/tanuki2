package golly.tanuki2.qa;

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
@SuiteClasses( {HelperTest.class, DataTest.class, FilenameParserTest.class, EngineTest.class})
public class AllTests {
	public static Test suite() {
		return new JUnit4TestAdapter(AllTests.class);
	}
}