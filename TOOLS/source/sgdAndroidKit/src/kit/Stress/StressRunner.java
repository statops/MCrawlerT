/**
 * 
 */
package kit.Stress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import kit.Config.Config;
import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Tree;
import kit.TestRunner.AbstractCrawler;
import kit.TestRunner.IStateChecker;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.test.UiThreadTest;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.robotium.solo.Solo;

/**
 * @author Stassia
 * 
 */
public class StressRunner extends AbstractCrawler {
	/**
	 * @param pkg
	 * @param activityClass
	 */
	private static State mState_To_Stress;
	// private ScenarioData mPath;
	protected HashMap<Integer, Event> mEventList;
	private final String EVENT_LIST_PATH = Environment
			.getExternalStorageDirectory()
			+ Config.TESTRESULTS
			+ File.separator + Config.EVENTS;
	private final String EXECUTED_EVENT_LIST_PATH = Environment
			.getExternalStorageDirectory()
			+ Config.TESTRESULTS
			+ File.separator + Config.EXECUTED_EVENTS;

	public StressRunner(String pkg,
			@SuppressWarnings("rawtypes") Class activityClass) {
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
		HashMap<Integer, Event> eventList = new HashMap<Integer, Event>(
				content.size());
		do {
			String ev = content_iterator.next();
			if (ev.equalsIgnoreCase("") || ev.equalsIgnoreCase(" ")
					|| ev.equalsIgnoreCase("\n")) {
				continue;
			}
			eventList.put(SgUtils.get_eventKey(ev), SgUtils.get_event(ev));
		} while (content_iterator.hasNext());
		return eventList;
	}

	private static final String TAG = StressRunner.class.getName();
	private StressObserver obs;

	public void testStress() throws InterruptedException,
			CloneNotSupportedException {

		if (!waitTargetActivity(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME)) {
			assertTrue("Unreached activity "
					+ INITIAL_STATE_ACTIVITY_FULL_CLASSNAME, false);
		}

		mPath = readPath();
		mState_To_Stress = readState();
		mEventList = readPushedEventList();
		// State lastState = SgUtils.get_last_state(mPath);
		/*
		 * go path
		 */
		mScenarioData = mPath;

		if (mState_To_Stress != null) {
			//

			// goToLastState(mPath.getInitialState(), mState);

			_blind_goToLastState(mState_To_Stress);
			/**
			 * Check current state is the target state
			 */
			State source = get_current_Source();
			if (Config.DEBUG) {
				Log.d(TAG, "testStress ");
				Log.d(TAG, "currentsource " + source.toString());
				Log.d(TAG, "last state" + mState_To_Stress.toString());
			}
			assertNotNull(source);
			assertNotNull(mState_To_Stress);
			source.setUserEnvironment(mState_To_Stress.get_user_environment());
			assertTrue("Target state is not Reached", source.getName()
					.equalsIgnoreCase(mState_To_Stress.getName()));
		} else {
			assertFalse("State to reach is null", false);

		}

		/**
		 * Stress the state
		 */
		Set<Integer> keyset = mEventList.keySet();

		// StressObserver obs = new StressObserver(mSolo);
		int threadSize = 2;
		int i = 0;
		Vector<Event> twoevent = new Vector<Event>(threadSize);
		for (int key : keyset) {
			twoevent.add(i, mEventList.get(key));
			new WriteEvent("" + key, mEventList.get(key), new File(
					EXECUTED_EVENT_LIST_PATH)).start();
			if (i < threadSize) {
				i++;
				continue;
			}
			i = 0;
			try {
				/**
				 * ecrire les evenement effectuer dans un thread.
				 */
				// Event event = mEventList.get(key);
				/*
				 * writer = new WriteEvent("" + key, event, new File(
				 * EXECUTED_EVENT_LIST_PATH)); writer.start();
				 */
				// trigger(event);
				trigger(twoevent);
			} catch (Exception e) {
				e.printStackTrace();
			} catch (Error e) {
				Log.d("there is an error on trigger Action", e.getMessage());
				e.printStackTrace();
			}
			/*
			 * handle unchanged state
			 */
			// mstate must not be null
			assertNotNull(mSolo);
			assertNotNull(mState_To_Stress);

			if (mSolo.getCurrentViews().size() == 0
					|| !SgUtils.isEqualState(get_current_Source(),
							mState_To_Stress.clone())) {
				backTrack(mState_To_Stress);
			}
		}
	}

	private void trigger(Vector<Event> twoevent) throws InterruptedException {
		// launch vector in parallel
		for (final Event currentEvent : twoevent) {
			Runnable runnable_stressSender = new Runnable() {
				Solo solo = mSolo;
				Context ctx = mContext;

				@Override
				public void run() {
					if (Config.DEBUG) {
						Log.d(TAG,
								"trigger " + currentEvent.getName() + " "
										+ currentEvent.getType() + "  "
										+ currentEvent.getValue());
					}
					SgUtils.trigger(solo, currentEvent, ctx);
				}
			};
			try {
				runnable_stressSender.run();
			} catch (Error e) {
				/**
				 * due to click error
				 */
				e.printStackTrace();
				continue;
			}
			Thread.sleep(100);
		}
		// obs.update();
	}

	/**
	 * @throws CloneNotSupportedException
	 * 
	 */
	private void complete_back_track() throws CloneNotSupportedException {
		try {
			restartActivity();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (mPath == null) {
			if (Config.DEBUG) {
				Log.d(TAG, "complete_back_track ");
				Log.d(TAG, "mPath is Null ");
			}
			// assertFalse ()
		}
		// goToLastState(mPath.getInitialState(),
		// SgUtils.get_last_state(mPath));
		_blind_goToLastState(mState_To_Stress.clone());
	}

	/**
	 * @param back
	 *            to state
	 * @throws InterruptedException
	 * @throws CloneNotSupportedException
	 */
	private void backTrack(State state) throws InterruptedException,
			CloneNotSupportedException {
		if (!goBack(state.getShortName(), 0)) {
			complete_back_track();
		}
	}

	/**
	 * @param shortName
	 * @param i
	 * @return
	 */

	/**
	 * trigger event on current view
	 * 
	 * @param currentEvent
	 * @throws InterruptedException
	 */
	private void trigger(Event currentEvent) throws InterruptedException {
		if (Config.DEBUG) {
			Log.d(TAG,
					"trigger " + currentEvent.getName() + " "
							+ currentEvent.getType() + "  "
							+ currentEvent.getValue());
		}
		SgUtils.trigger(mSolo, currentEvent, mContext);
		obs.update();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#getEditTextSet(java.util.ArrayList,
	 * boolean)
	 */
	@Override
	protected ArrayList<String> getEditTextSet(
			ArrayList<EditText> currentViews, boolean b) {
		// TODO Auto-generated method stub
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
	protected Tree performActionOnWidgets(Activity mainActivity,
			ArrayList<View> viewToHandle, String pairWiseSequence,
			Tree current_Tree, State current_state) throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#handleInErrorViewCase(boolean,
	 * android.app.Activity, kit.Scenario.ScenarioData, kit.Scenario.Tree)
	 */
	@Override
	protected void handleInErrorViewCase(boolean viewError,
			Activity currentActivity, ScenarioData mScenarioData2,
			Tree current_Tree) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#handleOtherCase(android.app.Activity,
	 * android.app.Activity, java.util.ArrayList, kit.Scenario.Transition,
	 * kit.Scenario.Tree)
	 */
	@Override
	protected void handleOtherCase(Activity currentActivity,
			Activity mainActivity, ArrayList<View> currentViews, Transition tr,
			Tree current_Tree) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * kit.TestRunner.AbstractCrawler#handleNewActivity(android.app.Activity,
	 * android.app.Activity, kit.Scenario.Transition, kit.Scenario.Tree)
	 */
	@Override
	protected void handleNewActivity(Activity currentActivity,
			Activity mainActivity, Transition tr, Tree current_Tree)
			throws InterruptedException {

	}

	public class WriteEvent extends Thread {
		/**
		 * 
		 */
		Event mEvent;
		File mfileToSaveEvent;

		public WriteEvent(String name, Event ev, File toSave) {
			super(name);
			mEvent = ev;
			mfileToSaveEvent = toSave;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			try {

				FileUtils.writeStringToFile(mfileToSaveEvent, getName()
						+ Config.EVENTS_DELIMITERS + mEvent.toString() + "\n",
						Config.UTF8, true);
			} catch (IOException e) {
				if (Config.DEBUG) {
					Log.e(TAG, "run ");
					Log.e(TAG, e.getMessage());
				}
				e.printStackTrace();
			}
		}
	}

	public class StressObserver implements IStateChecker {
		private Solo mSolo;

		public StressObserver(Solo solo) {
			mSolo = solo;
		}

		public void checkCurrentState() throws InterruptedException {
			if (mSolo.getCurrentViews().size() == 0
					|| !SgUtils.isEqualState(get_current_Source(),
							mState_To_Stress)) {
				try {
					backTrack(mState_To_Stress);
				} catch (CloneNotSupportedException e) {

					e.printStackTrace();
				}
			}

		}

		@Override
		public synchronized void update() throws InterruptedException {
			if (Config.DEBUG) {
				Log.d(TAG, "update() current State");
			}
			checkCurrentState();

		}
	}

}
