/**
 * 
 */
package kit.Inj;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import junit.framework.AssertionFailedError;
import kit.Config.Config;
import kit.RandomValue.RandomValue;
import kit.RandomValue.RandomValueData;
import kit.RandomValue.RandomValueParser;
import kit.Scenario.State;
import kit.Stress.Event;
import kit.Utils.SgUtils;
import kit.customExceptions.VulnerableLocationException;

import org.apache.commons.io.FileUtils;

import android.os.Environment;
import android.util.Log;

/**
 * @author Stassia
 * 
 */
public class SQLRequestInjectionTestRunner extends AbstractInjectionRunner {

	/**
	 * @param pkg
	 * @param activityClass
	 */
	@SuppressWarnings("rawtypes")
	public SQLRequestInjectionTestRunner(String pkg, Class activityClass) {
		super(pkg, activityClass);
	}

	private static final String TAG = SQLRequestInjectionTestRunner.class.getName();

	/**
	 * Unique test executant des algo
	 * 
	 * @throws InterruptedException
	 * @throws CloneNotSupportedException
	 * @throws VulnerableLocationException
	 */
	public void testInj() throws InterruptedException, CloneNotSupportedException,
			VulnerableLocationException {
		if (!waitForActivity(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME)) {
			assertTrue("Unreached activity " + INITIAL_STATE_ACTIVITY_FULL_CLASSNAME, false);
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
		// retrieve events
		getEventsList();
		try {
			initTestTable();
		} catch (IOException e) {
			// Stop
			e.printStackTrace();
			return;
		}
		assertNotNull(mTable);
		buildInjectionData();
		// trigger each event for all injections or limited
		if (!SgUtils.hasEditText(mState_To_Inj)) {
			// will not be treated
			return;
		}
		for (int keyEvent : mEventList.keySet()) {
			Event currentEvent = mEventList.get(keyEvent);
			for (int keyInj : injectionList.keySet()) {
				/**
				 * trigger each sequence
				 */
				ArrayList<Event> seqEditText = injectionList.get(keyInj);
				Stimulate("" + keyInj + "" + keyEvent, currentEvent, seqEditText);
				/**
				 * crash handle by sga
				 */
				// assert verdict of current state
				// read current table of TestDable
				Table currentTable = null;
				try {
					currentTable = createTableObject();
				} catch (android.database.sqlite.SQLiteException vulState) {

					throw new VulnerableLocationException("VULNERABLE:" + vulState.getMessage());

				}

				mTable = null;
				mTable = currentTable;
				try {
					// assertTrue("VULNERABLE : testTable changed",
					// do_testTable());
					do_testTable();
					// read current view Values

					do_User_Interface_Test();

				} catch (AssertionFailedError e) {
					if (Config.DEBUG) {
						Log.e(TAG, "verdict : " + e.getMessage());
					}
					writeFailReport(currentEvent, seqEditText, e.getMessage());
					// add explicitely error
					// throw new VulnerableLocationException(e.getMessage());
				} catch (AssertionError e) {
					if (Config.DEBUG) {
						Log.e(TAG, "verdict : " + e.getMessage());
					}
					writeFailReport(currentEvent, seqEditText, e.getMessage());
					// add explicitely error
					// throw new VulnerableLocationException(e.getMessage());

				}
				backTrack(mState_To_Inj);
			}

		}

	}

	private void do_User_Interface_Test() {
		ArrayList<Object> view_Values = getViewValues();
		assertTrue("VULNERABLE : UIvalues appeared on current Views",
				!SgUtils.inElement(view_Values, mTable.getContent()));

	}

	private final String EVENT_LIST_PATH = Environment.getExternalStorageDirectory() + Config.TESTRESULTS
			+ File.separator + Config.EVENTS;

	/**
	 * build event list from the injection data
	 * 
	 * @return
	 */
	private void getEventsList() {
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
		mEventList = new Hashtable<Integer, Event>(content.size());
		do {
			String ev = content_iterator.next();
			mEventList.put(SgUtils.get_eventKey(ev), SgUtils.get_event(ev));
		} while (content_iterator.hasNext());
	}

	private Hashtable<Integer, Event> mEventList;
	private Hashtable<Integer, ArrayList<Event>> injectionList;

	/**
	 * 
	 */
	private void buildInjectionData() {
		// read injType data in testData
		HashSet<RandomValueData> data = new HashSet<RandomValueData>();
		RandomValueParser parser = new RandomValueParser();
		data = parser.parse(getTestDataFile());
		injectionList = buildEditTextSqlInjection(mState_To_Inj, data, mTable);
		// add trigerred events at the end of list
		addTriggeredEvent();
	}

	/**
	 * 
	 */
	private void addTriggeredEvent() {
		// TODO pa n�cessaire encore

	}

	/**
	 * @param mState2
	 * @param data
	 * @param mTable2
	 * @return
	 */
	private Hashtable<Integer, ArrayList<Event>> buildEditTextSqlInjection(State state,
			HashSet<RandomValueData> data, Table table) {
		ArrayList<String> injValues = SgUtils.getTestDataSet(data, RandomValue.INJ);
		ArrayList<String> testData = SgUtils.getTestDataSet(data, RandomValue.TEXT);
		ArrayList<String> injectionResult = new ArrayList<String>();
		buildInjectionSet(injValues, testData, injectionResult, table);
		// fill editext Event
		Hashtable<Integer, ArrayList<Event>> FinalResult = new Hashtable<Integer, ArrayList<Event>>();
		int i = 0;
		for (String inj : injectionResult) {
			ArrayList<Event> editSet = SgUtils.get_ediText_List(state);
			ArrayList<Event> toAdd = new ArrayList<Event>();
			for (Event edit : editSet) {
				edit.updateValue(inj);
				toAdd.add(edit);
			}
			FinalResult.put(i, toAdd);
			i++;
		}
		return FinalResult;
	}

	/**
	 * @param injValues
	 * @param testData
	 * @param injectionResult
	 * @param table
	 */
	private void buildInjectionSet(ArrayList<String> injValues, ArrayList<String> testData,
			ArrayList<String> injectionResult, Table table) {
		// Complete injection from Table
		String accRegx = "\\{'data'[ 0-9]*\\}|\\{data[ 0-9]*\\}|\\{values[ 0-9]*\\}|\\{table[ 0-9]*\\}|\\{column[ 0-9]*\\}";
		// Complete injection from Table

		for (String inj : injValues) {
			String result = inj;
			// detection de type table values ou data
			System.out.println("=================");
			System.out.println(inj);

			try {
				Pattern p = Pattern.compile(accRegx);
				Matcher m = p.matcher(inj);
				while (m.find()) {
					String initfound = m.group();
					System.out.println("Found a " + initfound);
					String found = initfound.replaceAll("\\{|\\}", "");
					System.out.println("Found a " + found);
					// detection du nombre d'element � ins�r�(s�par�
					// par des
					// virgules)
					int number = 1;
					StringTokenizer stk = new StringTokenizer(found);
					String type = stk.nextToken();
					if (stk.hasMoreTokens()) {
						number = Integer.parseInt(stk.nextToken());
					}
					System.out.println("type :" + type);
					System.out.println("Number of element :" + number);

					// remplacemnet regex
					if (type.equalsIgnoreCase("table")) {
						result = inj.replaceAll("\\{" + found + "\\}", table.mName);
						inj = result;
						// continue;
					}
					if (type.equalsIgnoreCase("data")) {
						result = inj.replaceAll("\\{" + found + "\\}",
								SgUtils.get_a_random_testData(testData));
						inj = result;
						// continue;
					}
					if (type.equalsIgnoreCase("'data'")) {
						result = inj.replaceAll("\\{" + found + "\\}", getDataValues(number, testData));
						inj = result;
						// continue;
					}
					if (type.equalsIgnoreCase("column")) {
						result = inj.replaceAll("\\{" + found + "\\}", table.getColumns(number));
						inj = result;
						// continue;
					}

					if (type.equalsIgnoreCase("values")) {
						result = inj.replaceAll("\\{" + found + "\\}", table.getValues(number));
						inj = result;
						// continue;
					}

				}
				System.out.println("Result :" + result);
				injectionResult.add(result);
				System.out.println("=================");
			} catch (PatternSyntaxException e) {
				System.out.println(e.getMessage());
			}

		}
	}

	/**
	 * @param number
	 * @param testData
	 * @return
	 */
	private String getDataValues(int number, ArrayList<String> testData) {
		String data = "";
		for (int i = 0; i < number; i++) {
			data = data + ",'" + SgUtils.get_a_random_testData(testData) + "'";
		}
		return data.substring(1);
	}

}
