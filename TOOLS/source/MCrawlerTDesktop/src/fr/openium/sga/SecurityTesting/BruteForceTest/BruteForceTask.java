package fr.openium.sga.SecurityTesting.BruteForceTest;

import java.io.File;
import java.util.ArrayList;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTestTask;

public class BruteForceTask extends AbstractTestTask{
	/**
	 * dico File
	 */
	//private File mDico;

	public BruteForceTask(State target, ScenarioData path,
			ArrayList<String> testdata) {
		
		super(target, path, testdata);
	}
	
	
	public BruteForceTask(State target, ScenarioData path,
			File dico) {
		
		super(target, path, null);
		//mDico=dico;
	}
	
	
	

}
