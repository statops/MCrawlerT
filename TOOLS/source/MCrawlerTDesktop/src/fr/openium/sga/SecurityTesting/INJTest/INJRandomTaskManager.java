/**
 * 
 */
package fr.openium.sga.SecurityTesting.INJTest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Utils.SgUtils;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTestTask;
import fr.openium.sga.emmatest.SgdEnvironnement;

/**
 * @author Stassia
 * 
 */
public class INJRandomTaskManager extends INJRequestTaskManager {
	public static final int _STRATEGY_ID = 112;

	/**
	 * @param model
	 * @param testDataFile
	 * @param env
	 */
	public INJRandomTaskManager(ScenarioData model, File testDataFile,
			SgdEnvironnement env) {
		super(model, testDataFile, env);
	}

	@SuppressWarnings("unused")
	private static final String TAG = INJRandomTaskManager.class.getName();

	/**
	 * override Task Generation add transition
	 */
	@Override
	public HashSet<AbstractTestTask> getGenerateTaskLists() {
		/**
		 * cas initial
		 */
		ScenarioData init_path = getInitTask();
		State initState = init_path.getInitialState();
		if (initState != null && SgUtils.hasEditText(initState)) {
			tasks.add(new INJTask(init_path.getInitialState(), init_path,
					mTestDataFile));
		} else if (initState == null)
			return null;
		/**
		 * other cases
		 */
		for (State targetState : mTree.getStates()) {

			if (targetState.isInit()) {
				continue;
			}

			if (!targetState.isDest()) {
				continue;
			}

			if (targetState.isFinal()) {
				continue;
			}

			/**
			 * d�j� ajout� dans tasks
			 */

			if (isInTasks(targetState)) {
				continue;
			}

			if (hasNoEditText(targetState)) {
				continue;
			}
			try {
				ScenarioData path = SgUtils.getConcretePath(targetState, mTree);

				/**
				 * creer tous les chemins apres targetState
				 */
				ArrayList<Transition> outputTransition = SgUtils
						.get_out_transitions(targetState, mTree);
				for (Transition tr : outputTransition) {
					tasks.add(new INJTask(targetState, path, mTestDataFile, tr));
					System.out.println("add task : " + targetState.toString());
					System.out.println("path task : " + path.toString());
				}

			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				continue;
			}
		}
		return tasks;
	}

}
