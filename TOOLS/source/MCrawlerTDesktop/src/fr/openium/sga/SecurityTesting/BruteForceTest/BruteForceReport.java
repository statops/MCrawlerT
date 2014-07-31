package fr.openium.sga.SecurityTesting.BruteForceTest;

import java.util.ArrayList;


import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Utils.SgUtils;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager.SPReport;

public class BruteForceReport extends SPReport {
	/**
	 * list of dest state after each injection
	 */
	private ScenarioData mDestState;
	private final ArrayList<String> mTestExcecutionTime;
	//private final ArrayList<String> mBruteForceEvents;

	public BruteForceReport(AbstractTaskManager abstractTaskManager,
			State state, ScenarioData path, String verdict, String message,
			ScenarioData destStates, ArrayList<String> bruteevent,
			ArrayList<String> excecutionTimes) {
		abstractTaskManager.super(state, path, verdict, message);
		setDestState(destStates);
		//mBruteForceEvents = bruteevent;
		mTestExcecutionTime = excecutionTimes;

	}

	public ScenarioData getDestState() {
		return mDestState;
	}

	public void setDestState(ScenarioData mDestState) {
		this.mDestState = mDestState;
	}

	/**
	 * verdict is deduced from timeExcecution
	 */

	@Override
	public String getVerdict() {
		// TODO Auto-generated method stub
		/**
		 * detect anomalie accordind to time
		 */
		/**
		 * calcule de l'entropie + approximation
		 */

		/**
		 * detect on dest state change if all dest state are equals a part of
		 * targetstate so its vulnerable beacuse. Analyse text value.
		 */
		State dialogue_View_State = null;
		for (State dest : mDestState.getStates()) {
			if (dest.getId().equalsIgnoreCase(targetState.getId())) {
				continue;
			}
			if (dialogue_View_State == null) {
				dialogue_View_State = dest;
				continue;
			}

			if (!SgUtils.isEqualState(dest, dialogue_View_State)
					&& !dest.getName().equalsIgnoreCase(
							dialogue_View_State.getName())) {
				/**
				 * may be due to activityname or widget property // accèes a une
				 * nouvelle vue
				 */
				return "" + true;
			}

			if (!SgUtils.isStrictEqualState(dest, dialogue_View_State)) {
				/**
				 * comportement normale
				 */
				/**
				 * comparer le contenu des textes (strict mode)
				 */

				/**
				 * si Activité différente authentification reussie
				 */
				// if
				// (dest.getName().equalsIgnoreCase(dialogue_View_State.getName())){
				// return "" + false;
				// }
				// else {
				// return "" + true;
				// }
				return "" + false;
			}

		}
		return "" + true;
	}

	@Override
	public String toString() {
		return super.toString() + " test Time Log" + mTestExcecutionTime
				+ "\n \n \n";
	}
}
