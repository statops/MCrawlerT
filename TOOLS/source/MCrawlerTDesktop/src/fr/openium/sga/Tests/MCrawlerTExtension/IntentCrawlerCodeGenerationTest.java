package fr.openium.sga.Tests.MCrawlerTExtension;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import kit.Config.Config;

import org.junit.Test;

import com.sun.codemodel.JClassAlreadyExistsException;

import fr.openium.sga.codegen.ScenarioCodeGen;

public class IntentCrawlerCodeGenerationTest {
	@Test
	public void testIntentCode() throws JClassAlreadyExistsException,
			IOException {
		ScenarioCodeGen sc = new ScenarioCodeGen(new File(
				Config.CURRENT_DIRECTORY
						+ "/TEST/extension/manifest/AndroidManifest.xml"),
				new File(Config.CURRENT_DIRECTORY + "/TEST/extension/manifest"));
		sc.setExpectectedActivity("test");
		sc.setPairwiseSequence("" + 0);
		sc.setStrategyType("500");
		sc.setLauncherActivity("BookCatalogue");
		sc.generate();
		assertTrue(new File(
				Config.CURRENT_DIRECTORY
						+ "/TEST/extension/manifest/com/eleybourn/bookcatalogue/test/IntentCrawler.java")
				.exists());

	}

	/**
	 * do not commit
	 * 
	 * @throws JClassAlreadyExistsException
	 * @throws IOException
	 */
	@Test
	public void testIntentCodeGenerationExemples()
			throws JClassAlreadyExistsException, IOException {
		ScenarioCodeGen sc = new ScenarioCodeGen(new File(
				Config.CURRENT_DIRECTORY
						+ "/TEST/extension/eij/AndroidManifest.xml"), new File(
				Config.CURRENT_DIRECTORY + "/TEST/extension/eij"));
		sc.setExpectectedActivity("fr.crauvergne.eij.activity.ActivityDashboard");
		sc.setPairwiseSequence("" + 0);
		sc.setStrategyType("500");
		sc.generate();
		assertTrue(new File(
				Config.CURRENT_DIRECTORY
						+ "/TEST/extension/eij/fr/crauvergne/eij/test/IntentCrawler.java")
				.exists());

	}

}
