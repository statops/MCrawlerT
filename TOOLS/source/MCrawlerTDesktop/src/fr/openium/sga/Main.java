/**
 * 
 */
package fr.openium.sga;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import fr.openium.JunitTestCasesGenerator;
import fr.openium.sga.SecurityTesting.BruteForceTest.BruteForceManager;
import fr.openium.sga.SecurityTesting.INJTest.INJRequestTaskManager;
import fr.openium.sga.SecurityTesting.StressTest.StressTaskManager;
import fr.openium.sga.Utils.ActivityCoverageUtils;
import fr.openium.sga.Utils.Utils;
import fr.openium.sga.codegen.SgdCodeGen;
import fr.openium.sga.displayer.Displayer;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.reporter.ModelReporter;

/**
 * @author Stassia
 * 
 */
public class Main {
	private static final String TAG = Main.class.getName();

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (ConfigApp.DEBUG) {
			info(TAG);
			System.out.println("Main");
		}

		// [OPTIONS ][activityCovergae/Explore/codeGen]
		if (args.length == 0) {
			help();
			return;
		}

		if (args[0].equals("display")) {
			System.err
					.println("Please use STSDisplayer instead of SGD project.");
			return;
		}

		if (args[0].equals("storyBoard")) {
			info("display storyBoard ");
			Displayer.main(args);
			return;
		}

		if (args[0].equals("modelReport")) {
			info("generate report ");
			ModelReporter.main(args);
			return;
		}

		if (args[0].equals("crashTest")) {
			info("generate test cases for detected crash ");
			JunitTestCasesGenerator.main(args);
			return;
		}

		if (args[0].equals("activityCoverage")) {
			info("Activity Coverage Options");
			File manifest = null;
			File out = null;
			if ((args.length) > 4 && !pair(args.length)) {
				for (int i = 1; i < args.length - 1;) {
					if (args[i].equals("-manifest")) {
						manifest = new File((args[i + 1]));
					}
					if (args[i].equals("-scen")) {
						out = new File((args[i + 1]));
					}
					i++;
				}
				ActivityCoverageUtils actCov = new ActivityCoverageUtils(
						manifest, out);
				info("Activity coverage = " + actCov.getActivityCoverage()
						+ " %");
				return;
			}
		}
		if (args.length == 2) {
			if (args[0].equals("inputfile")) {
				List<String> param = FileUtils.readLines(new File(args[1]));
				args = null;
				ArrayList<String> attr = new ArrayList<String>();
				for (String paramElt : param) {
					StringTokenizer st = new StringTokenizer(paramElt);
					while (st.hasMoreTokens()) {
						attr.add(st.nextToken());
					}
				}

				args = attr.toArray(new String[0]);

			}
			if (args == null) {
				help();
				return;
			}
			for (String elt : args) {
				if (ConfigApp.DEBUG) {
					System.out.println(elt);
				}
			}

		}

		if (args[0].equals("explore")) {
			if (ConfigApp.DEBUG) {
				info(TAG);
				System.out.println("explore");
			}
			Emma.main(args);
			return;
		}

		if (args[0].equals("stress")) {
			if (ConfigApp.DEBUG) {
				info(TAG);
				System.out.println("stress");
			}
			StressTaskManager.main(args);
			return;
		}

		if (args[0].equals("inj")) {
			if (ConfigApp.DEBUG) {
				info(TAG);
				System.out.println("inj");
			}
			INJRequestTaskManager.main(args);
			return;
		}

		if (args[0].equals("bruteForce")) {
			if (ConfigApp.DEBUG) {
				info(TAG);
				System.out.println("bruteForce");
			}
			BruteForceManager.main(args);
			return;
		}

		if (args[0].equals("codeGen")) {
			if (ConfigApp.DEBUG) {
				info(TAG);
				System.out.println("codeGen");
			}
			SgdCodeGen.main(args);
			return;

		} else {
			help();
		}
		if (ConfigApp.DEBUG) {
			info(TAG);
			System.out.println("Exit");
		}
		System.exit(0);
	}

	public static void info(String string) {
		Utils.info(string);

	}

	/**
	 * 
	 */
	private static void help() {
		if (ConfigApp.DEBUG) {
			info(TAG);
			System.out.println("help");
		}
		System.out
				.println("please choose at least one of thses options  "
						+ "  \n activityCoverage \n or explore \n or codeGen \n or stress \n or bruteForce \n display \n storyBoard \n modelReport \n crashTest");
	}

	/**
	 * @param string
	 */
	public static void errorLog(String string) {
		if (ConfigApp.DEBUG) {
			Utils.getLogger().log(Level.SEVERE, string);
		}

	}

	/**
	 * @param length
	 * @return
	 */
	public static boolean pair(int length) {
		if (length % 2 == 0) {
			return true;
		}
		return false;
	}

}
