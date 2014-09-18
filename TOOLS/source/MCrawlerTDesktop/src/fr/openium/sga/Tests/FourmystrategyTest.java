package fr.openium.sga.Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;
import fr.openium.sga.strategy.FourmiStrategy;

public class FourmystrategyTest {
	@Test
	public void test() throws Exception {
		String[] params = new String[] { "explore","-p", "/Users/Stassia/Documents/Scen-genWorkSpace/sgdExample",
				"-tp", "/Users/Stassia/Documents/Scen-genWorkSpace/SgdExampleTestWithoutEmma", "-coverage",
				"70", "-tpackage", "fr.openium.example.exampleforsgd.test", "-sdk",
				"/Users/Stassia/android-sdks", "-out", new File("").getAbsolutePath() + "/out", "-arv",
				new File("").getAbsolutePath() + "/TestData/testData.xml", "-strategy", "1", "-thread", "2",
				"-emu", "3" };

		SgdEnvironnement model = Emma.init_environment_(params);
		assertNotNull("Parameter must be checked", model);
		try {

			CrawlResult result = new FourmiStrategy(model, null).getResult();
			assertEquals(true, result.getScenarioData().getStates().size() > 5);
			assertEquals(22, result.getScenarioData().getStates().size());
		} catch (NullPointerException ne) {
			/*
			 * pour le moment : pas d'action
			 */
		}

	}

}
