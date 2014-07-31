/**
 * 
 */
package fr.openium.sga.SecurityTesting.StressTest;

import java.util.ArrayList;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTestTask;

/**
 * @author Stassia
 * 
 */
public class StressTask extends AbstractTestTask {

	/**
	 * @param scen
	 */
	public StressTask(State target, ScenarioData scen, ArrayList<String> data) {
		// path to reach a state for a initial state
		super(target, scen, data);
	}

}
