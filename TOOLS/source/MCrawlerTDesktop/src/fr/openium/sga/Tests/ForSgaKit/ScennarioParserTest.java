package fr.openium.sga.Tests.ForSgaKit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;


import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Tree;

import org.junit.Test;
import org.xml.sax.SAXException;

public class ScennarioParserTest {
	@Test
	public void testGenerator() throws SAXException, IOException,
			ParserConfigurationException {
		ScenarioParser pa = new ScenarioParser();
		@SuppressWarnings("static-access")
		ScenarioData value = pa
				.parse(new File(
						(new File("").getAbsolutePath() + "/src/fr/openium/sga/Tests/ForSgaKit/scenario.xml")));
		// checkScenarioData(value);
		/*
		 * File out = new File(new File("").getAbsolutePath() +
		 * "/src/fr/openium/sga/Tests/ForSgaKit/out.xml"); ScenarioGenerator gen
		 * = new ScenarioGenerator(out.getPath()); gen.generateXml(value);
		 * assertTrue(out.exists());
		 * 
		 * value = ScenarioParser.parse(out); checkScenarioData(value);
		 * System.out.println(SgUtils.get_a_transition_Id(value));
		 */

	}

	@Test
	public void testParser() throws SAXException, IOException,
			ParserConfigurationException {
		ScenarioParser pa = new ScenarioParser();
		@SuppressWarnings("static-access")
		ScenarioData value = pa
				.parse(new File(
						(new File("").getAbsolutePath() + "/src/fr/openium/sga/Tests/ForSgaKit/scenario.xml")));
		// assertTrue("version", value.getVersion().equalsIgnoreCase("1"));

		checkScenarioData(value);

	}

	private void checkScenarioData(ScenarioData value) {
		int State_size = value.getStates().size();
		assertEquals(State_size, 5);
		int transition_size = value.getTransitions().size();
		assertEquals(transition_size, 2);
		Iterator<State> states = value.getStates().iterator();
		while (states.hasNext()) {
			State st = states.next();
			System.out.println(st.getName() + "  " + st.getId());
			if (st.getName().equalsIgnoreCase("activityNameX")
					&& st.getId().equalsIgnoreCase("1")) {
				assertTrue(st.getType().equalsIgnoreCase("dest"));
				assertTrue(st.getTime().equalsIgnoreCase("12:03"));
				for (kit.Scenario.Widget wig : st.getWidgets()) {
					if (wig.getName().equalsIgnoreCase("bouton X")) {
						assertTrue(wig.getType().equalsIgnoreCase("bouton"));
						// assertTrue(st.getWidgets().get(0).getName().equalsIgnoreCase("bouton X"));
						assertTrue(wig.getPosX().equalsIgnoreCase("25"));
						assertTrue(wig.getPosY().equalsIgnoreCase("36"));
					}
				}
				// assertTrue(st.getWidgets().get(0).getType().equalsIgnoreCase("bouton"));
				// assertTrue(st.getWidgets().get(0).getName().equalsIgnoreCase("bouton X"));
				// assertTrue(st.getWidgets().get(0).getPosX().equalsIgnoreCase("25"));
				// assertTrue(st.getWidgets().get(0).getPosY().equalsIgnoreCase("36"));

			}
			if (st.getName().equalsIgnoreCase("activityNameX")
					&& st.getId().equalsIgnoreCase("0")) {
				assertTrue(st.getType().equalsIgnoreCase("dest"));
				assertTrue(st.getTime().equalsIgnoreCase("12:01"));
				for (kit.Scenario.Widget wig : st.getWidgets()) {
					if (wig.getName().equalsIgnoreCase("EditText 1")) {
						assertTrue(wig.getType().equalsIgnoreCase("Edit"));
						// assertTrue(st.getWidgets().get(0).getName().equalsIgnoreCase("bouton X"));
						assertTrue(wig.getPosX().equalsIgnoreCase("20"));
						assertTrue(wig.getPosY().equalsIgnoreCase("30"));
					}
				}

			}

		}
		/**
		 * test sur les transitions
		 */
		Iterator<Transition> trs = value.getTransitions().iterator();
		while (trs.hasNext()) {
			Transition tr = trs.next();
			System.out.println(tr.toString());

			if (tr.getId().equalsIgnoreCase("1")) {
				assertTrue(tr.getSource().getId().equalsIgnoreCase(tr.getId()));
				assertTrue(tr.getDest().getId().equalsIgnoreCase(tr.getId()));
				assertEquals(tr.getDest().getName(), "activityNameX");
				assertEquals(tr.getSource().getName(), "MainActivity_0");
				// assertTrue(tr.getAction().get(0).getName().equalsIgnoreCase("click button"));

			}
		}

	}

	@Test
	public void testTreeElement() throws SAXException, IOException,
			ParserConfigurationException {

		ScenarioData value = ScenarioParser
				.parse(new File(
						(new File("").getAbsolutePath() + "/src/fr/openium/sga/Tests/ForSgaKit/TreeScenario.xml")));

		assertTrue(value.getTrees().size() == 3);
		for (Tree tree : value.getTrees()) {
			if (tree.getId().equalsIgnoreCase("3")) {
				assertTrue(tree.getTransitions().size() == 1);
			}

		}

		/**
		 * generer out
		 */

		File out = new File(new File("").getAbsolutePath()
				+ "/src/fr/openium/sga/Tests/ForSgaKit/TreeOut.xml");
		ScenarioGenerator gen = new ScenarioGenerator(out.getPath());
		gen.generateXml(value);
		assertTrue(out.exists());
		value = ScenarioParser.parse(out);
		assertTrue(value.getTrees().size() == 3);

	}

	/*@Test
	public void testTreeId() {
		ScenarioData value = ScenarioParser
				.parse(new File(
						(new File("").getAbsolutePath() + "/src/fr/openium/sga/Tests/ForSgaKit/TreeScenario.xml")));

		assertTrue(value.getTrees().size() == 3);
		// assertEquals("1", SgUtils.getTreeIdOfTransition(value, "1"));
		assertEquals("3", SgUtils.getTreeIdOfTransition(value, "1"));
		// assertEquals("3", SgUtils.get);
		assertEquals("4", SgUtils.get_last_transition(value).getId());
		SgUtils.addError(value, "Crashh");
		Transition tr = value.getTransitions("4");
		assertEquals("1", SgUtils.getTreeIdOfTransition(value, "4"));
		// assertEquals("0", SgUtils.getTreeIdOfTransition(value, "4"));
		Action act = tr.getAction().get(tr.getAction().size() - 1);
		// assertTrue(act.getError().equalsIgnoreCase("Crashh"));

		*//**
		 * generer out
		 *//*

		File out = new File(new File("").getAbsolutePath()
				+ "/src/fr/openium/sga/Tests/ForSgaKit/TreeOut.xml");
		ScenarioGenerator gen = new ScenarioGenerator(out.getPath());
		gen.generateXml(value);
		assertTrue(out.exists());
		value = ScenarioParser
				.parse(new File(
						(new File("").getAbsolutePath() + "/src/fr/openium/sga/Tests/ForSgaKit/TreeOut.xml")));
		SgUtils.addError(value, "Crashh");

		gen = new ScenarioGenerator(out.getPath());
		gen.generateXml(value);
		assertTrue(out.exists());

	}
*/
}
