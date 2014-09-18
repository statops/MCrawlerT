/**
 * 
 */
package kit.Inj;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import junit.framework.AssertionFailedError;
import kit.Config.Config;
import kit.RandomValue.RandomValue;
import kit.RandomValue.RandomValueData;
import kit.RandomValue.RandomValueParser;
import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Widget;
import kit.Stress.Event;
import kit.Utils.SgUtils;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;

/**
 * @author Stassia
 * 
 */
public class SQLRandomDataInjectionRunner extends AbstractInjectionRunner {

	private State destState;
	private ArrayList<Event> editTextList;
	private Event targetTriggerEvent;

	/**
	 * @param pkg
	 * @param activityClass
	 */
	public SQLRandomDataInjectionRunner(String pkg,
			@SuppressWarnings("rawtypes") Class activityClass) {
		super(pkg, activityClass);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	private static final String TAG = SQLRandomDataInjectionRunner.class
			.getName();

	// private static final int MAX_INJ = 10;

	/**
	 * Unique test executant des algo
	 * 
	 * @throws InterruptedException
	 * @throws CloneNotSupportedException
	 */
	public void testInj() throws InterruptedException,
			CloneNotSupportedException {
		if (!waitTargetActivity(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME)) {
			assertTrue("Unreached activity "
					+ INITIAL_STATE_ACTIVITY_FULL_CLASSNAME, false);
		}

		readTargetStatePath();
		readTargetState();
		if (mState_To_Inj == null || mPath == null) {
			if (Config.DEBUG) {
				Log.d(TAG, "testInj ");
				Log.d(TAG, "target_state is null ");
			}
			return;
		}

		preambuleStep();
		Transition tr = readOutPutTransition();
		if (tr == null) {
			if (Config.DEBUG) {
				Log.d(TAG, "testInj ");
			}
			return;
		}
		destState = tr.getDest();
		setDeststateValue();

		get_event(tr);
		// go to target state

		if (!SgUtils.hasEditText(mState_To_Inj)) {
			// will not be treated
			return;
		}
		buildInjectionData();
		for (int keyInj : injectionList.keySet()) {
			/**
			 * trigger each sequence
			 */
			ArrayList<Event> seqEditText = injectionList.get(keyInj);
			Stimulate("" + keyInj, targetTriggerEvent, seqEditText);
			/**
			 * crash handle by sga
			 */
			/**
			 * compare current view value to destValue
			 */
			try {
				assertTrue("VULNERABLE : view Change changed",
						do_test_View(seqEditText));
			} catch (AssertionFailedError e) {
				if (Config.DEBUG) {
					Log.e(TAG, "verdict : " + e.getMessage());
				}
				writeFailReport(targetTriggerEvent, seqEditText, e.getMessage());
				return;
				// add error
			} catch (AssertionError e) {
				if (Config.DEBUG) {
					Log.e(TAG, "verdict : " + e.getMessage());
				}
				writeFailReport(targetTriggerEvent, seqEditText, e.getMessage());
				return;
			}

			backTrack(mState_To_Inj);
		}

	}

	/**
	 * @return
	 */
	private boolean do_test_View(ArrayList<Event> seqEditText) {

		ArrayList<String> added_value = new ArrayList<String>();
		for (Event event : seqEditText) {
			added_value.add(event.getValue());
		}
		ArrayList<Object> current_view_Values = getViewValues();
		/**
		 * get destSetValue
		 */
		ArrayList<Object> expected_view_Values = new ArrayList<Object>();
		expected_view_Values.addAll(dest_values);
		expected_view_Values.addAll(getExpectedViewValues(added_value));
		assertTrue("VULNERABLE : UIvalues changed",
				SgUtils.inElement(current_view_Values, expected_view_Values));
		/**
		 * test pass
		 */
		return true;
	}

	private ArrayList<Object> dest_values = new ArrayList<Object>();

	private void setDeststateValue() {
		for (Widget wig : destState.getWidgets()) {
			StringTokenizer token = new StringTokenizer(wig.getValue());
			if (!token.hasMoreTokens())
				continue;
			do {
				dest_values.add(token.nextToken());
			} while (token.hasMoreTokens());

		}
	}

	/**
	 * @return
	 */
	private ArrayList<Object> getExpectedViewValues(ArrayList<String> added) {
		ArrayList<Object> values = new ArrayList<Object>();
		for (String wig : added) {
			StringTokenizer token = new StringTokenizer(wig);
			if (!token.hasMoreTokens())
				continue;
			do {
				values.add(token.nextToken());
			} while (token.hasMoreTokens());
		}
		return values;
	}

	/**
	 * @param tr
	 * @return
	 */
	private void get_event(Transition tr) {
		editTextList = new ArrayList<Event>();
		for (kit.Scenario.Action act : tr.getAction()) {
			if (act.getWidget() == null) {
				continue;
			}
			Widget wig = act.getWidget();
			if (wig.getType().equalsIgnoreCase(EditText.class.getSimpleName())) {
				editTextList.add(new Event(wig.getName(), wig.getType(), wig
						.getValue()));
				continue;
			} else {
				targetTriggerEvent = new Event(wig.getName(), wig.getType(),
						wig.getValue());
				if (Config.DEBUG) {
					Log.d(TAG, "target get_event " + targetTriggerEvent);
				}
			}

		}
	}

	private Hashtable<Integer, ArrayList<Event>> injectionList;

	/**
	 */
	private void buildInjectionData() {
		injectionList = new Hashtable<Integer, ArrayList<Event>>();
		HashSet<RandomValueData> data = new HashSet<RandomValueData>();
		RandomValueParser parser = new RandomValueParser();
		data = parser.parse(getTestDataFile());
		ArrayList<String> injValues = SgUtils.getTestDataSet(data,
				RandomValue.INJ);
		filter(injValues);
		for (int i = 0; i < mPairwise_sequence; i++) {
			ArrayList<Event> listTemp = new ArrayList<Event>();
			for (Event editext : editTextList) {
				String value = "" + editext.getValue() + ""
						+ SgUtils.get_a_random_testData(injValues);
				Event currentEvent = new Event(editext.getName(),
						editext.getType(), value);
				System.out.println("result : " + currentEvent.getValue());
				listTemp.add(currentEvent);
			}
			injectionList.put(i, listTemp);
		}

	}

	private void filter(ArrayList<String> injValues) {
		Iterator<String> testData = injValues.iterator();
		while (testData.hasNext()) {
			String currentString = testData.next();
			if (currentString.contains("{") && currentString.contains("}")) {
				testData.remove();
			}
		}

	}

	/**
	 * @return
	 */
	private Transition readOutPutTransition() {
		/***
		 * read a out transition of mState
		 */
		File outPutFile = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ Config.TESTRESULTS
				+ File.separator
				+ "tr_out");
		if (!outPutFile.exists()) {
			return null;
		}
		ScenarioData tr_out = readPath(outPutFile);
		if (tr_out.getTransitions().isEmpty())
			return null;

		return tr_out.getTransitions().get(0);
	}
}
