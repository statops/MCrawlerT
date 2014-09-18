package fr.openium.sga.Tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.junit.Test;



import kit.Config.Config;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Scenario.Widget;
import kit.Utils.SgUtils;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTestTask;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager.SPReport;
import fr.openium.sga.SecurityTesting.BruteForceTest.BruteForceEventsGenerator;
import fr.openium.sga.SecurityTesting.BruteForceTest.BruteForceManager;
import fr.openium.sga.SecurityTesting.BruteForceTest.BruteForceTask;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;

public class BruteForceTest {

	@Test
	public void testTaskGeneration() throws Exception {

		String[] params = new String[] {
				"bruteforce",
				"-p",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/log",
				"-tp",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/logTest",
				"-tpackage",
				"com.example.loggingproject.test",
				"-sdk",
				"/home/stassia/Documents/adt-bundle-linux-x86_64-20140321/sdk",
				"-out",
				new File("").getAbsolutePath() + "/outLog",
				"-arv",
				new File("").getAbsolutePath() + "/TestData/testData.xml",
				"-strategy",
				"122",
				"-thread",
				"1",
				"-launcherActivity",
				"com.example.loggingproject.MainActivity",
				"-maxEvent",
				"500",
				"-class",
				"com.example.loggingproject.test.Maintest",
				"-pApk",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/log/bin/log.apk",
				"-stopError",
				"true",
				"-dico",
				new File(Config.CURRENT_DIRECTORY + File.separator
						+ "TEST/bruteForce/dico").getPath() };
		ScenarioData model = ScenarioParser.parse(new File(new File("")
				.getAbsolutePath() + "/TEST/bruteForce/out.xml"));
		assertNotNull(model);
		SgdEnvironnement env = Emma.init_environment_(params);
		BruteForceManager brute = new BruteForceManager(env);
		HashSet<AbstractTestTask> taskList = brute.getGenerateTaskLists();
		assertEquals(3, taskList.size());
		/**
		 * compare states are unique
		 * 
		 */
		for (AbstractTestTask br : taskList) {
			assertNotNull(((BruteForceTask) br).getPath());
		}
		// brute.launchSecurityTestJob();
		// stress.launchSecurityTestJob();

	}

	@Test
	public void testJobExecution() throws Exception {

		String[] params = new String[] {
				"bruteforce",
				"-p",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/log",
				"-tp",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/logTest",
				"-tpackage",
				"com.example.loggingproject.test",
				"-pPackage",
				"com.example.loggingproject",
				"-sdk",
				"/home/stassia/Documents/adt-bundle-linux-x86_64-20140321/sdk",
				"-out",
				new File("").getAbsolutePath() + "/outLog",
				"-arv",
				new File("").getAbsolutePath() + File.separator
						+ "TEST/bruteForce/testData.xml",
				"-strategy",
				"122",
				"-thread",
				"1",
				"-launcherActivity",
				"com.example.loggingproject.MainActivity",
				"-maxEvent",
				"5",
				"-class",
				"com.example.loggingproject.test.BruteTest",
				"-pApk",
				"/home/stassia/Documents/CAPTURETEST/LoggingProjectSample/bin/LoggingProjectSample.apk",
				"-stopError",
				"true",
				"-bruteDico",
				new File(Config.CURRENT_DIRECTORY + File.separator
						+ "TEST/bruteForce/dico").getPath() };
		ScenarioData model = ScenarioParser.parse(new File(new File("")
				.getAbsolutePath() + "/TEST/bruteForce/out.xml"));
		assertNotNull(model);
		SgdEnvironnement env = Emma.init_environment_(params);
		BruteForceManager brute = new BruteForceManager(env);
		HashSet<AbstractTestTask> taskList = brute.getGenerateTaskLists();
		assertEquals(3, taskList.size());
		/**
		 * compare states are unique
		 * 
		 */
		for (AbstractTestTask br : taskList) {
			assertNotNull(((BruteForceTask) br).getPath());
		}

		ArrayList<SPReport> testResults = brute.launchSecurityTestJob();
		assertNotNull(testResults);

	}

	@Test
	public void testDataGeneration() throws IOException {

		File pathTestDataForLogging = new File(new File("").getAbsolutePath()
				+ File.separator + "TEST/bruteForce/testData.xml");
		State targetState = ScenarioParser.parse(
				new File(Config.CURRENT_DIRECTORY + File.separator
						+ "TEST/bruteForce/out.xml")).getState("0_3",
				Scenario.DEST);
		assertNotNull(targetState);
		for (Widget wig : targetState.getWidgets()) {
			System.out.println(wig.toString());
			if (wig.getName().equalsIgnoreCase(
					"com.example.loggingproject:id/editText1"))
				assertEquals("128", wig.getInputType());
		}
		// assertNull(targetState);

		double maxNumber = 10;
		File outputFile = new File(new File("").getAbsolutePath()
				+ File.separator + "TEST/bruteForce/events");
		File testDataForDico = new File(Config.CURRENT_DIRECTORY
				+ File.separator + "TEST/bruteForce/dico");
		// outputFile.deleteOnExit();
		BruteForceEventsGenerator ev = new BruteForceEventsGenerator(
				targetState, pathTestDataForLogging, testDataForDico, maxNumber);
		assertTrue(ev.generateBrutForceTest(outputFile));
		ArrayList<String> bruteData = (ArrayList<String>) FileUtils
				.readLines(outputFile);
		assertNotNull(bruteData);
		assertEquals(20, bruteData.size());

	}

	@Test
	public void testJobExecutionOnePath() throws Exception {

		String[] params = new String[] {
				"bruteforce",
				"-p",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/log",
				"-tp",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/logTest",
				"-tpackage",
				"com.example.loggingproject.test",
				"-pPackage",
				"com.example.loggingproject",
				"-sdk",
				"/home/stassia/Documents/adt-bundle-linux-x86_64-20140321/sdk",
				"-out",
				new File("").getAbsolutePath() + "/outLog",
				"-arv",
				new File("").getAbsolutePath() + File.separator
						+ "TEST/bruteForce/testData.xml",
				"-strategy",
				"122",
				"-thread",
				"1",
				"-launcherActivity",
				"com.example.loggingproject.MainActivity",
				"-maxEvent",
				"1",
				"-class",
				"com.example.loggingproject.test.BruteTest",
				"-pApk",
				"/home/stassia/Documents/CAPTURETEST/LoggingProjectSample/bin/LoggingProjectSample.apk",
				"-stopError",
				"true",
				"-bruteDico",
				new File(Config.CURRENT_DIRECTORY + File.separator
						+ "TEST/bruteForce/dico").getPath() };
		ScenarioData model = ScenarioParser.parse(new File(new File("")
				.getAbsolutePath() + "/TEST/bruteForce/_path0_3.xml"));
		assertNotNull(model);
		SgdEnvironnement env = Emma.init_environment_(params);
		BruteForceManager brute = new BruteForceManager(env);
		HashSet<AbstractTestTask> taskList = brute.getGenerateTaskLists();
		assertEquals(1, taskList.size());
		/**
		 * compare states are unique
		 * 
		 */
		for (AbstractTestTask br : taskList) {
			assertNotNull(((BruteForceTask) br).getPath());
		}

		ArrayList<SPReport> testResults = brute.launchSecurityTestJob();
		assertNotNull(testResults);

	}

	@Test
	public void testTwoJobsExecution() throws Exception {

		String[] params = new String[] {
				"bruteforce",
				"-p",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/log",
				"-tp",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/logTest",
				"-tpackage",
				"com.example.loggingproject.test",
				"-pPackage",
				"com.example.loggingproject",
				"-sdk",
				"/home/stassia/Documents/adt-bundle-linux-x86_64-20140321/sdk",
				"-out",
				new File("").getAbsolutePath() + "/outLog",
				"-arv",
				new File("").getAbsolutePath() + File.separator
						+ "TEST/bruteForce/testData.xml",
				"-strategy",
				"122",
				"-thread",
				"1",
				"-launcherActivity",
				"com.example.loggingproject.MainActivity",
				"-maxEvent",
				"100",
				"-class",
				"com.example.loggingproject.test.BruteTest",
				"-pApk",
				"/home/stassia/Documents/CAPTURETEST/LoggingProjectSample/bin/LoggingProjectSample.apk",
				"-stopError",
				"true",
				"-bruteDico",
				new File(Config.CURRENT_DIRECTORY + File.separator
						+ "TEST/bruteForce/dico").getPath() };
		ScenarioData model = ScenarioParser
				.parse(new File(
						"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/output/forBruteForceFinal/out.xml"));
		assertNotNull(model);
		SgdEnvironnement env = Emma.init_environment_(params);
		BruteForceManager brute = new BruteForceManager(env);
		HashSet<AbstractTestTask> taskList = brute.getGenerateTaskLists();
		assertEquals(3, taskList.size());
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

	@Test
	public void testBruteForceEbayVerdict() throws Exception {
		ScenarioData model = ScenarioParser
				.parse(new File(
						"/home/stassia/Documents/ebay/output/outForBruteForce/Scenario/_out2.xml"));
		assertNotNull(model);

	}

	@Test
	public void testBruteForceEbay() throws Exception {

		String[] params = new String[] {
				"bruteforce",
				"-p",
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
		State st1 = it.next().getTargetState();

		State st2 = it.next().getTargetState();

		// assertTrue(SgUtils.isEqualState(st1, st2));
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
