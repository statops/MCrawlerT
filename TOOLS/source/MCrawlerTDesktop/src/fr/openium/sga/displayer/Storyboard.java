package fr.openium.sga.displayer;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;

import org.graphstream.graph.Graph;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Utils.TransitionIdComparator;

public class Storyboard extends Observable implements Observer {

	private GraphGenerator graph;

	/**
	 * 
	 * @param tree
	 * @param appName
	 * @param robotiumScreenShot
	 * @param story_board_type
	 *            : 1 = tree 2 =simple
	 * @throws Exception
	 */
	public Storyboard(ScenarioData tree, String appName,
			File robotiumScreenShot, int story_board_type,int pad, int x, int y) throws Exception {
		CssGenerator css = new CssGenerator(appName, tree,
				robotiumScreenShot.getAbsolutePath(), story_board_type,pad,x,y);
		graph = new GraphGenerator(appName, css);
		ArrayList<Transition> Edges = tree.getTransitions();
		Collections.sort(Edges, new TransitionIdComparator());
		for (Transition path : Edges) {
			update(null, path);
		}

		for (int i=Edges.size();i>0;i--) {
			graph.createGraph(Edges.get(i-1));
		}

	}

	@Override
	public void update(Observable source, Object info) {
		try {
			if (info instanceof Transition) {
				addTransition((Transition) info);
			} else if (info instanceof State) {
				addState((State) info, false);
			}

		} catch (Exception e) {

			inform(e);
		}
	}

	private void addState(State e, boolean finalState) throws Exception {
		graph.addState(e, finalState);
	}

	private void addTransition(Transition ch) throws Exception {
		graph.addTransition(ch);
	}

	private void inform(Object info) {
		if (info instanceof Exception) {
			((Exception) info).printStackTrace();
		}
		System.err.println(info.toString());
		setChanged();
		notifyObservers(info);
	}

	public Component getStoryBoard() {
		return graph.getStoryboard();
	}

	public void _exportGraph(String path) {
		graph._exportStoryboard(path);
	}

	public Graph getGraph() {
		return graph.getGraph();
	}

}
