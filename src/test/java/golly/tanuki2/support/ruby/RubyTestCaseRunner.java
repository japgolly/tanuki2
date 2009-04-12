package golly.tanuki2.support.ruby;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

/**
 * Uses {@link RubyTestCaseWrapper}s to create instances of {@link IRubyTest} and then runs those.
 * 
 * @author Golly
 * @since 11/04/2009
 */
@SuppressWarnings("nls")
public class RubyTestCaseRunner extends Runner {
	private final Description suiteDesc;
	private final Map<String, Description> testNameToDescMap;
	private final RubyTestCaseWrapper wrapper;
	private final ScriptEngineManager scriptEngineManager;
	private final ScriptEngine rubyEngine;
	private final ScriptContext scriptContext;
	private final Invocable invocableEngine;
	private final IRubyTest rubyTest;

	public RubyTestCaseRunner(Class<? extends RubyTestCaseWrapper> testClass) throws InstantiationException, IllegalAccessException, FileNotFoundException, UnsupportedEncodingException, ScriptException {
		super();
		this.wrapper= testClass.newInstance();

		// Prepare scripting engine
		this.scriptEngineManager= new ScriptEngineManager();
		this.rubyEngine= scriptEngineManager.getEngineByName("jruby");
		this.scriptContext= rubyEngine.getContext();
		this.invocableEngine= (Invocable) rubyEngine;
		wrapper.setInitialContext(scriptContext);

		// Load script
		final ClassLoader classLoader= Thread.currentThread().getContextClassLoader();
		InputStream is= classLoader.getResourceAsStream(wrapper.getFilename());
		if (is == null) {
			throw new FileNotFoundException(wrapper.getFilename() + " not found.");
		}
		Object rubyObject= rubyEngine.eval(new InputStreamReader(is, wrapper.getEncoding()), scriptContext);
		this.rubyTest= invocableEngine.getInterface(rubyObject, IRubyTest.class);

		// Discover and register tests
		this.suiteDesc= Description.createSuiteDescription(testClass);
		Map<String, Description> testNameToDescMap= new LinkedHashMap<String, Description>();
		for (String testName : rubyTest.getTestNames()) {
			final Description desc= Description.createTestDescription(testClass, testName);
			suiteDesc.addChild(desc);
			testNameToDescMap.put(testName, desc);
		}
		this.testNameToDescMap= Collections.unmodifiableMap(testNameToDescMap);
	}

	@Override
	public Description getDescription() {
		return suiteDesc;
	}

	@Override
	public void run(RunNotifier notifier) {
		// BeforeClass
		try {
			rubyTest.beforeClass();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Run tests
		boolean errorThrown= true;
		try {
			run2(notifier);
			errorThrown= false;
		}

		// AfterClass
		finally {
			try {
				rubyTest.afterClass();
			} catch (Exception e) {
				if (errorThrown) {
					e.printStackTrace();
				} else {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void run2(RunNotifier notifier) {
		for (final Entry<String, Description> entry : testNameToDescMap.entrySet()) {
			final String name= entry.getKey();
			final Description desc= entry.getValue();
			boolean tearDown= false;
			boolean failed= false;
			try {
				// Start
				notifier.fireTestStarted(desc);
				try {

					// Setup
					rubyTest.setup();
					tearDown= true;

					// Run test
					rubyTest.runTest(name);
					notifier.fireTestFinished(desc);

				} catch (Exception e) {
					// Test failed
					notifier.fireTestFailure(new Failure(desc, e));
					failed= true;
				}
			}

			// Teardown
			finally {
				if (tearDown) {
					try {
						rubyTest.teardown();
					} catch (Exception e) {
						// Only report a teardown failure if the test passed
						if (!failed) {
							notifier.fireTestFailure(new Failure(desc, e));
						}
					}
				}
			}
		}
	}
}
