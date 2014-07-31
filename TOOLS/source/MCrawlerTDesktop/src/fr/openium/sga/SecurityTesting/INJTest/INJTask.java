/**
 * 
 */
package fr.openium.sga.SecurityTesting.INJTest;

import java.util.ArrayList;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTestTask;

/**
 * @author Stassia
 * 
 */
public class INJTask extends AbstractTestTask {
	private Transition mSuccessor = null;

	/**
	 * @param scen
	 */
	public INJTask(State target, ScenarioData scen, ArrayList<String> data,
			Transition successor) {
		super(target, scen, data);
		setSuccessor(successor);

	}

	/**
	 * @param scen
	 */
	public INJTask(State target, ScenarioData scen, ArrayList<String> data) {
		super(target, scen, data);
		setSuccessor(null);

	}

	/**
	 * get successor as a Scenario Data
	 * 
	 * @return
	 */
	public ScenarioData getSuccessor() {
		if (mSuccessor == null) {
			return null;
		}
		return new ScenarioData(mSuccessor);
	}

	public void setSuccessor(Transition suc) {
		this.mSuccessor = suc;
	}

}
