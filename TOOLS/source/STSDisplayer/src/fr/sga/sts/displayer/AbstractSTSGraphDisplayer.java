package fr.sga.sts.displayer;

import java.io.File;

import fr.openium.sga.bissimulation.SgaGraph;

import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;

abstract class AbstractSTSGraphDisplayer {
	
	protected final ScenarioData treeModel;
	protected SgaGraph[] results;
	public AbstractSTSGraphDisplayer(String outXml) {
	
		treeModel=ScenarioParser.parse(new File(outXml));
	}
	
	public AbstractSTSGraphDisplayer(ScenarioData out) {
		
		treeModel=out;
	}
	
	public abstract SgaGraph[] computeTreeTransformation();
	

	public void displayTree() {
		results[0].forcedisplay();
		
	}

	
	public void displayMinTree() {
		results[1].forcedisplay();
		
	}
	
	public void displayAll() {
		displayTree();
		displayMinTree();	
	}
	


}
