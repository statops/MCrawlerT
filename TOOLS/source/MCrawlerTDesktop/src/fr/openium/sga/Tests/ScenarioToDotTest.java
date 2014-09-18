package fr.openium.sga.Tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import kit.Config.Config;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;

import org.junit.Test;

import fr.openium.sga.dot.ScenarioToDot;

public class ScenarioToDotTest {
	@Test
	public void test() throws IOException, InterruptedException {

		ScenarioData value = ScenarioParser.parse(new File(
				Config.CURRENT_DIRECTORY + "/TEST/out2.xml"));
		ScenarioToDot scToD = new ScenarioToDot(value, new File(
				Config.CURRENT_DIRECTORY + "/TEST/dot"));
		/**
		 * verification avec la ligne de commande l'image
		 */
		/**
		 * dot -Tgif /Users/Stassia/Documents/Scen-genWorkSpace/sgd/out/dot/test
		 * -o /Users/Stassia/Documents/Scen-genWorkSpace/sgd/out/dot/test.gif
		 */
		assertTrue(scToD.generateDotFilePerTrace(10));
		// assertTrue(scToD.generateDotFileFor_limitedTransition(7));
	}

	@Test
	public void testBuiildTrnsition() throws IOException {
		ScenarioToDot scToD = new ScenarioToDot(null, new File(
				Config.CURRENT_DIRECTORY + "/TEST/dot"));
		HashMap<String, String> transition_parameter = new HashMap<String, String>();
		transition_parameter.put("label", "labelValue");
		transition_parameter.put("color", "blue");
		assertTrue(scToD.buildAttribute(transition_parameter).contains("blue"));
		System.out.print(scToD.buildAttribute(transition_parameter));
		assertTrue(scToD.buildAttribute(transition_parameter).equalsIgnoreCase(
				"[color=blue,label=labelValue]"));

	}
}
