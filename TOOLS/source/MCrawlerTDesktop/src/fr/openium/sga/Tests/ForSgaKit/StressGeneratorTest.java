/**
 * 
 */
package fr.openium.sga.Tests.ForSgaKit;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import kit.Inj.InjDataGenerator;
import kit.RandomValue.RandomValue;
import kit.RandomValue.RandomValueData;
import kit.RandomValue.RandomValueParser;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Stress.StressDataGenerator;
import kit.Utils.SgUtils;

import org.junit.Test;

/**
 * @author Stassia
 * 
 */
public class StressGeneratorTest {
	private static final String TAG = StressGeneratorTest.class.getName();

	@Test
	public void testGenerate() {
		ScenarioParser pa = new ScenarioParser();
		ScenarioData value = pa.parse(new File(
				(new File("").getAbsolutePath() + "/src/fr/openium/sga/Tests/ForSgaKit/stress_out.xml")));

		State exemple = value.getInitialState();
		ArrayList<String> testData = new ArrayList<String>();
		testData.add("1");
		testData.add("2");
		testData.add("10000");
		testData.add("kjdbksqld");
		testData.add("slnsqnlqlqs");
		testData.add("1");
		testData.add("2");
		testData.add("10000");
		testData.add("kjdbksqld");
		testData.add("slnsqnlqlqs");
		testData.add("1");
		testData.add("2");
		testData.add("10000");
		testData.add("kjdbksqld");
		testData.add("!!!!!!!!");
		StressDataGenerator gen = new StressDataGenerator(exemple, testData, 500);
		assertTrue(gen.generateStressTest(new File(
				(new File("").getAbsolutePath() + "/src/fr/openium/sga/Tests/ForSgaKit/events"))));

	}

	@Test
	public void testInjGenerate() {
		ScenarioParser pa = new ScenarioParser();
		ScenarioData value = pa.parse(new File(
				(new File("").getAbsolutePath() + "/src/fr/openium/sga/Tests/ForSgaKit/stress_out.xml")));

		State exemple = value.getInitialState();
		ArrayList<String> testData = new ArrayList<String>();
		InjDataGenerator gen = new InjDataGenerator(exemple, testData, 500);
		assertTrue(gen.generateInjectionTestData(new File(
				(new File("").getAbsolutePath() + "/src/fr/openium/sga/Tests/ForSgaKit/events"))));
	}

	@Test
	public void testInvalidEvents() {
		File in = new File(
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/OK/TOMDROID/tom/output/out.xml");
		File out = new File(
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/OK/TOMDROID/tom/output/events");
		File dataFile = new File("/Users/Stassia/Documents/Scen-genWorkSpace/sgd/TestData/testData.xml");
		HashSet<RandomValueData> data = new HashSet<RandomValueData>();
		RandomValueParser parser = new RandomValueParser();
		data = parser.parse(dataFile);
		ArrayList<String> testData = SgUtils.getTestDataSet(data, RandomValue.STRESS);
		ScenarioParser pa = new ScenarioParser();
		ScenarioData value = pa
				.parse(new File(
						("/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/OK/TOMDROID/tom/output/out.xml")));

		State targetState = value.getInitialState();
		StressDataGenerator dataGen = new StressDataGenerator(targetState, testData, 100);
		dataGen.generateStressTest(out);

	}
}
