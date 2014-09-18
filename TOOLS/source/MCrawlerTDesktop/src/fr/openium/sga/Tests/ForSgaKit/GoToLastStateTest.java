package fr.openium.sga.Tests.ForSgaKit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Utils.SgUtils;

import org.junit.Test;

/**
 * test go to last state
 * 
 * @author Utilisateur
 * 
 */
public class GoToLastStateTest {

	private ScenarioData get_scenarioData() {
		ScenarioData value2 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/path/_path.xml")));
		return value2;
	}

	@Test
	public void test_get_last_state() {
		ScenarioData path = get_scenarioData();
		State st = SgUtils.get_last_state(path);
		assertEquals("last state name is not equal",
				"com.siri.budgetdemo.AddExpense", st.getName());

	}

	@Test
	public void test_get_dest() {
		ScenarioData path = get_scenarioData();
		State st = path.getState("0_9_15", Scenario.SOURCE,
				"com.siri.budgetdemo.AddExpense");
		assertEquals("source name is not equal",
				"com.siri.budgetdemo.AddExpense", st.getName());
		State dest = SgUtils.get_dest(st, path);
		assertEquals("dest name is not equal",
				"com.siri.budgetdemo.AddExpense", dest.getName());

		st = path.getState("0_9", Scenario.SOURCE, "com.siri.budgetdemo.Menu");
		assertEquals("source name is not equal", "com.siri.budgetdemo.Menu",
				st.getName());
		dest = SgUtils.get_dest(st, path);
		assertEquals("dest name is not equal",
				"com.siri.budgetdemo.AddExpense", dest.getName());

	}

	@Test
	public void test_closest_pred_sup() {
		ArrayList<Integer> inList = new ArrayList<Integer>();
		inList.add(10);
		inList.add(8);
		inList.add(20);
		inList.add(3);
		inList.add(17);
		inList.add(9);
		assertEquals(9, SgUtils.get_closest_pred_sup(8, inList));
		assertEquals(10, SgUtils.get_closest_pred_sup(9, inList));
		assertEquals(17, SgUtils.get_closest_pred_sup(10, inList));
		assertEquals(Integer.MAX_VALUE,
				SgUtils.get_closest_pred_sup(20, inList));
		assertTrue(SgUtils.is_closest_max_id(10, 9, inList));
		assertTrue(SgUtils.is_closest_max_id(Integer.MAX_VALUE, 20, inList));

	}

	@Test
	public void test_closest_pred_inf() {
		ArrayList<Integer> inList = new ArrayList<Integer>();
		inList.add(10);
		inList.add(8);
		inList.add(20);
		inList.add(3);
		inList.add(17);
		inList.add(9);
		assertEquals(9, SgUtils.get_closest_pred_inf(10, inList));
		assertEquals(10, SgUtils.get_closest_pred_inf(17, inList));
		assertEquals(17, SgUtils.get_closest_pred_inf(20, inList));
		assertEquals(Integer.MAX_VALUE, SgUtils.get_closest_pred_inf(3, inList));

	}

	@Test
	public void test_get_sequence() {

		ScenarioData path = get_scenarioData();
		State st = path.getState("0_9_15_23", Scenario.SOURCE);
		// Transition tr=path.getTransitions("2");
		// HashMap<String, String> seq=SgUtils.get_ediText_sequence(tr);
		HashMap<String, String> seq = SgUtils.get_ediText_sequence(st, path);
		assertEquals("sequence number", 6, seq.size());
		assertEquals("seq5", "erft@wanadoo.fr",
				seq.get("com.siri.budgetdemo:id/addexpense_editpayee1"));
		assertEquals("seq2", "world",
				seq.get("com.siri.budgetdemo:id/addexpense_editpayee2"));
		assertEquals("seq3", "test",
				seq.get("com.siri.budgetdemo:id/addexpense_editpayee3"));
		assertEquals("seq4", "0",
				seq.get("com.siri.budgetdemo:id/addexpense_editpayee5"));
	}

	@Test
	public void test_get_Event() {
		ScenarioData path = get_scenarioData();
		State st = path.getState("0_9_15_23", Scenario.SOURCE);
		HashMap<String, String> event = SgUtils.get_event(st, path);
		assertEquals("event number", 1, event.size());
		assertEquals("event", "Click clickableView",
				event.get("com.siri.budgetdemo:id/addexpense_editrepeat"));

		st = path.getState("0_9_15", Scenario.SOURCE);
		event = SgUtils.get_event(st, path);
		assertEquals("event number", 1, event.size());
		assertEquals("event", "click radio_button",
				event.get("com.siri.budgetdemo:id/repeat_on"));

	}

	@Test
	public void test_perform_action_() {

	}

	@Test
	public void test_Test_Event() {
		/**
		 * generate a path
		 */
		ScenarioData path = get_scenarioData();
		ScenarioData re_build_path = new ScenarioData();
		re_build_path.setAuthors(path.getAuthors());
		re_build_path.setVersion(path.getVersion());
		re_build_path.setActions(path.getActions());
		re_build_path.setStates(path.getInitialState(), true, false);
		for (Transition tr : path.getTransitions()) {
			State source = path.getState(tr.getId(), Scenario.SOURCE);
			State dest = path.getState(tr.getId(), Scenario.DEST);
			re_build_path.addStates(source);
			re_build_path.addStates(dest);
			re_build_path.setTransitions(tr);

		}
		new ScenarioGenerator(new File("").getAbsolutePath() + "/TEST/out.xml")
				.generateXml(re_build_path);

	}

}
