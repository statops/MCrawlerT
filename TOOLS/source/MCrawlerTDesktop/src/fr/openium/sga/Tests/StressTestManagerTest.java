/**
 * 
 */
package fr.openium.sga.Tests;

import java.io.File;

import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;

import org.junit.Test;

import fr.openium.sga.SecurityTesting.StressTest.StressTaskManager;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;

/**
 * @author Stassia
 * 
 */
public class StressTestManagerTest {
	private static final String TAG = StressTestManagerTest.class.getName();

	@Test
	public void testInMethod() throws Exception {

		String[] params = new String[] { "stress", "-p",
				"/Users/Stassia/Documents/Scen-genWorkSpace/ConverterExample", "-tp",
				"/Users/Stassia/Documents/Scen-genWorkSpace/ConverterExampleTest", "-tpackage",
				"fr.openium.converterexample.test", "-sdk", "/Users/Stassia/android-sdks", "-out",
				new File("").getAbsolutePath() + "/outConverterStressAPath", "-arv",
				new File("").getAbsolutePath() + "/TestData/testData.xml", "-strategy", "1", "-thread", "1",
				"-launcherActivity", "fr.openium.converterexample.MainActivity", "-maxEvent", "500",
				"-class", "fr.openium.converterexample.test.Maintest", "-pApk",
				"/Users/Stassia/Documents/Scen-genWorkSpace/ConverterExample/bin/ConverterExample.apk",
				"-stopError", "true" };
		ScenarioData model = ScenarioParser.parse(new File(
				"/Users/Stassia/Documents/Scen-genWorkSpace/sgd/TEST/stressTask/out.xml"));
		SgdEnvironnement env = Emma.init_environment_(params);
		StressTaskManager stress = new StressTaskManager(model, new File(env.getAllTestDataPath()), env);
		stress.launchSecurityTestJob();

	}
}
