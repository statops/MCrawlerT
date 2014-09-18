package fr.openium.sga.Tests.MCrawlerTExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import kit.Config.Config;
import kit.Intent.AndroidManifestParser;
import kit.Intent.MCrawlerTIntent;
import kit.Intent.ManifestData;
import kit.Intent.StartingIntentGenerator;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;

import org.junit.Test;

import fr.openium.sga.Main;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;
import fr.openium.sga.richModel.LauncherMultipleIntent;

public class LauncherMultipleIntentTest {

	private String[] getUe() {
		return new String[] {
				"explore",
				"-p",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/project/paypal",
				"-manifest",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/project/paypal/app/AndroidManifest.xml",
				"-tp",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/project/paypalTest",
				"-tpackage",
				"com.paypal.here.test",
				"-pPackage",
				"com.paypal.here",
				"-pApk",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/project/paypal/appsigned.apk",
				"-sdk",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/ANDROID_SDK/adt-bundle-mac-x86_64-20140321/sdk",
				"-arv",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/input/testData.xml",
				"-strategy",
				"500",
				"-thread",
				"1",
				"-launcherActivity",
				"com.paypal.here.StartUpActivity ",
				"-maxEvent",
				"50",
				"-class",
				"com.example.loggingproject.test.Maintest",
				"-stopError",
				"true",
				"-maxTime",
				"140000",
				"-emu",
				"null",
				"-cp",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/input/cp",
				"-db",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/input/db",
				"-out",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/out/paypalWIKOOutput",
				"-dico",
				new File(Config.CURRENT_DIRECTORY + File.separator
						+ "TEST/bruteForce/dico").getPath() };

	}

	@Test
	public void testTaskGeneration() throws Exception {

		String[] params = getUe();
		SgdEnvironnement env = Emma.init_environment_(params);
		LauncherMultipleIntent launcher = new LauncherMultipleIntent(env);
		HashMap<MCrawlerTIntent, CrawlResult> results = launcher
				.getListResults();
		assertNotNull(results);
		for (MCrawlerTIntent result : results.keySet()) {
			assertNotNull(results.get(result).getScenarioData());
		}

	}

	@Test
	public void testMainExcecution() throws Exception {
		String[] params = getUe();
		Main.main(params);
	}

	@Test
	public void testBuildIntentScenario() throws Exception {

		/**
		 * creer un intent
		 */
		File manifest = new File(Config.CURRENT_DIRECTORY
				+ "/TEST/extension/eij/AndroidManifest.xml");
		assertTrue(manifest.exists());
		ManifestData data = AndroidManifestParser.parse(new FileInputStream(
				manifest));
		assertNotNull(data);
		ArrayList<MCrawlerTIntent> _ints = new StartingIntentGenerator(data)
				.generate();
		assertTrue(_ints.size() > 0);
		assertEquals(_ints.size(), 2);
		LauncherMultipleIntent launcher = new LauncherMultipleIntent(null);
		CrawlResult result = new CrawlResult(new File(Config.CURRENT_DIRECTORY
				+ "/TEST/extension/eij"), null);
		result = launcher.buildIntentTransition(_ints.get(0), result);
		assertNotNull(result);
		assertEquals(2, result.getScenarioData().getStates().size());
		assertEquals(1, result.getScenarioData().getTransitions().size());

		/**
		 * creer un scenario a partir de cet intent
		 */

		assertTrue(new ScenarioGenerator(Config.CURRENT_DIRECTORY
				+ "/TEST/extension/eij/out.xml").generateXml(result
				.getScenarioData()));
		assertTrue(new File(Config.CURRENT_DIRECTORY
				+ "/TEST/extension/eij/out.xml").exists());

		/**
		 * lire le scenario
		 */
		ScenarioData scne = ScenarioParser.parse(new File(
				Config.CURRENT_DIRECTORY + "/TEST/extension/eij/out.xml"));
		assertNotNull(scne);
		assertEquals(2, scne.getStates().size());
		State initState = scne.getInitialState();
		assertNotNull(initState);
		assertEquals(true, initState.isCalledWithIntent());
		assertEquals("source", scne.getInitialState().getType());

		assertEquals(1, scne.getTransitions().size());

	}
}