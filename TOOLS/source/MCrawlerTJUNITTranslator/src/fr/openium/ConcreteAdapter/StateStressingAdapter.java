/**
 * 
 */
package fr.openium.ConcreteAdapter;

import java.io.File;
import java.util.HashSet;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;

import fr.openium.AbstractGenerator.AbstractJuniGenerator;

/**
 * @author Stassia
 * 
 */
public class StateStressingAdapter extends AbstractJuniGenerator {
	private static final String TAG = StateStressingAdapter.class.getName();

	// private final State mState;
	// private final ScenarioData mPath;
	// private final HashSet<String> mTestData;

	/**
	 * 
	 */
	public StateStressingAdapter(State state_Stress, ScenarioData path_toReach_the_state) {
		// mState = state_Stress;
		// mPath = path_toReach_the_state;
		// mTestData = testData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.openium.AbstractGenerator.AbstractJuniGenerator#generate(java.lang
	 * .String, java.lang.String, java.lang.String, java.io.File)
	 */
	@Override
	public HashSet<String> generate(String packagename, String test_name, String ActivityName, File out) {
		// generate source code for stressTesting a state:
		// goto the path.
		// the retrieve list of event and test data
		// generate event to perfom into a state
		// for each event: performEvent,
		// goBackTostate(mSate) tel que goback==retour simple, si etat actuel
		// #mState donc goto the path.

		JCodeModel cm = new JCodeModel();
		JDefinedClass dclass;
		addImportedClass(cm);
		try {
			dclass = cm._class(getTestClassName(packagename, test_name));
			JClass SgdTestCase = null;

			SgdTestCase = cm.ref(kit.Stress.StressRunner.class);

			JClass mextends = cm.ref(SgdTestCase.fullName());
			dclass._extends(mextends);
			/**
			 * the target package ID
			 */
			JVar TARGET_PACKAGE_ID = dclass.field(JMod.PRIVATE + JMod.STATIC + JMod.FINAL, String,
					"TARGET_PACKAGE_ID");
			TARGET_PACKAGE_ID.init(JExpr.lit(packagename));
			/**
			 * the LauncherActivity
			 */
			JVar LAUNCHER_ACTIVITY_FULL_CLASSNAME = dclass.field(JMod.PRIVATE + JMod.STATIC + JMod.FINAL,
					String, "LAUNCHER_ACTIVITY_FULL_CLASSNAME");
			LAUNCHER_ACTIVITY_FULL_CLASSNAME.init(JExpr.lit(ActivityName));
			dclass.direct(java.lang.String.format(TEMPLATE, test_name));
			for (JClass classToinsert : classToImport) {
				JVar newClass = dclass.field(JMod.PRIVATE + JMod.STATIC, classToinsert, " var_"
						+ classToImport.indexOf(classToinsert));

			}

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
			cm.build(out);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
}
