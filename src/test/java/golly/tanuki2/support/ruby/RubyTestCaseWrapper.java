package golly.tanuki2.support.ruby;

import javax.script.ScriptContext;

import org.eclipse.swt.widgets.Display;

/**
 * This is the Java interface to all Tanuki2 ruby tests.
 * 
 * @author Golly
 * @since 11/04/2009
 */
@SuppressWarnings("nls")
public abstract class RubyTestCaseWrapper {
	private static final String RUBY_DIR_ROOT= "ruby";
	private static final Display DISPLAY= new Display();

	private final String filename;
	private final String encoding= "UTF-8";

	public RubyTestCaseWrapper(String filename) {
		this.filename= RUBY_DIR_ROOT + "/" + filename;
	}

	public String getFilename() {
		return filename;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setInitialContext(ScriptContext context) {
		context.setAttribute("TANUKI2", RUBY_DIR_ROOT, ScriptContext.GLOBAL_SCOPE);
		context.setAttribute("SWT_DISPLAY", DISPLAY, ScriptContext.ENGINE_SCOPE);
	}
}
