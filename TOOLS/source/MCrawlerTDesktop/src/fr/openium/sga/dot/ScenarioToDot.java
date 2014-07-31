package fr.openium.sga.dot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import kit.Config.Config;
import kit.Scenario.Action;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;

import org.apache.commons.io.FileUtils;

public class ScenarioToDot {
	private final ScenarioData mScenarioData;
	/**
	 * the directory to store valu
	 */
	private File outFile;
	private final static String TRACE = "trace";
	private static final String OUT_SCEN = "outScen";

	public ScenarioToDot(ScenarioData scenario, File dotDirectory) throws IOException {
		mScenarioData = scenario;
		outFile = dotDirectory;

		if (!outFile.isDirectory()) {
			throw new IOException(outFile.getName() + "must be a directory");
		}
		if (!outFile.exists()) {
			if (!outFile.mkdirs()) {
				throw new IOException(outFile.getName() + "is not created");
			}
		}
	}

	public String start_graph(String name) {
		return "digraph " + name + " {";
	}

	public String end_graph() {
		return "}";
	}

	private StringBuilder graph = new StringBuilder();

	public void addln(StringBuilder target_graph, String line) {
		target_graph.append(line + "\n");
	}

	/**
	 * generate the DotFiles Directly from the Scenario. This will be a huge
	 * graph
	 * 
	 * @return true if file is generated
	 * @throws InterruptedException
	 */
	public boolean generateDirect_Mode_DotFile() throws InterruptedException {
		addln(graph, start_graph(mScenarioData.getInitialStateName()));
		/**
		 * set first state as shape box
		 */
		addTransitionIngraph(graph, mScenarioData.getTransitions().get(0), true);

		for (Transition tr : mScenarioData.getTransitions()) {
			addTransitionIngraph(graph, tr, false);
		}
		addln(graph, end_graph());
		return writeGraphInDotFile(new File(outFile.getPath() + File.separator + "all"), graph);

	}

	@SuppressWarnings("unused")
	private String start_graph(State initialState) {

		return start_graph(initialState.getName());
	}

	/**
	 * generate the DotFiles direcly from the Scenario with limit transitions
	 * 
	 * @param limit
	 *            : Max number of transitions
	 * @return true if file is generated
	 * @throws InterruptedException
	 */
	public boolean generateDotFileFor_limitedTransition(int limit) throws InterruptedException {
		addln(graph, start_graph(mScenarioData.getInitialStateName()));
		int i = 0;

		for (Transition tr : mScenarioData.getTransitions()) {
			addTransitionIngraph(graph, tr, false);
			if (i > limit) {
				break;
			}
			i++;
		}
		addln(graph, end_graph());
		return writeGraphInDotFile(new File(outFile.getPath() + File.separator + "all"), graph);
	}

	/***
	 * generate dot file per trace.
	 * 
	 * Trace is an ordered list of transitions that produce a path from the main
	 * activity to an ending child.
	 * 
	 * @param limit
	 * @return
	 * @throws InterruptedException
	 */
	public boolean generateDotFilePerTrace(int limit) throws InterruptedException {
		File outDirectory = outFile;
		ArrayList<Transition> trace = new ArrayList<Transition>();
		trace = mScenarioData.getTransitions();
		ArrayList<Transition> traceTemp = new ArrayList<Transition>();
		System.out.println("Number of transition : " + trace.size());
		if (trace.isEmpty()) {
			return false;
		}
		int i = 0;
		do {
			traceTemp = getOneTraceFromScenario(trace);
			if (traceTemp != null) {
				System.out.println("Current number of trace:  " + traceTemp.size());
				System.out.println("Current Number of transition :  " + trace.size());
				String outfilePath = outDirectory.getPath() + File.separator + OUT_SCEN + i;
				i++;
				if (!generateDirectTrace(TRACE, new StringBuilder(), traceTemp, new File(outfilePath))) {
					System.out.println(outfilePath + "is not generated");
					return false;
				}
			}
			if (trace.isEmpty()) {
				break;
			}
		} while (trace.size() > limit);
		return true;
	}

	/**
	 * Get a trace from a scenario's transition
	 * 
	 * 
	 * @param s_transitions
	 * @return a list of ordered transitions of trace.
	 * @throws InterruptedException
	 */
	private ArrayList<Transition> getOneTraceFromScenario(ArrayList<Transition> s_transitions)
			throws InterruptedException {

		ArrayList<Transition> traceTemp = new ArrayList<Transition>();
		traceTemp.add(s_transitions.get(0));
		int i;
		/**
		 * find index of last location =i.
		 */
		for (i = 1; i < s_transitions.size(); i++) {
			if (s_transitions.get(i).getSource().getName()
					.equalsIgnoreCase(s_transitions.get(i - 1).getDest().getName())) {
				traceTemp.add(s_transitions.get(i));
			} else {
				break;
			}
		}
		// if loop finish without back transition
		if (s_transitions.size() == i) {
			// s_transitions.clear();
			return traceTemp;
		}

		/*
		 * delete already generated path in trace
		 */

		/**
		 * find first location of the end of path
		 */
		int j = 0;
		for (j = 0; j < s_transitions.size(); j++) {
			if (s_transitions.get(j).getSource().getName()
					.equalsIgnoreCase(s_transitions.get(i).getSource().getName())) {
				break;
			}

		}
		System.out.println("j= " + j + "  i=" + i);
		/**
		 * remove generated trace in the list of transition of scenario
		 */
		if (i == j) {
			remove(s_transitions, 0, i);
			return traceTemp;
		}
		remove(s_transitions, j, i);
		return traceTemp;
	}

	/**
	 * remove in the initial trace transition between i and j
	 * 
	 * @param trace
	 * @param j
	 * @param i
	 */
	private void remove(ArrayList<Transition> trace, int j, int i) {
		int reper = 0;
		Iterator<Transition> it = trace.iterator();

		if (j == 0) {// point de depart
			do {
				it.next();
				if (i >= reper) {
					it.remove();
				}
				reper++;
			} while (it.hasNext());
			return;

		}

		do {
			it.next();
			reper++;
			if (i >= reper && reper > j) {
				it.remove();
			}
		} while (it.hasNext());

	}

	/**
	 * 
	 * @param graphName
	 * @param graphTrace
	 * @param trace
	 * @param outGraphfile
	 * @return
	 * @throws InterruptedException
	 */
	public boolean generateDirectTrace(String graphName, StringBuilder graphTrace,
			ArrayList<Transition> trace, File outGraphfile) throws InterruptedException {
		graphTrace = new StringBuilder();
		addln(graphTrace, start_graph(graphName));
		/**
		 * set first state as shape box
		 */
		if (mScenarioData.getTransitions() == null || mScenarioData.getTransitions().isEmpty()) {
			return false;
		}
		addTransitionIngraph(graphTrace, mScenarioData.getTransitions().get(0), true);

		for (Transition tr : trace) {
			addTransitionIngraph(graphTrace, tr, false);
		}
		addln(graphTrace, end_graph());
		return writeGraphInDotFile(outGraphfile, graphTrace);
	}

	private boolean writeGraphInDotFile(File outFile2, StringBuilder graph2) {
		System.out.println(graph2.toString());
		try {
			/**
			 * si existe changer de nom
			 */
			int i = 0;
			String name = outFile2.getAbsolutePath() + "%s";
			String filename;
			do {
				i++;
				filename = String.format(name, i);
			} while (new File(filename).exists());

			FileUtils.write(new File(filename), graph2.toString(), Config.UTF8);
			graph2.setLength(0);
			mDotFile = new File(filename);
			return true;
		} catch (IOException e) {
			return false;
		}

	}

	private void addTransitionIngraph(StringBuilder targetGraph, Transition tr, boolean isMain) {
		addln(targetGraph, buildTransitionAttributes(tr, isMain));
	}

	private static final String ARROW = "->";

	/**
	 * 
	 * @param tr
	 *            : transition to translate in dot file
	 * @param isMain
	 * @return
	 */
	private String buildTransitionAttributes(Transition tr, boolean isMain) {
		/**
		 * each parameter is stored in a HashMAp
		 */
		HashMap<String, String> transition_parameter = new HashMap<String, String>();
		/**
		 * build label parameter
		 */
		transition_parameter = buildLabel(tr.getAction(), transition_parameter);
		if (isMain) {
			// add shape node
			HashMap<String, String> node_parameter = new HashMap<String, String>();
			node_parameter.put("shape", "box");
			return buildNode(tr.getSource(), buildAttribute(node_parameter)) + buildInitTransition(tr)
					+ buildAttribute(transition_parameter);

		}
		return buildInitTransition(tr) + buildAttribute(transition_parameter);
	}

	private String buildNode(State source, String param) {
		return getShortName(source.getName()) + param + "\n";

	}

	private String buildInitTransition(Transition tr) {
		return getState(tr.getSource().getName()) + ARROW + getState(tr.getDest().getName());
	}

	public String buildAttribute(HashMap<String, String> transition_parameter) {
		String param = "";
		for (String key : transition_parameter.keySet()) {
			if (!param.equalsIgnoreCase("")) {
				param = param + "," + key + "=" + transition_parameter.get(key);
			} else {
				param = param + key + "=" + transition_parameter.get(key);
			}
		}
		return "[" + param + "]";
	}

	/**
	 * get short name of state
	 * 
	 * @param name
	 *            : the long name of state
	 * 
	 * 
	 * @return the short name of state
	 */
	private String getState(String name) {
		// short name
		return getShortName(name);
	}

	/**
	 * get short name
	 * 
	 * @param name
	 *            : the long name
	 * 
	 * 
	 * @return the short name. eg: a.b.c.d => d
	 */
	private String getShortName(String name) {
		String temp = "";
		StringTokenizer st = new StringTokenizer(name, ".");
		do {
			temp = st.nextToken();
		} while (st.hasMoreElements());
		return temp;
	}

	private HashMap<String, String> buildLabel(ArrayList<Action> actions,
			HashMap<String, String> transition_parameter) {
		String label = "";
		for (Action act : actions) {
			label = label + act.getName() + ":"
					+ (act.getWidget().getValue() != null ? act.getWidget().getValue() : "") + "|"
					+ Scenario.ERROR + ":" + (act.getError() != null ? act.getError() : "") + "|";
			if (act.getError() != null) {
				transition_parameter.put("color", "red");
				break;
			}
		}
		transition_parameter.put("label", "\"" + label + "\"");
		return transition_parameter;
	}

	/**
	 * 
	 * @param inputFile
	 *            : the dot file
	 * @param outputFile
	 *            : the output forma
	 * @param t
	 *            : the type of the ouput as listetd below: String type = "gif";
	 *            // String type = "dot"; // String type = "fig"; // open with
	 *            xfig // String type = "pdf"; // String type = "ps"; // String
	 *            type = "svg"; // open with inkscape // String type = "plain";
	 * @return true if graph is created
	 * @throws InterruptedException
	 */
	public boolean generateGraph(File inputFile, String t) throws InterruptedException {
		//
		String type = t;
		File out = new File(inputFile.getAbsolutePath() + "." + type); // Linux
		String args = Config.DOT_EXEC + " -T " + type + " " + inputFile.getAbsolutePath() + " -o "
				+ out.getAbsolutePath();
		System.out.println(args);
		try {
			Process p;
			p = Runtime.getRuntime().exec(args);
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	private File mDotFile;

	public File getDotFile() {
		return mDotFile;
	}
}
