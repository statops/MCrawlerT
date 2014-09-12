package fr.sga.sts.displayer;

import fr.openium.sga.bissimulation.SgaGraph;

public class ExtrapolatedTree extends AbstractSTSGraphDisplayer {

	public ExtrapolatedTree(String outXml) {
		super(outXml);
		results = computeTreeTransformation();
	}

	@Override
	public SgaGraph[] computeTreeTransformation() {

		Refinement displayer = new Refinement(treeModel);
		SgaGraph[] graph = null;
		try {
			graph = displayer.computeExtrapolatedModel();
		} catch (Exception e) {
			return null;
		}

		System.out.println("init_state :" + graph[0].getVertices().size());
		System.out.println("extrapolate_state :"
				+ graph[1].getVertices().size());
		return graph;

	}

}
