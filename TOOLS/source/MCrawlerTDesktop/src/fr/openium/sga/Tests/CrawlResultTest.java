/**
 * 
 */
package fr.openium.sga.Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;

import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Utils.SgUtils;

import org.junit.Test;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.bissimulation.SgaGraph;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;
import fr.openium.sga.strategy.StrategyFactory;
import fr.openium.sga.threadPool.CrawlerTask;

/**
 * @author Stassia
 * 
 */
public class CrawlResultTest {
	

	@Test
	public void test() throws CloneNotSupportedException {
		
		ScenarioData path =ScenarioParser
				.parse(new File(
						(new File("").getAbsolutePath() + "/TEST/crawlResult/crawlResult.xml")));
		ScenarioData scen = ScenarioParser
				.parse(new File(
						(new File("").getAbsolutePath() + "/TEST/crawlResult/crawlResult2.xml")));
		System.out.println("==========================");
		HashSet<State> new_statStates = SgUtils.getNewStates(scen, path);
		System.out.println("Result states");
		int i = 0;
		for (State st : new_statStates) {
			if (st.isFinal()) {
				continue;

			}
			i++;
			System.out.println("States " + i + "  " + st.toString());
			ScenarioData path_ = SgUtils._getPath(st, path);

			System.out.println("PATH:" + path_.toString());

		}

		assertEquals(2, i);

		/**
		 * lire une tache vide
		 */

		/**
		 * lire une tache avec scen initiale
		 */

		/**
		 * lire
		 */

	}

	@Test
	public void test_init_scen() {

		/**
		 * lire une tache avec scen initiale
		 */

	}
	private String androidHome = System.getenv("HOME") + "/android-sdks";
	@Test
	public void test_not_empty_scen() throws FileNotFoundException {
		String[] params = new String[] {"explore",
				"-p",
				"/Users/Stassia/Documents/Scen-genWorkSpace/sgdExample",
				"-tp",
				"/Users/Stassia/Documents/Scen-genWorkSpace/SgdExampleTestWithoutEmma",
				"-coverage", "70", "-tpackage",
				"fr.openium.example.exampleforsgd.test", "-sdk",
				androidHome, "-out",
				new File("").getAbsolutePath() + "/out", "-arv",
				new File("").getAbsolutePath() + "/TestData/testData.xml",
				"-strategy", "1", "-thread", "2", "-emu", "3" };

		SgdEnvironnement model = Emma.init_environment_(params);
		assertNotNull("CheckParameters", model);
		/**
		 * lire une tache avec scen initiale
		 */

		CrawlResult mResult = new CrawlResult(
				new File(model.getOutDirectory()),
				StrategyFactory.getNewStrategy(model));
		ScenarioData scen_init0 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/init0.xml")));
		/*
		 * ScenarioData path0 = ScenarioParser.parse(new File( (new
		 * File("").getAbsolutePath() + "/TEST/path0.xml"))); ScenarioData out1
		 * = ScenarioParser .parse(new File((new File("").getAbsolutePath() +
		 * "/TEST/out1.xml")));
		 */
		ScenarioData scen_init1 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/init1.xml")));
		/*
		 * ScenarioData path1 = ScenarioParser.parse(new File( (new
		 * File("").getAbsolutePath() + "/TEST/path1.xml"))); ScenarioData out2
		 * = ScenarioParser .parse(new File((new File("").getAbsolutePath() +
		 * "/TEST/out2.xml")));
		 */
		ScenarioData scen_init2 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/init2.xml")));
		/*
		 * ScenarioData path2 = ScenarioParser.parse(new File( (new
		 * File("").getAbsolutePath() + "/TEST/path2.xml"))); ScenarioData out3
		 * = ScenarioParser .parse(new File((new File("").getAbsolutePath() +
		 * "/TEST/out3.xml"))); ScenarioData scen_init3 =
		 * ScenarioParser.parse(new File( (new File("").getAbsolutePath() +
		 * "/TEST/init3.xml"))); ScenarioData path3 = ScenarioParser.parse(new
		 * File( (new File("").getAbsolutePath() + "/TEST/path3.xml")));
		 * ScenarioData out4 = ScenarioParser .parse(new File((new
		 * File("").getAbsolutePath() + "/TEST/out4.xml")));
		 */

		CrawlerTask from_Android_crawler = new CrawlerTask(scen_init0);
		List<CrawlerTask> response = mResult.update(from_Android_crawler);
		assertEquals(1, response.size());
		ScenarioData path_1 = response.get(0).getPath();
		assertEquals(3, path_1.getStates().size());
		// new state Main activity
		// assertEquals("fr.openium.example.exampleforsgd.MainActivity",
		// SgUtils.get_last_state(path_1)
		// .getName());

		from_Android_crawler = new CrawlerTask(scen_init1);
		response = mResult.update(from_Android_crawler);
		assertEquals(1, response.size());
		path_1 = response.get(0).getPath();
		assertEquals(1, path_1.getTransitions().size());
		// new state Button activity
		assertEquals("fr.openium.example.exampleforsgd.ButtonActivity", SgUtils
				.get_last_state(path_1).getName());

		from_Android_crawler = new CrawlerTask(scen_init2);
		response = mResult.update(from_Android_crawler);
		assertEquals(2, response.size());
		path_1 = response.get(0).getPath();
		ScenarioData path_2 = response.get(1).getPath();
		
		assertEquals(1, path_1.getTransitions().size());
		assertEquals(1, path_2.getTransitions().size());
/*
 * Inutile
		ScenarioData final_graph = mResult.getScenarioData();
		if (ConfigApp.DEBUG) {
			System.out.println(final_graph.toString());
		}
		Refinement displayer = new Refinement(final_graph);
		SgaGraph graph = null;
		try {
			graph = displayer.generate_graphe(final_graph);
		} catch (Exception e) {
			return;
		}
	//	graph.display();
		if (ConfigApp.DEBUG) {
			System.out.println(graph.toDot());
		}
		//
		//graph = displayer.computeBissModel()[1];
		//graph.display();
*/
	}
}
