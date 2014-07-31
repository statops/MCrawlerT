package fr.openium.sga.result;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.State;
import kit.Utils.SgUtils;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.bissimulation.SgaGraph;
import fr.openium.sga.dot.model.Refinement;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.strategy.AbstractStrategy;
import fr.openium.sga.threadPool.CrawlerTask;
import fr.openium.taskPool.IResultReceiver;

/**
 * mod�le de resultat l'arbre final
 * 
 * @author Stassia
 * 
 */
public class CrawlResult implements IResultReceiver {
	private SgaGraph mGraph;
	private SgaGraph mInitGraph;
	private long mCoverage;
	private ScenarioData mScenarioData;
	private File mPath_directory;
	private File mTree_directory;
	private AbstractStrategy mStrategyType;

	// private int mStrategyType = AbstractStrategy.DFS_BFS_STRATEGY_ID;// by
	// default

	// public CrawlResult(File path) {
	// this(path, 0);
	// }

	public CrawlResult(File path, AbstractStrategy strategy) {
		mScenarioData = null;
		mTree_directory = new File(path.getPath() + ConfigApp.SCENARII);
		mPath_directory = new File(path.getPath() + File.separator
				+ ConfigApp.PATH);
		/**
		 * init newState
		 */
		newState.add(0);
		mStrategyType = strategy;

	}

	public SgaGraph getFinalGraph() {
		if (mGraph == null) {
			return new Refinement(mScenarioData).generate_graphe(mScenarioData);
		}
		return mGraph;
	}

	public void setGraph(SgaGraph Graph) {
		if (Graph != null)
			this.mGraph = Graph;
	}

	public long getCoverage() {
		return mCoverage;
	}

	public void setCoverage(long mCoverage) {
		this.mCoverage = mCoverage;
	}

	private ArrayList<ScenarioData> mPath_list = new ArrayList<ScenarioData>();

	/**
	 * 
	 * @param crawl_result
	 *            : le resultat d'un emulateur � ajouter
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public synchronized ArrayList<CrawlerTask> add(CrawlerTask crawl_result)
			throws CloneNotSupportedException {
		if (crawl_result == null) {
			save_Scenario();
			return null;
		}
		HashSet<State> new_states = null;
		new_states = null;
		ArrayList<CrawlerTask> task = new ArrayList<CrawlerTask>();
		solve(crawl_result);
		new_states = get_new_state(crawl_result);
		if (crawl_result.getPath().getInitialState() == null) {
			if (ConfigApp.DEBUG) {
				System.out.println("Initial_state_is_null");
			}
		}
		HashSet<String> new_state_id = get_new_state_id(new_states);
		if (crawl_result.getPath().getTransitions().isEmpty()) {
			task = handle_empty_path(crawl_result, new_states, new_state_id,
					task);
		} else {
			task = handle_non_empty_path(crawl_result, new_states,
					new_state_id, task);
		}
		save_Scenario();
		return task;
	}

	/**
	 * @param task
	 * @param new_state_id
	 * @param new_states
	 * @param crawl_result
	 * @return
	 * @throws CloneNotSupportedException
	 */
	private ArrayList<CrawlerTask> handle_non_empty_path(
			CrawlerTask crawl_result, HashSet<State> new_states,
			HashSet<String> new_state_id, ArrayList<CrawlerTask> task)
			throws CloneNotSupportedException {
		for (State st : crawl_result.getPath().getStates()) {
			boolean is_new_state = new_state_id.contains(st.getId());
			if (new_states != null && is_new_state && ((st.isDest()))
					&& (!st.isFinal())) {
				if (ConfigApp.DEBUG) {
					System.out.println("New state: " + st.toString());
				}
				/**
				 * y ajouter les transitions menant � la nouvelle etat
				 */
				if (st.getName().contains("CheckboxActivity")
						|| st.getName().contains("ListViewActivity")) {
					if (ConfigApp.DEBUG) {
						System.out.println("debug");
					}
				}
				ScenarioData path = add_new_state_of_path(st,
						crawl_result.getPath());
				if (path == null) {
					continue;
				}
				if (ConfigApp.DEBUG) {
					System.out.println(st.getName() + "  -");
					System.out.println("PATH to reach the new state"
							+ path.toString());
				}
				if (st.getType() == null || st.getWidgets() == null
						|| st.getWidgets().isEmpty()) {
					continue;
				}
				if (path.getInitialState() == null) {
					/**
					 * trouver la source d'une transition
					 */
					task = get_path_from_main_tree(st, path, task);
					continue;
				} else {
					save(path);
					task.add(new CrawlerTask(st.getName(), null, path, false,
							getRank(path, st)));
				}
			} else {
				/**
				 * not new state
				 */
				/**
				 * ajouter l'etat et la transition contenant l'etat dans le
				 * scenario
				 */
				if (st.isDest()) {
					add_new_state_of_path(st, crawl_result.getPath());
				}

			}

		}
		return task;
	}

	private int getRank(ScenarioData path, State st) {
		System.err.println("getRank: ");
		return rank_per_strategy(path, st);
	}

	private int rank_per_strategy(ScenarioData path, State st) {
		int rank = mStrategyType.getRank(st, path);
		System.err.println("rank " + rank);
		return rank;
	}

	/**
	 * @param st
	 * @param path
	 * @return
	 * @throws CloneNotSupportedException
	 */
	private ArrayList<CrawlerTask> get_path_from_main_tree(State st,
			ScenarioData path, ArrayList<CrawlerTask> task)
			throws CloneNotSupportedException {
		State source = SgUtils.get_source_state(path);
		// ScenarioData path_from_source = SgUtils.getPath(source,
		// mScenarioData);
		ScenarioData path_from_source = SgUtils.getConcretePath(source,
				mScenarioData);

		if (path_from_source.getInitialState() != null) {
			if (ConfigApp.DEBUG) {
				System.out.println(source.getName() + "  -");
				System.out.println("PATH to reach the new state"
						+ path_from_source.toString());
			}
			save(path_from_source);
			task.add(new CrawlerTask(st.getName(), null, path, false, getRank(
					path, st)));
		}
		mScenarioData.add_(path, true);
		return task;
	}

	/**
	 * @param crawl_result
	 * @param new_states
	 * @param new_state_id
	 * @param task
	 * @return
	 * @throws CloneNotSupportedException
	 */
	private ArrayList<CrawlerTask> handle_empty_path(CrawlerTask crawl_result,
			HashSet<State> new_states, HashSet<String> new_state_id,
			ArrayList<CrawlerTask> task) throws CloneNotSupportedException {
		/**
		 * cas o� il n'y a pas de transition
		 */
		HashSet<State> path_states = get_unique_instance_of_dest(crawl_result
				.getPath().getStates());
		for (State st : path_states) {
			boolean is_new_state = new_state_id.contains(st.getId());
			ScenarioData path = add_new_state_of_path(st,
					crawl_result.getPath());
			if (path == null) {
				continue;
			}
			/**
			 * une nouvelle t�che que pour ceux qui sont � l'etat end ce qui
			 * ne sont pas � l'etat end
			 */

			if (new_states == null || !is_new_state) {
				continue;
			}
			if (st.getType() == null || st.getWidgets() == null
					|| st.getWidgets().isEmpty()) {
				continue;
			}
			if (st.isFinal()) {
				continue;
			}
			if (path.getInitialState() == null) {
				continue;
			}
			save(path);
			task.add(new CrawlerTask(st.getName(), null, path, false, getRank(
					path, st)));

		}
		return task;
	}

	/**
	 * Comparer avec l'arbre actuelle
	 * 
	 * @param crawl_result
	 * @return
	 */
	private HashSet<State> get_new_state(CrawlerTask crawl_result) {
		HashSet<State> new_states = null;
		if (mScenarioData == null) {
			mScenarioData = crawl_result.getPath();
			new_states = mScenarioData.getStates();
		} else {
			/**
			 * add into the Tree the state and the path to get the state
			 */

			new_states = SgUtils.getNewStates(mScenarioData,
					crawl_result.getPath());
			/**
			 * voir le nouveau etat et ces widgets environnement
			 */

			/**
			 * voir le rank
			 */

			/** */

		}
		return new_states;

	}

	/**
	 * @param st
	 * @param path
	 * @return
	 * @throws CloneNotSupportedException
	 */
	private ScenarioData add_new_state_of_path(State st, ScenarioData path)
			throws CloneNotSupportedException {
		// ScenarioData the_path = SgUtils.getPath(st, path);
		ScenarioData the_path = SgUtils.getConcretePath(st, path);
		if (the_path != null) {
			mPath_list.add(the_path);
		}
		add_new_state_of_path(the_path);
		return the_path;
	}

	/**
	 * @param states
	 * @return
	 */
	private HashSet<State> get_unique_instance_of_dest(HashSet<State> states) {
		HashSet<State> state = new HashSet<State>();
		for (State path_state : states) {
			if (!state.contains(path_state)) {
				state.add(path_state);
			}
		}
		return state;
	}

	/**
	 * @param path
	 * @throws CloneNotSupportedException
	 */
	private void add_new_state_of_path(ScenarioData path)
			throws CloneNotSupportedException {
		if (path != null) {
			mScenarioData.add_(path, true);
		}
		display_current_tree();
	}

	private void display_current_tree() {
		if (mScenarioData != null) {
			SgaGraph graph = new Refinement(mScenarioData)
					.generate_graphe(mScenarioData);
			graph.display();
		}

	}

	public ArrayList<ScenarioData> getPathList() {
		return mPath_list;
	}

	/**
	 * @param st
	 * @param path
	 * @return
	 */
	/*
	 * private boolean is_init(State st, ScenarioData path) { State initState =
	 * (path.getInitialState()); if (initState == null) { initState =
	 * mScenarioData.getInitialState(); } if (SgUtils.isEqualState(st,
	 * initState)) { return true; } return false; }
	 */

	/**
	 * @param new_states
	 * @return
	 */
	private HashSet<String> get_new_state_id(HashSet<State> new_states) {
		HashSet<String> new_state_id = new HashSet<String>();
		for (State st : new_states) {
			new_state_id.add(st.getId());
		}
		return new_state_id;
	}

	/**
	 * 
	 */
	private void save_Scenario() {
		if (mScenarioData == null) {
			return;
		}
		if (!mTree_directory.exists()) {
			mTree_directory.mkdirs();
		}
		String temp = new File("").getAbsolutePath() + File.separator + "_out";
		new ScenarioGenerator(temp).generateXml(mScenarioData);
		Emma.savegeneric_file(new File(temp), mTree_directory, ".xml");
	}

	/**
	 * @param path
	 */
	private void save(ScenarioData path) {
		if (!mPath_directory.exists()) {
			mPath_directory.mkdirs();
		}
		String temp = new File("").getAbsolutePath() + File.separator + "_path";
		new ScenarioGenerator(temp).generateXml(path);
		Emma.savegeneric_file(new File(temp), mPath_directory, ".xml");
	}

	/**
	 * check if path is valid
	 * 
	 * @param crawl_result
	 */
	private CrawlerTask solve(CrawlerTask crawl_result) {
		crawl_result.setPath(SgUtils.solve(crawl_result.getPath()));
		return crawl_result;

	}

	public ScenarioData getScenarioData() {
		if (mScenarioData == null) {
			return null;
		}
		// mScenarioData.clean_();
		return mScenarioData;
	}

	public void setScenarioData(ScenarioData mScenarioData) {
		this.mScenarioData = mScenarioData;
	}

	public void compute_bissimilation() {
		if (mScenarioData == null) {
			return;

		}
		if (mScenarioData.getTransitions() == null
				|| mScenarioData.getTransitions().isEmpty()) {
			System.err
					.println("No graph is computed because transitions are null or empty");
			return;
		}
		Refinement biss = new Refinement(mScenarioData);
		SgaGraph[] result = biss.computeBissModel();
		mGraph = result[1];
		mInitGraph = result[0];

		// mGraph=biss.generate_graphe(mScenarioData);
		// mInitGraph=biss.generate_graphe(mScenarioData);
	}

	public SgaGraph getInigraph() {
		return mInitGraph;
	}

	@Override
	public synchronized ArrayList<CrawlerTask> update(Object result) {
		availability = false;
		ArrayList<CrawlerTask> tasks = null;
		try {
			tasks = add((CrawlerTask) result);

		} catch (Exception e) {
			if (ConfigApp.DEBUG) {
				System.out.println(e.getMessage());
			}
		}
		availability = true;
		compute_bissimilation();
		// if (mGraph != null) {
		// mGraph.display();
		// }
		System.err.println("current time: " + Emma.getTime());
		return tasks;
	}

	private boolean availability = true;

	/**
	 * @return
	 */
	public boolean isAvailable() {
		return availability;
	}

	ArrayList<Integer> newState = new ArrayList<Integer>();

	/**
	 * @return
	 */
	public ArrayList<Integer> getListNewStates() {
		if (mScenarioData == null) {
			return newState;
		}
		newState.clear();
		for (State st : mScenarioData.getStates()) {
			if (st.isDest()) {
				if (!SgUtils.is_source_equivalent_exist(mScenarioData, st)) {
					newState.add(Integer.parseInt(st.getId()));
				}
			}
		}
		return newState;
	}

	public AbstractStrategy getStrategyType() {
		return mStrategyType;
	}

}
