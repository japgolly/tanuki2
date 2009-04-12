package golly.tanuki2.support.ruby;

/**
 * This interface acts as a bridge between Java and test cases defined in Ruby. All Ruby test suites should implement
 * this interface.
 * 
 * @author Golly
 * @since 10/04/2009
 */
public interface IRubyTest {

	/**
	 * Returns a list of test names.
	 */
	String[] getTestNames();

	/**
	 * Invokes a test method. Doesn't call setup or teardown.
	 */
	void runTest(String name) throws Exception;

	/**
	 * This should be called once upon class initialisation, just like {@link org.junit.BeforeClass}.
	 */
	void beforeClass() throws Exception;

	/**
	 * This should be called once after before a test class is dereferenced, just like {@link org.junit.AfterClass}.
	 */
	void afterClass() throws Exception;

	/**
	 * This is called before every test method is run, just like {@link org.junit.Before}.
	 */
	void setup() throws Exception;

	/**
	 * This is called after every test method is run, just like {@link org.junit.After}.
	 */
	void teardown() throws Exception;
}
