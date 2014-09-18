package fr.openium.sga.Tests.MCrawlerTExtension;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import kit.Config.Config;
import kit.Scenario.BroadCastEnvironment;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Utils.SgUtils;

import org.junit.Test;
import org.xml.sax.SAXException;

public class ScenarioTest {

	@Test
	public void testUeParse() throws SAXException, IOException,
			ParserConfigurationException {
		ScenarioData value = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/extension/scenario.xml")));

		State st2 = value.getState("2", Scenario.DEST);
		assertNotNull(st2);
		assertEquals(st2.getUserEnvironments().size(), 3);

		State st33 = value.getState("33", Scenario.DEST);
		assertNotNull(st33);
		assertEquals(st33.getUserEnvironments().size(), 3);
		assertTrue(!SgUtils.isEqualState(st2, st33));
		assertTrue(SgUtils.isEqualState(value.getState("333", Scenario.DEST),
				st33));
		assertTrue(!SgUtils.isEqualState(value.getState("333", Scenario.DEST),
				st2));

	}

	@Test
	public void testBroadCastParse() throws SAXException, IOException,
			ParserConfigurationException {
		ScenarioData value = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/extension/scenario.xml")));

		State st2 = value.getState("2", Scenario.DEST);
		assertNotNull(st2);
		assertEquals(st2.getBroadCastEnvironments().size(), 2);

		State st33 = value.getState("33", Scenario.DEST);
		assertNotNull(st33);
		assertEquals(st33.getBroadCastEnvironments().size(), 1);
		BroadCastEnvironment b1 = st33.getBroadCastEnvironments().get(0);
		assertEquals(b1.getName(), "b1");
		assertEquals(b1.getProperties().get("status"), "true");

	}

	@Test
	public void testUeGenerate() throws SAXException, IOException,
			ParserConfigurationException {
		ScenarioData value = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/extension/scenario.xml")));

		File out = new File(Config.CURRENT_DIRECTORY
				+ "/TEST/extension/gen.xml");
		ScenarioGenerator gen = new ScenarioGenerator(out.getPath());
		gen.generateXml(value);
		assertTrue(out.exists());

		value = ScenarioParser.parse(new File(
				(new File("").getAbsolutePath() + "/TEST/extension/gen.xml")));

		State st2 = value.getState("2", Scenario.DEST);
		assertNotNull(st2);
		assertEquals(st2.getUserEnvironments().size(), 3);

		State st33 = value.getState("33", Scenario.DEST);
		assertNotNull(st33);
		assertEquals(st33.getUserEnvironments().size(), 3);
		assertTrue(!SgUtils.isEqualState(st2, st33));
		assertTrue(SgUtils.isEqualState(value.getState("333", Scenario.DEST),
				st33));
		assertTrue(!SgUtils.isEqualState(value.getState("333", Scenario.DEST),
				st2));

	}

	@Test
	public void testBdGenerate() throws SAXException, IOException,
			ParserConfigurationException {
		ScenarioData value = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/extension/scenario.xml")));

		File out = new File(Config.CURRENT_DIRECTORY
				+ "/TEST/extension/gen.xml");
		ScenarioGenerator gen = new ScenarioGenerator(out.getPath());
		gen.generateXml(value);
		assertTrue(out.exists());

		value = ScenarioParser.parse(new File(
				(new File("").getAbsolutePath() + "/TEST/extension/gen.xml")));

		State st2 = value.getState("2", Scenario.DEST);
		assertNotNull(st2);
		assertEquals(st2.getBroadCastEnvironments().size(), 2);

		State st33 = value.getState("33", Scenario.DEST);
		assertNotNull(st33);
		assertEquals(st33.getBroadCastEnvironments().size(), 1);
		BroadCastEnvironment b1 = st33.getBroadCastEnvironments().get(0);
		assertEquals(b1.getName(), "b1");
		assertEquals(b1.getProperties().get("status"), "true");

	}

}
