package kit.TestRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestResult;
import kit.Config.Config;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Tree;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

public abstract class SgdFourmyStrategy extends AbstractCrawler {
	protected static final File RED_WIDGET_LIST = new File(SCPATH
			+ File.separator + Config.RED_WIDGET);
	protected static final File CRASH_SCENARIO_FILE = new File(SCPATH
			+ File.separator + Config.CRASH_FILE);
	private State initialState;
	private State last_stateState;

	@SuppressWarnings("rawtypes")
	public SgdFourmyStrategy(String pkg, Class activityClass) {
		super(pkg, activityClass);
	}

	/**
	 * Cross View
	 * 
	 * @throws IOException
	 */
	protected ScenarioData generateScenario(Activity activity, final File path)
			throws InterruptedException, IOException {
		// registreToAllSystemProviderObservers();
		if (Config.DEBUG) {
			Log.d(TAG, "generateScenario ");
		}
		Thread.sleep(1000);
		Thread run = new Thread(

		) {

			@Override
			public void run() {
				mScenarioData = readPath(path);
				/**
				 * path also
				 */
				mPath = readPath(path);
			}
		};
		run.start();
		do {
			Thread.sleep(2000);
		} while (run.isAlive());

		/**
		 * cas initial null
		 */
		if (mScenarioData == null) {

			return initScenario(activity, path);
		}

		// initScenario(activity, path);
		initialState = mScenarioData.getInitialState();
		last_stateState = SgUtils.get_last_state(mScenarioData);

		if (last_stateState != null) {

			mScenarioData.setTaskId(last_stateState.getId());
			target_sourceUserEnvironment = last_stateState
					.get_user_environment();
			// goToLastState(initialState, last_stateState);
			_blind_goToLastState(last_stateState);
			activity = mSolo.getCurrentActivity();
			/**
			 * current id
			 */
		} else {
			/**
			 * initial mode
			 */
			mScenarioData.setTaskId(initialState.getId());
		}

		List<String> redWidgList = new ArrayList<String>();
		if (!RED_WIDGET_LIST.exists()) {
			RED_WIDGET_LIST.createNewFile();
		}
		joinScenarioWithCrashFileIfExist();
		redWidgList = FileUtils.readLines(RED_WIDGET_LIST, Config.UTF8);
		crawlUiInActivity(activity, redWidgList,
				SgUtils.get_a_tree_Id(mScenarioData, ""));
		// SgUtils.endScenarioData(mScenarioData);
		// unregisterContentObserver();
		return mScenarioData;
	}

	private void joinScenarioWithCrashFileIfExist() {
		if (!CRASH_SCENARIO_FILE.exists()) {
			if (Config.DEBUG) {
				Log.d(TAG, "joinScenarioWithCrashFileIfExist()");
				Log.d(TAG, "No Crash file ");
			}
			return;
		}

		if (Config.DEBUG) {
			Log.d(TAG, "join_scenarii ");
		}

		mScenarioData = ScenarioParser.parse(CRASH_SCENARIO_FILE);
	}

	/**
	 * Initial state and all actions.
	 * 
	 * @param activity
	 * 
	 * @param currentActivity
	 * @throws InterruptedException
	 */
	private ScenarioData initScenario(Activity activity, File path)
			throws InterruptedException {
		if (mScenarioData != null) {
			return mScenarioData;
		} else {
			mScenarioData = new ScenarioData();
		}
		/**
		 * version ˆ definir ****
		 */
		String version = mScenarioData.getVersion();
		if (version == null || version.equalsIgnoreCase("")) {
			mScenarioData.setVersion("" + 0);
		} else {
			mScenarioData.setVersion("" + (Integer.parseInt(version) + 1));
		}
		// mScenarioData.setAuthor;

		/***
		 * add mainActivityStates
		 */

		State initialState = new State((activity.getClass().getName()), true,
				false, "0");
		initialState.setType(Scenario.SOURCE);
		initialState.setTime(getTime());
		/**
		 * wait view size >0
		 */

		if (Config.DEBUG) {
			Log.d(TAG, "addWidgetsInState ");
			Log.d(TAG, "View Size:" + mSolo.getCurrentViews().size());
		}

		initialState = SgUtils.addWidgetsInState(initialState, get_views(),
				mContext);

		/**
		 * add widgets
		 */
		mScenarioData.setStates(initialState, true, false);
		SgUtils.setActionInScenarioData(mScenarioData, mSolo);
		/***
		 * add an End state
		 */
		mScenarioData = SgUtils.setDefaultEndState(mScenarioData);
		/**
		 * save scenario
		 */
		if (Config.DEBUG) {
			Log.d(TAG, "initScenario ");
			Log.d(TAG, "initState :" + initialState.toString());

		}
		SgUtils.saveScenario(mScenarioGenerator, mScenarioData);
		take_src_screen_shot(initialState.getId());
		/**
		 * the id of the current tree is 0
		 */
		return mScenarioData;
	}

	private final static String TAG = SgdFourmyStrategy.class.getSimpleName();

	protected void handleOtherCase(Activity currentActivity,
			Activity mainActivity, ArrayList<View> currentViews, Transition tr,
			Tree current_Tree) throws InterruptedException {
		if (Config.DEBUG) {
			Log.e(TAG, "handleOtherCase ");

		}
		/**
		 * belong to over package or Dialog view
		 */
		int n = 5;
		boolean unsupported_status = false;
		do {
			Thread.sleep(MAX_SLEEPER);
			if (n > 5) {
				unsupported_status = true;
				break;
			}
			n++;
		} while (unsuported_view());
		if (unsupported_status) {
			if (Config.DEBUG) {
				Log.e(TAG, "unsuported_view() ");

			}
			try {
				handleUnsupportedView(currentActivity, tr);
			} catch (NullPointerException nu) {
				addTransitionDestination(tr, currentActivity, true);
			}
		} else {
			/**
			 * stay in the same Activity
			 */
			if (Config.DEBUG) {
				Log.e(TAG, "handleUnchangedActivity");

			}
			handleUnchangedActivity(currentActivity, tr, current_Tree);

		}

	}

	protected void handleInErrorViewCase(boolean viewError, Activity activity,
			ScenarioData scenarioData, Tree current_Tree)
			throws InterruptedException {
		viewError = false;
		if (Config.DEBUG) {
			Log.e(TAG, "handleInErrorViewCase ");
			Log.e(TAG, "error ");
		}

		/**
		 * add to red list widget name
		 */

		/*
		 * try { // crawlUiInActivity(activity,
		 * FileUtils.readLines(RED_WIDGET_LIST, // Config.UTF8), //
		 * current_Tree.getId()); } catch (IOException e) { e.printStackTrace();
		 * }
		 */
	}

	private void handleUnchangedActivity(Activity currentActivity,
			Transition tr, Tree current_Tree) throws InterruptedException {
		if (Config.DEBUG) {
			Log.d(TAG, "handleUnchangedActivity ");
		}
		crawl_and_goBack(currentActivity, currentActivity, tr, current_Tree);
	}

	private void handleUnsupportedView(Activity currentActivity, Transition tr)
			throws InterruptedException {

		if (Config.DEBUG) {
			Log.d(TAG, "handleUnsupportedView ()");
		}

		/**
		 * Add End transition
		 */
		if (unsuported_view()) {
			if (Config.DEBUG) {
				Log.d(TAG, "unsupportedView ()");
			}
			addTransitionDestination(tr, null, true);
		} else {
			if (Config.DEBUG) {
				Log.d(TAG, "not unsupportedView ()");
			}
			addTransitionDestination(tr, mSolo.getCurrentActivity(), true);
		}
		if (Config.DEBUG) {
			Log.e(TAG, "restartActivity ");
		}
		boolean init = restartActivity();
		if (last_stateState != null && init) {
			// restartActivity();
			// goToLastState(initialState, last_stateState);
			_blind_goToLastState(last_stateState);
		}
		// if (!init){
		// goBack(tr.getSource().getName());
		// }
	}

	protected void handleNewActivity(final Activity currentActivity,
			final Activity mainActivity, Transition tr, Tree current_Tree)
			throws InterruptedException {
		crawl_and_goBack(currentActivity, mainActivity, tr, current_Tree);
		mthreadnumber++;
	}

	private void crawl_and_goBack(Activity currentActivity,
			Activity mainActivity, Transition tr, Tree current_Tree)
			throws InterruptedException {
		if (!is_a_sink_state(currentActivity)) {
			addTransitionDestination(tr, currentActivity, false);
			// s'arr�ter et indiquer new state enregistrer un fichier
			// crawlUiInActivity(currentActivity, null, current_Tree.getId());
		} else {
			addTransitionDestination(tr, currentActivity, true);
		}
		goBack(mainActivity.getClass().getSimpleName(),
				BACKTRACK_DFAULT_SLEEPER);
	}

	private boolean is_a_sink_state(Activity currentActivity) {
		/**
		 * A voir si la vue actuelle est dans le Path de tree
		 */
		for (State st : mScenarioData.getStates()) {
			if (SgUtils
					.isEqualState(get_views(), mContext, currentActivity, st)) {
				/**
				 * verifier si la transition existe
				 */
				/*
				 * if (SgUtils.isTransitionExist(st.getId(), mScenarioData)) {
				 * return true; }
				 */
				return true;

			}

		}
		SgUtils.setActionInScenarioData(mScenarioData, mSolo);
		return false;
	}

	@Override
	protected ArrayList<String> getEditTextSet(
			ArrayList<EditText> currentViews, boolean crawlBackStatus) {

		if (currentViews == null || currentViews.isEmpty()) {
			return new ArrayList<String>();
		}

		/**
		 * take only visible view.
		 */
		Iterator<EditText> ediIt = currentViews.iterator();
		EditText current;
		do {
			current = ediIt.next();
			if (current.getVisibility() != View.VISIBLE) {
				ediIt.remove();
			}
		} while (ediIt.hasNext());

		ArrayList<String> name = new ArrayList<String>();
		for (int i = 0; i < currentViews.size(); i++) {
			name.add(mContext.getResources().getResourceName(
					(currentViews.get(i)).getId()));
		}
		String rvPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ Config.TESTRESULTS
				+ File.separator
				+ Config.TESTDATA_XML;
		ArrayList<String> generatedPairWise = SgUtils.generatePairWiseSequence(
				rvPath, currentViews, mPairwise_sequence, name, mScenarioData,
				mContext);
		/**
		 * ˆ comparer si ce n'est pas
		 */

		if (!crawlBackStatus) {
			generatedPairWise = checkGeneratedSequence(generatedPairWise);
		}
		return generatedPairWise;
	}

	private ArrayList<String> checkGeneratedSequence(
			ArrayList<String> generatedPairWise) {
		if (Config.DEBUG) {
			Log.i(TAG, "checkGeneratedSequence ");
		}
		if (generatedPairWise.isEmpty()) {
			return generatedPairWise;
		}
		Iterator<String> generatedPairWiseIterator = generatedPairWise
				.iterator();
		try {
			File file = new File(SCPATH + Config.RVDONE);
			if (!file.exists()) {
				file.createNewFile();
			}
			List<String> rv_done = FileUtils.readLines(new File(SCPATH
					+ Config.RVDONE), Config.UTF8);
			do {
				String toCheck = generatedPairWiseIterator.next();
				if (rv_done.contains(toCheck)) {
					generatedPairWiseIterator.remove();
				}
			} while (generatedPairWiseIterator.hasNext());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return generatedPairWise;

	}

	protected boolean goBack(String simpleName, long wait)
			throws InterruptedException {
		try {
			do {
				Activity current = mSolo.getCurrentActivity();
				if (Config.DEBUG) {
					Log.i(TAG, "goBack ");
					Log.i(TAG, "current " + current.getClass().getSimpleName());
				}
				if (current.getClass().getSimpleName()
						.equalsIgnoreCase(simpleName)) {
					return true;
				}
				mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
				Thread.sleep(wait);
				if (Config.DEBUG) {
					Log.i(TAG, "after back "
							+ mSolo.getCurrentActivity().getClass()
									.getSimpleName());
				}

			} while (!mSolo.getCurrentActivity().getClass().getSimpleName()
					.equalsIgnoreCase(simpleName));
		} catch (SecurityException ignored) {
			return false;
		}
		return true;
	}

	public static ScenarioData getScenarioData() {
		return mScenarioData;
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			mSolo.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		super.tearDown();
	}

	@Override
	public TestResult run() {
		try {
			try {
				return super.run();
			} catch (Throwable e) {
				e.printStackTrace();
				assertTrue(true);
			}
		} catch (Exception e) {
			SgUtils.addError(mScenarioData, e.getMessage());
			mScenarioGenerator.generateXml(mScenarioData);
			if (Config.DEBUG) {
				Log.e(TAG, "runBare ");
				e.printStackTrace();
			}
			assertTrue(true);
		}
		return null;
	}

	/* / */
	@Override
	public void runBare() {
		try {
			try {
				super.runBare();
			} catch (Throwable e) {
				e.printStackTrace();
				assertTrue(true);
			}
		} catch (Exception e) {
			SgUtils.addError(mScenarioData, e.getMessage());
			mScenarioGenerator.generateXml(mScenarioData);
			if (Config.DEBUG) {
				Log.e(TAG, "runBare ");
				e.printStackTrace();
			}
			assertTrue(true);
		}
	}

	@Override
	public void runTestOnUiThread(Runnable r) {
		try {
			try {
				super.runTestOnUiThread(r);
			} catch (Throwable e) {
				assertTrue(true);
			}
		} catch (Exception e) {
			SgUtils.addError(mScenarioData, e.getMessage());
			mScenarioGenerator.generateXml(mScenarioData);
			Log.e("runTestOnUiThread", e.getMessage());
			assertTrue(true);

		}
	}

	/**
	 * default = 5
	 */

	public void test1() throws Throwable {

		/**
		 * call the activity for the first time
		 */

		if (!waitForActivity(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME)) {
			/**
			 * enregistrer l'activit� actuelle
			 */
			if (Config.DEBUG) {
				Log.d(TAG, "test1 ");
				Log.d(TAG, "current activity ");
				Log.d(TAG, mSolo.getCurrentActivity().getClass()
						.getSimpleName());

			}
			assertTrue("the initial target activity : "
					+ INITIAL_STATE_ACTIVITY_FULL_CLASSNAME
					+ "  is not reached", false);
		}
		waitProgressBar();

		mSolo.waitForActivity(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME);
		Activity activity = mSolo.getCurrentActivity();
		mSolo.assertCurrentActivity("Current activity not valid in Solo ",
				activity.getClass().getSimpleName());
		if (Config.DEBUG) {
			Log.d(TAG, "Activity before Crawl : "
					+ activity.getClass().getName());
		}

		if (mSolo.getViews().size() != 0) {
			try {
				generateScenario(activity, new File(SCENARIO_PATH));
			} catch (Error e) {
				e.printStackTrace();
				if (Config.DEBUG) {
					Log.e(TAG, e.getMessage());
				}
				assertTrue(true);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(true);
			}
		}
		if (mScenarioData != null) {
			SgUtils.saveScenario(mScenarioGenerator, mScenarioData);
		}
		assertTrue(true);
		setActivity(null);
		tearDown();
	}

	protected void fillEditTextWithValues(String seq, Transition tr)
			throws InterruptedException {
		ArrayList<String> value = sequenceToList(seq);
		ArrayList<EditText> edits = mSolo.getCurrentViews(EditText.class);
		for (EditText edit : edits) {
			String value_to_insert = value.get(edits.indexOf(edit));
			try {
				addTransitionAction(edit, value_to_insert, tr);
			} catch (android.content.res.Resources.NotFoundException ns) {
				/**
				 * ne trouve pas la ressource
				 */
				assertTrue(true);
			}
			/**
			 * delete current text
			 */

			mSolo.clearEditText(edit);
			Thread.sleep(10);
			mSolo.enterText(edit, value_to_insert);
		}
	}

	@Override
	protected void runTest() {
		try {
			try {
				super.runTest();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			SgUtils.saveScenario(mScenarioGenerator, mScenarioData);
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param mainActivity
	 *            : the handled activity
	 * @param viewToHandle
	 *            : widgets to be handle
	 * @param pairWiseSequence
	 *            : test data for EditText
	 * @param current_Tree
	 * @return
	 * @throws InterruptedException
	 * @throws CloneNotSupportedException
	 */
	protected Tree performActionOnWidgets(final Activity mainActivity,
			ArrayList<View> viewToHandle, String pairWiseSequence,
			Tree current_Tree, State current_state_)
			throws InterruptedException {
		/**
		 * le point de depart
		 */
		SgUtils.setActionInScenarioData(mScenarioData, mSolo);
		State source = init_source_of_current_crawl(current_state_);
		boolean solveresult = true;
		for (int i = 0; i < viewToHandle.size(); i++) {
			View currentViewTocross = viewToHandle.get(i);
			if (solveresult) {
				if (unsuported_view()
						|| not_supported_activity(source.getName())) {
					if (Config.DEBUG) {
						Log.e(TAG, "Unsupported View ");
						Log.e(TAG, "restart ??");
					}
					restartActivity();
					// break;
				}

				if (Config.DEBUG) {
					Log.e(TAG, "performActionOnWidgets ");
					Log.e(TAG, "currentViewTocross " + currentViewTocross);
				}
				if (SgUtils.is_notClickableView(currentViewTocross)) {
					if (Config.DEBUG) {
						Log.e(TAG, "View is not Clickable "
								+ currentViewTocross);
					}
					continue;
				}
				checkCurrentSource(source);
				if (source == null) {
					if (Config.DEBUG) {
						Log.e(TAG, "performActionOnWidgets ");
						Log.e(TAG, "Source is not reached ");
					}
					break;
				}

			}

			Transition tr = get_a_transition_with_source(source);
			/**
			 * check if no crash operation [src and editTextValue not_� Crash
			 * values for curretViewToCross] and non already executed operation
			 */
			if (!solve(pairWiseSequence, currentViewTocross, tr)) {
				solveresult = false;
				continue;
			}
			solveresult = true;
			fill_edit_text_if_exist(pairWiseSequence, tr);
			boolean viewError = false;
			viewError = handleViewPerType(mainActivity, currentViewTocross, tr,
					current_Tree);
			// Thread.sleep(MAX_SLEEPER);
			Activity currentActivity = mSolo.getCurrentActivity();
			if (!viewError) {
				ArrayList<View> currentViews = get_views();
				if (newActivity_and_belong_to_application(currentActivity,
						mainActivity)) {
					handleNewActivity(currentActivity, mainActivity, tr,
							current_Tree);
				} else {
					handleOtherCase(currentActivity, mainActivity,
							currentViews, tr, current_Tree);
				}
			} else {
				{
					/**
					 * the tested Activity changed during action performing
					 */
					handleInErrorViewCase(viewError, currentActivity,
							mScenarioData, current_Tree);
				}
			}
			current_Tree.setTransitions(tr, true);
		}
		return current_Tree;
	}

	private boolean not_supported_activity(String name) {
		Log.d("not_supported_activity", name);
		Log.d("getCurrentActivityFromActivityManager()",
				getCurrentActivityFromActivityManager());
		return !(getCurrentActivityFromActivityManager().equalsIgnoreCase(name) || getCurrentActivityFromActivityManager()
				.contains(name));
	}

	/**
	 * @throws InterruptedException
	 * 
	 */
	private void checkCurrentSource(State source) throws InterruptedException {
		/**
		 * verifier � chaque fois si on est � l'etat de d�part
		 */
		if (!isCurrentStateEqualTo(source)) {
			if (Config.DEBUG) {
				Log.i(TAG, "checkCurrentSource ");
				Log.e(TAG, "Source has been changed");
			}
			source = handle_changed_source(source);
		}
	}

	/**
	 * Constraints Solving
	 * 
	 * @param pairWiseSequence
	 * @param currentViewTocross
	 * @param tr
	 * @return
	 */
	protected boolean solve(String pairWiseSequence, View currentViewTocross,
			Transition tr) {

		if (!CRASH_SCENARIO_FILE.exists()) {
			if (Config.DEBUG) {
				Log.d(TAG, "solve ");
				Log.d(TAG, "No Crash file ");
			}
			return true;
		}

		if (Config.DEBUG) {
			Log.d(TAG, "solve ");
			Log.d(TAG, "has Crash file ");
		}
		// read crash_scenario file
		if (mCrashScenarioData == null) {
			mCrashScenarioData = ScenarioParser.parse(CRASH_SCENARIO_FILE);
		}

		/**
		 * pour chaque transition, lire la valeur des edits texts et
		 * l'evenement.
		 */
		Transition tr_clone = null;
		try {
			tr_clone = tr.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		tr_clone = SgUtils.build_transition_with_sequence(pairWiseSequence,
				currentViewTocross, tr_clone, mSolo, mContext);
		if (Config.DEBUG) {
			Log.d(TAG, "transition to check" + tr_clone.toString());
		}
		boolean crash_status = SgUtils
				.will_be_a_crash_transition_or_repeated_transition(
						mCrashScenarioData, tr_clone);
		// boolean
		// covered_constraint=SgUtils.isCoveredTransition(mCrashScenarioData,
		// tr_clone);

		if (Config.DEBUG) {
			Log.d(TAG, "solve  result");
			Log.d(TAG, "transition" + tr_clone.toString());
			Log.d(TAG, "status : " + (!crash_status));
		}
		return (crash_status ? false : true);
	}

	/**
	 * @param source
	 * @return
	 * @throws InterruptedException
	 */
	private State handle_changed_source(State source)
			throws InterruptedException {
		if (Config.DEBUG) {
			Log.e(TAG, "Current state invalid " + source);
		}
		/*
		 * recharger
		 */
		boolean init = restartActivity();
		if (!init) {
			source = get_current_Source();
		} else {
			if (last_stateState != null) {
				// goToLastState(initialState, last_stateState);
				_blind_goToLastState(last_stateState);
				try {
					source = last_stateState.clone();
				} catch (CloneNotSupportedException e) {
					source = null;
					e.printStackTrace();

				}
			}
		}
		return source;
	}

	/**
	 * @param source
	 * @return
	 */
	private Transition get_a_transition_with_source(State source) {
		Transition tr = new Transition(
				SgUtils.get_a_transition_id(mScenarioData));
		tr = addInTransitionSource(tr, source);
		return tr;
	}

	/**
	 * @param pairWiseSequence
	 * @param tr
	 * @throws InterruptedException
	 */
	private void fill_edit_text_if_exist(String pairWiseSequence, Transition tr)
			throws InterruptedException {
		if (pairWiseSequence != null) {
			fillEditTextWithValues(pairWiseSequence, tr);
		}
	}

	/**
	 * @param current_state_
	 * @return
	 */
	private State init_source_of_current_crawl(State current_state_) {
		State source;
		try {
			if (last_stateState != null) {
				source = last_stateState.clone();
			} else {
				if (Config.DEBUG) {
					Log.e(TAG, "performActionOnWidgets ");
					Log.e(TAG, "Last State is null ");
				}
				source = current_state_.clone();
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			source = get_current_Source();
		}
		source.setType(Scenario.SOURCE);
		source.setTime(getTime());
		/**
		 * always initially false
		 */
		// source.setUserEnvironment(false);
		return source;
	}

	private boolean unsuported_view() {
		int viewNumber = mSolo.getCurrentViews().size();
		if (Config.DEBUG) {
			Log.e(TAG, " unsuported_view() ");
			Log.e(TAG, "activity " + mSolo.getCurrentActivity() == null ? ""
					: mSolo.getCurrentActivity().getClass().getSimpleName());
			Log.e(TAG, " view Size " + viewNumber);
		}

		return (viewNumber == 0);
	}

	private boolean isCurrentStateEqualTo(State source) {
		State current_source = get_current_Source();
		return SgUtils.isEqualState(source, current_source);
	}

}
