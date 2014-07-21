package fr.openium;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import kit.Scenario.Action;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Utils.SgUtils;
import kit.Utils.TransitionIdComparator;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;

import fr.openium.AbstractGenerator.AbstractJuniGenerator;

public class JunitTestCasesGenerator extends AbstractJuniGenerator {
	/**
	 * Liste des chemins
	 */
	private HashSet<ScenarioData> mPaths = new HashSet<ScenarioData>();

	private HashSet<String> mTestList = new HashSet<String>();

	public JunitTestCasesGenerator(File manifestXml, File output) {

	}

	/**
	 * 
	 * 
	 * @param output
	 *            JUnit file .java
	 * @return
	 */

	public HashSet<java.lang.String> generate(String packagename,
			String test_name, String ActivityName, File out) {
		JCodeModel cm = new JCodeModel();
		JDefinedClass dclass;
		addImportedClass(cm);
		try {
			dclass = cm._class(getTestClassName(packagename, test_name));
			JClass SgdTestCase = null;

			SgdTestCase = cm.ref(fr.openium.function.MCrawlerTTestCase.class);

			JClass mextends = cm.ref(SgdTestCase.fullName());
			dclass._extends(mextends);
			/**
			 * the target package ID
			 */
			JVar TARGET_PACKAGE_ID = dclass.field(JMod.PRIVATE + JMod.STATIC
					+ JMod.FINAL, String, "TARGET_PACKAGE_ID");
			TARGET_PACKAGE_ID.init(JExpr.lit(packagename));

			/**
			 * the LauncherActivity
			 */
			JVar LAUNCHER_ACTIVITY_FULL_CLASSNAME = dclass.field(JMod.PRIVATE
					+ JMod.STATIC + JMod.FINAL, String,
					"LAUNCHER_ACTIVITY_FULL_CLASSNAME");
			LAUNCHER_ACTIVITY_FULL_CLASSNAME.init(JExpr.lit(ActivityName));

			/**
			 * the LauncherActivity
			 */
			String ExpectedActivityName = ActivityName;

			for (ScenarioData scen : mPaths) {
				if (scen != null) {
					if (scen.getInitialState() != null) {
						ExpectedActivityName = scen.getInitialStateName();
						break;
					}
				}

			}

			JVar EXPECTED_INITIAL_STATE_ACTIVITY = dclass.field(JMod.PRIVATE
					+ JMod.STATIC + JMod.FINAL, String,
					"EXPECTED_INITIAL_STATE_ACTIVITY");

			EXPECTED_INITIAL_STATE_ACTIVITY.init(JExpr
					.lit(ExpectedActivityName));

			dclass.direct(java.lang.String.format(TEMPLATE, test_name));
			for (JClass classToinsert : classToImport) {
				JVar newClass = dclass.field(JMod.PRIVATE + JMod.STATIC,
						classToinsert,
						" var_" + classToImport.indexOf(classToinsert));

			}

			/**
			 * add test methods
			 */
			Iterator<ScenarioData> scen = mPaths.iterator();
			int path_occurrence = 0;
			/**
			 * a test per path test_i ()
			 */
			do {
				if (!scen.hasNext()) {
					break;
				}
				path_occurrence++;
				ScenarioData current = scen.next();

				// test_i ()
				JMethod test = dclass.method(JMod.PUBLIC, cm.VOID,
						TESTCASE_NAME + path_occurrence);
				JBlock testBlock = test.body();

				int j = 0;
				/**
				 * trier les transitions
				 */
				Collections.sort(current.getTransitions(),
						new TransitionIdComparator());

				for (int i = current.getTransitions().size() - 1; i >= 0; i--) {
					/**
					 * source
					 */
					kit.Scenario.Transition tr = current.getTransitions()
							.get(i);
					State s1 = tr.getSource();
					/**
					 * add state widget source
					 */
					ArrayList<String> WigName = new ArrayList<String>();
					for (int h = 0; h < s1.getWidgets().size(); h++) {
						kit.Scenario.Widget wig = s1.getWidgets().get(h);
						String sourceState = java.lang.String.format(
								WIDGET_FORMAT, "" + i + j, "" + h,
								wig.getName(), wig.getType(), wig.getPosX(),
								wig.getPosY(), wig.getVisibility(),
								wig.getSatusEnable(), wig.getStatusPressed(),
								wig.getStatusShown(), wig.getStatusSelection(),
								wig.getValue());
						testBlock.directStatement(sourceState);
						WigName.add(java.lang.String.format(WIDGET_NAME_FORMAT,
								"" + i + j, "" + h));
					}
					/**
					 * add state source
					 */
					String sourceState = java.lang.String.format(STATE_FORMAT,
							"" + j, s1.getName(), "" + s1.isInit(),
							"" + s1.isFinal(), s1.getId());
					for (String wiString : WigName) {
						sourceState = sourceState + "," + wiString;
					}
					testBlock.directStatement(sourceState + ");");
					testBlock.directStatement(java.lang.String.format(
							STATE_CURRENT_FORMAT, "" + j));
					testBlock.directStatement(java.lang.String.format(
							IS_EQUAL_FORMAT, "" + j, "" + j));
					/**
					 * perform Action
					 */
					WigName = new ArrayList<String>();
					for (int h = 0; h < tr.getAction().size(); h++) {
						Action current_action = tr.getAction().get(h);
						kit.Scenario.Widget wig = current_action.getWidget();
						String wigetBlock = java.lang.String.format(
								ACTION_WIDGET_FORMAT, "" + i, "" + h,
								wig.getName(), wig.getType(), wig.getPosX(),
								wig.getPosY(), wig.getVisibility(),
								wig.getSatusEnable(), wig.getStatusPressed(),
								wig.getStatusShown(), wig.getStatusSelection(),
								wig.getValue());
						testBlock.directStatement(wigetBlock);
						String wig_name = java.lang.String.format(
								ACTION_WIDGET_NAME_FORMAT, "" + i, "" + h);
						String actionBlock = java.lang.String.format(
								ACTION_FORMAT, "" + i, "" + h,
								current_action.getName(), wig_name, wig_name);

						testBlock.directStatement(actionBlock);
						WigName.add(java.lang.String.format(ACTION_NAME_FORMAT,
								"" + "" + i, "" + h));
					}

					sourceState = java.lang.String.format(
							PERFORM_ACTION_FORMAT, "0L");
					for (String wiString : WigName) {
						sourceState = sourceState + "," + wiString;
					}
					testBlock.directStatement(sourceState + ");");

					/**
					 * dest
					 */
					State s2 = tr.getDest();
					/**
					 * add state widget source
					 */
					if (SgUtils.contains_error(tr.getAction())) {
						break;
					}
					WigName = new ArrayList<String>();
					for (int h = 0; h < s2.getWidgets().size(); h++) {
						kit.Scenario.Widget wig = s2.getWidgets().get(h);
						String destState = java.lang.String.format(
								WIDGET_FORMAT, "" + i + j + 1, "" + h,
								wig.getName(), wig.getType(), wig.getPosX(),
								wig.getPosY(), wig.getVisibility(),
								wig.getSatusEnable(), wig.getStatusPressed(),
								wig.getStatusShown(), wig.getStatusSelection(),
								wig.getValue());
						testBlock.directStatement(destState);
						WigName.add(java.lang.String.format(WIDGET_NAME_FORMAT,
								"" + i + j + 1, "" + h));
					}

					/**
					 * add state dest
					 */
					String destState = java.lang.String.format(STATE_FORMAT, ""
							+ j + 1, s2.getName(), "" + s2.isInit(),
							"" + s2.isFinal(), s2.getId());
					for (String wiString : WigName) {
						destState = destState + "," + wiString;
					}
					testBlock.directStatement(destState + ");");
					testBlock.directStatement(java.lang.String.format(
							STATE_CURRENT_FORMAT, "" + j + 1));
					testBlock.directStatement(java.lang.String.format(
							IS_EQUAL_FORMAT, "" + j + 1, "" + j + 1));

					j++;
				}
				mTestList.add(getTestMethodeId(dclass, test));
			} while (scen.hasNext());

			/**
			 * String.format(
			 * "[monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s"
			 * ,
			 * 
			 * this.executor.getPoolSize(),
			 * 
			 * this.executor.getCorePoolSize(),
			 * 
			 * this.executor.getActiveCount(),
			 * 
			 * this.executor.getCompletedTaskCount(),
			 * 
			 * this.executor.getTaskCount(),
			 * 
			 * this.executor.isShutdown(),
			 * 
			 * this.executor.isTerminated()));
			 */
			if (!out.exists()) {
				out.mkdirs();
			}
			cm.build(out);
		} catch (JClassAlreadyExistsException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();

		}

		return mTestList;
	}

	/**
	 * @param dclass
	 * @param test
	 * @return
	 */
	private java.lang.String getTestMethodeId(JDefinedClass dclass, JMethod test) {
		return dclass.fullName() + "#" + test.name();
	}

	/**
	 * @param packagename
	 * @param test_name
	 * @return
	 */

	public boolean generate(File papth, File out) {
		return false;
	}

	private final static String WIDGET_FORMAT = "MCrawlerVariable.Widget s%suielt%s= mVariable.new Widget(\"%s\",\"%s\",\"%s"
			+ "\",\"%s"
			+ "\",\"%s"
			+ "\",\"%s"
			+ "\",\"%s"
			+ "\",\""
			+ "%s"
			+ "\",\"" + "%s" + "\", \"" + "%s" + "\");";
	private final static String WIDGET_NAME_FORMAT = "s" + "%s" + "uielt"
			+ "%s";
	private final static String STATE_FORMAT = "MCrawlerVariable.State s"
			+ "%s" + " = mVariable.new State(\"" + "%s" + "\", " + "%s" + ", "
			+ "%s" + ",\"" + "%s" + "\"";// ,
											// \""
											// +
											// "%s"
											// +
											// "\", \""
											// +
											// "%s"
											// +
											// "\",\""
											// +
											// "%s"
											// +
											// "\");";
	private final static String STATE_CURRENT_FORMAT = "MCrawlerVariable.State s"
			+ "%s" + "_current = getCurrentState();";
	private final static String IS_EQUAL_FORMAT = "assertTrue(MCrawlerFunctions.isEqualState(s"
			+ "%s" + ", s" + "%s" + "_current));";
	private final static String ACTION_WIDGET_NAME_FORMAT = "t" + "%s"
			+ "uielt" + "%s";
	private final static String ACTION_WIDGET_FORMAT = "MCrawlerVariable.Widget t"
			+ "%s"
			+ "uielt"
			+ "%s"
			+ " = mVariable.new Widget(\""
			+ "%s"
			+ "\",\""
			+ "%s"
			+ "\",\""
			+ "%s"
			+ "\",\""
			+ "%s"
			+ "\", \""
			+ "%s"
			+ "\", \""
			+ "%s"
			+ "\",\""
			+ "%s"
			+ "\",\""
			+ "%s"
			+ "\",\"" + "%s" + "\", \"" + "%s" + "\");";
	private final static String ACTION_FORMAT = "MCrawlerVariable.Action t%s_action_%s= mVariable.new Action(\"%s\",%s"
			+ ");";
	private final static String ACTION_NAME_FORMAT = "t" + "%s" + "_action_"
			+ "%s";
	private final static String PERFORM_ACTION_FORMAT = "performAction(%s"; // ajouter
	private final static String TESTCASE_NAME = "test_";

	// la
	// liste
	// d'action

	/**
	 * @param testPath
	 */
	public void addPaths(ScenarioData testPath) {
		mPaths.add(testPath);
	}

	/**
	 * modelReport out.xml OuputDirectory
	 * 
	 * @param args
	 * @throws CloneNotSupportedException
	 */
	public static void main(String[] args) throws CloneNotSupportedException {
		ScenarioData model = ScenarioParser.parse(new File(args[1]));
		generateCrashTest(new File(args[2]), model);

	}

	public static ArrayList<String> generateCrashTest(File outputFile,
			ScenarioData treeModel) throws CloneNotSupportedException {
		ArrayList<String> testName = new ArrayList<String>();

		/**
		 * cas de test pour chaque crash
		 */

		HashSet<String> errorNumber = SgUtils.get_error_number(treeModel);
		if (errorNumber.isEmpty()) {
			System.out.println("No JUNIT for crash testing is generated");
			return testName;
		}

		Iterator<String> errors = errorNumber.iterator();
		if (errors.hasNext()) {
			String currentErrors;
			do {

				currentErrors = errors.next();

				/**
				 * add path
				 */
				ScenarioData path = SgUtils.getConcretePath(
						treeModel.getState(currentErrors, Scenario.DEST),
						treeModel);
				if (path == null || path.getTransitions() == null
						|| path.getTransitions().isEmpty()) {
					continue;
				}

				JunitTestCasesGenerator gen = new JunitTestCasesGenerator(null,
						outputFile);
				gen.addPaths(path);

				/**
				 * pack.cl#test_x
				 */
				testName.addAll(gen.generate("test", "CrashTest"
						+ currentErrors, treeModel.getInitialState()
						.getShortName(), outputFile));

			} while (errors.hasNext());

		}
		System.out.println(" Crash test may be available in: "
				+ outputFile.toString());
		return testName;

	}

}
