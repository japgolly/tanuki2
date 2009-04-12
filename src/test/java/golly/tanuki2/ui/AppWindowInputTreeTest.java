package golly.tanuki2.ui;

import golly.tanuki2.support.ruby.RubyTestCaseRunner;
import golly.tanuki2.support.ruby.RubyTestCaseWrapper;

import org.junit.runner.RunWith;

/**
 * Tests for {@link InputTree}.
 * 
 * @author Golly
 * @since 10/04/2009
 */
@RunWith(RubyTestCaseRunner.class)
public class AppWindowInputTreeTest extends RubyTestCaseWrapper {

	@SuppressWarnings("nls")
	public AppWindowInputTreeTest() {
		super("ui/app_window_input_tree_test.rb");
	}
}
