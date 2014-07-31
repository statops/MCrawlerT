package fr.openium.sga.dot;

import java.io.File;
import java.io.IOException;

import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;

public class GenerateTraces {

	private static String mScenarioXmlDirectory;
	private static String mOutPutFileDirectory;
	private static String mtrace;

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length > 3) {
			for (int i = 0; i < args.length - 1;) {
				if (args[i].equals("-xml")) {
					mScenarioXmlDirectory = args[i + 1];
					System.out.println("Scenarion Path " + mScenarioXmlDirectory);
				}
				if (args[i].equals("-dotDirectory")) {
					mOutPutFileDirectory = args[i + 1];
					System.out.println("Out PutFile " + mOutPutFileDirectory);
				}
				if (args[i].equals("-tracenumber")) {
					mtrace = args[i + 1];
					System.out.println("trace max: " + mtrace);
				}
				i++;
			}

		} else {
			/**
			 * TODO
			 */
			System.out.println("help");
			System.exit(-1);
		}

		for (File file : new File(mScenarioXmlDirectory).listFiles()) {
			generateGifTrace(file);
		}

	}

	private static void generateGifTrace(File file) throws IOException, NumberFormatException,
			InterruptedException {
		ScenarioData value = ScenarioParser.parse(file);
		ScenarioToDot scToD = new ScenarioToDot(value, new File(mOutPutFileDirectory));
		if (scToD.generateDotFilePerTrace(Integer.parseInt(mtrace))) {
			scToD.generateGraph(scToD.getDotFile(), "gif");
		}

	}
}
