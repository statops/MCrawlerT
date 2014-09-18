package kit.Scenario;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import kit.Config.Config;
import kit.Utils.SgUtils;

/**
 * The Scenario object containing all states, actions and transitions
 * 
 * @author Stassia
 * 
 */
public class ScenarioData {

	/**
	 * version
	 */

	private String version;
	/**
	 * list of actions (Alphabet)
	 */
	private HashSet<Action> mActions = new HashSet<Action>();
	/**
	 * list of states
	 * 
	 * @author Stassia
	 * 
	 */
	private HashSet<State> mStates = new HashSet<State>();
	/**
	 * List of transitions
	 * 
	 * @author Stassia
	 * 
	 */
	private ArrayList<Transition> mTransitions = new ArrayList<Transition>();

	/**
	 * List of authors : activity that generate the scenario
	 * 
	 * @author Stassia
	 * 
	 */

	private ArrayList<Author> mAuthors = new ArrayList<Author>();

	/**
	 * List of Trees
	 * 
	 * @author Stassia
	 * 
	 */
	private HashSet<Tree> mTrees = new HashSet<Tree>();

	public ScenarioData() {
	}

	/**
	 * @param successor
	 * @param state
	 */
	public ScenarioData(Transition successor) {
		super();
		this.setAuthors(new Author(""));
		this.setStates(successor.getSource(), true, false);
		this.setStates(successor.getDest(), successor.getDest().isInit(),
				successor.getDest().isFinal());
		this.setTransitions(successor);
		Tree tree = new Tree("0", "0");
		tree.setTransitions(successor);
		this.setTrees(tree);
	}

	public HashSet<Action> getActions() {
		return mActions;
	}

	public void setActions(Action action) {
		mActions.add(action);
	}

	public HashSet<State> getStates() {
		return mStates;
	}

	/**
	 * 
	 * @param st
	 * @param initState
	 * @param finalState
	 */
	public void setStates(State st, boolean initState, boolean finalState) {
		st.setInit(initState, finalState);
		mStates.add(st);
	}

	public ArrayList<Transition> getTransitions() {
		return mTransitions;
	}

	public boolean set_unique_Transitions(Transition tr) {
		System.out.println("add unique transition");
		/**
		 * eviter les doublons de transition
		 */
		if (tr != null) {
			Iterator<Transition> tr_iterator = mTransitions.iterator();
			if (!tr_iterator.hasNext()) {
				setTransitions(tr);
				return true;
			}
			for (Transition tran : mTransitions) {
				if (SgUtils.isEqualTransition(tr, tran)) {
					/**
					 * check valid transition
					 */
					System.out.println("is Equal: " + tr.toString() + " wig: "
							+ tr.getWidgets().toString() + " ==  "
							+ tran.toString() + " wig: "
							+ tr.getWidgets().toString());
					return false;
				}
			}

			setTransitions(tr);
			return true;
		} else {
			throw new IllegalStateException("transition must not be null");
		}
	}

	public void setTransitions(Transition tr) {
		/**
		 * eviter les doublons de transition
		 */
		if (tr != null) {
			/*
			 * if (!getStateIds().contains(tr.getId())) {
			 * addStates(tr.getSource(), true); addStates(tr.getDest(), true); }
			 */
			mTransitions.add(tr);
		} else {
			throw new IllegalStateException("transition must not be null");
		}
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version
	 *            the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	public String getInitialStateName() {
		String name = (getInitialState() == null) ? null : getInitialState()
				.getName();
		return name;
	}

	public State getInitialState(boolean required_transition) {
		if (!required_transition) {
			return getInitialState();
		} else {
			ArrayList<String> id = new ArrayList<String>();
			ArrayList<String> name = new ArrayList<String>();
			init_state_id_name(id, name);
			for (State state : mStates) {
				String idd = state.getId();
				String namem = state.getName();
				String type = state.getType();
				if (is_init_state(id, idd, namem, type, name)) {
					if (getTransitions(state.getId()) != null) {
						state.setInit(true);
						return state;
					}
				}
			}
			System.out.println("No initial state");
			return null;
		}

	}

	/**
	 * list of all initial state
	 */

	public ArrayList<State> getInitialStateLists() {
		ArrayList<State> lists = new ArrayList<State>();

		ArrayList<String> id = new ArrayList<String>();
		ArrayList<String> name = new ArrayList<String>();
		init_state_id_name(id, name);
		for (State state : mStates) {
			String idd = state.getId();
			String namem = state.getName();
			String type = state.getType();
			if (is_init_state(id, idd, namem, type, name)) {
				if (getTransitions(state.getId()) != null) {
					state.setInit(true);
					lists.add(state);
				}
			}
		}
		System.out.println("No initial state");
		return lists;

	}

	/**
	 * @param id
	 * @param name
	 * @param type
	 * @param namem
	 * @param idd
	 * @return
	 */
	private boolean is_init_state(ArrayList<String> id, String idd,
			String namem, String type, ArrayList<String> name) {
		return id.contains(idd)
				&& name.contains(namem)
				&& (type.equalsIgnoreCase(Scenario.SRC) || type
						.equalsIgnoreCase(Scenario.SOURCE));
	}

	/**
	 * @param id
	 * @param name
	 */
	private void init_state_id_name(ArrayList<String> id, ArrayList<String> name) {
		for (State state : mStates) {
			if (state.isInit()) {
				id.add(state.getId());
				name.add(state.getName());
			}
		}
		/**
		 * ajouter les etats equivalents
		 */
		/**
		 * normalement les etats initiales doivent �tre tous equivalents
		 */
		State valid_initial_state = SgUtils.get_a_valid_state(mStates, id);
		if (valid_initial_state == null) {
			return;
		}
		valid_initial_state.setInit(true);
		for (State state : mStates) {
			if (!id.contains(state.getId())) {
				if (SgUtils.isEqualState(state, valid_initial_state)) {
					id.add(state.getId());
					break;
				}

			}
		}
	}

	public State getInitialState() {
		ArrayList<String> id = new ArrayList<String>();
		ArrayList<String> name = new ArrayList<String>();
		for (State state : mStates) {
			if (state.isInit()) {
				id.add(state.getId());
				name.add(state.getName());
				// break;
			}
		}
		/**
		 * voir si une transition existe au m�me id.
		 */
		// s0_x

		/**
		 * ou ˆ chercher par Id par defaut initial state ˆ definir par 0
		 */
		for (State state : mStates) {
			String idd = state.getId();
			String namem = state.getName();
			String type = state.getType();
			if (id.contains(idd)
					&& name.contains(namem)
					&& (type.equalsIgnoreCase(Scenario.SRC) || type
							.equalsIgnoreCase(Scenario.SOURCE))) {
				return state;
			}
		}
		System.out.println("No initial state");
		return null;
	}

	public ArrayList<Author> getAuthors() {
		return mAuthors;
	}

	public void setAuthors(Author aut) {
		if (!mAuthors.contains(aut)) {
			this.mAuthors.add(aut);
		}
	}

	public void addStates(State source, boolean replace) {
		if (!replace) {
			addStates(source);
			return;
		}
		Iterator<State> state_iterator = mStates.iterator();
		if (!state_iterator.hasNext()) {
			addStates(source);
			return;
		}
		do {
			State current_state = state_iterator.next();
			if (current_state.getId().equalsIgnoreCase(source.getId())
					&& current_state.getType().equalsIgnoreCase(
							source.getType())) {
				state_iterator.remove();
			}
		} while (state_iterator.hasNext());
		addStates(source);
	}

	public void addStates(State source) {
		mStates.add(source);
	}

	public HashSet<Tree> getTrees() {
		if (mTrees.isEmpty() && !mTransitions.isEmpty()) {
			Tree trTemp = new Tree("", "0");
			trTemp.setTransitions(mTransitions);
			mTrees.add(trTemp);
		}
		return mTrees;
	}

	public void setTrees(Tree tree, boolean erase) {
		if (!erase) {
			setTrees(tree);
			return;
		}

		/**
		 * verification si le tree id existe et le supprimer si erase
		 */
		Iterator<Tree> traIt = mTrees.iterator();
		while (traIt.hasNext()) {
			Tree temp = traIt.next();
			if (temp.getId().equalsIgnoreCase(tree.getId())) {
				traIt.remove();
				break;
			}
		}
		/**
		 * verifier les repetitions de transition
		 */
		setTrees(tree);
	}

	public void setTrees(Tree tree) {
		if (tree == null) {
			return;
		}
		if (tree.getTransitions() != null || (!tree.getTransitions().isEmpty())) {
			/*
			 * ArrayList<String> transition_id=new ArrayList<String>(); for
			 * (Tree tr:mTrees){ transition_id.addAll(tr.getTransitions_ids());
			 * } Iterator<Transition> traIt =tree.getTransitions().iterator();
			 * while (traIt.hasNext()) { Transition temp = traIt.next(); if
			 * (transition_id.contains(temp.getId())) { traIt.remove(); } }
			 */
			mTrees.add(tree);
		}

	}

	public void setTransitions(Transition tr, String treeID, boolean b) {
		setTransitions(tr, b);
		for (Tree tree : mTrees) {
			if (tree.getId().equalsIgnoreCase(treeID)) {
				tree.setTransitions(tr, b);
				break;
			}
		}

	}

	public void setTransitions(Transition tr, boolean erase) {
		/**
		 * verification si la transition id existe et le supprimer si erase
		 */
		if (tr == null) {
			return;
		}
		if (!erase)
			setTransitions(tr);
		else {
			Iterator<Transition> traIt = mTransitions.iterator();
			if (!traIt.hasNext()) {
				while (traIt.hasNext()) {
					Transition temp = traIt.next();
					if (temp.getId().equalsIgnoreCase(tr.getId())) {
						traIt.remove();
						break;
					}
				}
			}

			setTransitions(tr);
		}

	}

	public void setTransitions_with_newID(Transition tr, String id) {
		tr.setId(id);
		tr.getSource().setId(id);
		tr.getDest().setId(id);
		setTransitions(tr);

	}

	public Transition getTransitions(String transitionId) {
		for (Transition tr : mTransitions) {
			if (tr.getId().equalsIgnoreCase(transitionId)) {
				return tr;
			}
		}
		return null;
	}

	public void remove_transition(String id) {
		Iterator<Transition> transIt = mTransitions.iterator();
		if (!transIt.hasNext()) {
			return;
		}

		do {
			Transition nextTr = transIt.next();
			if (nextTr.getId().equals(id)) {
				transIt.remove();
				remove_states(id);
			}

		} while (transIt.hasNext());
	}

	public void addPath(State discovered_state, ScenarioData path) {
		/**
		 * recupérer le tree auquel appartien l'eta dans path
		 */
		/**
		 * path must not be null
		 */
		if (path == null) {
			throw new NullPointerException("path is null");
		}
		Tree temp = path.getTree(SgUtils.getTreeIdOfTransition(path,
				discovered_state.getId()));
		setTrees(temp, false);
		setTransitions(path.getTransitions(discovered_state.getId()), false);
	}

	public Tree getTree(String treeId) {
		if (treeId == null) {
			return null;
		}
		for (Tree tr : mTrees) {
			if (tr.getId().equalsIgnoreCase(treeId)) {
				return tr;
			}
		}
		return null;
	}

	/**
	 * Create a non repeated Transitions, trees and state
	 */
	public void clean_() {
		System.out.println("*****************Clean************************");
		/**
		 * get one instance of each transition: clean by id
		 */
		Iterator<Transition> tr_iterator = mTransitions.iterator();
		if (!tr_iterator.hasNext()) {
			return;
		}
		do {
			Transition current_tr = tr_iterator.next();
			int i = 0;
			for (Transition tr : mTransitions) {
				if (tr.getId().equalsIgnoreCase(current_tr.getId())) {
					i++;
				}
				if (i == 2) {
					break; // for
				}
			}
			if (i == 2) {
				tr_iterator.remove();
			}

		} while (tr_iterator.hasNext());

		/**
		 * delete the same transition value
		 */
		/*
		 * tr_iterator = mTransitions.iterator(); if (!tr_iterator.hasNext()) {
		 * return; } do { Transition current_tr = tr_iterator.next(); int i = 0;
		 * for (Transition tr : mTransitions) { if
		 * (SgUtils.isEqualTransistion(tr, current_tr)) { i++; } if (i == 2) {
		 * break; // for } } if (i == 2) { tr_iterator.remove(); } } while
		 * (tr_iterator.hasNext());
		 */

	}

	/**
	 * @param path
	 * @param is_in_parallele_mode
	 * @throws CloneNotSupportedException
	 */
	public void add_(ScenarioData path, boolean is_in_parallele_mode)
			throws CloneNotSupportedException {
		if (path == null) {
			return;
		}

		HashSet<String> id = getTransitions_id();
		// System.out.println("Old state initizal state:" +
		// path.getInitialState().toString());
		Tree tree_temp = new Tree("", SgUtils.get_a_tree_Id(this, ""));
		if (path.getTransitions().isEmpty() && !path.getStates().isEmpty()) {
			for (State st : path.getStates()) {
				/**
				 * ajouter que les etats initials
				 */
				if (st.isInit()) {
					mStates.add(st);
				}
			}
			return;
		}

		for (Transition transition : path.getTransitions()) {
			State source = path.getState(transition.getId(), Scenario.SOURCE);
			State dest = path.getState(transition.getId(), Scenario.DEST);
			if (source == null || dest == null) {
				continue;
			}
			State source_temp = source.clone();
			State dest_temp = dest.clone();
			Transition transition_temp = transition.clone();
			System.out
					.println("*************************************************************");
			if (Config.DEBUG) {
				System.out.println("old State source: " + source_temp + " ");// source_temp.getId()
																				// +
																				// " : "
				// + source_temp.toString());
				System.out.println("old State dest: " + dest_temp.getId()
						+ " : " + dest_temp.toString());
				System.out.println("old Transition dest: "
						+ transition_temp.toString());
			}

			if (is_in_parallele_mode && contains_state(dest_temp)) {
				System.out.println("change state to end state");
				dest_temp.setInit(false, true);
			}
			if (Config.DEBUG) {
				System.out.println("new State source: " + source_temp.getId()
						+ " : " + source_temp.toString());
				System.out.println("new State dest: " + dest_temp.getId()
						+ " : " + dest_temp.toString());
				System.out.println("new Transition dest: "
						+ transition_temp.toString());
			}
			System.out
					.println("*************************************************************");
			id.add(transition_temp.getId());
			if (set_unique_Transitions(transition_temp)) {
				tree_temp.setTransitions(transition_temp);
				mStates.add(source_temp);
				mStates.add(dest_temp);
			}
			for (State st : mStates) {
				if (Config.DEBUG) {
					System.out.println(" mState-- " + st.getId() + " : "
							+ st.toString());
				}
			}
		}

		/**
		 * ajouter les transitions avec des identifiants uniques
		 */
		setTrees(tree_temp);

	}

	/**
	 * @param dest_temp
	 * @return
	 */
	private boolean contains_state(State dest_temp) {
		for (State st : mStates) {
			if (st.isDest() && SgUtils.isEqualState(st, dest_temp)) {
				if (!st.isFinal() || (st.isFinal() == dest_temp.isFinal())) {// ){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return
	 */
	private HashSet<String> getTransitions_id() {
		HashSet<String> id = new HashSet<String>();
		for (Transition transition : mTransitions) {
			if (!id.contains(transition.getId())) {
				id.add(transition.getId());
			}
		}
		return id;
	}

	/**
	 * @param new_states
	 */
	public void addStates(HashSet<State> new_states) {
		mStates.addAll(new_states);

	}

	/**
	 * Add path leading to new states
	 * 
	 * @param new_states
	 * @param path
	 */
	public void addTransitions(HashSet<State> new_states, ScenarioData path) {
		for (State st : new_states) {
			for (Transition tr : path.getTransitions()) {
				if (st.getId().equalsIgnoreCase(tr.getId())) {
					setTransitions(tr);
				}
			}

		}

	}

	/**
	 * @param authors
	 */
	public void setAuthors(ArrayList<Author> authors) {
		mAuthors.addAll(authors);

	}

	/**
	 * @param actions
	 */
	public void setActions(HashSet<Action> actions) {
		mActions.addAll(actions);

	}

	/**
	 * @param transitions
	 */
	public void setTransitions(ArrayList<Transition> path) {
		if (path == null) {
			return;
		}
		for (Transition tr : path) {
			setTransitions(tr, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String transition = "";
		for (Transition tr : mTransitions) {
			transition = transition + "  \n" + tr.toString();
		}
		return transition;
	}

	public State getState(String id) {
		for (State st : mStates) {
			if (st.getId().equalsIgnoreCase(id)) {
				return st;
			}
		}
		return null;
	}

	/**
	 * 
	 * @param id
	 * @param type
	 *            : - source or dest
	 * @return
	 */
	public State getState(String id, String type) {
		State end = null;
		try {
			for (State st : mStates) {
				if (st.getId() == null) {
					continue;
				}
				if (st.getId().equalsIgnoreCase(id)
						&& st.getType().equalsIgnoreCase(type)) {// &&
																	// ////
																	// //
																	// !st.getWidgets().isEmpty()
					if (st.getName().equalsIgnoreCase(Scenario.END)) {
						end = st;
					} else
						return st;
				}
			}

		} catch (Exception e) {
			return null;
		}
		return end;
	}

	/**
	 * 
	 * @param id
	 * @param type
	 * @param state_name
	 * @return
	 */
	public State getState(String id, String type, String state_name) {
		for (State st : mStates) {
			if (st.getId().equalsIgnoreCase(id)
					&& st.getType().equalsIgnoreCase(type)
					&& st.getName().equalsIgnoreCase(state_name)) {
				return st;
			}
		}
		return null;
	}

	/**
	 * get the tree id containing tr
	 * 
	 * @param tr
	 */
	public String getTreeId(Transition tr) {
		for (Tree tree : getTrees()) {
			for (Transition transition_in_tree : tree.getTransitions()) {
				if (tr.getId().equalsIgnoreCase(transition_in_tree.getId())) {
					return tree.getId();
				}
			}
		}
		return null;
	}

	/**
	 * @return
	 */
	public boolean isEmpty() {

		return mTransitions == null || mTransitions.isEmpty();
	}

	/**
	 * by default 0
	 */
	private String mTaskId = "0";

	/**
	 * @return
	 */
	public String getTaskId() {

		return mTaskId;
	}

	public void setTaskId(String id) {

		mTaskId = id;
	}

	public ArrayList<String> getTransitionsId() {
		ArrayList<String> allTr = new ArrayList<String>();
		for (Transition tr : getTransitions())

		{
			String currentId = tr.getId();
			if (!allTr.contains(currentId))
				allTr.add(currentId);

		}
		return allTr;
	}

	public void remove_states(String id) {
		Iterator<State> itState = mStates.iterator();
		while (itState.hasNext()) {
			if (itState.next().getId().equalsIgnoreCase(id)) {
				itState.remove();
			}
		}
		;
	}

	public ArrayList<String> getStateIds() {
		ArrayList<String> allSt = new ArrayList<String>();
		for (State tr : getStates())

		{
			String currentId = tr.getId();
			if (!allSt.contains(currentId))
				allSt.add(currentId);

		}
		return allSt;
	}

}
