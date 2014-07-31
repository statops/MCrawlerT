/**
 * 
 */
package fr.openium.sga.SecurityTesting.AbstractTest;

import java.util.ArrayList;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import fr.openium.sga.threadPool.CrawlerTask;

/**
 * @author Stassia
 * 
 */
public abstract class AbstractTestTask extends CrawlerTask {
	protected final ArrayList<String> testData;
	protected final State targetState;

	/**
	 * @param path
	 */
	public AbstractTestTask(State target, ScenarioData path, ArrayList<String> data) {
		// path to reach a state for a initial state
		super(path);
		testData = data;
		targetState = target;

	}

	@Override
	public String toString() {
		return "Location : " + targetState.toString() + "\n" + "Location's Path : "
				+ ((getPath() == null) ? "" : getPath().toString());
	}

	/**
	 * @return
	 */
	public State getTargetState() {
		return targetState;
	}

	public ArrayList<String> getTestData() {
		return testData;
	}

}
