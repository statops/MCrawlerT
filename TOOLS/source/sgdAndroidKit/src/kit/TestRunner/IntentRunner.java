package kit.TestRunner;

import java.io.File;

import junit.framework.TestResult;
import kit.Config.Config;
import kit.Intent.MCrawlerIntentReader;
import kit.Utils.SgUtils;

public class IntentRunner extends SgdFourmyStrategy {

	private static final String LAUNCHER_INTENT_PATH = SCPATH + File.separator
			+ Config.INTENT_XML;

	public IntentRunner(String pkg, Class activityClass) {
		super(pkg, activityClass);
	}

	@Override
	protected void setUp() throws Exception {
		readIntent();
		super.setUp();

	}

	/**
	 * read the intent pushed by job
	 */
	private void readIntent() {
		/**
		 * read intent.xml
		 */
		if (Config.DEBUG) {
			System.out
					.println("==================================================================");
			System.out.println("read " + LAUNCHER_INTENT_PATH);
			System.out
					.println("==================================================================");
		}

		launcherIntent = MCrawlerIntentReader.parse(new File(
				LAUNCHER_INTENT_PATH));

	}

	/* / */
	@Override
	public void runBare() {
		if (mScenarioData != null) {
			try {
				try {
					super.runBare();
				} catch (Throwable e) {

				}
			} catch (Exception e) {
				if (mScenarioData != null) {
					SgUtils.addError(mScenarioData, e.getMessage());
					mScenarioGenerator.generateXml(mScenarioData);
				}
				assertTrue(true);
			}
			return;
		}
		super.runBare();
	}

	@Override
	public TestResult run() {
		return super.run();
	}

	@Override
	public void runTestOnUiThread(Runnable r) {
		super.runTestOnUiThread(r);
	}
}
