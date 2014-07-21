package fr.openium.sga.bissimulation;

/**
 * graph transitions : Tα [ p] = {q} means an α- transition from state p to
 * state q. Tα−1[q] = p means an inverse α-transition
 * 
 * @author STASSIA
 * 
 */
public class GraphTranstion {

	private final GraphState mSource;
	private final GraphState mDest;
	private final GraphAction mAct;

	public GraphTranstion(GraphState src, GraphState dest, GraphAction act) {
		mSource = src;
		mDest = dest;
		mAct = act;
	}

	public GraphState getSource() {
		return mSource;
	}

	public GraphState getDest() {
		return mDest;
	}

	public GraphAction getAction() {
		return mAct;
	}

}
