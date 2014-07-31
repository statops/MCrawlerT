package fr.openium.sga.codegen;

import java.io.File;
import java.io.IOException;

import com.sun.codemodel.JClassAlreadyExistsException;

import fr.openium.sga.ConfigApp;

/**
 * Call for generating code
 * 
 * args: - manifest the manifest Path
 * 
 * - tp the test project
 * 
 * @author Stassia
 * 
 */
public class SgdCodeGen {
	private static String mTestProject = null;
	private static String mManifestPath = null;
	private static String mPairwiseSequenceNumber = null;
	private static String mStrategy;
	private static String mExpectedActivity = null;
	private static String mLauncherActivity;

	/**
	 * @param args
	 * @throws IOException
	 * @throws JClassAlreadyExistsException
	 */
	public static void main(String[] args) throws JClassAlreadyExistsException, IOException {
		if (args.length > 10) {

			for (int i = 0; i < args.length - 1;) {
				if (args[i].equals("-manifest")) {
					mManifestPath = args[i + 1];
				}
				if (args[i].equals("-tp")) {
					mTestProject = args[i + 1];
				}
				if (args[i].equals("-pn")) {
					mPairwiseSequenceNumber = args[i + 1];
				}

				if (args[i].equals("-strategy")) {
					mStrategy = args[i + 1];
				}
				if (args[i].equals("-expectedAct")) {
					mExpectedActivity = args[i + 1];
				}
				if (args[i].equals("-launcherAct")) {
					mLauncherActivity = args[i + 1];
				}

				i++;
			}

		} else {
			System.out
					.println("[help]: Command: - manifest the manifest Path,\n -tp the testProject \n -pn the number of pairwise sequence \n -strategy the type of strategy \n -expectedAct the expected activity after a Splash");
			return;
		}
		if (mTestProject != null && mManifestPath != null) {
			if (ConfigApp.DEBUG) {
				System.out.println("SgdCodeGen");
			}
			ScenarioCodeGen sc = new ScenarioCodeGen(new File(mManifestPath), new File(mTestProject + "/src"));
			sc.setPairwiseSequence(mPairwiseSequenceNumber);
			sc.setStrategyType(mStrategy);
			sc.setExpectectedActivity(mExpectedActivity);
			if (mLauncherActivity!=null)
			sc.setLauncherActivity(mLauncherActivity);
			sc.generate();
		} else {
			System.out.println("[info]: null parameters");

		}

	}
}
