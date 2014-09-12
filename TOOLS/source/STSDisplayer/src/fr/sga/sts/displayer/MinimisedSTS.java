package fr.sga.sts.displayer;


import kit.Scenario.State;
import kit.Scenario.Transition;
import fr.openium.sga.bissimulation.SgaGraph;

public class MinimisedSTS extends AbstractSTSGraphDisplayer{
	public MinimisedSTS(String out ) {
		super(out);
		results =computeTreeTransformation();
	}

	@Override
	public SgaGraph[] computeTreeTransformation() {
		
		
		for (State st : treeModel.getStates()) {
			System.out.println(st.toString());
		}
		for (Transition st : treeModel.getTransitions()) {
			System.out.println(st.toString());
		}

		Refinement displayer = new Refinement(treeModel);
		SgaGraph[] graph = null;
		try {
			graph = displayer.computeBissModel();
		} catch (Exception e) {
			return null;
		}
		//graph[0].forcedisplay();
		//graph[1].forcedisplay();

		System.out.println("init_state :" + graph[0].getVertices().size());
		System.out.println("biss_state :" + graph[1].getVertices().size());
		return graph;
	}


}
