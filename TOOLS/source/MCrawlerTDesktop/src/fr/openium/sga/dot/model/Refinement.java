package fr.openium.sga.dot.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import kit.Scenario.Action;
import kit.Scenario.Author;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Scenario.Transition;
import fr.openium.sga.bissimulation.Bissimulation;
import fr.openium.sga.bissimulation.GraphAction;
import fr.openium.sga.bissimulation.GraphState;
import fr.openium.sga.bissimulation.GraphTranstion;
import fr.openium.sga.bissimulation.SgaGraph;
import fr.openium.sga.result.CrawlResult;

public class Refinement {

	private SgaGraph[] mGraphs = new SgaGraph[2];
	private ScenarioData mScenarioData = new ScenarioData();
	private boolean mode = true;

	public Refinement(ScenarioData... scenarioDatas) {
		combineScenario(scenarioDatas);

	}

	private void combineScenario(ScenarioData[] scenarioDatas) {
		for (ScenarioData scen : scenarioDatas) {
			add(scen);
		}
	}

	private void add(ScenarioData scen) {
		if (scen == null) {
			return;
		}
		/**
		 * addAll(scen.getStates());
		 * 
		 * addAll(scen.getTransitions());
		 */
		try {
			mScenarioData.add_(scen, mode);
		} catch (CloneNotSupportedException e) {

			e.printStackTrace();
		}
		// addAll(scen.getTrees());
		addAllActions(scen.getActions());
		addAuthors(scen.getAuthors());

	}

	private void addAuthors(ArrayList<Author> authors) {
		mScenarioData.getAuthors().addAll(authors);

	}

	private void addAllActions(HashSet<Action> actions) {
		mScenarioData.getActions().addAll(actions);

	}

	

	public Refinement(File... scenarioDataFiles) {
		/**
		 * parser
		 */
		Set<ScenarioData> scenarii = new HashSet<ScenarioData>();
		for (File scen_in_xml : scenarioDataFiles) {
			ScenarioData value = ScenarioParser.parse(scen_in_xml);
			scenarii.add(value);
		}

		combineScenario(scenarii);
	}

	public Refinement(CrawlResult mResult) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param pathList
	 */
	public Refinement(ArrayList<ScenarioData> pathList) {
		HashSet<ScenarioData> scenarii = new HashSet<ScenarioData>(pathList);
		combineScenario(scenarii);
	}

	private void combineScenario(Set<ScenarioData> scenarii) {
		ScenarioData[] array_of_scen = new ScenarioData[scenarii.size()];
		Iterator<ScenarioData> iterator_scenario = scenarii.iterator();
		for (int i = 0; i < scenarii.size(); i++) {
			array_of_scen[i] = iterator_scenario.next();
		}
		combineScenario(array_of_scen);

	}

	/**
	 * 
	 * @return array of graphs containing the original graph and the
	 *         bissimilated graph
	 */
	public SgaGraph[] computeBissModel() {
		mScenarioData.clean_();

		/**
		 * appliquer la bissimilation
		 */
		// Construire uSgUtils.javane graphe avec les etats, les actions et les
		// transitions
		// // en faisant abstraction d'edit text
		// texte.
		compute();
		return mGraphs;
	}

	/**
	 * 
	 */
	private void compute() {
		SgaGraph scenarioGraph = generate_graphe(mScenarioData);
		mGraphs[0] = scenarioGraph;
		System.out.println("graphe initiale:  " + mGraphs[0].toDot());
		Bissimulation refinementAlgo = new Bissimulation(scenarioGraph);
		mGraphs[1] = refinementAlgo.compute();
		System.out.println("graphe finale:  " + mGraphs[1].toDot());

	}

	public SgaGraph[] computeExtrapolatedModel() {
		mScenarioData.clean_();
		for (State st : mScenarioData.getStates()) {
			st.setEndState("" + false);
		}
		compute();
		return mGraphs;
	}

	/**
	 * Graph to be bissimiled
	 * @param scen
	 * @return
	 */
	public SgaGraph generate_graphe(ScenarioData scen) {
		SgaGraph scenarioGraph = new SgaGraph();
		HashSet<GraphTranstion> graph_transition = new HashSet<GraphTranstion>();
		// SgUtils.solve(mScenarioData);
		/**
		 * recuperer l'ensemble des etats L dans
		 */
		// Collections.sort(mScenarioData.getTransitions());

		for (Transition scenario_tr : mScenarioData.getTransitions()) {
			String alpha = get_actionEvent(scenario_tr);
			GraphState state_source = new GraphState(getSourceState(scenario_tr));
			GraphState state_destination = new GraphState(getDestState(scenario_tr));
			//String constraint = getConstraints(scenario_tr.getAction());
			GraphAction action = new GraphAction(alpha);
			if (alpha != null) {
				graph_transition.add(new GraphTranstion(state_source, state_destination, action));
			}
		}
		scenarioGraph.createGraph(graph_transition);
		return scenarioGraph;
	}

	/**
	 * trace graph
	 * @param scen
	 * @return
	 */
	public SgaGraph generate_trace(ScenarioData scen) {
		SgaGraph scenarioGraph = new SgaGraph();
		HashSet<GraphTranstion> graph_transition = new HashSet<GraphTranstion>();
		// SgUtils.solve(mScenarioData);
		/**
		 * recuperer l'ensemble des etats L dans
		 */
		// Collections.sort(mScenarioData.getTransitions());

		for (Transition scenario_tr : mScenarioData.getTransitions()) {
			String alpha = get_actionEvent(scenario_tr);
			GraphState state_source = new GraphState(getTraceSourceState(scenario_tr));
			GraphState state_destination = new GraphState(getTraceDestState(scenario_tr));
		//	String constraint = getConstraints(scenario_tr.getAction());
			GraphAction action = new GraphAction(alpha);
			if (alpha != null) {
				graph_transition.add(new GraphTranstion(state_source, state_destination, action));
			}
		}
		scenarioGraph.createGraph(graph_transition);
		return scenarioGraph;
	}

	
	private String getTraceDestState(Transition scenario_tr) {
		// TODO Auto-generated method stub
		return null;
	}

	private String getTraceSourceState(Transition scenario_tr) {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unused")
	private String getConstraints(ArrayList<Action> action) {
		String constraint = "";
		for (Action alpha : action) {
			// System.out.println("action :" + alpha.getName());
			constraint = constraint + " " + alpha.getWidget().geValueToString();

		}
		return constraint;

	}

	private String getSourceState(Transition scenario_tr) {
		return getState(scenario_tr, true, false);
	}

	private String getDestState(Transition scenario_tr) {
		return getState(scenario_tr, false, true);
	}

	private String getState(Transition scenario_tr, boolean isSource, boolean isDest) {
		for (State st : mScenarioData.getStates()) {
			if (st.getId() == null) {
				continue;
			}
			if (st.getId().equalsIgnoreCase(scenario_tr.getId())
					&& st.getType().equalsIgnoreCase(Scenario.SOURCE) && isSource) {
				return st.getGraphStateId();
			}
			if (st.getId().equalsIgnoreCase(scenario_tr.getId())
					&& st.getType().equalsIgnoreCase(Scenario.DEST) && isDest) {
				return st.getGraphStateId();
			}

		}
		return scenario_tr.getSource().getGraphStateId();
	}

	public ScenarioData getScenarioData() {
		return mScenarioData;
	}

	private String get_actionEvent(Transition scenario_tr) {
		/**
		 * Au lieu d'utiliser l'action ==> utiliser action -activit�_dest
		 */
		for (Action alpha : scenario_tr.getAction()) {

			if (!alpha.getName().contains(Scenario.EDITTEXT)
					&& !alpha.getName().equalsIgnoreCase("")) //!alpha.getName().contains("TextView") && 

			{
				System.out.println("action-dest :" + alpha.getName() + "-"
						+ scenario_tr.getDest().getShortName());
				//alpha.getName() + "-" +
				return (alpha.getWidget().getType()+" :"+alpha.getWidget().getName());//+ "-"+ scenario_tr.getSource().getShortName()); // +
																							// "-"
																							// +
																							// scenario_tr.getDest().getShortName());
			}

		}
		return null;
	}

	public SgaGraph generate_graphe() {

		return generate_graphe(mScenarioData);
	}

	public boolean isMode() {
		return mode;
	}

	public void setMode(boolean mode) {
		this.mode = mode;
	}

	/**
	 * @return
	 */
	public SgaGraph[] computeTraceModel() {
		mScenarioData.clean_();
		SgaGraph scenarioGraph = generate_trace_graphe(mScenarioData);
		mGraphs[0] = scenarioGraph;
		System.out.println("graphe initiale:  " + mGraphs[0].toDot());
		return mGraphs;
	}

	/**
	 * @param mScenarioData2
	 * @return
	 */
	private SgaGraph generate_trace_graphe(ScenarioData mScenarioData2) {
		SgaGraph scenarioGraph = new SgaGraph();
		HashSet<GraphTranstion> graph_transition = new HashSet<GraphTranstion>();

		for (Transition scenario_tr : mScenarioData.getTransitions()) {
			String alpha = get_actionEvent(scenario_tr);
			GraphState state_source = new GraphState(trace_getSourceState(scenario_tr));
			GraphState state_destination = new GraphState(trace_getDestState(scenario_tr));
			GraphAction action = new GraphAction(alpha);
			if (alpha != null) {
				graph_transition.add(new GraphTranstion(state_source, state_destination, action));
			}
		}
		scenarioGraph.createGraph(graph_transition);
		return scenarioGraph;
	}

	/**
	 * @param scenario_tr
	 * @return
	 */
	private String trace_getDestState(Transition scenario_tr) {
		for (State st : mScenarioData.getStates()) {
			if (st.getId() == null) {
				continue;
			}
			if (st.getId().equalsIgnoreCase(scenario_tr.getId())
					&& st.getType().equalsIgnoreCase(Scenario.SOURCE)) {
				return st.getTraceGraphStateId();
			}

		}
		return scenario_tr.getSource().getTraceGraphStateId();
	}

	/**
	 * @param scenario_tr
	 * @return
	 */
	private String trace_getSourceState(Transition scenario_tr) {
		for (State st : mScenarioData.getStates()) {
			if (st.getId() == null) {
				continue;
			}

			if (st.getId().equalsIgnoreCase(scenario_tr.getId())
					&& st.getType().equalsIgnoreCase(Scenario.DEST)) {
				return st.getTraceGraphStateId();
			}

		}
		return scenario_tr.getSource().getTraceGraphStateId();
	}
}
