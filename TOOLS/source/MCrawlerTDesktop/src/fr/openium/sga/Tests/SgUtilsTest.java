package fr.openium.sga.Tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import kit.Config.Config;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Widget;
import kit.Stress.Event;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

import android.text.InputType;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;
import fr.openium.sga.strategy.FourmiStrategy;
import fr.openium.sga.threadPool.CrawlerTask;

public class SgUtilsTest {
	@Test
	public void testInMethod() throws SAXException, IOException,
			ParserConfigurationException {
		ScenarioParser pa = new ScenarioParser();
		@SuppressWarnings("static-access")
		// generer un scenarioData
		ScenarioData value = pa
				.parse(new File(
						(new File("").getAbsolutePath() + "/src/fr/openium/sga/Tests/scenario.xml")));
		/*
		 * creer un transition virtuel
		 */
		for (Transition transition : value.getTransitions())
			assertTrue(SgUtils.in(transition, value.getTransitions()));
	}

	@Test
	public void testgetNonHandledWidgetMethod() throws SAXException,
			IOException, ParserConfigurationException {
		ScenarioParser pa = new ScenarioParser();
		@SuppressWarnings("static-access")
		// generer un scenarioData
		ScenarioData value = pa
				.parse(new File(
						(new File("").getAbsolutePath() + "/src/fr/openium/sga/Tests/scenario.xml")));
		System.out.println("NonHandled  "
				+ SgUtils.getNonHandledWidget(value).size());
		System.out.print("  " + SgUtils.getNonHandledWidget(value).size());
		for (Widget wig : SgUtils.getNonHandledWidget(value)) {
			System.out.println("  " + wig.getName());
		}

		System.out.println("All  " + SgUtils.getAllWidget(value).size());
		for (Widget wig : SgUtils.getAllWidget(value)) {
			System.out.println("  " + wig.getName());
		}

		System.out
				.println("Handled  " + SgUtils.getHandledWidget(value).size());
		for (Widget wig : SgUtils.getHandledWidget(value)) {
			System.out.println("  " + wig.getName());
		}

		assertTrue(SgUtils.getMustReCrossActivity(value).contains(
				"activityNameY"));
		// assertFalse(SgUtils.getMustReCrossActivity(value).contains("activityNameX"));

	}

	@Test
	public void testgetRv() {
		String value = SgUtils.getRv(new File(new File("").getAbsolutePath()
				+ "/src/fr/openium/sga/Tests/out.xml"));
		System.out.println(value);
		if (value != null) {
			assertTrue((value.contains(Config.XMLEXTENSION)));
			assertTrue((value.endsWith(">")));
		} else {
			assertTrue(!new File(new File("").getAbsolutePath()
					+ "/src/fr/openium/sga/Tests/out.xml").exists());
		}

	}

	@Test
	public void testgreadRV() throws FileNotFoundException {
		String value = SgUtils.read(
				new Scanner((new File(new File("").getAbsolutePath()
						+ "/src/fr/openium/sga/Tests/rv"))), 1);
		assertNotNull(value);
		assertTrue((value.equalsIgnoreCase("1")));

		value = SgUtils.read(
				new Scanner((new File(new File("").getAbsolutePath()
						+ "/src/fr/openium/sga/Tests/rv"))), 2);

		assertTrue((value.equalsIgnoreCase("2")));

		value = SgUtils.read(
				new Scanner((new File(new File("").getAbsolutePath()
						+ "/src/fr/openium/sga/Tests/rv"))), 3);
		assertTrue((value.equalsIgnoreCase("3")));
	}

	@Test
	public void testPairWise() {
		ArrayList<String> test = new ArrayList<String>();
		test.add("" + 0);
		test.add("" + 13);
		test.add("" + 2);
		test.add("" + 16);
		test.add("" + 23);

		List<String> value = SgUtils
				.generateEditTextPairWiseSequence(
						new File("").getAbsolutePath()
								+ "/src/fr/openium/sga/Tests/rv", test, 3);

		for (String v : value) {
			System.out.println(v);
			String regex = "([[.][^\\:]]+)";
			Pattern inPattern = Pattern.compile(regex);
			Matcher matcher = inPattern.matcher(v);
			while (matcher.find()) {
				System.out.println(matcher.group(0));

			}
		}

	}

	@Test
	public void testBinaryOperation() {
		int value = SgUtils.getNativeTypeOf(InputType.TYPE_CLASS_NUMBER
				| InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		assertEquals(
				value,
				SgUtils.getNativeTypeOf(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
		value = kit.Utils.SgUtils.getNativeTypeOf(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		assertEquals(
				value,
				SgUtils.getNativeTypeOf(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));

	}

	@Test
	public void test_gen_new_states() {
		ScenarioData value1 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/sgutils/state1.xml")));
		ScenarioData value2 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/sgutils/state2.xml")));
		System.out.println();
		HashSet<State> new_statStates = SgUtils.getNewStates(value2, value1);
		System.out.println("Result states");
		if (new_statStates != null)
			for (State st : new_statStates)
				System.out.println(st.toString());
		assertEquals(50, new_statStates.size());
		/**
		 * activityNameZ
		 */

	}

	@Test
	public void test_get_path() throws CloneNotSupportedException {
		ScenarioData value2 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/Sgutils/out.xml")));

		java.util.Iterator<State> it = value2.getStates().iterator();
		State initState = value2.getInitialState();
		// assertEquals("MainActivity_0", initState.getName());
		assertEquals("2", initState.getId());
		// assertEquals("src", initState.getType());

		State to_check = null;
		do {
			to_check = it.next();
			if (to_check.getName().equalsIgnoreCase(
					"fr.openium.example.exampleforsgd.ButtonActivity")) {
				break;

			}
		} while (it.hasNext());
		ScenarioData result = SgUtils._getPath(to_check, value2);

		// assertEquals(3, result.getTransitions().size());
		System.out.println("=========================Path:");
		for (Transition tr : result.getTransitions()) {
			if (Config.DEBUG) {

				System.out.println(tr.toString());
			}
		}
		/**
		 * activityNameZ
		 */

		File out = new File(new File("").getAbsolutePath()
				+ "/TEST/Sgutils/out2.xml");
		ScenarioGenerator gen = new ScenarioGenerator(out.getPath());
		gen.generateXml(result);
		assertTrue(out.exists());

	}

	@Test
	public void test_solve() {
		ScenarioData value1 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/Sgutils/scenario_solve.xml")));
		if (ConfigApp.DEBUG) {
			System.out.println(value1.toString());
		}
		assertEquals(5, value1.getTransitions().size());

		Transition tr = value1.getTransitions("5");
		// assertEquals("end", tr.getDest().getName());

		value1 = SgUtils.solve(value1);
		assertTrue(value1.getState("5") != null);
		tr = value1.getTransitions("5");
		assertEquals("fr.openium.example.exampleforsgd.ListViewActivity", tr
				.getDest().getName());
		assertEquals(3, value1.getTransitions().size());
		if (ConfigApp.DEBUG) {
			System.out.println("after solve : ");
			System.out.println(value1.toString());
		}

	}

	@Test
	public void testSolve() {
		ScenarioData value1 = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/sg_utils_solve.xml")));
		value1 = SgUtils.solve(value1);
		assertTrue(value1.getStates().contains(
				new State(Scenario.END, false, true, "4")));
	}

	@Test
	public void testGenerateId() {
		HashSet<String> idls = new HashSet<String>();
		idls.add("" + 1);
		idls.add("" + 100);
		idls.add("" + 600);
		idls.add("" + 700);
		assertEquals("701", SgUtils.generateId(idls));
	}

	@Test
	public void testEqualTransition() {
		ScenarioData value1 = ScenarioParser
				.parse(new File(
						(new File("").getAbsolutePath() + "/TEST/Sgutils/sgutils_eq_transition.xml")));
		Transition t1 = value1.getTransitions("13");
		Transition t2 = value1.getTransitions("3");
		assertTrue(SgUtils.isEqualTransition(t1, t2));

	}

	@Test
	public void testEqualState() {
		ScenarioData value1 = ScenarioParser
				.parse(new File(
						(new File("").getAbsolutePath() + "/TEST/Sgutils/sgutils_eq_transition.xml")));
		State st1 = value1.getState("13", Scenario.SOURCE);
		State st2 = value1.getState("3", Scenario.SOURCE);
		assertTrue(SgUtils.isEqualState(st1, st2)
				&& (st1.isFinal() && st2.isFinal())
				|| (!st1.isFinal() && !st2.isFinal()));

	}

	@Test
	public void test_non_contain_error() {
		String out = "this is a Crash";
		assertFalse(SgUtils.report_not_contain_crash_(out));
		out = "this is a normal result";
		assertTrue(SgUtils.report_not_contain_crash_(out));
		out = "may be this one contain an "
				+ Exception.class.getCanonicalName();
		assertFalse(SgUtils.report_not_contain_crash_(out));

	}

	@Test
	public void test_Event_identification() {
		String out = "1" + Config.EVENTS_DELIMITERS + "android:id/title"
				+ Config.EVENTS_DELIMITERS + "android.widget.TextView"
				+ Config.EVENTS_DELIMITERS + "Simple Converter";
		Event event = SgUtils.get_event(out);
		assertEquals("key ", "1", "" + SgUtils.get_eventKey(out));
		assertEquals("name", "android:id/title", "" + event.getName());
		assertEquals("type", "android.widget.TextView", "" + event.getType());
		if (ConfigApp.DEBUG) {
			System.out.println(event.getType());
		}
		if (ConfigApp.DEBUG) {
			System.out.println(event.getValue());
		}
		assertEquals("value", "Simple Converter", event.getValue());

	}

	@Test
	public void test_GET_Event() {
		File eventFile = new File(new File("").getAbsolutePath()
				+ "/src/fr/openium/sga/Tests/events");
		if (!eventFile.exists()) {
			assertFalse("Events list does not exist", true);
		}
		ArrayList<String> content = null;
		try {
			content = (ArrayList<String>) FileUtils.readLines(eventFile);
		} catch (IOException e) {
			e.printStackTrace();
			assertFalse("Events list does not exist", true);

		}
		Iterator<String> content_iterator = content.iterator();
		if (!content_iterator.hasNext()) {
			throw new NullPointerException("The event List is Empty");
		}
		HashMap<Integer, Event> eventList = new HashMap<Integer, Event>(
				content.size());
		do {
			String ev = content_iterator.next();
			if (ConfigApp.DEBUG) {
				System.out.println(ev);
			}
			eventList.put(SgUtils.get_eventKey(ev), SgUtils.get_event(ev));
		} while (content_iterator.hasNext());

		assertEquals(50, eventList.size());
		assertEquals("" + 2, eventList.get(0).getName());
		assertEquals("kit.TestRunner.MenuView", eventList.get(0).getType());
		assertEquals(" '", eventList.get(0).getValue());

	}

	@Test
	public void test_get_in_transition() {
		ScenarioData tree = ScenarioParser.parse(new File(
				Config.CURRENT_DIRECTORY
						+ "/src/fr/openium/sga/Tests/scenario_solve.xml"));

		State source = tree.getState("3");
		ArrayList<Transition> list = SgUtils.get_out_transitions(source, tree);
		if (ConfigApp.DEBUG) {
			System.out.println(list.toString());
		}
		assertEquals(5, list.size());
		list = SgUtils.get_in_transitions(source, tree);
		if (ConfigApp.DEBUG) {
			System.out.println(list.toString());
		}
		assertEquals(2, list.size());

	}

	@Test
	public void test_hasEditText() {
		ScenarioData tree = ScenarioParser.parse(new File(
				Config.CURRENT_DIRECTORY + File.separator
						+ "TEST/Sgutils/edit.xml"));

		State third = tree.getState("0_14_16_30");
		State second = tree.getState("0_2");

		assertTrue(SgUtils.hasEditText(third));
		assertTrue(!SgUtils.hasEditText(second));

	}

	@Test
	public void EditTextEvent() {
		ScenarioData tree = ScenarioParser.parse(new File(
				Config.CURRENT_DIRECTORY + File.separator
						+ "TEST/Sgutils/edit.xml"));
		State third = tree.getState("0_14_16_30");
		assertTrue(SgUtils.hasEditText(third));
		ArrayList<Event> evebtList = SgUtils.get_ediText_List(third);
		System.out.println(third.toString());
		assertEquals(1, evebtList.size());

	}

	@Test
	public void testGetIdWithIdTask() {
		String id = "_10";
		String value = getid(id);
		assertTrue(value.equalsIgnoreCase("10"));
		id = "10_20";
		value = getid(id);
		assertTrue(value.equalsIgnoreCase("20"));
		assertTrue(getid("10_20_1").equalsIgnoreCase("1"));
		assertTrue(getid("10_").equalsIgnoreCase("10"));
		assertTrue(getid("10_20_1____").equalsIgnoreCase("1"));
		assertTrue(getid("___10___20_1____123").equalsIgnoreCase("123"));
	}

	@Test
	public void testGetParentId() {

		assertTrue(getParentid("10_20_1_").equalsIgnoreCase("10_20"));
		assertTrue(getParentid("0_").equalsIgnoreCase("0"));
		assertTrue(getParentid("10_20_1_").equalsIgnoreCase("10_20"));
		assertTrue(getParentid("_0_").equalsIgnoreCase("0"));

	}

	@Test
	public void testConcretPath() {

		ArrayList<String> id = SgUtils
				.get_transition_id_from_state_id_("0_1_2_3");
		assertTrue(!id.isEmpty());
		assertTrue(id.size() == 3);
		assertEquals("0_1", id.get(0));
		assertEquals("0_1_2_3", id.get(2));

	}

	@Test
	public void testConcretPathInScenario() throws CloneNotSupportedException {

		ArrayList<String> id = SgUtils.get_transition_id_from_state_id_("0_2");
		assertTrue(!id.isEmpty());
		assertTrue(id.size() == 1);

		ScenarioData tree = ScenarioParser.parse(new File(
				Config.CURRENT_DIRECTORY + File.separator
						+ "TEST/sgutils/edit.xml"));
		ScenarioData path = (SgUtils.getConcretePath(
				tree.getState("0_3", Scenario.DEST), tree));
		assertTrue("", path.getInitialState().getId().equalsIgnoreCase("0_3"));
	}

	@Test
	public void testIsin() {

		ArrayList<String> id1 = SgUtils
				.get_transition_id_from_state_id_("0_1_2_3");
		ArrayList<String> id2 = SgUtils
				.get_transition_id_from_state_id_("0_1_2_3_5_1_3_8_3");

		assertTrue(SgUtils.isIn(id1, id2));
		id1 = SgUtils.get_transition_id_from_state_id_("0_2_2_3");
		id2 = SgUtils.get_transition_id_from_state_id_("0_1_2_3_5_1_3_8_3");

		assertTrue(!SgUtils.isIn(id1, id2));
	}

	@Test
	public void testStringsorter() {

		ArrayList<String> id2 = SgUtils
				.get_transition_id_from_state_id_("0_9_2_3_5_1_3_8_3");
		ArrayList<String> id1 = SgUtils
				.get_transition_id_from_state_id_("0_2_2_3");
		id2.addAll(id1);

		assertEquals(SgUtils.get_highest_id(id2), "0_9_2_3_5_1_3_8_3");

	}

	@Test
	public void testPathGeneration() throws IOException {
		File out = new File(Config.CURRENT_DIRECTORY + File.separator
				+ "/TEST/testPath");

		/**
		 * voir le scenario pour les nouvelles etats
		 */
		SgdEnvironnement env = new SgdEnvironnement();
		env.setOutDirectory(out.getPath());
		env.setThread_number(1);
		/**
		 * '/home/stassia/Documents/EXP/output/out/Scenario/_out0.xml'
		 * /home/stassia/Documents/CAPTURETEST/sgd/TEST/testPath
		 */
		/*
		 * FileUtils .copyFileToDirectory( new File(
		 * "/home/stassia/Documents/EXP/output/out/Scenario/_out0.xml"), new
		 * File( "/home/stassia/Documents/CAPTURETEST/sgd/TEST/testPath"));
		 */
		FileUtils.deleteDirectory(new File(out.getPath() + "/PATH"));
		ScenarioData tree = ScenarioParser.parse(new File(
				Config.CURRENT_DIRECTORY + File.separator
						+ "/TEST/Sgutils/edit.xml"));
		CrawlResult res = new CrawlResult(out, new FourmiStrategy(env,
				"MainActivity"));

		ArrayList<CrawlerTask> tasks = res.update(new CrawlerTask(tree));

		/**
		 * assertions
		 */

		assertNotNull(tree);

		assertNotNull(res.getScenarioData());
		assertNotNull(res.getScenarioData().getTransitions());
		assertTrue(!res.getScenarioData().getTransitions().isEmpty());

		assertNotNull(tasks);
		assertNotNull(!tasks.isEmpty());
		/**
		 * analyse de chaque tâche
		 */
		int i = 0;
		assertEquals(27, tasks.size());
		for (CrawlerTask taskscen : tasks) {
			System.err.println("tache " + i++);
			assertNotNull(taskscen);
			assertNotNull(taskscen.getPath().getTransitions());
			assertTrue(!taskscen.getPath().getTransitions().isEmpty());
			//assertEquals(1, taskscen.getPath().getTransitions().size());
			assertEquals(taskscen.getPath().getInitialState().getId(), taskscen
					.getPath().getTransitionsId().get(0));

		}

	}

	/**
	 * @param string
	 * @return
	 */
	private String getParentid(String id) {
		StringTokenizer st = new StringTokenizer(id, Scenario.IDSEPARATOR);

		String value = "";
		if (!st.hasMoreTokens()) {
			value = "_" + id;
		}
		do {
			value = value + "_" + st.nextToken();

		} while (st.countTokens() > 1);
		if (ConfigApp.DEBUG) {
			System.out.println(value);
		}
		return value.substring(1);
	}

	/**
	 * @param id
	 * @return
	 */
	private String getid(String id) {
		StringTokenizer st = new StringTokenizer(id, Scenario.IDSEPARATOR);
		String value;
		if (!st.hasMoreTokens()) {
			;
			value = id;
		}
		do {
			value = st.nextToken();

		} while (st.hasMoreTokens());
		if (ConfigApp.DEBUG) {
			System.out.println(value);
		}
		return value;
	}
}
