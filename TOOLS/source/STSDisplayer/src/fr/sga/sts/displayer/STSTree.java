package fr.sga.sts.displayer;

import java.io.File;

import org.python.antlr.PythonParser.return_stmt_return;

import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Scenario.Transition;
import fr.openium.sga.bissimulation.SgaGraph;

public class STSTree extends AbstractSTSGraphDisplayer {
	private static final String TAG = null;

	public STSTree(String outXMl) {
		super(outXMl);
	}

	public SgaGraph[] computeTreeTransformation() {

		System.out.println("displayTrace");

		for (State st : treeModel.getStates()) {
			System.out.println(st.toString());
		}
		for (Transition st : treeModel.getTransitions()) {
			System.out.println(st.toString());
		}
		Refinement displayer = new Refinement(treeModel);
		SgaGraph[] graph = null;
		try {
			graph = displayer.computeTraceModel();
		} catch (Exception e) {
			return graph;
		}
		System.out.println("displayTrace");
		System.out.println("number of states " + graph[0].getVertices().size());
		
		return graph;
	}
}
