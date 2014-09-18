package fr.openium.sga.Tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;

import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Utils.SgUtils;

import org.junit.Test;

import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.reporter.ModelReporter;
import fr.openium.sga.result.CrawlResult;
public class ModelReporterTest {
	/**
	 * lancer un test
	 */

	/**
	 * valider la présence du raport
	 */

	/**
	 * valider le contenu
	 */

	@Test
	public void testModelReportGenerator() throws Exception {

		String[] params = getParams();
		ScenarioData model = getAmodel();
		assertNotNull(model);
		assertTrue(SgUtils.path_contains_error(model));
		SgdEnvironnement env = Emma.init_environment_(params);
		CrawlResult result = new CrawlResult(new File(env.getOutDirectory()),
				null);
		result.setScenarioData(model);
		ModelReporter testReport = new ModelReporter(env, result, "" + 10000000
				+ "sec");
		testReport.generate();

		assertTrue(new File(env.getOutDirectory() + File.separator
				+ ModelReporter.MODEL_REPORT_NAME).exists());
		System.out.println(testReport.toString());

	}

	@Test
	public void testCrashJunitGeneration() throws Exception {

		String[] params = getParams();
		ScenarioData model = getAmodel();
		assertNotNull(model);
		assertTrue(SgUtils.path_contains_error(model));
		SgdEnvironnement env = Emma.init_environment_(params);
		ArrayList<String> tets = Emma
				.generateCrashTest(env, new File(env.getOutDirectory()
						+ File.separator + "CrashJUNIT"), model);
		assertTrue(tets.size() == 2);
	}

	private ScenarioData getAmodel() {
		return ScenarioParser
				.parse(new File(
						"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/output/outTest1/out.xml"));
	}

	private String[] getParams() {
		return new String[] {
				"stress",
				"-p",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/log",
				"-tp",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/logTest",
				"-tpackage",
				"com.example.loggingproject.test",
				"-pPackage",
				"com.example.loggingproject",
				"-sdk",
				"/Users/Stassia/android-sdks",
				"-out",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/output/outTest1",
				"-arv",
				new File("").getAbsolutePath() + "/TestData/testData.xml",
				"-strategy",
				"1",
				"-thread",
				"1",
				"-launcherActivity",
				"com.example.loggingproject.MainActivity",
				"-maxEvent",
				"500",
				"-class",
				"fr.openium.converterexample.test.Maintest",
				"-pApk",
				"/home/stassia/Documents/PAPERRESULT/LOGEXAMPLEAPP/project/log/bin/MainActivity-instrumented.apk",
				"-stopError", "true" };

	}

}
