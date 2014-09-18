package fr.openium.sga.Tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Utils.SgUtils;

import org.junit.Test;
import org.xml.sax.SAXException;

public class TreeOperationTest {

	@Test
	public void testEquivalence() throws SAXException, IOException,
			ParserConfigurationException {

		ScenarioData value = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/converter/twoStates.xml")));

		State st1 = value.getState("0_3", Scenario.SOURCE);
		State st2 = value.getState("0_3", Scenario.DEST);

		assertFalse(SgUtils.isEqualState(st1, st2));

		State st3 = value.getState("0");
		assertTrue(SgUtils.isEqualState(st1, st3));
		State st4 = value.getState("0_2", Scenario.SOURCE);
		State st5 = value.getState("0_2", Scenario.DEST);
		assertTrue(SgUtils.isEqualState(st1, st4));
		assertFalse(SgUtils.isEqualState(st5, st4));
		assertFalse(SgUtils.isEqualState(st5, st1));
		assertFalse(SgUtils.isEqualState(st5, st2));

		/**
		 * test transitions equivalence
		 */
		Transition tr1 = value.getTransitions("0_3");
		Transition tr2 = value.getTransitions("0_2");
		assertFalse(SgUtils.isEqualTransition(tr1, tr2));
		assertTrue(SgUtils.isEqualTransition(tr1, tr1));

		/**
		 * 
		 */

	}

	@Test
	public void testPathSelection() throws SAXException, IOException,
			ParserConfigurationException, CloneNotSupportedException {

		ScenarioData value1 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/converter/twoStates.xml")));

		Transition tr1 = value1.getTransitions("0_3");
		Transition tr2 = value1.getTransitions("0_2");

		ScenarioData path1 = SgUtils.getConcretePath(
				value1.getState("0_3", Scenario.DEST), value1);
		assertTrue(path1.getTransitions().size() == 1);
		assertTrue(SgUtils
				.isEqualTransition(tr1, path1.getTransitions().get(0)));
		for (State st : path1.getStates()) {
			assertEquals("0_3", st.getId());
		}

		ScenarioData path2 = SgUtils.getConcretePath(
				value1.getState("0_2", Scenario.DEST), value1);
		for (State st : path2.getStates()) {
			assertEquals("0_2", st.getId());
		}
		assertTrue(path2.getTransitions().size() == 1);
		assertTrue(SgUtils
				.isEqualTransition(tr2, path2.getTransitions().get(0)));

		ScenarioData path3 = SgUtils.getConcretePath(
				value1.getState("0_6", Scenario.DEST), value1);
		assertNull(path3);

	}

	@Test
	public void testPathSelelectionLevel2() throws SAXException, IOException,
			ParserConfigurationException, CloneNotSupportedException {

		ScenarioData value1 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/converter/twoStates.xml")));

		ScenarioData path1 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/converter/pathlevel2.xml")));
		assertNotNull(path1);
		assertEquals(6, path1.getTransitions().size());
		value1.add_(path1, true);
		assertEquals(7, value1.getTransitions().size());

		State st1 = value1.getState("0_2_3", Scenario.SOURCE);
		State st2 = value1.getState("0_2_4", Scenario.SOURCE);
		ScenarioData path2 = SgUtils.getConcretePath(st1, value1);
		for (State st : path2.getStates()) {
			// System.err.println(st.getId());
			assertTrue(st.getId().equalsIgnoreCase("0_2_3")
					|| st.getId().equalsIgnoreCase("0_2"));
		}
		assertTrue(path2.getTransitions().size() == 2);
		assertTrue(SgUtils.isEqualTransition(value1.getTransitions("0_2"),
				path2.getTransitions("0_2")));
		assertTrue(SgUtils.isEqualTransition(value1.getTransitions("0_2_3"),
				path2.getTransitions("0_2_3")));

		ScenarioData path3 = SgUtils.getConcretePath(st2, value1);
		for (State st : path3.getStates()) {
			// System.err.println(st.getId());
			assertTrue(st.getId().equalsIgnoreCase("0_2_4")
					|| st.getId().equalsIgnoreCase("0_2"));
		}
		assertTrue(path3.getTransitions().size() == 2);
		assertTrue(SgUtils.isEqualTransition(value1.getTransitions("0_2"),
				path3.getTransitions("0_2")));
		assertTrue(SgUtils.isEqualTransition(value1.getTransitions("0_2_4"),
				path3.getTransitions("0_2_4")));

		assertEquals("7", "" + SgUtils.get_end_id("0_2_7"));
		assertEquals("0_2_7", SgUtils.get_last_transition(value1).getId());

		/**
		 * test action equivalence
		 */
		Transition toTest = path1.getTransitions("0_2_3");
		Transition equiv = path1.getTransitions("0_2_5");
		Transition not_equiv = path1.getTransitions("0_2");

		assertTrue(SgUtils.contains_error(toTest.getAction()));
		assertTrue(!SgUtils.isEqualTransition(toTest, equiv));
		System.err.println(toTest.toString());
		System.err.println(equiv.toString());

		assertTrue(SgUtils.isEqualState(toTest.getSource(), equiv.getSource()));
		assertTrue(!SgUtils.isEqualActions(toTest.getAction(),
				equiv.getAction()));
		assertTrue(SgUtils.will_be_a_crash_transition_or_repeated_transition(
				path1, toTest));
		assertTrue(SgUtils.will_be_a_crash_transition_or_repeated_transition(
				path1, equiv));
		path1.remove_transition("0_2");
		assertTrue(!SgUtils.will_be_a_crash_transition_or_repeated_transition(
				path1, not_equiv));

	}

}
