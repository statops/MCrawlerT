package kit.BruteForce;

/**
 * 
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CyclicBarrier;

import kit.Config.Config;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Tree;
import kit.Stress.Event;
import kit.TestRunner.AbstractCrawler;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

/**
 * @author Stassia
 * 
 */
@SuppressLint({ "NewApi", "UseSparseArrays" })
public class BruteForceRunner extends AbstractCrawler {
	/**
	 * @param pkg
	 * @param activityClass
	 */
	private static State mState_To_Stress;
	protected HashMap<Integer, Event> mEventList;
	private final String EVENT_LIST_PATH = Environment.getExternalStorageDirectory() + Config.TESTRESULTS
			+ File.separator + Config.EVENTS;

	public BruteForceRunner(String pkg, @SuppressWarnings("rawtypes") Class activityClass) {
		super(pkg, activityClass);

	}

	/**
	 * @return
	 */
	private ScenarioData readPath() {
		if (Config.DEBUG) {
			Log.d(TAG, "readPath ");
		}
		return readPath(getScenarioPathFile());
	}

	/**
	 * @return
	 */
	private State readState() {
		if (Config.DEBUG) {
			Log.d(TAG, "readState ");
		}
		return SgUtils.get_last_state(mPath);
	}

	/**
	 * lire un fichier contenant les �v�nements numerot�
	 * 
	 * 
	 * 1 nom_widget valeur
	 * 
	 * @return
	 * @throws IOException
	 */

	protected HashMap<Integer, Event> readPushedEventList() {
		File eventFile = new File(EVENT_LIST_PATH);
		if (!eventFile.exists()) {
			assertFalse("Events list does not exist", true);
		}
		ArrayList<String> content = null;
		try {
			content = (ArrayList<String>) FileUtils.readLines(eventFile);
		} catch (IOException e) {
			e.printStackTrace();
			assertFalse("Events list does not exist", true);

		}
		Iterator<String> content_iterator = content.iterator();
		if (!content_iterator.hasNext()) {
			throw new NullPointerException("The event List is Empty");
		}
		HashMap<Integer, Event> eventList = new HashMap<Integer, Event>(content.size());
		do {
			String ev = content_iterator.next();
			if (ev.equalsIgnoreCase("") || ev.equalsIgnoreCase(" ") || ev.equalsIgnoreCase("\n")) {
				continue;
			}
			eventList.put(SgUtils.get_eventKey(ev), SgUtils.get_event(ev));
		} while (content_iterator.hasNext());
		return eventList;
	}

	private static final String TAG = BruteForceRunner.class.getName();

	public void testBrut() throws Exception {

		goToAuthorisationLocation();

		/**
		 * Check current state is the target state
		 */

		_test_preambule_requirement();

		/**
		 * prepare input
		 */
		prepareInput();
		/**
		 * Brute force the state
		 */
		bruteForce();
		/**
		 * add output to scenario
		 */
		// setCurrentState();
		/**
		 * verdict
		 */
		setCurrentState();
		assertTrue(mSolo.getCurrentViews().size() > 0);
	}

	private String init_id;
	private String current_id;
	private int brute_id = 0;
	private ScenarioData bruteResult = null;

	private void setCurrentState() {
		/**
		 * create new
		 */
		State currentState = get_current_Dest();
		/**
		 * save current state avec ID
		 */
		if (bruteResult == null) {
			bruteResult = SgUtils.get_init_Tree(mScenarioData);
			bruteResult.addStates(mState_To_Stress);
		}

		if (current_id == null) {
			current_id = init_id + Scenario.IDSEPARATOR + (brute_id);
		} else {
			current_id = init_id + Scenario.IDSEPARATOR + brute_id;

		}

		brute_id++;
		// bruteResult.remove_states(id);
		currentState.setId(current_id);
		currentState.setInit(true);
		bruteResult.setStates(currentState, true, true);
		/**
		 * toujours dans out.xml
		 */
		mScenarioGenerator.generateXml(bruteResult);
		take_dest_screen_shot(current_id);

	}

	protected State get_current_Dest() {
		Activity act = mSolo.getCurrentActivity();
		State source = new State(act.getClass().getName(), "");
		source = SgUtils.addWidgetsInState(source, get_views(), mContext);
		source.setType(Scenario.DEST);
		source.setTime(getTime());
		source.setUserEnvironment(getUserEnvironmentCurrentState());
		return source;

	}

	/**
	 * by default 2
	 */

	private int mEditNumber = 2;
	private double mTimeBenchMark = 0;

	/**
	 * brute force test
	 * 
	 * @throws Exception
	 */
	private void bruteForce() throws Exception {
		int i = 1;
		for (int key : mEventList.keySet()) {
			_setup();
			try {
				_test(key, i);
				_end_test();
			} catch (Exception e) {
				setCurrentState();
				e.printStackTrace();
				_end_test();
			} catch (Error e) {
				Log.d("there is an error on trigger Action", e.getMessage());
				setCurrentState();
				complete_local_back_track();
			} finally {
				i++;

			}
		}

	}

	/**
	 * take initial time
	 */
	private void _setup() {
		mTimeBenchMark = System.currentTimeMillis();
	}

	/**
	 * save execution duration
	 * 
	 * @throws IOException
	 */
	private void _end_test() throws IOException {
		saveExcecutionTime(System.currentTimeMillis() - mTimeBenchMark);
	}

	private final static String TimeFile = SCPATH + File.separator + Config.BRUTE_TIME;

	private void saveExcecutionTime(double time) throws IOException {
		if (!new File(TimeFile).exists()) {
			new File(TimeFile).createNewFile();
		}
		FileUtils.write(new File(TimeFile), " " + time, Config.UTF8, true);
	}

	private void _test(int key, int i) throws Exception {
		_test(key);
		if (i % mEditNumber == 0) {
			_post_test();
		}

	}

	private void _test(int eventkey) throws InterruptedException {
		trigger(mEventList.get(eventkey));

	}

	/**
	 * goback to loggig view
	 * 
	 * @throws Exception
	 */
	private void _post_test() throws Exception {
		sendIM_ACTION(toListen, ime);
		Thread.sleep(1000);
		/**
		 * wait progress barr
		 */
		waitProgressBar();
		setCurrentState();
		goBack();
		Thread.sleep(100);
	}

	/*
	 * click on first button available on dialogue view.
	 */
	private void goBack() throws CloneNotSupportedException {
		System.out.println("*******************************Go back *******************");
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
		mSolo.sleep(500);
		/**
		 * a choisir un bouton des bouton du dialogue
		 */
		/*
		 * if (clickButton == null) { currentButtonNumber =
		 * mSolo.getCurrentViews(Button.class).size(); clickButton =
		 * mSolo.getCurrentViews(Button.class).get(0).getText() .toString(); }
		 * if (currentButtonNumber !=
		 * mSolo.getCurrentViews(Button.class).size()) {
		 *//**
		 * dialogue view changed what to do
		 */
		/*
		 * 
		 * }
		 * 
		 * mSolo.clickOnText(clickButton); mSolo.sleep(500); try { goBack("",
		 * 1000); } catch (InterruptedException e) { // TODO Auto-generated
		 * catch block e.printStackTrace(); }
		 */
	}

	/**
	 * @throws CloneNotSupportedException
	 * @throws IOException
	 * 
	 */
	private void complete_local_back_track() throws CloneNotSupportedException, IOException {
		try {
			restartActivity();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			_blind_goToLastState(mState_To_Stress.clone());
		} catch (Error er) {
			er.printStackTrace();
		}
		_test_preambule_requirement_post_backTrack();
		/**
		 * if dest state is not equal to target state (without dialogue view,
		 * suppose we are in connected mode)
		 * 
		 */

	}

	private void _test_preambule_requirement_post_backTrack() throws IOException {
		_end_test();
		_test_preambule_requirement();

	}

	private int ime;

	private EditText toListen;

	private void getImeAction() {
		ArrayList<Integer> IMEACTION = new ArrayList<Integer>();
		IMEACTION.add(EditorInfo.IME_ACTION_DONE);
		IMEACTION.add(EditorInfo.IME_ACTION_GO);
		IMEACTION.add(EditorInfo.IME_ACTION_SEND);
		for (EditText edit : mSolo.getCurrentViews(EditText.class)) {
			if (IMEACTION.contains(edit.getImeOptions())) {
				ime = edit.getImeOptions();
				toListen = edit;
			}
		}
		/**
		 * by default
		 */
		ime = EditorInfo.IME_ACTION_DONE;
		toListen = mSolo.getCurrentViews(EditText.class).get(0);
	}

	@SuppressLint("NewApi")
	private void sendIM_ACTION(final EditText editText, final int ime_action) throws Exception {
		final CyclicBarrier barrier = new CyclicBarrier(2);
		Runnable runnable_imeSender = new Runnable() {
			@Override
			public void run() {
				editText.onEditorAction(ime_action);
				try {
					barrier.await();
				} catch (Exception e) {
					Log.e("BRUTETest", "Interupted on UI thread pressing IME next", e);
				}
			}
		};
		// Use Solo to get the current activity, and pass our runnable to the UI
		// thread.
		mSolo.getCurrentActivity().runOnUiThread(runnable_imeSender);
		// Waits until the barrier is met in the runnable
		barrier.await();
		Thread.sleep(2000);
	}

	private void prepareInput() {
		mEventList = readPushedEventList();
		mScenarioData = mPath;
		mEditNumber = mSolo.getCurrentViews(EditText.class).size();
		getImeAction();
		init_id = mState_To_Stress.getId();
		take_src_screen_shot(mState_To_Stress.getId());
	}

	private void _test_preambule_requirement() {
		State source = get_current_Source();
		assertNotNull(source);
		assertNotNull("No target State", mState_To_Stress);
		if (Config.DEBUG) {
			Log.d(TAG, "testBrute ");
			Log.d(TAG, "currentsource " + source.toString());
			Log.d(TAG, "last state" + mState_To_Stress.toString());
		}
		source.setUserEnvironment(mState_To_Stress.get_user_environment());
		assertTrue("Target state is not Reached : another activity",
				source.getName().equalsIgnoreCase(mState_To_Stress.getName()));

		assertTrue("Target state is not Reached : Application state has changed",
				SgUtils.isEqualState(mState_To_Stress, get_current_Dest()));

		/**
		 * there must have at least 2 ediText
		 */
		assertTrue("There is No editText on the current view",
				mSolo.getCurrentViews(EditText.class).size() > 0);

	}

	private void goToAuthorisationLocation() throws InterruptedException {
		if (!waitTargetActivity(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME)) {
			assertTrue("Unreached activity " + INITIAL_STATE_ACTIVITY_FULL_CLASSNAME, false);
		}

		mPath = readPath();
		mState_To_Stress = readState();
		if (mState_To_Stress == null) {
			assertFalse("State to reach is null", false);
		}

		_blind_goToLastState(mState_To_Stress);

	}

	/**
	 * trigger event on current view
	 * 
	 * @param currentEvent
	 * @throws InterruptedException
	 */
	private void trigger(Event currentEvent) throws InterruptedException {
		if (Config.DEBUG) {
			Log.d(TAG, "trigger ==> " + currentEvent.getName() + " " + currentEvent.getType() + "  "
					+ currentEvent.getValue());
		}
		fillEditText(currentEvent);
	}

	private void fillEditText(Event event) throws InterruptedException {
		if (Config.DEBUG) {
			Log.d(TAG, "fillEditText ");
		}
		;
		EditText edit = EditText.class.cast(mSolo.getView(mContext.getResources().getIdentifier(
				event.getName(), null, null)));
		assertTrue(" EditText is not Visible", edit.getVisibility() == View.VISIBLE);
		mSolo.clearEditText(edit);
		mSolo.enterText(edit, event.getValue());
		Thread.sleep(100);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#getEditTextSet(java.util.ArrayList,
	 * boolean)
	 */
	@Override
	protected ArrayList<String> getEditTextSet(ArrayList<EditText> currentViews, boolean b) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * kit.TestRunner.AbstractCrawler#performActionOnWidgets(android.app.Activity
	 * , java.util.ArrayList, java.lang.String, kit.Scenario.Tree,
	 * kit.Scenario.State)
	 */
	@Override
	protected Tree performActionOnWidgets(Activity mainActivity, ArrayList<View> viewToHandle,
			String pairWiseSequence, Tree current_Tree, State current_state) throws InterruptedException {
		// Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#handleInErrorViewCase(boolean,
	 * android.app.Activity, kit.Scenario.ScenarioData, kit.Scenario.Tree)
	 */
	@Override
	protected void handleInErrorViewCase(boolean viewError, Activity currentActivity,
			ScenarioData mScenarioData2, Tree current_Tree) throws InterruptedException {
		// Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#handleOtherCase(android.app.Activity,
	 * android.app.Activity, java.util.ArrayList, kit.Scenario.Transition,
	 * kit.Scenario.Tree)
	 */
	@Override
	protected void handleOtherCase(Activity currentActivity, Activity mainActivity,
			ArrayList<View> currentViews, Transition tr, Tree current_Tree) throws InterruptedException {
		// Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * kit.TestRunner.AbstractCrawler#handleNewActivity(android.app.Activity,
	 * android.app.Activity, kit.Scenario.Transition, kit.Scenario.Tree)
	 */
	@Override
	protected void handleNewActivity(Activity currentActivity, Activity mainActivity, Transition tr,
			Tree current_Tree) throws InterruptedException {
		// Auto-generated method stub

	}

}
