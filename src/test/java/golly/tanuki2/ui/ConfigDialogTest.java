package golly.tanuki2.ui;

import golly.tanuki2.support.ruby.RubyTestCaseRunner;
import golly.tanuki2.support.ruby.RubyTestCaseWrapper;

import org.junit.runner.RunWith;

/**
 * Tests for {@link ConfigDialog}.
 * 
 * @author Golly
 * @since 13/04/2009
 */
@RunWith(RubyTestCaseRunner.class)
public class ConfigDialogTest extends RubyTestCaseWrapper {

	@SuppressWarnings("nls")
	public ConfigDialogTest() {
		super("ui/config_dialog_test.rb");
	}
}
