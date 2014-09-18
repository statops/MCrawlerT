package fr.openium.sga.displayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import java.util.StringTokenizer;

import kit.Scenario.Scenario;
import kit.Scenario.State;
import kit.Scenario.Transition;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.implementations.AdjacencyListGraph;
import org.graphstream.ui.swingViewer.View;
import org.graphstream.ui.swingViewer.Viewer;

import fr.openium.sga.ConfigApp;

public class GraphGenerator {
	public final static int TREE_TYPE = 1;
	public final static int SIMPLE_TYPE = 2;

	private Graph graph;

	private CssGenerator cssStyle;

	private String appName;

	private PositionManager mPositionManager = new PositionManager();

	private List<String> ids = new ArrayList<String>();

	public GraphGenerator(String appName) throws Exception {
		this(appName, new CssGenerator(appName));
	}

	public GraphGenerator(String appName, CssGenerator cssStyle)
			throws NullPointerException, IOException {
		setGraph(appName, cssStyle);
	}

	private void setAppName(String appName) throws NullPointerException {
		if (appName == null)
			throw new NullPointerException(
					"Le nom de l'application doit �tre renseigner.");

		this.appName = appName;
	}

	private void setCssFile(CssGenerator cssFile) throws NullPointerException {
		if (cssFile == null)
			throw new NullPointerException(
					"Le g�n�rateur de fichier css doit �tre renseigner.");
		this.cssStyle = cssFile;
	}

	private void setGraph(String appName, CssGenerator cssStyle)
			throws NullPointerException, IOException {
		setAppName(appName);
		setCssFile(cssStyle);
		graph = new AdjacencyListGraph(appName);
		graph.addAttribute("ui.stylesheet",
				StyleConverter.convert(new File(cssStyle.getStylesheet())));
		graph.addAttribute("ui.quality");
		// graph.addAttribute("ui.antialias");

	}

	public View getStoryboard() {	
		/**
		 * centrer les images etage par étage
		 */

		Iterator<Edge> edges = graph.getEdgeIterator();
		if (edges.hasNext()) {

			do {
				Edge currentedge = edges.next();
				Object[] xy = currentedge.getSourceNode().getAttribute("xyz");
				if (xy == null) {
					xy = currentedge.getSourceNode().getAttribute("xy");
				}
				System.out.println("edge: " + currentedge.getId());
				System.out.println("source: "
						+ currentedge.getSourceNode().getId());

				System.out.println("xy: " + xy[0].toString() + "  "
						+ xy[1].toString());
				System.out.println("dest: "
						+ currentedge.getTargetNode().getId());
				xy = currentedge.getTargetNode().getAttribute("xyz");
				if (xy == null) {
					xy = currentedge.getTargetNode().getAttribute("xy");
				}
				System.out.println("xy: " + xy[0].toString() + "  "
						+ xy[1].toString());
			} while (edges.hasNext());
			// add screenshot attribute

		}
		switch (cssStyle.getStoryBoardType()) {
		case TREE_TYPE:
			graph.addAttribute("ui", cssStyle.getRobotium() + File.separator
					+ "tree.png");
			break;
		case SIMPLE_TYPE:
			graph.addAttribute("ui", cssStyle.getRobotium() + File.separator
					+ "storyboard.png");
			break;
		default:
			throw new IllegalStateException("No type of storyboard defined");
		}

		Viewer viewer = new Viewer(graph,
				Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		View view = viewer.addDefaultView(false);
		view.setName(appName);
		return view;
	}

	public void addState(State state, boolean finalState) throws IOException,
			NullPointerException, Exception {
		state = state.clone();
		finalState = false;
		if (state == null)
			throw new NullPointerException("Il faut renseigner l'�l�ment");
		String stateId;
		if (state.isDest()) {
			stateId = state.getId();
		} else {
			stateId = getSource(state.getId());
			state.setId(stateId);
		}

		if (ConfigApp.DEBUG) {
			System.out.println("Current state inserted: ");
			System.out.println("- id : " + stateId);
			System.out.println("type : " + state.getType());
		}

		if (!finalState) {
			if (graph.getNode(stateId) != null)
				return;
			cssStyle.addState(state, finalState);
			graph.addNode(stateId);
			graph.getNode(stateId).addAttribute("ui.class", "id" + stateId);
		} else {
			if (graph.getNode(stateId + "end") != null)
				return;
			cssStyle.addState(state, finalState);
			graph.addNode(stateId + "end");
			graph.getNode(stateId + "end").addAttribute("ui.class",
					"id" + stateId + "end");
		}
		graph.addAttribute("ui.stylesheet",
				StyleConverter.convert(new File(cssStyle.getStylesheet())));

		// graph.addAttribute("x", x);
		// graph.addAttribute("y", y);

		// x++;
	}

	public void addTransition(Transition path) throws NullPointerException,
			Exception {
		switch (cssStyle.getStoryBoardType()) {
		case TREE_TYPE:
			setTreeGraph(path);
			break;
		case SIMPLE_TYPE:
			setNormalGraph(path);
			break;
		default:
			throw new IllegalStateException("No type of storyboard defined");

		}

	}

	private HashMap<Integer, ArrayList<Object>> sortedState = new HashMap<Integer, ArrayList<Object>>();
	HashSet<String> handleIds = new HashSet<String>();

	// private Float unit = (float) 15;

	private void setNormalGraph(Transition path)
			throws CloneNotSupportedException, IOException {
		/**
		 * Utiliser un seul id des états, rechercher l'etat equivalent
		 */

		System.out.println("Before All path: "
				+ path.getSource().clone().getId() + "   " + path.getId()
				+ "  " + path.getDest().clone().getId());
		State source = path.getSource().clone();
		State dest = path.getDest().clone();

		State graphSource = getUniqueEquivalentState(source);
		State graphDest = getUniqueEquivalentState(dest);
		/**
		 * voir si une transition existe déja
		 */

		if (transitionExist(graphSource.getId(), graphDest.getId())) {
			return;
		}
		updateSortedStateList(graphSource, graphDest);

	}

	public void createGraph(Transition path) throws NullPointerException,
			Exception {

		switch (cssStyle.getStoryBoardType()) {
		case TREE_TYPE:
			createTreeGraph(path);
			break;
		case SIMPLE_TYPE:
			createSimpleGraph(path);
			break;
		default:
			throw new IllegalStateException("No type of storyboard defined");

		}

	}

	private void createSimpleGraph(Transition path)
			throws CloneNotSupportedException, IOException {
		State source = path.getSource().clone();
		State dest = path.getDest().clone();

		State graphSource1 = getUniqueEquivalentState(source);
		State graphDest1 = getUniqueEquivalentState(dest);
		/**
		 * voir si une transition existe déja
		 */

		if (transitionExist(graphSource1.getId(), graphDest1.getId())) {
			return;
		}

		String action1 = getAction(path);

		System.out.println("add Source: ");
		addGraphState(graphSource1);

		System.out.println("add Dest: ");
		addGraphState(graphDest1);

		graph.addEdge(String.valueOf(path.getId()), graphSource1.getId(),
				graphDest1.getId(), true);
		graph.getEdge(String.valueOf(path.getId())).setAttribute("ui.label",
				action1);

		/**
		 * positioner les noeuds
		 */

		adjustNode(graphSource1, graphDest1);

	}

	private void updateSortedStateList(State graphSource, State graphDest) {
		// int i=getLevel(graphSource);
		updateSortedStateList(graphSource);

		updateSortedStateList(graphDest);

	}

	private void updateSortedStateList(State graphSource) {
		/**
		 * si existe dans un des elements
		 */
		try {
			if (handleIds.contains(graphSource.getId())) {
				return;
			}
			sortedState.get(getLevel(graphSource)).add(graphSource);

		} catch (NullPointerException empty) {
			ArrayList<Object> state = new ArrayList<Object>();
			state.add(graphSource);
			sortedState.put(getLevel(graphSource), state);
		} finally {
			handleIds.add(graphSource.getId());
		}

	}

	private void updateSortedStateList(String id) {
		/**
		 * si existe dans un des elements
		 */
		try {
			if (handleIds.contains(id)) {
				return;
			}
			sortedState.get(getLevel(id)).add(id);

		} catch (NullPointerException empty) {
			ArrayList<Object> state = new ArrayList<Object>();
			state.add(id);
			sortedState.put((Integer) getLevel(id), state);
		} finally {
			handleIds.add(id);
		}

	}

	private int getLevel(String nodeId) {
		return (new StringTokenizer(nodeId, Scenario.IDSEPARATOR))
				.countTokens();
	}

	private int getLevel(State graphSource) {

		return (new StringTokenizer(graphSource.getId(), Scenario.IDSEPARATOR))
				.countTokens();
	}

	private void adjustNode(State graphSource, State graphDest) {
		getAPosition(graphSource.getId());
		getAPosition(graphDest.getId());

	}

	private void getAPosition(String nodeId) {

		if (graph.getNode(nodeId).getAttribute("xyz") == null) {
			int xSource = (new StringTokenizer(nodeId, Scenario.IDSEPARATOR))
					.countTokens();
			// unit=(float) (10/getNumberOfNode(xSource));
			/*
			 * Float x_ = (mPositionManager.getPosition(xSource)*unit) - ((10) /
			 * 2);
			 */
			Float x_ = (mPositionManager.getPosition(xSource))
					- (getNumberOfNode(xSource) / 2);
			// mPositionManager.getPosition(xSource)-((xSource));
			Float y_ = (-3) * (Float.parseFloat("" + (xSource)));
			graph.getNode(nodeId).setAttribute("xyz", x_, y_, 0);

			Object[] xy = graph.getNode(nodeId).getAttribute("xyz");
			System.out.println("New position:  ");
			System.out.println("xy: " + xy[0].toString() + "  "
					+ xy[1].toString());
		} else {
			Object[] xy = graph.getNode(nodeId).getAttribute("xyz");
			System.out.println("Already affected node:  ");
			System.out.println("xy: " + xy[0].toString() + "  "
					+ xy[1].toString());
		}

	}

	private int getNumberOfNode(int level) {

		// return cssStyle.getNumberOfNode(level);
		return sortedState.get(level).size();

	}

	private boolean transitionExist(String source, String dest) {
		Iterator<Edge> edges = graph.getEdgeIterator();
		if (!edges.hasNext()) {
			return false;
		}
		do {
			Edge curentEdge = edges.next();
			String sourId = graph.getNode(curentEdge.getSourceNode().getId())
					.getId();
			String des = graph.getNode(curentEdge.getTargetNode().getId())
					.getId();
			if (source.equalsIgnoreCase(sourId) && (des.equalsIgnoreCase(dest))) {
				return true;
			}

		} while (edges.hasNext());
		return false;
	}

	private void addGraphState(State graphSource) throws IOException,
			CloneNotSupportedException {
		if (graphSource == null)
			throw new NullPointerException("Il faut renseigner l'�l�ment");

		graphSource = graphSource.clone();
		if (ids.contains(graphSource.getId())) {
			return;
		}
		ids.add(graphSource.getId());

		if (graph.getNode(graphSource.getId()) != null)
			return;

		cssStyle.addUniqueState(graphSource, false);
		graph.addNode(graphSource.getId());
		graph.getNode(graphSource.getId()).addAttribute("ui.class",
				"id" + graphSource.getId());

		graph.addAttribute("ui.stylesheet",
				StyleConverter.convert(new File(cssStyle.getStylesheet())));

	}

	private State getUniqueEquivalentState(State source) {
		return cssStyle.getEquvalentState(source);
	}

	private void setTreeGraph(Transition path) throws NullPointerException,
			IOException, Exception {
		State source = path.getSource().clone();
		State dest = path.getDest().clone();
		updateSortedStateList(getSource(source.getId()));
		updateSortedStateList(dest.getId());

	}

	public void createTreeGraph(Transition path) throws NullPointerException,
			IOException, Exception {
		if (graph.getEdge(path.getId()) != null) {
			return;
		}
		System.out.println("Before All path: "
				+ path.getSource().clone().getId() + "   " + path.getId()
				+ "  " + path.getDest().clone().getId());

		String sourceId = getSource((path.getSource().clone().getId()));
		String action = getAction(path);
		String destId = path.getDest().clone().getId();

		System.out.println("After All path: " + sourceId + "   " + path.getId()
				+ "  " + destId);
		/**
		 * ajouter la source et destination
		 */
		System.out.println("add Source: ");
		addState(sourceId);
		System.out.println("add Dest: ");
		addState(path.getDest().clone(), path.getDest().isFinal());

		System.out.println("edge id: " + String.valueOf(path.getId()));
		System.out.println("All: " + sourceId + "   " + path.getId() + "  "
				+ path.getDest().getId());

		graph.addEdge(String.valueOf(path.getId()), sourceId, path.getDest()
				.clone().getId(), true);
		graph.getEdge(String.valueOf(path.getId())).setAttribute("ui.label",
				action);
		destId = path.getId();
		// updateSortedStateList(sourceId);
		// updateSortedStateList(destId);
		ajustTreeNode(sourceId, destId);

	}

	private void addState(String sourceId) throws IOException,
			IdAlreadyInUseException {

		if (sourceId == null)
			throw new NullPointerException("Il faut renseigner l'�l�ment");

		String stateId = sourceId;
		if (graph.getNode(sourceId) != null)
			return;
		cssStyle.addState(sourceId);
		graph.addNode(stateId);
		graph.getNode(stateId).addAttribute("ui.class", "id" + stateId);
		graph.addAttribute("ui.stylesheet",
				StyleConverter.convert(new File(cssStyle.getStylesheet())));

	}

	private void ajustTreeNode(String sourceId, String destId) {
		getAPosition(sourceId);
		getAPosition(destId);

	}

	private String getAction(Transition path) {
		/*
		 * String edgeLabe = ""; for (Action action : path.getAction()) {
		 * edgeLabe = edgeLabe + action.getName() + " +";
		 * 
		 * }
		 */

		// return edgeLabe.substring(0, edgeLabe.length() - 1);
		// currentActionOcc++;
		// return currentActionString+currentActionOcc;
		return "";
	}

	HashSet<String> placed_node = new HashSet<String>();

	private String getSource(String id) {
		int index = id.lastIndexOf(Scenario.IDSEPARATOR);
		System.out.println("id : " + id);
		System.out.println("index:" + index);
		if (index == 0 || index < 0) {
			System.out.println("graph id: " + id);
			return id;
		}
		System.out.println("graph id: " + id.substring(0, index));
		return id.substring(0, index);
	}

	public void _exportStoryboard(String path) throws NullPointerException {
		if (path == null)
			throw new NullPointerException(
					"Il faut renseigner le chemin de sortie pour l'export du soryboard.");

		graph.addAttribute("ui.screenshot", path + "screen.png");
	}

	public void saveStoryboardInFile(String path) throws NullPointerException,
			IOException, Exception {
		if (path == null)
			throw new NullPointerException(
					"Il faut renseigner le chemin de sortue pour enregistrer le storyboard dans un fichier.");

		graph.write(path);
	}

	public void addErrorState(String idDestError) throws IOException {
		if (idDestError == null)
			throw new NullPointerException("Il faut renseigner le chemin");
		if (graph.getNode(idDestError) != null)
			return;
		cssStyle.addErrorState(idDestError);
		graph.addNode(idDestError);
		graph.getNode(idDestError).addAttribute("ui.class", "id" + idDestError);

		graph.addAttribute("ui.stylesheet",
				StyleConverter.convert(new File(cssStyle.getStylesheet())));
		// graph.addAttribute("x", x);
		// graph.addAttribute("y", y);

		// x++;
	}

	public Graph getGraph() {
		return graph;
	}

	private final int xSlot = 1;

	public class PositionManager {
		HashMap<Integer, Float> mNodePositions = new HashMap<Integer, Float>();

		public void setCurrentPosition(int level, float affected) {
			float newX = affected + Float.parseFloat("" + xSlot);
			mNodePositions.remove(level);
			mNodePositions.put(level, newX);
		}

		public Float getPosition(Integer level) {
			System.out.println("position for : " + level);
			float pos;
			if (mNodePositions.get(level) == null) {
				pos = 0;
			} else {
				pos = mNodePositions.get(level);

			}
			setCurrentPosition(level, pos);
			System.out.println("pos : " + pos);
			return pos;
		}

	}
}
