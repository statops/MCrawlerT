package fr.openium.sga.bissimulation;

import grph.Grph;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

public class SgaGraph extends Grph {

	private HashMap<Integer, GraphTranstion> mEdgeMap = new HashMap<Integer, GraphTranstion>();
	private HashMap<Integer, GraphState> mStateMap = new HashMap<Integer, GraphState>();
	private HashMap<Integer, GraphAction> mActionsMap = new HashMap<Integer, GraphAction>();
	private HashSet<String> mActions = new HashSet<String>();
	private int vertexId = 0;
	/**
	 */
	private static final long serialVersionUID = 8771052346630128126L;

	public int addEdge(GraphTranstion transition) {
		int v1 = getVertex(transition.getSource());
		getVertexLabelProperty().setValue(v1, transition.getSource().getName());
		int v2 = getVertex(transition.getDest());
		getVertexLabelProperty().setValue(v2, transition.getDest().getName());
		int edge = addSimpleEdge(v1, v2, true);
		mEdgeMap.put(edge, transition);
		return edge;
	}

	public void saveGraph(File output) {

	}
	

	public void forcedisplay() {
		super.display();
	};
	
	@Override
	public void display() {
		//super.display();
	};

	public void createGraph(HashSet<GraphTranstion> trs) {
		for (GraphTranstion tr : trs) {
			GraphAction action = tr.getAction();
			int e = addEdge(tr);
			this.getEdgeLabelProperty().setValue(e, action.toString());
			mActions.add(action.getValue());
			action.setEdgeId(e);
			mActionsMap.put(e, tr.getAction());
		}
	}

	/**
	 * generate a vertex for a State object - check is state is already
	 * registred in mState map - otherwise generate state
	 * 
	 * @param s
	 *            : the state
	 * @return
	 */
	private int getVertex(GraphState s) {
		if (mStateMap.containsValue(s)) {
			return getStateId(s);
		}
		vertexId++;
		mStateMap.put(vertexId, s);
		return vertexId;
	}

	private int getStateId(GraphState s) {
		for (int i : mStateMap.keySet()) {
			if (mStateMap.get(i).equals(s)) {
				return i;
			}
		}
		return vertexId++;

	}

	/**
	 * 
	 * @return the map edge number et edge
	 */
	public HashMap<Integer, GraphTranstion> getEdgeMap() {
		return mEdgeMap;
	}

	public HashSet<String> getActions() {
		return mActions;

	}

	public int getVertexFromLabel(String label) {
		for (int key : mStateMap.keySet()) {
			if (mStateMap.get(key).equals(new GraphState(label))) {
				return key;
			}
		}
		throw new NullPointerException("No vertice id maps to :" + label);
	}

	/**
	 * @param i
	 * @return
	 */
	public GraphState getState(int i) {
		return mStateMap.get(i);
	}
}
