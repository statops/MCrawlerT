package fr.openium.sga.displayer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Utils.SgUtils;
import kit.Utils.TransitionIdComparator;

public class CssGenerator {

	private File cssFile;

	private FileWriter writer;

	private String appName;
	private String mRobotium;
	// byDefault
	private final int mStoryBoardType;

	private ScenarioData mTree;

	private List<String> ids;

	private ArrayList<State> uniqueState = new ArrayList<State>();

	public CssGenerator(String appName) throws NullPointerException, Exception {
		ids = new ArrayList<String>();
		this.appName = appName;
		mStoryBoardType = GraphGenerator.SIMPLE_TYPE;
		boxPadding = 30;
		boxX = 30;
		boxY = 45;

	}

	public CssGenerator(String appName, ScenarioData tree, String robotium,
			int tree_type, int pad, int x, int y) throws NullPointerException,
			Exception {
		mStoryBoardType = tree_type;
		mRobotium = robotium;
		boxPadding = pad;
		boxX = x;
		boxY = y;
		ids = new ArrayList<String>();
		this.appName = appName;
		mTree = tree;
		if (mTree == null) {
			throw new NullPointerException(" Tree is null");
		}
		setUniqueState();
		setCssFile();

	}

	private void setUniqueState() {
		/**
		 * add an initState
		 */

		State init_State = mTree.getInitialState(true);
		if (init_State == null) {
			throw new NullPointerException("No initial state");
		}
		String id = getSource(init_State.getId());

		uniqueState.add(mTree.getState(id, Scenario.SOURCE));

		/*
		 * if (mTree.getTransitions(mTree.getInitialState(true).getId()) !=
		 * null) { uniqueState.add(mTree.getInitialState(true)); }
		 */

		for (State state : mTree.getStates()) {
			if (!state.isFinal() && state.isDest()
					&& (mTree.getTransitions(state.getId()) != null)) {
				uniqueState.add(state);
			}
		}
		/**
		 * trier
		 */
		Collections.sort(uniqueState, new StateComparator());

	}

	private void setCssFile() throws NullPointerException, IOException,
			CloneNotSupportedException {
		if (appName == null)
			throw new NullPointerException(
					"le nom de l'application doit �tre renseigner.");

		StringBuilder path = new StringBuilder();
		if (mRobotium != null) {
			cssFile = new File(mRobotium, appName + ".css");
			cssFile.createNewFile();
			defaultNode();
			return;

		}

		path.append("applications/");
		File appFolder = new File(path.toString());
		appFolder.mkdirs();
		cssFile = new File(appFolder, appName + ".css");
		cssFile.createNewFile();

		defaultNode();
	}

	public String getStylesheet() {
		return cssFile.getAbsolutePath();
	}

	public void addState(State state, boolean finalState) throws IOException {
		String stateId;
		finalState = false;
		if (state.isDest()) {
			stateId = state.getId();
		} else {
			stateId = getSource(state.getId());
			state.setId(stateId);
		}
		if (!finalState && ids.contains(state.getId())) {
			return;
		} else {
			if (finalState && ids.contains(state.getId() + "end")) {
				return;
			}
		}
		// if(finalState)
		// ids.add(state.getId()+"end");
		// else
		ids.add(state.getId());
		StringBuilder argument = new StringBuilder();
		writer = new FileWriter(cssFile, true);
		argument.append("node.id").append(state.getId());
		if (finalState)
			argument.append("end");
		addBox(argument);
		if (finalState)
			argument.append("stroke-color : grey;\n");
		else
			argument.append("stroke-color : green;\n");

		argument = addImage(argument, state);

		writer.write(argument.toString());
		close();
	}

	private final int boxPadding;
	private final int boxX;
	private final int boxY;

	private void addBox(StringBuilder argument) {
		argument.append("{\n").append("shape: box;\n")
				.append("padding: " + "" + boxPadding + "px;\n")
				.append("size: " + "" + boxX + "px, " + "" + boxY + "px;\n")
				.append("stroke-mode : plain;\n");

	}

	private StringBuilder addImage(StringBuilder argument, State state) {
		String imagePath = _getScreen(state);
		if (imagePath == null) {
			argument.append("}\n\n");
			return argument;
		}
		argument.append("fill-mode: image-scaled;\n")
				.append("fill-image: url('").append(imagePath).append("');\n")
				.append("}\n\n");
		return argument;
	}

	private void defaultNode() throws IOException, CloneNotSupportedException {

		if (mTree != null) {
			if (mStoryBoardType == GraphGenerator.SIMPLE_TYPE) {
				buildSimple();
				return;
			}
			buildTree();
			return;
		}
		StringBuilder argument = new StringBuilder();
		writer = new FileWriter(cssFile);
		argument.append("node").append("{\n").append("size: 20px, 50px;\n")
				.append("}\n").append("\n").append("graph").append("{\n")
				.append("fill-color : grey;\n").append("}\n");

		writer.write(argument.toString());

		close();
	}

	private void buildSimple() throws IOException, CloneNotSupportedException {
		StringBuilder argument = new StringBuilder();
		writer = new FileWriter(cssFile);
		argument.append("node").append("{\n").append("size: 20x, 50px;\n")
				.append("}\n").append("\n").append("graph").append("{\n")
				.append("fill-color : grey;\n").append("}\n");
		writer.write(argument.toString());
		for (Transition tr : mTree.getTransitions()) {
			/**
			 * addState, add edge,
			 */
			addUniqueState(tr.getSource().clone(), false);
			addUniqueState(tr.getDest().clone(), tr.getDest().isFinal());

		}
		close();

	}

	public void addUniqueState(State state, boolean b) throws IOException {
		state = getEquvalentState(state);
		if (ids.contains(state.getId())) {
			return;
		}
		ids.add(state.getId());
		writer = new FileWriter(cssFile, true);
		StringBuilder argument = new StringBuilder();
		argument.append("node.id").append(state.getId());
		addBox(argument);
		argument.append("stroke-color : green;\n");
		argument = addImage(argument, state);
		writer.write(argument.toString());
		close();

	}

	public State getEquvalentState(State state) {

		for (State unique : uniqueState) {
			if (SgUtils.isEqualState(unique, state)) {
				return unique;
			}
		}
		uniqueState.add(state);
		return state;
	}

	private void buildTree() throws IOException, CloneNotSupportedException {
		StringBuilder argument = new StringBuilder();
		writer = new FileWriter(cssFile);
		argument.append("node").append("{\n").append("size: 20x, 50px;\n")
				.append("}\n").append("\n").append("graph").append("{\n")
				.append("fill-color : grey;\n").append("}\n");
		writer.write(argument.toString());
		ArrayList<Transition> Edges = mTree.getTransitions();
		Collections.sort(Edges, new TransitionIdComparator());

		for (int i = Edges.size(); i > 0; i--) {
			Transition tr = Edges.get(i - 1);
			/**
			 * addState, add edge,
			 */
			addState(getSource(tr.getSource().getId()));
			addState(tr.getDest().clone(), tr.getDest().isFinal());

		}

		close();

	}

	private String _getScreen(State state) {
		StringBuilder pathScreen = new StringBuilder();
		/**
		 * nom du fichier en fonction du type de l'état
		 */

		pathScreen.append(mRobotium + File.separator).append(state.getType())
				.append("_").append(state.getId()).append(".jpg");
		File file = new File(pathScreen.toString());
		if (!file.exists()) {
			return null;
		}
		return file.getAbsolutePath();
		// return pathScreen.toString();
	}

	private void close() throws IOException {
		writer.close();
	}

	public void addErrorState(String idDestError) throws IOException {
		if (ids.contains(idDestError))
			return;
		else
			ids.add(idDestError);
		StringBuilder argument = new StringBuilder();
		writer = new FileWriter(cssFile, true);
		argument.append("node.id").append(idDestError).append("{\n")
				.append("padding: 50px;\n").append("size: 155px, 200px;\n")
				.append("fill-color : red;\n").append("}\n");

		writer.write(argument.toString());
		close();
	}

	private String getSource(String id) {
		int index = id.lastIndexOf(Scenario.IDSEPARATOR);
		if (index == 0 || index < 0) {
			return id;
		}
		return id.substring(0, index);

	}

	public int getStoryBoardType() {
		return mStoryBoardType;
	}

	public class StateComparator implements Comparator<State> {

		@Override
		public int compare(State t1, State t2) {
			/**
			 * case of integerType
			 */
			try {

				return compare(Integer.parseInt(t1.getId()),
						Integer.parseInt(t2.getId()));

			} catch (NumberFormatException ex) {
				// String id
				if (t1.getId().length() > t2.getId().length()) {
					return 1;
				}
				if ((t1.getId()).length() == (t2.getId()).length()) {
					/**
					 * the last id
					 */
					int t1id = SgUtils.get_end_id(t1.getId());
					int t2id = SgUtils.get_end_id(t2.getId());
					return compare(t1id, t2id);

				}
				return -1;

			}

		}

		/*
	 * 
	 * 
	 */
		private int compare(int t1id, int t2id) {
			if (t1id > t2id) {
				return 1;
			}
			if (t1id == t2id) {
				return 0;
			}
			return -1;

		}
	}

	public int getNumberOfNode(int level) {
		int nodeNumber = 0;
		for (State st : uniqueState) {
			int currentNode = (new StringTokenizer(st.getGraphStateId(),
					Scenario.IDSEPARATOR)).countTokens();
			if (currentNode == level) {
				nodeNumber++;
			}
		}
		return nodeNumber;
	}

	public int getMAxLevel() {
		return (new StringTokenizer(uniqueState.get(uniqueState.size() - 1)
				.getId(), Scenario.IDSEPARATOR)).countTokens();
	}

	public ArrayList<String> getNodeId(int level) {
		ArrayList<String> ids = new ArrayList<String>();
		for (State st : uniqueState) {
			int currentNode = (new StringTokenizer(st.getGraphStateId(),
					Scenario.IDSEPARATOR)).countTokens();
			if (currentNode == level) {
				ids.add(st.getGraphStateId());
			}

		}
		return ids;
	}

	public void addState(String sourceId) throws IOException {
		if (ids.contains(sourceId)) {
			return;
		}
		ids.add(sourceId);
		StringBuilder argument = new StringBuilder();
		writer = new FileWriter(cssFile, true);
		argument.append("node.id").append(sourceId);
		addBox(argument);
		argument.append("stroke-color : green;\n");
		argument = addImage(argument, sourceId);
		writer.write(argument.toString());
		close();

	}

	private StringBuilder addImage(StringBuilder argument, String sourceId) {
		StringBuilder pathScreen = new StringBuilder();
		String imagePath;
		/**
		 * nom du fichier en fonction du type de l'état
		 */
		pathScreen.append(mRobotium + File.separator).append(Scenario.SOURCE)
				.append("_").append(sourceId).append(".jpg");
		File file = new File(pathScreen.toString());

		if (!file.exists()) {
			imagePath = null;
		}
		imagePath = file.getAbsolutePath();
		// return pathScreen.toString();
		if (imagePath == null) {
			argument.append("}\n\n");
			return argument;
		}
		argument.append("fill-mode: image-scaled;\n")
				.append("fill-image: url('").append(imagePath).append("');\n")
				.append("}\n\n");
		return argument;
	}

}
