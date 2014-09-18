package fr.openium.sga.Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import kit.Config.Config;
import kit.Scenario.Action;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;

import org.junit.Test;
import org.xml.sax.SAXException;

public class ScenarioGeneratorTest {
	@Test
	public void test() throws SAXException, IOException, ParserConfigurationException {
		ScenarioParser pa = new ScenarioParser();
		@SuppressWarnings("static-access")
		// generer un scenarioData
		ScenarioData value = pa.parse(new File(Config.CURRENT_DIRECTORY + "/TEST/scenario/scenario.xml"));

		File out = new File(Config.CURRENT_DIRECTORY + "/TEST/scenario/out.xml");
		ScenarioGenerator gen = new ScenarioGenerator(out.getPath());
		gen.generateXml(value);
		assertTrue(out.exists());

		// reparser out
		value = ScenarioParser.parse(out);
		// copier test parser
		assertTrue("version", value.getVersion().equalsIgnoreCase("1"));
		assertEquals(7, value.getActions().size());
		for (Action act : value.getActions()) {
			System.out.println("action :" + act.toString());
			assertTrue(
					"actions",
					act.getName().equalsIgnoreCase("click button")
							|| act.getName().equalsIgnoreCase("click item")
							|| act.getName().equalsIgnoreCase("check box")
							|| act.getName().equalsIgnoreCase("")
							|| act.getName().equalsIgnoreCase("click imageview")
							|| act.getName().equalsIgnoreCase("editText")
							|| act.getName().equalsIgnoreCase("schroll") ? true : false);
		}

		for (State st : value.getStates()) {
			assertTrue(
					"initial state",
					st.isInit() ? st.getName().equalsIgnoreCase(
							"fr.openium.example.exampleforsgd.MainActivity") : true);
		}

	}

	@Test
	public void testCoveragename() throws IOException {
		boolean name_available = false;
		int i = 0;
		String file = Config.CURRENT_DIRECTORY + "/TEST/coverage";
		String coverageFormat = file + File.separator + "mCoverage" + "%d.ec";
		String coverageFilePath = String.format(coverageFormat, i);
		do {
			if (!(new File(coverageFilePath).exists())) {
				name_available = true;
			} else {
				i++;
				coverageFilePath = String.format(coverageFormat, i);
			}
			System.out.println(coverageFilePath);
		} while (!name_available);
	}
}
