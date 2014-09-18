package fr.openium.sga.Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import kit.Config.Config;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Utils.SgUtils;

import org.junit.Test;

import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTestTask;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager.SPReport;
import fr.openium.sga.SecurityTesting.BruteForceTest.BruteForceManager;
import fr.openium.sga.SecurityTesting.BruteForceTest.BruteForceTask;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;

public class DisplayerTest {
	
	@Test
	public void testBruteForceEbay() throws Exception {

		String[] params = new String[] {"test",
				"/home/stassia/Documents/ebay/output/outModel/Scenario/_out0.xml", "testapp",
				"/home/stassia/Documents/ebay/output/outModel1010/"+Config.ROBOTIUM_SCREENSHOTS,
				"/home/stassia/Documents/ebay/project/Ebay",
				"-tp",
				"/home/stassia/Documents/ebay/project/EbayTest",
				"-tpackage",
				"com.ebay.mobile.test",
				"-pPackage",
				"com.ebay.mobile",
				"-sdk",
				"/home/stassia/Documents/adt-bundle-linux-x86_64-20140321/sdk",
				"-out",
				"/home/stassia/Documents/ebay/output/BruteForceResultWithValidPasswd",
				"-arv",
				new File("").getAbsolutePath() + File.separator
						+ "TEST/bruteForce/testData.xml",
				"-strategy",
				"122",
				"-thread",
				"1",
				"-launcherActivity",
				"com.ebay.mobile.ebay",
				"-maxEvent",
				"100",
				"-class",
				"com.ebay.mobile.test.BruteTest",
				"-pApk",
				"/home/stassia/Documents/ebay/project/Ebay/appsigned.apk",
				"-stopError",
				"true",
				"-bruteDico",
				new File(Config.CURRENT_DIRECTORY + File.separator
						+ "TEST/bruteForce/dico").getPath() };
		
		ScenarioData model = ScenarioParser
				.parse(new File(
						"/home/stassia/Documents/ebay/output/outForBruteForce/Scenario/_out2.xml"));
		assertNotNull(model);
		SgdEnvironnement env = Emma.init_environment_(params);
		env.setModel(model);
		BruteForceManager brute = new BruteForceManager(env);
		HashSet<AbstractTestTask> taskList = brute.getGenerateTaskLists();
		assertEquals(2, taskList.size());
		assertTrue(SgUtils.isEqualState(taskList.iterator().next()
				.getTargetState(), taskList.iterator().next().getTargetState()));
		java.util.Iterator<AbstractTestTask> it = taskList.iterator();
		
		/**
		 * compare states are unique
		 * 
		 */
		for (AbstractTestTask br : taskList) {
			assertNotNull(((BruteForceTask) br).getPath());
		}

		/**
		 * launch a job
		 */

		/**
		 * analyse remote result
		 */

		/**
		 * compara remote and loca result
		 */
		ArrayList<SPReport> testResults = brute.launchSecurityTestJob();
		assertNotNull(testResults);

	}


	

}
