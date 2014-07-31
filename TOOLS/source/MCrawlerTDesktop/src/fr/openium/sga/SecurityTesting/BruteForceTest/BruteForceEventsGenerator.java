package fr.openium.sga.SecurityTesting.BruteForceTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;

import android.text.InputType;
import android.view.View;

import kit.Config.Config;
import kit.RandomValue.RandomValue;
import kit.RandomValue.RandomValueData;
import kit.RandomValue.RandomValueParser;
import kit.Scenario.State;
import kit.Scenario.Widget;
import kit.Utils.SgUtils;

public class BruteForceEventsGenerator {
	private static final String TAG = BruteForceEventsGenerator.class.getName();
	private final State mTargetState;
	private final ArrayList<String> mLogData;
	private final ArrayList<String> mPasswdData;
	private final double eventNumber;
	private ArrayList<String> mGenaratedSequence;

	/**
	 * add InpuTtype property in the widget property
	 */

	private static Widget mLog;
	private static Widget mPasswd;

	public BruteForceEventsGenerator(State targetState,
			File pathTestDataForLogging, File testDataForDico, double maxNumber)
			throws IOException {
		mTargetState = targetState;
		mLogData = readLog(pathTestDataForLogging);
		mPasswdData = readPasswd(testDataForDico);
		eventNumber = maxNumber;
	}

	private void getLoggPasswd(ArrayList<Widget> stateWidgets) {
		ArrayList<Widget> editTextWidgets = new ArrayList<Widget>();
		for (Widget wig : stateWidgets)
			if (SgUtils.isEditText(wig)
					&& wig.getVisibility().equalsIgnoreCase("" + View.VISIBLE)) {
				/*
				 * int inputType =
				 * Integer.parseInt(wig.getInputType())|InputType
				 * .TYPE_TEXT_VARIATION_POSTAL_ADDRESS;
				 * 
				 * System.out.println("Wig  name" + wig.getName() + ":");
				 * System.out.println("Real Type :" +inputType + " HEX : " +
				 * Integer.toHexString(inputType)); System.out
				 * .println("Target Type 1:"
				 * +InputType.TYPE_TEXT_VARIATION_PASSWORD + " HEX : " + Integer
				 * .toHexString(InputType.TYPE_TEXT_VARIATION_PASSWORD));
				 * System.out
				 * .println("Target Type 2:"+InputType.TYPE_CLASS_TEXT +
				 * " HEX : " + Integer .toHexString(InputType.TYPE_CLASS_TEXT));
				 * 
				 * System.out .println("Mask 1 :" +
				 * (InputType.TYPE_TEXT_VARIATION_PASSWORD & inputType) +
				 * " HEX : " + Integer
				 * .toHexString(InputType.TYPE_TEXT_VARIATION_PASSWORD &
				 * inputType));
				 * 
				 * 
				 * System.out .println("Mask 2 :" + (InputType.TYPE_CLASS_TEXT &
				 * inputType) + " HEX : " + Integer
				 * .toHexString(InputType.TYPE_CLASS_TEXT & inputType));
				 * System.out.println("& 1:  " +
				 * (InputType.TYPE_TEXT_VARIATION_PASSWORD & inputType));
				 * 
				 * System.out.println("& 2:  " + (InputType.TYPE_CLASS_TEXT&
				 * inputType));
				 * 
				 * System.out.println("& 3:  " +
				 * (InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS& inputType));
				 * 
				 * System.out.println("Native type:" +
				 * SgUtils.getNativeTypeOf(Integer.parseInt(wig
				 * .getInputType())));
				 */

				switch (SgUtils.getNativeTypeOf(Integer.parseInt(wig
						.getInputType()))) {
				case InputType.TYPE_TEXT_VARIATION_PASSWORD:
					mPasswd = wig;
					break;
				case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
					mPasswd = wig;
					break;
				default:
					mLog = wig;
					break;
				}
				editTextWidgets.add(wig);
			}
		System.out.println("edidtText set number: " + editTextWidgets.size());

		if (mPasswd == null || mLog == null) {
			throw new NullPointerException(" No Logging Password detected ");

		}
		System.out.println("Has log/psswd: ");

	}

	public boolean generateBrutForceTest(File outputFile) {
		if (Config.DEBUG) {
			System.out.println(TAG);
		}
		mGenaratedSequence= new ArrayList<String>();
		ArrayList<Widget> stateWidgets = mTargetState.getWidgets();
		stateWidgets.addAll(SgUtils.get_menu_widgets());
		stateWidgets.addAll(SgUtils.get_ListViewElements(mTargetState));
		/**
		 * get List of EditText
		 */
		getLoggPasswd(stateWidgets);
		String logValue = getInitData();

		double j = 1;
		for (int i = 1; i < eventNumber + 1; i++, j++) {
			add(j, mLog, logValue);
			j++;
			/**
			 * en mode Random
			 */
			/*
			 * generated_sequence = add(j, mPasswd,
			 * SgUtils.get_a_random_testData(mPasswdData), generated_sequence);
			 */
			/**
			 * combinatoire
			 */
			add(j, mPasswd, get_a_passwd(i));
		}
		try {
			FileUtils.writeLines(outputFile, mGenaratedSequence);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private String get_a_passwd(int i) {
		if (mPasswdData.size() < i) {
			return SgUtils.get_a_random_testData(mPasswdData);
		}
		return mPasswdData.get(i-1);
	}
	
	@SuppressWarnings("unused")
	private String get_a_log(int i) {
		if (mPasswdData.size() < i) {
			return SgUtils.get_a_random_testData(mLogData);
		}
		return mLogData.get(i);
	}

	private ArrayList<String> add(double sequence, Widget log_or_passwd,
			String logValue) {
		mGenaratedSequence.add(build_sequence_((int) sequence, log_or_passwd,
				logValue));
		return mGenaratedSequence;

	}

	protected ArrayList<String> readLog(File testDataFile) {
		HashSet<RandomValueData> data = new HashSet<RandomValueData>();
		RandomValueParser parser = new RandomValueParser();
		data = parser.parse(testDataFile);
		ArrayList<String> logData = SgUtils.getTestDataSet(data,
				RandomValue.LOG);
		return logData;
	}

	private ArrayList<String> readPasswd(File testDataForDico)
			throws IOException {
		return (ArrayList<String>) FileUtils.readLines(testDataForDico);
	}

	private String getInitData() {
		return SgUtils.get_a_random_testData(mLogData);

	}

	private String build_sequence_(int j, Widget randomWidget, String value) {
		if (randomWidget == null) {
			return "";
		}

		return "" + j + Config.EVENTS_DELIMITERS + randomWidget.getName()
				+ Config.EVENTS_DELIMITERS + randomWidget.getType()
				+ Config.EVENTS_DELIMITERS + value;
	}

	public ArrayList<String> getGenratedSequence() {
		return mGenaratedSequence;
	}
}
