/**
 * 
 */
package kit.TestRunner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import kit.Config.Config;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Tree;
import kit.Scenario.Widget;
import kit.TestRunner.ListViewElement;
import kit.TestRunner.MenuView;
import kit.TestRunner.SgdView;
import kit.TestRunner.SgdViewFactory;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Environment;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;



/**
 * @author Stassia
 * 
 */

@SuppressWarnings("rawtypes")
public abstract class AbstractCrawler extends ActivityInstrumentationTestCase2 {
	public static String INITIAL_STATE_ACTIVITY_FULL_CLASSNAME;

	@SuppressWarnings({ "unchecked", "deprecation" })
	public AbstractCrawler(String pkg, Class activityClass) {
		super(pkg, activityClass);
	}

	protected static int mthreadnumber = 0;
	protected int mLimit = 0;
	protected CharSequence PACKAGE_ID;

	private static final String TAG = AbstractCrawler.class.getName();
	protected static final String SCPATH = Environment
			.getExternalStorageDirectory() + Config.TESTRESULTS;
	/***
	 * Do a copy of all output i
	 */
	protected static final String SCENARIO_PATH = Environment
			.getExternalStorageDirectory()
			+ Config.TESTRESULTS
			+ Config.OutXMLPath;
	protected static final String DB_PATH = Environment
			.getExternalStorageDirectory()
			+ Config.TESTRESULTS
			+ File.separator + Config.DB;
	protected Solo mSolo;
	protected Context mContext;
	protected Instrumentation mInstrumentation;
	protected static ScenarioData mScenarioData;
	protected static ScenarioData mCrashScenarioData;
	protected ScenarioGenerator mScenarioGenerator;
	protected ScenarioData mPath;
	protected File JUnitOutput;
	protected static final int LIMIT = 100;
	public static final int MENU_ITEM_SIZE = 6;
	private static final long MAX_TIME_OUT = 5000;
	private static final long MIN_TIMEOUT = 100;
	private static final int MAX_LIST = 10;
	protected static Class<?> Main;
	protected Time mTime;

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
		mPath = new ScenarioData();
		mScenarioGenerator = new ScenarioGenerator(SCPATH + Config.OutXMLPath);
		mLimit = 0;
		mTime = new Time();
		// readCpList();
		getActivity();
		/**
		 * reset Robotium_Screen_shot if there 's no crash file
		 */
		resetRobotiumDirectory();
		/**
		 * save Log
		 * 
		 */
		// LogRecord log=new Log
		super.setUp();
	}

	private void resetRobotiumDirectory() throws IOException {
		if (new File(SCPATH + File.separator + Config.CRASH_FILE).exists()) {
			return;
		}
		File robotiumDir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + Config.ROBOTIUM_SCREENSHOTS);
		if (robotiumDir.exists() && robotiumDir.isDirectory()) {
			for (File jpg : robotiumDir.listFiles())
				FileUtils.forceDelete(jpg);
		}

	}

	protected boolean target_sourceUserEnvironment;

	// @formatter:on
	/**
	 * Register to the system Content provider
	 */
	protected void _blind_goToLastState(State last_stateState) {

		if (Config.DEBUG) {
			Log.d(TAG, "_blind_goToLastState() ");
		}
		/**
		 * get last state
		 */
		if (last_stateState == null) {
			return;
		}

		/**
		 * execute each path
		 */
		ArrayList<String> ids = SgUtils
				.get_transition_id_from_state_id_(last_stateState.getId());
		/**
		 * execute and check t_1_2_3 ==> t_1 t_1_2 t_1_2_3
		 */
		for (String to_execute : ids) {
			if (Config.DEBUG) {
				Log.d(TAG, "execute Transition:  " + to_execute);
			}

			State to_perform = mPath.getState(to_execute, Scenario.SOURCE);
			if (Config.DEBUG) {
				Log.d(TAG, "State to_perform:  " + to_perform);
			}
			assertNotNull("Unreachable state", to_perform);
			assertTrue("perform_action_on_current_state: Unreachable state ",
					perform_action_on_current_state(to_perform));

		}
		if (Config.DEBUG) {
			Log.d(TAG, "_blind_goToLastState() END ");
		}
		// get current activity abd check if last_State is reached
		String current_activityName = getCurrentActivityFromActivityManager();
		if (current_activityName.equalsIgnoreCase(last_stateState.getName())
				|| current_activityName.contains(last_stateState.getName())) {
			if (Config.DEBUG) {
				Log.d(TAG, "_LastState() reached  ***********");
			}
		} else {
			assertTrue(TAG + "_LastState() is not reached ", false);
		}
	}

	protected void goToLastState(State initial_State, State last_stateState) {
		// restartActivity();
		if (Config.DEBUG) {
			Log.d(TAG, "goToLastState ");
		}
		/**
		 * get last state
		 */
		if (last_stateState == null) {
			return;
		}
		last_stateState.setType(Scenario.SOURCE);
		State current_state = (initial_State == null) ? get__current_state(true)
				: initial_State;
		if (current_state == null) {
			return;
		}
		if (SgUtils.isEqualState(last_stateState, current_state)) {
			last_stateState.setType(Scenario.DEST);
			return;
		}
		// State expected_state=SgUtils.get_dest(current_state, mScenarioData);
		/**
		 * perform action
		 * 
		 */
		perform_action_on_current_state(current_state);
		last_stateState.setType(Scenario.DEST);
		goToLastState(null, last_stateState);
	}

	private boolean perform_action_on_current_state(State current_state) {
		assertNotNull("Path is Null", mPath);
		assertNotNull("Current_state is Null", current_state);

		if (Config.DEBUG) {
			Log.d(TAG, "perform_action_on_current_state() :  " + current_state);
		}
		/**
		 * 
		 * if there is an edit text
		 * 
		 */
		if (mSolo.getCurrentViews(EditText.class).size() > 0
				&& !SgUtils.hasEditText(current_state)) {
			assertTrue(
					" there is a litige caused by mSolo.getCurrentViews(EditText.class).size() >0 && !SgUtils.hasEditText(current_state)",
					false);

		}

		if (mSolo.getCurrentViews(EditText.class).size() == 0
				&& SgUtils.hasEditText(current_state)) {
			assertTrue(
					" there is a litige caused by mSolo.getCurrentViews(EditText.class).size() == 0 &&SgUtils.hasEditText(current_state)",
					false);

		}

		if (mSolo.getCurrentViews(EditText.class).size() > 0) {
			HashMap<String, String> sequence = SgUtils.get_ediText_sequence(
					current_state, mPath);
			fillEditTextWithValues(sequence);
		} else {
			if (Config.DEBUG) {
				Log.d(TAG,
						"perform_action_on_current_state() :  NO EDIT TEXT on current state");
			}

		}
		/**
		 * chercher l'action qui a men� � l'etat
		 */
		HashMap<String, String> event = SgUtils.get_event(current_state, mPath); // si
																					// event
																					// null
																					// ou
		// vide
		if (event == null || event.isEmpty()) {
			if (Config.DEBUG) {
				Log.d(TAG, "event null or empty () :  ");
			}
			// assertTrue ("event == null || event.isEmpty() ",false);
		}
		test_event(event);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		/**
		 * check if in real stte
		 */
		return true;
	}

	protected void test_event(HashMap<String, String> event) {
		Log.d(TAG, "test_Event ()");
		ArrayList<View> allWidgetInCurrentActivity = get_views();
		removeEditTextAndRedWidgetFromViewList(null, allWidgetInCurrentActivity);
		add_menu_to_widget_to_be_handle(allWidgetInCurrentActivity,
				getActivity());
		split_listView_and_AddIn(allWidgetInCurrentActivity);
		String name = event.keySet().iterator().next();
		Log.d("name of widget", name);
		for (View v : allWidgetInCurrentActivity) {
			SgdView view_to_handle = SgdViewFactory.createView(mContext,
					getActivity(), v, null, mSolo);

			/**
			 * case of menu
			 */
			if (SgUtils.isMenu(v)
					&& name.equals("" + (MenuView.class.cast(v)).getIndex())) {
				view_to_handle.performAction(false);
				break;
			}
			/**
			 * case list Element
			 */
			if (SgUtils.isListElementView(v)
					&& name.equals((ListViewElement.class.cast(v)).getIndex())) {
				view_to_handle.performAction(false);
				break;
			}
			String ressource_name = null;
			try {
				ressource_name = mContext.getResources().getResourceName(
						v.getId());
			} catch (Exception ex) {
				continue;
			}
			if (name.equals(ressource_name)
					|| name.equalsIgnoreCase(mContext.getResources()
							.getResourceName(v.getId()).replaceAll("id/", ""))) {
				Log.d("widget Matched", name);
				view_to_handle.performAction(false);
				break;
			}
		}
		Log.d(TAG, "test_Event Finished");
	}

	private void fillEditTextWithValues(HashMap<String, String> sequence) {

		ArrayList<EditText> edits = mSolo.getCurrentViews(EditText.class);
		for (EditText edit : edits) {
			String name = mContext.getResources().getResourceName(edit.getId());
			String value_to_insert = sequence.get(name);
			scrolltoView(edit);
			mSolo.clearEditText(edit);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mSolo.enterText(edit, value_to_insert);
		}

	}

	/**
	 * @param edit
	 */
	private void scrolltoView(EditText edit) {
		// TODO Auto-generated method stub

	}

	/**
	 * cas non etatt initial
	 * 
	 * @param b
	 * @return
	 */
	private ArrayList<String> source_id = new ArrayList<String>();

	private State get__current_state(boolean indic) {
		Activity act = mSolo.getCurrentActivity();
		State source = new State(act.getClass().getName(), "");
		/**
		 * 
		 * les info sur le les widgets
		 */
		source = SgUtils.addWidgetsInState(source, mSolo.getCurrentViews(),
				mContext);
		source.setType(Scenario.SOURCE);
		source.setUserEnvironment(getUserEnvironmentCurrentState());
		for (State st : mScenarioData.getStates()) {
			if (st.getType().equalsIgnoreCase(Scenario.SOURCE)
					&& SgUtils.isEqualState(st, source)
					&& !source_id.contains(st.getId())) {
				source_id.add(st.getId());
				return st;
			}
		}
		return null;
	}

	protected State m_New_state_toCrawl;
	protected String mCurrentTransitionID;
	protected String mCurrentTreeID;
	protected Tree mCurrentTree;

	public View get_view_at_index(final ListView element, final int indexInList) {
		ListView container = element;
		if (container != null) {
			if (indexInList <= container.getAdapter().getCount()) {
				scrollListTo(container, indexInList);
				int indexToUse = indexInList
						- container.getFirstVisiblePosition();
				return container.getChildAt(indexToUse);
			}
		}
		return null;
	}

	public <T extends AbsListView> void scrollListTo(final T listView,
			final int index) {
		mInstrumentation.runOnMainSync(new Runnable() {
			@Override
			public void run() {
				listView.setSelection(index);
			}
		});
		mInstrumentation.waitForIdleSync();
	}

	protected void split_listView_and_AddIn(
			ArrayList<View> allWidgetInCurrentActivity) {
		if (Config.DEBUG) {
			Log.d(TAG, "split_listView_and_AddIn ");
		}
		Iterator<View> it = allWidgetInCurrentActivity.iterator(); //
		if (!it.hasNext()) {
			return;
		}
		ArrayList<View> view_to_add = new ArrayList<View>();
		do {
			View v = it.next();
			if (SgUtils.isListView(v)) {
				ListView lList = (ListView.class.cast(v));
				if (lList == null || lList.getAdapter() == null) {
					continue;
				}
				for (int i = 0; i < lList.getAdapter().getCount()
						&& i < MAX_LIST; i++) {
					View elt = get_view_at_index(lList, i);
					if (elt != null) {
						ListViewElement view_elt = new ListViewElement(
								mContext, lList, elt, i);
						view_to_add.add(view_elt);
					}
					/**
					 * limiter le nombre de liste � 10
					 */

				}
				it.remove();
			}
		} while (it.hasNext());
		allWidgetInCurrentActivity.addAll(view_to_add);
	}

	/**
	 * 
	 * @param mainActivity
	 * @param tree_id
	 * @throws InterruptedException
	 */
	private void crawlUiInActivity(Activity mainActivity, String tree_id,
			ArrayList<View> allWidgetInCurrentActivity)
			throws InterruptedException {
		if (Config.DEBUG) {
			Log.d(TAG, "crawlUiInActivity ");
		}
		mLimit++;
		mCurrentTransitionID = SgUtils.get_a_transition_id(mScenarioData);
		ArrayList<String> pairwiseSequence = new ArrayList<String>();
		pairwiseSequence = getEditTextSet(
				mSolo.getCurrentViews(EditText.class), false);
		if (Config.DEBUG) {
			Log.i("PairWise_Sequence", "" + pairwiseSequence.toString());
		}
		Tree current_Tree = new Tree("", SgUtils.get_a_tree_Id(mScenarioData,
				tree_id));
		State current_state = get_a_source("0");
		if (pairwiseSequence.isEmpty()
				&& (mSolo.getCurrentViews(EditText.class) == null || mSolo
						.getCurrentViews(EditText.class).size() > 0)) {
			/**
			 * do nothing
			 */
		}

		else if (pairwiseSequence.isEmpty()) {
			current_Tree = performActionOnWidgets(mainActivity,
					allWidgetInCurrentActivity, null, current_Tree,
					current_state);
		}// Constraint c = GenerateConstrainte(allWidgetInCurrentActivity);
		else {
			for (String seq : pairwiseSequence) {
				current_Tree = performActionOnWidgets(mainActivity,
						allWidgetInCurrentActivity, seq, current_Tree,
						current_state);
			}
		}
		if (Config.DEBUG) {
			Log.i("Limit", "" + mLimit++);
		}

		/**
		 * (mLimit A d�finir Ulterieurement)
		 */
		if (mLimit > LIMIT) {
			assertTrue("Limit reached", false);
		}
		Tree current_TreeTemp = new Tree(current_Tree.getName(),
				SgUtils.get_a_tree_Id(mScenarioData, tree_id));
		current_TreeTemp.setTransitions(current_Tree.getTransitions());
		mScenarioData.setTrees(current_TreeTemp, true);
	}

	protected void crawlUiInActivity(Activity mainActivity,
			List<String> redWidgList, String tree_id)
			throws InterruptedException {
		/***
		 * attendre les progress_bars
		 */
		if (Config.DEBUG) {
			Log.e(TAG, "crawlUiInActivity  from "
					+ mainActivity.getClass().getName());
			Log.e(TAG, mainActivity.getClass().getName());

		}
		waitProgressBar();
		ArrayList<View> allWidgetInCurrentActivity = get_views(redWidgList,
				mainActivity);
		crawlUiInActivity(mainActivity, tree_id, allWidgetInCurrentActivity);
	}

	/**
	 * @param redWidgList
	 * @param mainActivity
	 * @return
	 */
	private ArrayList<View> get_views(List<String> redWidgList,
			Activity mainActivity) {
		if (Config.DEBUG) {
			Log.e(TAG, "get_view_s ");
		}
		ArrayList<View> allWidgetInCurrentActivity = new ArrayList<View>();
		HashSet<String> allWidgetName = new HashSet<String>();
		/**
		 * scroll down
		 */
		do {
			addViewsIn(allWidgetInCurrentActivity, allWidgetName);
			if (redWidgList != null)
				removeEditTextAndRedWidgetFromViewListName(redWidgList,
						allWidgetInCurrentActivity);
		} while (canScrollDown());
		scrollUp();
		add_menu_to_widget_to_be_handle(allWidgetInCurrentActivity,
				mainActivity);
		split_listView_and_AddIn(allWidgetInCurrentActivity);
		return allWidgetInCurrentActivity;
	}

	/**
	 * 
	 * @return the available view even if present under scroll bar
	 */
	protected ArrayList<View> get_views() {
		if (Config.DEBUG) {
			Log.e(TAG, "init state ");
			Log.e(TAG, "get_view_s ");
		}
		ArrayList<View> allWidgetInCurrentActivity = new ArrayList<View>();
		HashSet<String> allWidgetName = new HashSet<String>();
		/**
		 * scroll down
		 */
		do {
			addViewsIn(allWidgetInCurrentActivity, allWidgetName);
		} while (canScrollDown());
		scrollUp();
		return allWidgetInCurrentActivity;
	}

	/**
	 * 
	 */
	private void scrollUp() {
		if (mSolo.getCurrentViews(ScrollView.class).isEmpty()) {
			if (Config.DEBUG) {
				Log.e(TAG, "No scroll view");
			}
			return;
		}
		boolean scrollup = false;
		do {
			scrollup = mSolo.scrollUp();
			if (Config.DEBUG) {
				Log.e(TAG, "scrollUp ()" + scrollup);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} while (scrollup);

	}

	/**
	 * @param allWidgetInCurrentActivity
	 * @param allWidgetName
	 * @return
	 */
	private ArrayList<View> addViewsIn(
			ArrayList<View> allWidgetInCurrentActivity,
			HashSet<String> allWidgetName) {
		if (Config.DEBUG) {
			Log.e(TAG, "addViewsIn ");
		}
		ArrayList<View> curViews = RobotiumUtils.removeInvisibleViews(mSolo
				.getCurrentViews());
		for (View v : curViews) {
			// for (View v : mSolo.getCurrentViews()) {
			String name = getWidgetId(v);
			if (name != null && !allWidgetName.contains(name)) {
				allWidgetInCurrentActivity.add(v);
				allWidgetName.add(name);
				if (Config.DEBUG) {
					Log.e(TAG, "add : allWidgetName.add(name); " + name);
				}
			}
		}
		return allWidgetInCurrentActivity;
	}

	/**
	 * @return
	 */
	private boolean canScrollDown() {
		if (Config.DEBUG) {
			Log.e(TAG, "canScrollDown()");
		}
		if (mSolo.getCurrentViews(ScrollView.class).isEmpty()) {
			if (Config.DEBUG) {
				Log.e(TAG, "No scroll view");
			}
			return false;
		}
		boolean scroll = mSolo.scrollDown();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (Config.DEBUG) {
			Log.e(TAG, "can Scroll down ???" + scroll);
		}
		return scroll;
	}

	/**
	 * 
	 */
	protected void waitProgressBar() {
		if (mSolo.getCurrentViews(ProgressBar.class) == null) {
			return;
		}
		int progressNumber = mSolo.getCurrentViews(ProgressBar.class).size();
		if (Config.DEBUG) {
			Log.d(TAG, "waitProgressBar ");
			Log.d(TAG, "progress bar view number ?" + progressNumber);
		}
		if (progressNumber == 0) {
			return;
		}
		int n = 0;
		do {
			mSolo.sleep((int) MIN_TIMEOUT);
			n = (int) (n + MIN_TIMEOUT);
			if (n > (MAX_TIME_OUT * 10)) {
				if (Config.DEBUG) {
					Log.d(TAG, "waitProgressBar ");
					Log.d(TAG, "timeOut .... ");
				}
				break;
			}
		} while (progressBarVisible());

	}

	/**
	 * @return
	 */
	private boolean progressBarVisible() {
		ArrayList<ProgressBar> barView = mSolo
				.getCurrentViews(ProgressBar.class);
		if (barView.isEmpty()) {
			Log.d(TAG, "No progress_bar is on the top");
			return false;
		}

		for (ProgressBar elt : barView) {
			Log.d(TAG, "progressBarVisible ");
			if (isOnTheTop(elt)) {
				if (Config.DEBUG) {
					Log.d(TAG, "is on the top ");
					return true;
				} /*
				 * else { Log.d(TAG, "Not on the top"); }
				 */
			}
		}
		Log.d(TAG, "No progress_bar is on the top");
		return false;
	}

	/**
	 * @param elt
	 * @return
	 */
	private boolean isOnTheTop(ProgressBar elt) {
		Rect windows_elt = new Rect();
		elt.getHitRect(windows_elt);
		View parent = (View) elt.getParent();
		Rect windows = new Rect();
		parent.getHitRect(windows);
		if (Config.DEBUG) {
			Log.d("isOnTheTop ", "Parent :" + parent.toString());

		}
		// !elt.getLocalVisibleRect(windows) && windows.height() <
		// elt.getHeight()
		if (Rect.intersects(windows_elt, windows)) {
			if (Config.DEBUG) {
				Log.d(TAG, "true");
			}
			return true;
		}
		if (Config.DEBUG) {
			Log.d(TAG, "false");
		}
		return false;
	}

	/**
	 * @param allWidgetInCurrentActivity
	 */
	private void add_menu_to_widget_to_be_handle(
			ArrayList<View> allWidgetInCurrentActivity, Activity currentAct) {
		if (Config.DEBUG) {
			Log.d(TAG, "add_menu_to_widget_to_be_handle ");
		}
		for (int i = 0; i < MENU_ITEM_SIZE; i++) {
			MenuView menu_element = new MenuView(mContext, i, currentAct);
			allWidgetInCurrentActivity.add(menu_element);
		}

		/**
		 * add menu of ActionBar
		 */
	}

	private void removeEditTextAndRedWidgetFromViewListName(
			List<String> redWidgList, ArrayList<View> allCurrentView) {
		if (Config.DEBUG) {
			Log.d(TAG, "removeEditTextAndRedWidgetFromViewListName ");
		}
		/**
		 * remove editText in list of View && remove all nonhandable View in
		 * AllView
		 */
		remove_non_handable_and_edit_text(allCurrentView);
		/**
		 * remove redLisWidget
		 */
		if (redWidgList == null || redWidgList.isEmpty()) {
			return;
		}
		remove_red_widget(allCurrentView, redWidgList);

	}

	/**
	 * 
	 * @param listWidget
	 * @param allCurrentView
	 */
	public void removeEditTextAndRedWidgetFromViewList(
			ArrayList<Widget> listWidget, ArrayList<View> allCurrentView) {

		/**
		 * remove editText in list of View && remove all nonhandable View in
		 * AllView
		 */
		remove_non_handable_and_edit_text(allCurrentView);
		/**
		 * remove redLisWidget
		 */
		if (listWidget == null || listWidget.isEmpty()) {
			return;
		}
		ArrayList<String> red_Widget_name = new ArrayList<String>();
		for (Widget wi : listWidget) {
			red_Widget_name.add(wi.getName());
		}
		remove_red_widget(allCurrentView, red_Widget_name);
	}

	private String get_name_of_view(View view) {
		return mContext.getResources().getResourceName(view.getId());
	}

	private void remove_red_widget(ArrayList<View> allCurrentView,
			List<String> redWidgList) {
		Iterator<View> currentViewIterator = allCurrentView.iterator();
		while (currentViewIterator.hasNext()) {
			View view = currentViewIterator.next();
			if (redWidgList.contains(get_name_of_view(view))) {
				currentViewIterator.remove();
			}
		}

	}

	private void remove_non_handable_and_edit_text(
			ArrayList<View> allCurrentView) {
		Iterator<View> currentViewIterator = allCurrentView.iterator();
		while (currentViewIterator.hasNext()) {
			View view = currentViewIterator.next();
			try {
				mContext.getResources().getResourceName(view.getId());
			} catch (android.content.res.Resources.NotFoundException ns) {
				continue;
			}
			if (!SgUtils.isHandableView(view) || SgUtils.isEditText(view)) {
				currentViewIterator.remove();
			}
		}

	}

	/**
	 * @param currentViews
	 * @param b
	 * @return
	 */
	protected abstract ArrayList<String> getEditTextSet(
			ArrayList<EditText> currentViews, boolean b);

	protected boolean restart_activity_continue_to_run;

	/***
	 * add transition destination
	 * 
	 * @param currentActivity
	 *            - if null means END point
	 * @param isEndPoint
	 *            if true we are in END point
	 */
	protected void addTransitionDestination(Transition tr,
			Activity currentActivity, boolean isEndPoint) {

		if (Config.DEBUG) {
			Log.d(TAG, "addTransitionDestination () ");
		}
		if (tr != null) {
			take_dest_screen_shot(tr.getId());
		}
		// mSolo.takeScreenshot("d_"+tr.getId());
		if (tr == null) {
			if (Config.DEBUG) {
				Log.e(TAG, "transition is null ");
			}
			return;
		}

		if (currentActivity == null && isEndPoint) {
			if (Config.DEBUG) {
				Log.e(TAG, "current activity is null ");
			}
			State dest = getDestState(tr, Scenario.END);
			dest.setInit(false, true);
			// tr = new Transition(SgUtils.addWidgetsInState(new
			// State(mSolo.getCurrentActivity().getClass()
			// .getName(), tr.getId()), mSolo.getCurrentViews(), mContext),
			// null, dest, tr.getId());
			mScenarioData.addStates(dest, true);
			mScenarioData.setTransitions(tr);
			if (Config.DEBUG) {
				Log.i(TAG, "addTransitionDestination ");
				Log.i("TRANSITION" + tr.getId() + " :", tr.getSource()
						.getName()
						+ " "
						+ tr.getAction().size()
						+ " "
						+ tr.getDest().getName()
						+ " dest Status: "
						+ tr.getDest().isFinal());
			}
			return;
		}
		if (!isEndPoint) {
			State dest = getDestState(tr, currentActivity.getClass().getName());
			// dest = SgUtils.addWidgetsInState(dest, mSolo.getCurrentViews(),
			// mContext);
			tr.setDest(dest);
			mScenarioData.addStates(dest, true);
			mScenarioData.setTransitions(tr);
			if (Config.DEBUG) {
				Log.i(TAG, "addTransitionDestination ");
				Log.i("TRANSITION" + tr.getId() + " :", tr.getSource()
						.getName()
						+ " "
						+ tr.getAction().size()
						+ " "
						+ tr.getDest().getName()
						+ " dest Status: "
						+ tr.getDest().isFinal());
			}
			return;
		}
		if ((isEndPoint)) {
			State dest = getDestState(tr, currentActivity.getClass().getName());
			dest.setInit(false, true);
			tr.setDest(dest);
			mScenarioData.addStates(dest, true);
			mScenarioData.setTransitions(tr);
			if (Config.DEBUG) {
				Log.i(TAG, "addTransitionDestination ");
				Log.i("TRANSITION" + tr.getId() + " :", tr.getSource()
						.getName()
						+ " "
						+ tr.getAction().size()
						+ " "
						+ tr.getDest().getName()
						+ " dest Status: "
						+ tr.getDest().isFinal());
			}
			return;
		}
		if ((currentActivity == null)) {
			State dest = getDestState(tr, Scenario.END);
			tr.setDest(dest);
			mScenarioData.addStates(dest, true);
			mScenarioData.setTransitions(tr);
			if (Config.DEBUG) {
				Log.i(TAG, "addTransitionDestination ");
				Log.i("TRANSITION" + tr.getId() + " :", tr.getSource()
						.getName()
						+ " "
						+ tr.getAction().size()
						+ " "
						+ tr.getDest().getName()
						+ " dest Status: "
						+ tr.getDest().isFinal());
			}
			return;
		}
	}

	/**
	 * 
	 * @param tr
	 * @param name
	 * @return
	 */
	protected State getDestState(Transition tr, String name) {
		if (Config.DEBUG) {
			Log.e(TAG, "getDestState " + name);
		}
		State dest = new State(name, tr.getId());
		dest = SgUtils.addWidgetsInState(dest, get_views(), mContext);
		dest.setType(Scenario.DEST);
		dest.setUserEnvironment(getUserEnvironmentCurrentState());
		return dest;
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
	 * @param current_state
	 * @return
	 * @throws InterruptedException
	 * @throws CloneNotSupportedException
	 */
	protected abstract Tree performActionOnWidgets(final Activity mainActivity,
			ArrayList<View> viewToHandle, String pairWiseSequence,
			Tree current_Tree, State current_state) throws InterruptedException;

	/**
	 * @param viewError
	 * @param currentActivity
	 * @param mScenarioData2
	 * @param current_Tree
	 * @throws InterruptedException
	 */
	protected abstract void handleInErrorViewCase(boolean viewError,
			Activity currentActivity, ScenarioData mScenarioData2,
			Tree current_Tree) throws InterruptedException;

	/**
	 * @param currentActivity
	 * @param mainActivity
	 * @param currentViews
	 * @param tr
	 * @param current_Tree
	 * @throws InterruptedException
	 */
	protected abstract void handleOtherCase(Activity currentActivity,
			Activity mainActivity, ArrayList<View> currentViews, Transition tr,
			Tree current_Tree) throws InterruptedException;

	/**
	 * @param currentActivity
	 * @param mainActivity
	 * @param tr
	 * @param current_Tree
	 * @throws InterruptedException
	 */
	protected abstract void handleNewActivity(Activity currentActivity,
			Activity mainActivity, Transition tr, Tree current_Tree)
			throws InterruptedException;

	protected boolean newActivity_and_belong_to_application(
			Activity currentActivity, Activity mainActivity) {
		return (isNotTheSameActivity(currentActivity, mainActivity) && isInApplicationPackage(currentActivity
				.getClass().getName()));
	}

	private boolean isInApplicationPackage(String name) {
		boolean b = name.contains(PACKAGE_ID);
		return b;
	}

	private boolean isNotTheSameActivity(Activity currentActivity,
			Activity mainActivity) {
		return !currentActivity.getClass().getName()
				.equalsIgnoreCase(mainActivity.getClass().getName());
	}

	protected boolean handleViewPerType(Activity mainActivity,
			View currentViewTocross, Transition tr, Tree current_Tree)
			throws InterruptedException {
		if (Config.DEBUG) {
			Log.e(TAG, "handleViewPerType ");
		}
		boolean viewErrorStatus = true;
		SgdView v = SgdViewFactory.createView(mContext, mainActivity,
				currentViewTocross, tr, mSolo);
		if (v != null) {
			v.setScenarioData(mScenarioData);
			v.setScenarioGenerator(mScenarioGenerator);
			if (Config.DEBUG) {
				Log.d(TAG, "handleViewPerType ");
				Log.d(TAG, "current_Tree"
						+ ((current_Tree == null) ? " is null" : " not null"));

			}
			v.setCurrenTree(current_Tree);
			try {
				/**
				 * scroller dans le cas de ListViewElementWigdet
				 */
				if (SgUtils.isListViewElement(currentViewTocross)) {
					if (Config.DEBUG) {
						Log.e(TAG,
								"Scroll for list "
										+ ((ListViewElement) currentViewTocross)
												.getIndex());
					}
					get_view_at_index(
							(ListView) ((ListViewElement) currentViewTocross)
									.getListParent(),
							((ListViewElement) currentViewTocross).getIndex());
				}
				if (SgUtils.isMenu(v)) {
					if (Config.DEBUG) {
						Log.d(TAG, "Menu " + v.getId());
					}
				}
				v.performAction(true);
				Thread.sleep(1000);
			} catch (Error e) {
				if (Config.DEBUG) {
					Log.d(TAG, "handleViewPerType ");
					Log.d(TAG, "error on clicking" + v);
				}
			} finally {
				waitProgressBar();
			}
			viewErrorStatus = v.getStatus();
		} else {
			if (Config.DEBUG) {
				Log.d(TAG, "handleViewPerType ");
				Log.d(TAG, "Null for  " + currentViewTocross);
				Log.d(TAG, "View is Not Visible");

			}
		}
		return viewErrorStatus;
	}

	/**
	 * @param parent
	 * @param index
	 */
	private void get_view_at_index(ListView parent, String index) {
		get_view_at_index(parent, Integer.parseInt(index));
	}

	/**
	 * 
	 * @param seq
	 * @return
	 */
	protected ArrayList<String> sequenceToList(String seq) {
		return SgUtils.sequenceToList(seq);
	}

	/**
	 * 
	 * @param seq
	 * @param tr
	 * @throws InterruptedException
	 */
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
			// add inserted value
			Thread.sleep(1000);
		}
		/**
		 * sequence d�j� effectu�
		 */
		try {
			//
			addLine(new File(SCPATH + Config.RVDONE), seq, Config.UTF8);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param file
	 * @param value
	 * @param enconding
	 */
	private static void addLine(File file, String value, String enconding) {
		try {
			if (file.exists()) {
				List<String> line = FileUtils.readLines(file, enconding);
				line.add(value);
				// FileUtils.writeLines(file, enconding, line, false);
			} else {
				file.createNewFile();
				FileUtils.write(file, value, enconding);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	protected Transition addInTransitionSource(Transition tr, State source_) {
		/**
		 * take screenShot
		 */
		take_src_screen_shot(tr.getId());
		try {
			if (source_ == null) {
				source_ = get_a_source(tr.getId());
			} else {
				source_ = source_.clone();
				source_.setId(tr.getId());
				source_.setType(Scenario.SOURCE);
			}
			tr.setSource(source_.clone());
		} catch (CloneNotSupportedException e) {
			tr.setSource(source_);
		}
		source_.setTime(getTime());
		mScenarioData.addStates(source_);
		State dest = new State(Scenario.END, tr.getId());
		dest.setType(Scenario.DEST);
		tr.setDest(dest);
		return tr;
	}

	protected String getTime() {
		mTime.setToNow();
		return mTime.format("%k:%M:%S");
	}

	protected State get_a_source(String id) {
		State source = get_current_Source();
		source.setId(id);
		source.setTime(getTime());
		source.setUserEnvironment(false);
		return source;
	}

	/**
	 * 
	 * @param the
	 *            view to add in the transition
	 * @param value
	 *            inserted if editText
	 */
	protected Transition addTransitionAction(View v, String value, Transition tr) {
		return SgUtils.addTransitionAction(v, value, tr, mContext);
	}

	/**
	 * read the ue file
	 * 
	 * @return
	 */
	protected boolean getUserEnvironmentCurrentState() {
		String uri = null;
		try {
			File ue = new File(Environment.getExternalStorageDirectory()
					+ File.separator + Scenario.UE);
			uri = FileUtils.readFileToString(ue, Config.UTF8);
			if (ue.exists()) {
				if (Config.DEBUG) {
					Log.e(TAG, "getUserEnvironmentCurrentState ");
					Log.e(TAG, "===changed=== :" + uri);
				}
				ue.delete();
				return true;
			}
		} catch (IOException e) {
			if (Config.DEBUG) {
				Log.d(TAG, "getUserEnvironmentState ");
				Log.d(TAG, "uri :" + uri);
			}
		}
		return false;
	}

	protected void crawlUiInActivity(Activity mainActivity,
			ArrayList<Widget> redWidget, String tree_id)
			throws InterruptedException {
		ArrayList<View> allWidgetInCurrentActivity = mSolo.getCurrentViews();
		setViewEvent(redWidget, allWidgetInCurrentActivity, mainActivity);
		crawlUiInActivity(mainActivity, tree_id, allWidgetInCurrentActivity);
	}

	/**
	 * @param redWidget
	 * @param allWidgetInCurrentActivity
	 * @param mainActivity
	 */
	private void setViewEvent(ArrayList<Widget> redWidget,
			ArrayList<View> allWidgetInCurrentActivity, Activity mainActivity) {
		removeEditTextAndRedWidgetFromViewList(redWidget,
				allWidgetInCurrentActivity);
		split_listView_and_AddIn(allWidgetInCurrentActivity);
		add_menu_to_widget_to_be_handle(allWidgetInCurrentActivity,
				mainActivity);

	}

	protected static final long MAX_SLEEPER = 5000;
	protected static final long MIN_SLEEPER = 100;
	protected static final long BACKTRACK_DFAULT_SLEEPER = 500;
	public static final int LISTVIEW_SIZE = 10;

	protected State get_current_Source() {
		Activity act = mSolo.getCurrentActivity();
		State source = new State(act.getClass().getName(), "");
		source = SgUtils.addWidgetsInState(source, get_views(), mContext);
		source.setType(Scenario.SOURCE);
		source.setTime(getTime());
		// source.setUserEnvironment(mCurrentEnvironmentState);
		return source;
	}

	protected boolean restartActivity() throws InterruptedException {
		if (Config.DEBUG) {
			Log.e(TAG, "restartActivity ");
		}
		boolean init = true;
		Thread run = new Thread() {
			@Override
			public void run() {
				restart_activity_continue_to_run = true;
				while (restart_activity_continue_to_run) {
					mSolo.finishOpenedActivities();
					setActivity(null);
					mSolo = new Solo(mInstrumentation);
					// getActivity();
					try {
						if (!waitForActivity(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME)) {
							assertTrue("Unreached activity "
									+ INITIAL_STATE_ACTIVITY_FULL_CLASSNAME,
									false);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					restart_activity_continue_to_run = false;
				}

			}
		};
		run.start();
		int n = 0;
		do {
			if (Config.DEBUG) {
				Log.e(TAG, "restartActivity ");
				Log.e(TAG, "waiting ... ");
			}
			Thread.sleep(MAX_TIME_OUT);
			n++;
			if (n > 10) {
				restart_activity_continue_to_run = false;
				break;
			}
		} while (run.isAlive());
		Log.e(TAG, "restartActivity ");
		Log.e(TAG, "finished ... ");
		waitProgressBar();
		return init;
	}

	/**
	 * @param name
	 */
	protected void take_dest_screen_shot(String name) {
		take_screen_shot(Scenario.DEST + "_" + name);
	}

	/**
	 * @param name
	 */
	protected void take_src_screen_shot(String name) {
		take_screen_shot(Scenario.SOURCE + "_" + name);
	}

	private void take_screen_shot(String name) {
		if (Config.DEBUG) {
			Log.e(TAG, "take screen shot " + name);
		}
		mSolo.takeScreenshot(name);
	}

	protected boolean waitForActivity(String name) throws InterruptedException {
		// clearApplicationData(mContext);
		getActivity();
		if (Config.DEBUG) {
			Log.d(TAG, "Activity to reach :" + name);
		}
		// Activity currentActivity = mSolo.getCurrentActivity();
		final long endTime = SystemClock.uptimeMillis() + (MAX_TIME_OUT * 10);
		// long n = MIN_TIMEOUT;
		Thread.sleep(MIN_TIMEOUT);
		while (SystemClock.uptimeMillis() < endTime) {
			Log.d(TAG, "===============Wait for activity " + name
					+ " =====================" + SystemClock.uptimeMillis());
			Thread.sleep(MIN_TIMEOUT);
			String currentName = getCurrentActivityFromActivityManager();

			if (Config.DEBUG) {
				Log.d(TAG, "Current component name " + currentName);
			}
			if (currentName.equals(name) || currentName.contains(name)
					|| name.contains(currentName)) {
				if (Config.DEBUG) {
					Log.d(TAG, "Current component name " + currentName
							+ "  is reached");
				}

				try {
					Log.d(TAG,
							"Last activity in Solo :"
									+ mSolo.getActivityMonitor()
											.getLastActivity() != null ? mSolo
									.getActivityMonitor().getLastActivity()
									.getClass().getName() : "null");

				} catch (NullPointerException ne) {
					ne.printStackTrace();

				}

				/**
				 * check if use condition
				 */

				mSolo.waitForActivity(name);
				return true;
			} else {
				checkUseConditionView(currentName);
				currentName = getCurrentActivityFromActivityManager();
				if (Config.DEBUG) {
					Log.d(TAG, "Not in the top + " + currentName);
				}
			}
		}
		if (Config.DEBUG) {
			Log.d(TAG, "===============End ....... timed out ================="
					+ SystemClock.uptimeMillis());
		}
		return false;
	}

	/**
	 * @throws InterruptedException
	 * 
	 */
	private void checkUseConditionView(String currentActivity)
			throws InterruptedException {
		if (Config.DEBUG) {
			Log.e(TAG,
					"===========================================================");
		}
		// click sequentially current view
		if (Config.DEBUG) {
			Log.e(TAG, "******************checkUseConditionView :"
					+ currentActivity);
		}
		if (progressBarVisible()) {
			if (Config.DEBUG) {
				Log.e(TAG, "checkUseConditionView : progressBarVisible()");
			}
			return;

		}
		ArrayList<View> list = mSolo.getCurrentViews();
		for (View v : list) {
			/**
			 * only handle button type.
			 */
			if (SgUtils.isHandableView(v)) {
				/*
				 * if (!SgUtils.isClickableView(v)) { continue; }
				 */
				if (!SgUtils.isButtonView(v)) {
					continue;
				}
				if (Config.DEBUG) {
					Log.e(TAG,
							"===================Is a Button =====================================");
				}
				try {
					String id = getWidgetId(v);
					if (id == null || id.equalsIgnoreCase("")) {
						continue;
					}
					if (Config.DEBUG) {
						Log.e(TAG, "=================== id " + id
								+ "=====================================");
					}
					// handableView.performAction(false);
					if (Config.DEBUG) {
						Log.e("button", "before click on button");
					}
					View button = Button.class.cast(v);
					id = getWidgetId(button);
					mSolo.clickOnView(mSolo.getView(id));
					if (Config.DEBUG) {
						Log.d(TAG, "button clicked");
					}

					Thread.sleep(MAX_SLEEPER);
				} catch (Exception e) {
					if (Config.DEBUG) {
						Log.e(TAG, "checkUseConditionView ");
						e.printStackTrace();
					}
					if (!getCurrentActivityFromActivityManager()
							.equalsIgnoreCase(currentActivity)) {

						if (Config.DEBUG) {
							Log.e(TAG,
									"=================== Current activity Changed =====================================");
						}
						mSolo.finishOpenedActivities();
						setActivity(null);
						mSolo = new Solo(mInstrumentation);
						getActivity();
						if (Config.DEBUG) {
							Log.e(TAG,
									"=================== Restarted =====================================");
						}
					}
					continue;
				} catch (Error er) {
					if (Config.DEBUG) {
						Log.e(TAG, "checkUseConditionView ");
						Log.e(TAG, "==============Error=============");
						er.printStackTrace();
					}
					continue;
				} finally {
					if (Config.DEBUG) {
						Log.e(TAG,
								"=================== finally =====================================");
					}
					String cur_ = getCurrentActivityFromActivityManager();
					if (cur_.equalsIgnoreCase(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME)) {
						if (Config.DEBUG) {
							Log.e(TAG,
									"=================== Reached =====================================");
						}
						break;
					}
					if (!cur_.equalsIgnoreCase(currentActivity)) {
						if (Config.DEBUG) {
							Log.e(TAG,
									"=================== currentActvity is: "
											+ cur_);
						}

						mSolo.finishOpenedActivities();
						setActivity(null);
						mSolo = new Solo(mInstrumentation);
						getActivity();
						if (Config.DEBUG) {
							Log.e(TAG,
									"=================== Restarted in Finally=====================================");
						}

					}
					if (!getCurrentActivityFromActivityManager()
							.equalsIgnoreCase(currentActivity)) {
						break;
					}

				}
			} else {
				if (Config.DEBUG) {
					Log.e(TAG, "=================== NotHandable view ");
				}
				continue;
			}

		}
		if (Config.DEBUG) {
			Log.e(TAG,
					"====================== End =====================================");
		}

	}

	/**
	 * @param v
	 * @return
	 */
	private String getWidgetId(View view) {
		try {
			String id = mContext.getResources().getResourceName(view.getId())
					.replaceAll("id/", "");
			if (Config.DEBUG) {
				Log.e(TAG, "getWidgetId ");
				Log.e(TAG, id);
			}
			return id;
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * @return
	 */
	protected String getCurrentActivityFromActivityManager() {
		Context crawlerContext = mInstrumentation.getContext();
		ActivityManager am = (ActivityManager) crawlerContext
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
		Log.d("topActivity", "CURRENT Activity ::"
				+ taskInfo.get(0).topActivity.getClassName());
		ComponentName componentInfo = taskInfo.get(0).topActivity;
		return componentInfo.getClassName();
	}

	protected boolean goBack(String simpleName, long wait)
			throws InterruptedException {
		try {
			do {
				Activity current = mSolo.getCurrentActivity();
				if (current.getClass().getSimpleName()
						.equalsIgnoreCase(simpleName)) {
					return true;
				}
				mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
				Thread.sleep(wait);
			} while (!mSolo.getCurrentActivity().getClass().getSimpleName()
					.equalsIgnoreCase(simpleName));
		} catch (SecurityException ignored) {
			return false;
		}
		return true;
	}

	/**
	 * Get the ScenarioData (Read the latest generated scenario stored in the
	 * SdCard)
	 * 
	 * 
	 * @param scenarioXml
	 * @return
	 */
	protected ScenarioData readPath(File scenarioXml) {
		if (Config.DEBUG) {
			Log.d(TAG, "readPath ");
		}
		if (scenarioXml.exists()) {
			Log.d(TAG, "not null ");
			return ScenarioParser.parse(scenarioXml);
		} else {
			Log.d(TAG, "null ");
			return null;
		}

	}

	protected static int mPairwise_sequence = 1;// default =1

	protected void setPairwiseSequenceNumber(String sequence) {
		mPairwise_sequence = Integer.parseInt(sequence);
	}

	protected File getScenarioPathFile() {
		return new File(SCENARIO_PATH);
	}

	protected File getDataBaseFile() {
		return new File(DB_PATH);
	}

	protected String getCurrentActivityName() {
		// ou ActivityManager
		// return getCurrentActivityFromActivityManager();
		return mSolo.getCurrentActivity().getClass().getSimpleName();
	}

	protected void clearCache() {
		PackageManager pm = mContext.getPackageManager();
		// Get all methods on the PackageManager
		Method[] methods = pm.getClass().getDeclaredMethods();
		for (Method m : methods) {
			if (m.getName().equals("freeStorage")) {
				try {
					long desiredFreeStorage = 8 * 1024 * 1024 * 1024; // Request
																		// for
																		// 8GB
																		// of
																		// free
																		// space
					m.invoke(pm, desiredFreeStorage, null);
				} catch (Exception e) {
					// Method invocation failed. Could be a permission problem
				}
				break;
			}
		}
	}

	public void clearApplicationData() {
		Log.i("TAG", "**************** Clear Cache ApplicationData");
		File cache = mInstrumentation.getTargetContext().getCacheDir();
		File appDir = new File(cache.getParent());
		if (appDir.exists()) {
			String[] children = appDir.list();
			for (String s : children) {
				if (!s.equals("lib")) {
					deleteDir(new File(appDir, s));
					Log.i("TAG",
							"**************** File /data/data/APP_PACKAGE/" + s
									+ " DELETED *******************");
				}
			}
		}
	}

	public static boolean deleteDir(File dir) {
		Log.i("TAG", "**************** deleteDir");
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

}
