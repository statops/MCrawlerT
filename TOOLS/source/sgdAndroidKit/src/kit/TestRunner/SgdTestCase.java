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
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Tree;
import kit.Scenario.Widget;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.robotium.solo.Solo;

public abstract class SgdTestCase extends AbstractCrawler {

	@SuppressWarnings("rawtypes")
	public SgdTestCase(String pkg, Class activityClass) {
		super(pkg, activityClass);
	}

	@Override
	protected void setUp() throws Exception {
		/**
		 * get sgdInstrumentation
		 */
		/**
		 * create output file
		 */
		if (!new File(SCPATH).exists()) {
			new File(SCPATH).mkdirs();

		}
		mInstrumentation = getInstrumentation();
		mContext = mInstrumentation.getTargetContext();
		mSolo = new Solo(mInstrumentation);
		mScenarioData = new ScenarioData();
		mScenarioGenerator = new ScenarioGenerator(SCPATH + Config.OutXMLPath);
		mLimit = 0;
		mTime = new Time();
		super.setUp();
	}

	/**
	 * Cross View
	 */
	protected ScenarioData generateScenario(Activity activity, File path) throws InterruptedException {
		initScenario(activity, path);
		// goToLastState();
		crawlUiInActivity(activity, new ArrayList<Widget>(), SgUtils.get_a_tree_Id(mScenarioData, ""));
		SgUtils.endScenarioData(mScenarioData);

		return mScenarioData;
	}

	/**
	 * Initial state and all actions.
	 * 
	 * @param activity
	 * 
	 * @param currentActivity
	 */
	private ScenarioData initScenario(Activity activity, File path) {
		mScenarioData = readPath(path);
		if (mScenarioData != null) {
			return mScenarioData;
		} else {
			mScenarioData = new ScenarioData();
		}

		/**
		 * version à definir ****
		 */
		String version = mScenarioData.getVersion();
		if (version == null || version.equalsIgnoreCase("")) {
			mScenarioData.setVersion("" + 0);
		} else {
			mScenarioData.setVersion("" + (Integer.parseInt(version) + 1));
		}

		/***
		 * add mainActivityStates
		 */
		if (mScenarioData.getInitialState() == null) {
			State initilaState = new State((activity.getClass().getName()), true, false, "0");
			initilaState.setType(Scenario.SOURCE);
			initilaState.setTime(getTime());
			initilaState = SgUtils.addWidgetsInState(initilaState, mSolo.getCurrentViews(), mContext);
			mScenarioData.setStates(initilaState, true, false);
		}
		SgUtils.setActionInScenarioData(mScenarioData, mSolo);
		/***
		 * add an End state
		 */
		mScenarioData = SgUtils.setDefaultEndState(mScenarioData);
		/**
		 * save scenario
		 */
		SgUtils.saveScenario(mScenarioGenerator, mScenarioData);
		return mScenarioData;
	}

	private final static String TAG = SgdTestCase.class.getSimpleName();

	/**
	 * Test if component belong to the application under test
	 * 
	 * @param name
	 *            name of the component
	 * @return
	 */
	public void handleOtherCase(Activity currentActivity, Activity mainActivity,
			ArrayList<View> currentViews, Transition tr, Tree current_Tree) throws InterruptedException {
		if (Config.DEBUG) {
			Log.d(TAG, "handleOtherCase ");
		}
		/**
		 * belong to over package or Dialog view
		 */
		if (currentViews.size() == 0) {
			try {
				handleUnsupportedView(currentActivity, tr);
				crawl_until_(mainActivity);
			} catch (NullPointerException nu) {
				addTransitionDestination(tr, currentActivity, true);
			}
			/**
			 * stop crawl
			 */
			// assertTrue(false);
		} else {
			/**
			 * stay in the same Activity
			 */
			handleUnchangedActivity(currentActivity, tr, current_Tree);

		}

	}

	/**
	 * @param mainActivity
	 */
	private void crawl_until_(Activity mainActivity) {
		if (Config.DEBUG) {
			Log.d(TAG, "crawl_until_ ");
		}

	}

	@SuppressWarnings("unused")
	private boolean isNotTheSameActivity(Activity currentActivity, Activity mainActivity) {
		return !currentActivity.getClass().getName().equalsIgnoreCase(mainActivity.getClass().getName());
	}

	public void handleInErrorViewCase(boolean viewError, Activity activity, ScenarioData scenarioData,
			Tree current_Tree) throws InterruptedException {
		viewError = false;
		// crawlUiInActivity(activity, new ArrayList<Widget>(),
		// current_Tree.getId());
	}

	private void handleUnchangedActivity(Activity currentActivity, Transition tr, Tree current_Tree)
			throws InterruptedException {
		if (Config.DEBUG) {
			Log.d(TAG, "handleUnchangedActivity ");
		}
		crawl_and_goBack(currentActivity, currentActivity, tr, current_Tree);
	}

	private void handleUnsupportedView(Activity currentActivity, Transition tr) throws InterruptedException {
		/**
		 * Add End transition
		 */
		if (Config.DEBUG) {
			Log.d(TAG, "handleUnsupportedView ");
		}
		addTransitionDestination(tr, null, true);
		restartActivity();
	}

	public void handleNewActivity(final Activity currentActivity, final Activity mainActivity, Transition tr,
			Tree current_Tree) throws InterruptedException {
		if (Config.DEBUG) {
			Log.d(TAG, "handleNewActivity ");
		}
		/**
		 * Add transition
		 */
		crawl_and_goBack(currentActivity, mainActivity, tr, current_Tree);
		/**
		 * Handle the current activity (recursive) with thread
		 */

		/*
		 * CrawlChild crossChild = new CrawlChild(currentActivity.getClass()
		 * .getName(), currentActivity, mainActivity);
		 * Log.i("thread number start", "" + mthreadnumber); crossChild.start();
		 * crossChild.join(); do { Thread.sleep(100); } while
		 * (crossChild.isAlive());
		 */
		Log.i("thread number finished", "" + mthreadnumber);
		mthreadnumber++;

	}

	private void crawl_and_goBack(Activity currentActivity, Activity mainActivity, Transition tr,
			Tree current_Tree) throws InterruptedException {
		if (!is_a_sink_state(currentActivity)) {
			if (Config.DEBUG) {
				Log.d(TAG, currentActivity.getLocalClassName() + "is a New_state ");
			}
			addTransitionDestination(tr, currentActivity, false);
			crawlUiInActivity(currentActivity, new ArrayList<Widget>(), current_Tree.getId());
			Log.d(TAG, "Finsh to Crawl " + currentActivity.getLocalClassName());
		} else {
			addTransitionDestination(tr, currentActivity, true);
		}
		goBack(mainActivity.getClass().getSimpleName());
	}

	private boolean is_a_sink_state(Activity currentActivity) {
		/**
		 * A voir si la vue actuelle est dans le Path de tree
		 */
		for (State st : mScenarioData.getStates()) {
			if (SgUtils.isEqualState(mSolo.getCurrentViews(), mContext, currentActivity, st)) {
				/**
				 * verifier si la transition existe
				 */
				if (SgUtils.isTransitionExist(st.getId(), mScenarioData)) {
					return true;
				}

			}

		}

		return false;
	}

	private static int mthreadnumber = 0;

	public ArrayList<String> getEditTextSet(ArrayList<EditText> currentViews, boolean crawlBackStatus) {
		ArrayList<String> name = new ArrayList<String>();
		for (int i = 0; i < currentViews.size(); i++) {
			name.add(mContext.getResources().getResourceName((currentViews.get(i)).getId()));
		}
		String rvPath = Environment.getExternalStorageDirectory().getAbsolutePath() + Config.TESTRESULTS
				+ File.separator + Config.TESTDATA_XML;
		ArrayList<String> generatedPairWise = SgUtils.generatePairWiseSequence(rvPath, currentViews,
				mPairwise_sequence, name, mScenarioData, mContext);
		/**
		 * à comparer si ce n'est pas déja généré
		 */

		if (!crawlBackStatus) {
			generatedPairWise = checkGeneratedSequence(generatedPairWise);
		}
		return generatedPairWise;
	}

	private ArrayList<String> checkGeneratedSequence(ArrayList<String> generatedPairWise) {
		if (generatedPairWise.isEmpty())
			return generatedPairWise;
		Iterator<String> generatedPairWiseIterator = generatedPairWise.iterator();
		try {
			File file = new File(SCPATH + Config.RVDONE);
			if (!file.exists())
				file.createNewFile();
			List<String> rv_done = FileUtils.readLines(new File(SCPATH + Config.RVDONE), Config.UTF8);
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

	private boolean goBack(String simpleName) throws InterruptedException {
		try {
			do {
				Activity current = mSolo.getCurrentActivity();
				if (Config.DEBUG) {
					Log.i(TAG, "goBack to " + simpleName);
					Log.i(TAG, "current simpleName " + current.getClass().getSimpleName());
					Log.i(TAG, "current name " + current.getClass().getName());
				}
				if (current.getClass().getSimpleName().equalsIgnoreCase(simpleName)
						|| current.getClass().getName().equalsIgnoreCase(simpleName)
						|| current.getClass().getName()
								.equalsIgnoreCase(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME)) {
					return true;
				}
				mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
				Thread.sleep(500);
				if (Config.DEBUG) {
					Log.i(TAG, "after back " + mSolo.getCurrentActivity().getClass().getSimpleName());
				}

			} while (!mSolo.getCurrentActivity().getClass().getSimpleName().equalsIgnoreCase(simpleName));
		} catch (SecurityException ignored) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param listWidget
	 * @param allCurrentView
	 */
	public void removeEditTextAndRedWidgetFromViewList(ArrayList<Widget> listWidget,
			ArrayList<View> allCurrentView) {

		/**
		 * remove editText in list of View && remove all nonhandable View in
		 * AllView
		 */
		Iterator<View> currentViewIterator = allCurrentView.iterator();
		while (currentViewIterator.hasNext()) {
			View view = currentViewIterator.next();
			if (!SgUtils.isHandableView(view) || SgUtils.isEditText(view)) {
				currentViewIterator.remove();
			}
		}

	}

	public static ScenarioData getScenarioData() {
		return mScenarioData;
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			setActivity(null);
			mSolo.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		// getActivity().finish();
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

	@SuppressWarnings("unused")
	private boolean isInApplicationPackage(String name) {
		boolean b = name.contains(PACKAGE_ID);
		return b;
	}

	public void test1() throws Exception {
		if (!waitTargetActivity(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME)) {
			assertTrue(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME + "  is not reached", true);
		}

		Activity activity = mSolo.getCurrentActivity();
		if (Config.DEBUG) {
			Log.d(TAG, "init activity become " + activity.getLocalClassName());
		}
		if (mSolo.getViews().size() != 0) {
			try {
				generateScenario(activity, new File(SCENARIO_PATH));
			} catch (Error e) {
				assertTrue(true);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(true);
			}
		}
		SgUtils.saveScenario(mScenarioGenerator, mScenarioData);
		tearDown();
		assertTrue(true);
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

	class CrawlChild extends Thread {
		Activity currentActivity;
		Activity mainActivity;

		public CrawlChild(String name, Activity current, Activity main) {
			super(name);
			setParameters(current, main);
		}

		public void setParameters(Activity current, Activity main) {
			currentActivity = current;
			mainActivity = main;
		}

		public void run() {
			try {
				crawlUiInActivity(currentActivity, new ArrayList<Widget>(), "");
				goBack(mainActivity.getClass().getSimpleName());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		};

	}

	// private boolean mCurrentEnvironmentState = false;

	protected Tree performActionOnWidgets(final Activity mainActivity, ArrayList<View> viewToHandle,
			String pairWiseSequence, Tree current_Tree, State init) throws InterruptedException {

		State source = null;
		try {
			source = init.clone();
		} catch (CloneNotSupportedException e1) {
			e1.printStackTrace();
		}
		for (int i = 0; i < viewToHandle.size(); i++) {
			View currentViewTocross = viewToHandle.get(i);
			if (Config.DEBUG) {
				Log.d(TAG, "is Clickable ? " + currentViewTocross.isClickable());
			}
			if (SgUtils.is_notClickableView(currentViewTocross)) {
				if (Config.DEBUG) {
					Log.d(TAG, "is not explored " + currentViewTocross.toString());
				}
				continue;
			}
			if (Config.DEBUG) {
				Log.d(TAG, "to be explored " + currentViewTocross.toString());
			}
			Transition tr = new Transition(SgUtils.get_a_transition_id(mScenarioData));
			tr = addInTransitionSource(tr, source);
			if (pairWiseSequence != null) {
				fillEditTextWithValues(pairWiseSequence, tr);
			}
			boolean viewError = false;
			viewError = handleViewPerType(mainActivity, currentViewTocross, tr, current_Tree);
			Thread.sleep(1000);
			Activity currentActivity = mSolo.getCurrentActivity();
			if (!viewError) {
				ArrayList<View> currentViews = mSolo.getCurrentViews();
				if (newActivity_and_belong_to_application(currentActivity, mainActivity)) {
					handleNewActivity(currentActivity, mainActivity, tr, current_Tree);
				} else {
					handleOtherCase(currentActivity, mainActivity, currentViews, tr, current_Tree);
				}
			} else {
				{
					handleInErrorViewCase(viewError, currentActivity, mScenarioData, current_Tree);
				}
			}
			current_Tree.setTransitions(tr, true);
		}
		return current_Tree;
	}

}
