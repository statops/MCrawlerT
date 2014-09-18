package kit.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

import kit.Config.Config;
import kit.PairWise.PairWise;
import kit.RandomValue.RandomValue;
import kit.RandomValue.RandomValueData;
import kit.RandomValue.RandomValueParser;
import kit.Scenario.Action;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Tree;
import kit.Scenario.UserEnvironment;
import kit.Scenario.Widget;
import kit.Stress.Event;
import kit.TestRunner.AbstractCrawler;
import kit.TestRunner.ClickableView;
import kit.TestRunner.ListViewElement;
import kit.TestRunner.MenuView;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.robotium.solo.Solo;

public class SgUtils {

	public static final String BUTTON = Button.class.getName();
	public static final String IMAGE = ImageView.class.getName();
	public static final String LIST = ListView.class.getName();
	public static final String EDITTEXT = EditText.class.getName();
	public static final String CHEKBOX = CheckBox.class.getName();
	public static final String TEXTVIEW = TextView.class.getName();
	private final static String TAG = SgUtils.class.getSimpleName();
	private static String CLICKABLEVIEW = ClickableView.class.getSimpleName();

	public static String random(ArrayList<String> randomValue) {
		Random rd = new Random();
		int randomInt = rd.nextInt(randomValue.size());
		return randomValue.get(randomInt);
	}

	public static int getRandomView(int size) {
		Random rd = new Random();
		int randomInt = rd.nextInt(size);
		return randomInt;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean isViewFromClass(Class type, View currentViewTocross) {

		if (type.isAssignableFrom(currentViewTocross.getClass())) {
			return true;
		}
		return false;
	}

	public static boolean isTextView(View currentViewTocross) {
		return isViewFromClass(TextView.class, currentViewTocross);
	}

	public static boolean isImageView(View currentViewTocross) {
		if (ImageView.class.isAssignableFrom(currentViewTocross.getClass())) {
			// Log.d("CurrentView", "ImageView");
			return true;
		}
		return false;
	}

	public static boolean isButtonView(View currentViewTocross) {
		if (Button.class.isAssignableFrom(currentViewTocross.getClass())) {
			Log.d("CurrentView", "isButton");
			return true;
		}
		return false;
	}

	public static boolean isRadioButtonView(View currentViewTocross) {
		if (RadioButton.class.isAssignableFrom(currentViewTocross.getClass())) {
			Log.e("CurrentView", "isRadioButton");
			return true;
		}
		return false;
	}

	public static boolean isListElementView(View currentViewTocross) {
		if (ListViewElement.class.isAssignableFrom(currentViewTocross
				.getClass())) {
			// Log.d("CurrentView", "ListViewElement");
			return true;
		}
		return false;
	}

	public static boolean isCheckBox(View currentViewTocross) {
		if (CheckBox.class.isAssignableFrom(currentViewTocross.getClass())) {
			// Log.d("CurrentView", "CheckBox");
			return true;
		}
		return false;
	}

	public static boolean isListView(View currentViewTocross) {
		if (ListView.class.isAssignableFrom(currentViewTocross.getClass())) {
			return true;
		}
		return false;
	}

	public static boolean isEditText(View v) {
		if (EditText.class.isAssignableFrom(v.getClass())) {
			return true;
		}
		return false;
	}

	/**
	 * @param v
	 * @return
	 */
	private static boolean isScrollView(View v) {
		if (ScrollView.class.isAssignableFrom(v.getClass())) {
			return true;
		}
		return false;
	}

	public static boolean isClickable(View v) {
		if (v.isClickable()) {
			return true;
		}
		return false;
	}

	/**
	 * save trace in a file
	 * 
	 * @param name
	 * @throws IOException
	 */
	public static void save(String value, File file) throws IOException {
		FileUtils.write(file, value, true);
	}

	public static boolean in(Transition tr, ArrayList<Transition> listTr) {

		for (Transition eachlistTr : listTr) {
			/**
			 * cit�re d'ŽgalitŽ de 2 transition
			 */

			if (eachlistTr.getSource().getName()
					.equalsIgnoreCase(tr.getSource().getName())) {
				// Check actions
				// Empty action
				if (eachlistTr.getAction().isEmpty()) {
					// no "action" ==> s do nothing
					// check if at least if ther exist a transition that begin
					// with the same source state (ˆ voir)
				} else {
					ArrayList<Action> actionList = new ArrayList<Action>();
					actionList = eachlistTr.getAction();
					for (Action act : actionList) {
						/**
						 * check also widget
						 */
						for (Action actTr : tr.getAction())
							if (act.getWidget()
									.getName()
									.equalsIgnoreCase(
											actTr.getWidget().getName())) {
								if (Config.DEBUG) {
									System.out.println(act.getWidget()
											.getName());
								}

								return true;
							}

					}

				}

			}
		}
		return false;

	}

	/**
	 * 
	 * @return the name of activities that contains nonHandled Widget ==
	 */
	public static ArrayList<String> getMustReCrossActivity(ScenarioData scen) {
		ArrayList<String> name = new ArrayList<String>();
		/***
		 * if it exists an widget where it is not handled
		 */
		for (State activityName : scen.getStates())
			if (isNotHandled(activityName, scen))
				name.add(activityName.getName());
		return name;

	}

	private static boolean isNotHandled(State activityName, ScenarioData scen) {
		if (activityName.getWidgets().isEmpty())
			return false;
		for (Widget wig : activityName.getWidgets()) {
			if (getNonHandledWidget(scen).contains(wig)) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<Widget> getNonHandledWidget(ScenarioData scen) {
		/**
		 * list of all wigdet
		 */
		ArrayList<Widget> allWigets = new ArrayList<Widget>();
		allWigets = getAllWidget(scen);

		/***
		 * list of non handle
		 */
		for (Widget wig : getHandledWidget(scen)) {
			if (allWigets.contains(wig)) {
				// System.out.println(" remove  " + wig.getName());
				allWigets.remove(wig);
			}

		}

		return allWigets;
	}

	public static ArrayList<Widget> getHandledWidget(ScenarioData scen) {

		/**
		 * list all widgets handle in a transition
		 */
		ArrayList<Widget> tem = new ArrayList<Widget>();
		for (Transition tr : scen.getTransitions()) {
			for (Widget wig : tr.getWidgets()) {
				if (!tem.contains(wig)) {
					tem.add(wig);
				}
			}

		}

		return tem;
	}

	public static ArrayList<Widget> getAllWidget(ScenarioData scen) {
		ArrayList<Widget> wig = new ArrayList<Widget>();
		for (State st : scen.getStates()) {
			if (!st.getWidgets().isEmpty()) {
				wig.addAll(st.getWidgets());
			}
		}
		return wig;
	}

	/**
	 * gerer le vues supporter, si des vues seront ajoutées, il faut ajoute à ce
	 * niveau
	 * 
	 * @param v
	 * @return
	 */
	public static boolean isHandableView(View v) {
		if (isImageView(v) && isClickable(v))
			return true;
		if (isRadioButtonView(v))
			return true;
		if (isButtonView(v))
			return true;
		if (isCheckBox(v))
			return true;
		if (isListView(v))
			return true;
		if (isEditText(v))
			return true;
		if (isTextView(v))
			return true;
		if (isScrollView(v))
			return true;
		if (isClickable(v))
			return true;
		return false;

	}

	private final static String Excp = "Exception";
	private final static String Crash = "Crash";
	private final static String Crashed = "crashed";
	private final static String Process_Crash = "Process crashed";

	/***
	 * handle output error during test
	 * 
	 * @param out
	 * @return
	 */
	public static boolean report_not_contain_crash_(String out) {
		if (out.contains(Excp) || out.contains(Crash)
				|| out.contains(Process_Crash) || out.contains(Crashed)) {
			return false;
		} else
			return true;
	}

	private final static String TESTRESULTS = "testResults";
	private final static String OUT = "out.xml";
	private static final String RADIO_BUTTON = RadioButton.class.getName();;

	/**
	 * Parse the file and return the scenarioData Object
	 * 
	 * @param sdcard
	 *            path
	 * @param directory
	 *            testResultsDirectory
	 * @param scenName
	 *            the scenario file name
	 * @return
	 */
	public static ScenarioData readScenario(String path, String directory,
			String scenName) {
		/**
		 * if null take default value
		 */
		if (directory == null) {
			directory = TESTRESULTS;

		}
		if (scenName == null) {
			scenName = OUT;
		}
		return ScenarioParser.parse(new File(path + "/" + directory + "/"
				+ scenName));
	}

	/**
	 * add error into the last transition (by default)
	 * 
	 * @param scen
	 * @param out2
	 *            the error description
	 */

	public static ScenarioData addError(ScenarioData scen, String out2) {
		if (scen.getTransitions() == null || scen.getTransitions().isEmpty()) {
			return scen;
		}
		Transition tr = get_last_transition(scen);
		String treeID = getTreeIdOfTransition(scen, tr.getId());
		if (!tr.getAction().isEmpty()) {
			tr.getAction().get(tr.getAction().size() - 1).setError(out2);
		} else {
			Action act = new Action(Scenario.ERROR);
			act.setError(out2);
			tr.setAction((act));
		}
		State final_state = new State(Scenario.END, false, true, tr.getId());
		tr.setDest(final_state);
		scen.addStates(final_state);
		scen.setTransitions(tr, treeID, true);
		/**
		 * set the tree
		 */
		return scen;
	}

	public static Transition get_last_transition(ScenarioData scen) {
		ArrayList<Transition> id = scen.getTransitions();
		if (id.isEmpty()) {
			return null;
		}
		Collections.sort(id, new TransitionIdComparator());
		System.out.println("id: ");
		for (Transition idelt : id) {
			System.out.println(idelt.getId());
		}

		return id.get(0);
	}

	/**
	 * call to read random value in the Sdcard pushed by sgd in
	 * 
	 * @return
	 */
	public static String getRv(File path) {

		// Log.e("getRv", path.getAbsolutePath());
		if (path.exists()) {
			/**
			 * read file content
			 */
			return read(path, 1);
		} else {
			return null;
		}

	}

	/**
	 * Read file in line i
	 * 
	 * @param path
	 * @param i
	 *            number of line
	 * @return
	 */

	private static String read(File path, int i) {

		try {
			if (i == 1) {
				return new Scanner(path).nextLine();
			} else {
				for (int j = 1; j < i; j++) {
					new Scanner(path).nextLine();
				}
				return new Scanner(path).nextLine();
			}
		} catch (FileNotFoundException e) {
			return null;
		} catch (NoSuchElementException n) {
			return null;
		}

	}

	public static void deleteRv() {

		/**
		 * 
		 */
		File path = new File(Environment.getExternalStorageDirectory()
				+ kit.Config.Config.RV);
		if (path.exists()) {
			path.delete();
		}

	}

	public static String read(Scanner scan, int i) {
		try {
			if (i == 1) {
				i++;
				return scan.nextLine();
			} else {
				for (int j = 1; j < i;) {
					scan.nextLine();
					j++;
				}
				i++;
				return scan.nextLine();
			}
		} catch (NoSuchElementException n) {
			return null;
		}

	}

	/**
	 * generate Pairwise Sequence from a list of editText
	 * 
	 * @param rvPath
	 *            : the path of random value
	 */

	public static ArrayList<String> generatePairWiseSequence(String rvPath,
			ArrayList<EditText> ediTextView, int maxNumber,
			ArrayList<String> names, ScenarioData scenario, Context cyx) {

		ArrayList<String> editTextTypes = new ArrayList<String>();
		editTextTypes = getEditTextTtypeSet(editTextTypes, ediTextView);
		ArrayList<String> sequence = new ArrayList<String>();
		sequence = generateEditTextPairWiseSequence(rvPath, editTextTypes,
				maxNumber);
		/**
		 * delete already performed sequence in scenario
		 */
		for (Transition tr : scenario.getTransitions()) {
			if (numberOfEditTextIn(tr) == names.size()) {
				sequence = remove_editextSequenceValueIn(ediTextView, tr,
						sequence, cyx);
			}
		}
		Log.i(TAG, "List of pairwise sequences: ");
		for (String sq : sequence) {
			Log.i(TAG, sq);
		}

		return sequence;
	}

	private static ArrayList<String> getEditTextTtypeSet(
			ArrayList<String> editTextTypes, ArrayList<EditText> ediTextView) {
		for (EditText edit : ediTextView) {
			editTextTypes.add("" + edit.getInputType());
			if (Config.DEBUG) {
				System.out.println("EditText" + " Inputtype :"
						+ edit.getInputType());
			}

		}
		return editTextTypes;
	}

	// ******
	private static ArrayList<String> remove_editextSequenceValueIn(
			ArrayList<EditText> ediTextView, Transition tr,
			ArrayList<String> sequence, Context ctx) {
		ArrayList<String> editTextNames = new ArrayList<String>();
		editTextNames = getEditTextName(editTextNames, ediTextView, ctx);
		if (!tr.contains(editTextNames)) {
			return sequence;
		}
		if (Config.DEBUG) {
			Log.d(TAG, "remove_editextSequenceValueIn ");
			Log.d(TAG, tr.getId() + " contains :"
					+ editTextNames.toArray().toString());
		}
		/**
		 * verifier les valeurs des sequences
		 */
		ArrayList<String> transition_Editext_Sequence = getTransition_edit_Text_sequence(tr);
		Iterator<String> sequenceIterator = sequence.iterator();
		boolean isIn = true;
		String currentSequence = "";
		if (!sequenceIterator.hasNext()) {
			return sequence;
		}
		do {
			currentSequence = sequenceIterator.next();
			if (!listContentIsEqual(transition_Editext_Sequence,
					sequenceToList(currentSequence))) {
				isIn = false;
				break;
			}

		} while (sequenceIterator.hasNext());
		if (isIn) {
			sequence.remove(currentSequence);
		}
		return sequence;
	}

	private static boolean listContentIsEqual(
			ArrayList<String> transition_Editext_Sequence,
			ArrayList<String> sequenceToList) {
		Collections.sort(transition_Editext_Sequence);
		Collections.sort(sequenceToList);
		for (String s : sequenceToList) {
			if (!transition_Editext_Sequence.contains(s))
				return false;
		}
		return true;
	}

	private static ArrayList<String> getTransition_edit_Text_sequence(
			Transition tr) {
		String sequence = "";
		for (Widget wig : tr.getWidgets()) {
			if (wig.getType().equalsIgnoreCase(EditText.class.getName())) {
				sequence = sequence + wig.getValue() + PairWise.separator;
			}
		}
		return sequenceToList(sequence);
	}

	public static ArrayList<String> sequenceToList(String seq) {
		if (seq == null) {
			return null;
		}
		String[] seqTab = seq.split(PairWise.separator);
		ArrayList<String> sqArray = new ArrayList<String>(Arrays.asList(seqTab));
		return sqArray;
	}

	private static ArrayList<String> getEditTextName(
			ArrayList<String> editTextNames, ArrayList<EditText> ediTextView,
			Context ctx) {
		for (EditText editText : ediTextView) {
			editTextNames.add(ctx.getResources().getResourceName(
					editText.getId()));
		}
		return editTextNames;
	}

	private static int numberOfEditTextIn(Transition tr) {
		int i = 0;
		for (Action act : tr.getAction()) {
			if (act.getWidget().getType()
					.equalsIgnoreCase(EditText.class.getSimpleName())) {
				i++;
			}
		}
		return i;
	}

	/**
	 * 
	 * @param random_value_path
	 *            test value path
	 * @param editTextTypes
	 *            list of edit text type , eg: plain text, phone number
	 * @param maxNumber
	 * @return
	 */
	public static ArrayList<String> generateEditTextPairWiseSequence(
			String random_value_path, ArrayList<String> editTextTypes,
			int maxNumber) {
		if (maxNumber <= 0) {
			throw new IllegalArgumentException(
					"max number should be bigger than 0");
		}
		String ext = FilenameUtils.getExtension(random_value_path);
		if (ext.equalsIgnoreCase(Config.XMLEXTENSION)) {
			return generateFromXml(random_value_path, editTextTypes, maxNumber);
		} else {
			return generateFromText(random_value_path, editTextTypes, maxNumber);
		}

	}

	private static ArrayList<String> generateFromText(String random_value_path,
			ArrayList<String> editTextTypes, int maxNumber) {
		PairWise.setNbTestMax(maxNumber);
		PairWise pairWise = new PairWise();
		List<List<String>> variable = new ArrayList<List<String>>();
		/**
		 * Lire le contenu du fichier en une seule fois
		 * 
		 * */
		List<String> data = getText(random_value_path);
		for (String type : editTextTypes) {
			List<String> obt = new ArrayList<String>();
			obt = getSetofType(type, data);
			variable.add(obt);
		}
		pairWise.setSequence(variable);
		List<String> test = pairWise.generatePairwisedSequence();
		return (ArrayList<String>) test;
	}

	/**
	 * 
	 * @param random_value_path
	 *            : testData.xml
	 * @param editTextTypes
	 *            : list of the type of editText
	 * @param maxNumber
	 *            : Pairwise max number sequence
	 * @return
	 */
	private static ArrayList<String> generateFromXml(String random_value_path,
			ArrayList<String> editTextTypes, int maxNumber) {
		HashSet<RandomValueData> testData = getAllTestDataSet(random_value_path);
		PairWise.setNbTestMax(maxNumber);
		PairWise pairWise = new PairWise();

		List<List<String>> variable = new ArrayList<List<String>>();
		/**
		 * cas où il y a un type passwd
		 */
		if (hasPasswdelement(editTextTypes)) {
			variable = generateLoggingSet(variable, editTextTypes, testData);
		} else {
			variable = generateSimpleSet(variable, editTextTypes, testData);
		}

		pairWise.setSequence(variable);
		List<String> test = pairWise.generatePairwisedSequence();
		return (ArrayList<String>) test;
	}

	private static List<List<String>> generateLoggingSet(
			List<List<String>> variable, ArrayList<String> editTextTypes,
			HashSet<RandomValueData> testData) {

		for (String type : editTextTypes) {
			List<String> typeSet = new ArrayList<String>();
			switch (getNativeTypeOf(Integer.parseInt(type))) {
			case InputType.TYPE_TEXT_VARIATION_PASSWORD:
				typeSet = getPassWordSet(testData);
				break;
			default:
				typeSet = getLoggingSet(testData);
				break;
			}
			variable.add(typeSet);
		}
		return variable;

	}

	private static List<List<String>> generateSimpleSet(
			List<List<String>> variable, ArrayList<String> editTextTypes,
			HashSet<RandomValueData> testData) {
		for (String type : editTextTypes) {
			List<String> typeSet = new ArrayList<String>();
			typeSet = getSetofType(type, testData);
			variable.add(typeSet);
		}
		return variable;
	}

	private static boolean hasPasswdelement(ArrayList<String> editTextTypes) {
		for (String type : editTextTypes) {
			if (isPasswdType(type)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isPasswdType(String type) {
		return getNativeTypeOf(Integer.parseInt(type)) == InputType.TYPE_TEXT_VARIATION_PASSWORD;
	}

	public static HashSet<RandomValueData> getAllTestDataSet(String testDataPath) {
		HashSet<RandomValueData> testData = new HashSet<RandomValueData>();
		RandomValueParser parser = new RandomValueParser();
		testData = parser.parse(new File(testDataPath));
		return testData;

	}

	private static ArrayList<String> getEmailSet(
			HashSet<RandomValueData> testData) {
		return getTestDataSet(testData, RandomValue.MAIL);
	}

	private static ArrayList<String> getPassWordSet(
			HashSet<RandomValueData> testData) {
		return getTestDataSet(testData, RandomValue.PASSWORD);
	}

	private static ArrayList<String> getLoggingSet(
			HashSet<RandomValueData> testData) {
		return getTestDataSet(testData, RandomValue.LOG);
	}

	private static ArrayList<String> getDateSet(
			HashSet<RandomValueData> testData) {
		return getTextSet(testData);
	}

	private static ArrayList<String> getIntegerSet(
			HashSet<RandomValueData> testData) {
		return getTestDataSet(testData, RandomValue.INTEGER);
	}

	private static ArrayList<String> getTextSet(
			HashSet<RandomValueData> testData) {
		return getTestDataSet(testData, RandomValue.TEXT);
	}

	public static ArrayList<String> getStressSet(
			HashSet<RandomValueData> testData) {
		return getTestDataSet(testData, RandomValue.STRESS);
	}

	public static ArrayList<String> getInjSet(HashSet<RandomValueData> testData) {
		return getTestDataSet(testData, RandomValue.INJ);
	}

	public static ArrayList<String> getTestDataSet(
			HashSet<RandomValueData> testData, String tag) {
		Iterator<RandomValueData> it = testData.iterator();
		while (it.hasNext()) {
			RandomValueData rvd = it.next();
			if (rvd.getType().equals(tag)) {
				return new ArrayList<String>(rvd.getValue());
			}
		}
		return null;
	}

	private static List<String> getText(String rvPath) {
		File file = new File(rvPath);
		List<String> data = new ArrayList<String>();
		try {
			data = FileUtils.readLines(file);
		} catch (IOException e) {
			return null;
		}
		return (ArrayList<String>) data;
	}

	private static List<String> getSetofType(String type,
			HashSet<RandomValueData> testData) {
		ArrayList<String> list = new ArrayList<String>();
		// switch (getNativeTypeOf(Integer.parseInt(type, 16))) {
		switch (getNativeTypeOf(Integer.parseInt(type))) {
		case InputType.TYPE_CLASS_TEXT:
			list = getTextSet(testData);
			break;
		case InputType.TYPE_CLASS_PHONE:
			list = getIntegerSet(testData);
			break;
		case InputType.TYPE_CLASS_NUMBER:
			list = getIntegerSet(testData);
			break;
		case InputType.TYPE_CLASS_DATETIME:
			list = getDateSet(testData);
			break;
		case InputType.TYPE_TEXT_VARIATION_PASSWORD:
			list = getPassWordSet(testData);
			break;
		case InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
			list = getEmailSet(testData);
			break;
		case InputType.TYPE_NULL:
			list = getTextSet(testData);
			break;

		default:
			list = getTextSet(testData);
			break;
		}
		if (list == null) {
			list = new ArrayList<String>();
		}
		return list;
	}

	private static List<String> getSetofType(String type, List<String> data) {
		ArrayList<String> list = new ArrayList<String>();
		// switch (getNativeTypeOf(Integer.parseInt(type, 16))) {
		switch (getNativeTypeOf(Integer.parseInt(type))) {
		case InputType.TYPE_CLASS_TEXT:
			list = getTextSet(data);
			break;
		case InputType.TYPE_CLASS_PHONE:
			list = getIntegerSet(data);
			break;
		case InputType.TYPE_CLASS_NUMBER:
			list = getIntegerSet(data);
			break;
		case InputType.TYPE_CLASS_DATETIME:
			list = getDateSet(data);
			break;
		case InputType.TYPE_TEXT_VARIATION_PASSWORD:
			list = getPassWordSet(data);
			break;
		case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
			list = getPassWordSet(data);
			break;
		case InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
			list = getEmailSet(data);
			break;
		case InputType.TYPE_NULL:
			list = getTextSet(data);
			break;

		default:
			list = getTextSet(data);
			break;
		}
		if (list == null) {
			list = new ArrayList<String>();
		}
		return list;
	}

	public static int getNativeTypeOf(int inputType) {
		/**
		 * Bit definition operation
		 */
		// @formatter:off
		int[] typeTab = { InputType.TYPE_TEXT_VARIATION_PASSWORD,
				InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
				InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
				InputType.TYPE_CLASS_DATETIME, InputType.TYPE_CLASS_PHONE,
				InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_TEXT,
				InputType.TYPE_NULL };
		// @formatter:on
		for (int i = 0; i < typeTab.length; i++) {
			if ((inputType & typeTab[i]) == typeTab[i]) {
				return typeTab[i];
			}
		}
		return InputType.TYPE_NULL;
	}

	/**
	 * read the rvFile
	 * 
	 * @param data
	 * @return
	 */
	private static ArrayList<String> getEmailSet(List<String> data) {
		ArrayList<String> temp = new ArrayList<String>();
		/**
		 * by default
		 */
		temp.add("null");
		for (String st : data) {
			if (st.contains("@")) {
				temp.add(st);
			}
		}
		return temp;
	}

	private static ArrayList<String> getPassWordSet(List<String> data) {
		return getTextSet(data);
	}

	private static ArrayList<String> getDateSet(List<String> data) {
		return getIntegerSet(data);
	}

	private static ArrayList<String> getIntegerSet(List<String> data) {
		ArrayList<String> temp = new ArrayList<String>();
		/**
		 * by default
		 */
		temp.add("0");
		for (String st : data) {
			try {
				temp.add("" + Integer.parseInt(st));
			} catch (NumberFormatException nb) {

			}
		}
		return temp;
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private static ArrayList<String> getTextSet(List<String> data) {
		return (ArrayList<String>) data;
	}

	public static void setActionInScenarioData(ScenarioData scenarioData,
			Solo solo) {
		/**
		 * set actions (List of all possible actions)
		 */
		if (!solo.getCurrentViews(EditText.class).isEmpty()) {
			scenarioData.setActions(new Action(Scenario.EDITTEXT));
		}
		if (!solo.getCurrentViews(Button.class).isEmpty()) {
			scenarioData.setActions(new Action(Scenario.CLICK_BUTTON));
		}
		if (!solo.getCurrentViews(RadioButton.class).isEmpty()) {
			scenarioData.setActions(new Action(Scenario.CLICK_RADIO_BUTTON));
		}
		if (!solo.getCurrentViews(ToggleButton.class).isEmpty()) {
			scenarioData.setActions(new Action(Scenario.CLICK_TOOGLE_BUTTON));

		}
		/**
		 * if (!solo.getCurrentViews(Switch.class).isEmpty()) {
		 * scenarioData.setActions(new Action(Scenario.CLICK_SWITCH_BUTTON)); }
		 * if (!solo.getCurrentViews(ZoomButton.class).isEmpty()) {
		 * scenarioData.setActions(new Action(Scenario.CLICK_ZOOM_BUTTON)); }
		 **/
		if (!solo.getCurrentViews(CheckBox.class).isEmpty()) {
			scenarioData.setActions(new Action(Scenario.CHECK_BOX));
		}
		if (!solo.getCurrentViews(ListView.class).isEmpty()) {
			scenarioData.setActions(new Action(Scenario.CLICK_LIST));
		}
		if (!solo.getCurrentViews(ImageView.class).isEmpty()) {
			scenarioData.setActions(new Action(Scenario.CLICK_IMAGE));
		}
		if (!solo.getCurrentViews(ImageButton.class).isEmpty()) {
			scenarioData.setActions(new Action(Scenario.CLICK_IMAGE));
		}
		if (!solo.getCurrentViews(TextView.class).isEmpty()) {
			scenarioData.setActions(new Action(Scenario.CLICK_TEXT));
		}
		scenarioData.setActions(new Action(Scenario.MENU));

	}

	@SuppressLint("NewApi")
	public static State addViewsIntoState(State state, View currentViewTocross,
			Context context) {
		try {
			/*
			 * handle EditText
			 */
			String posX;
			String posY;
			String isDirty = "";
			try {
				posX = (currentViewTocross.getX() == 0) ? "" : ""
						+ currentViewTocross.getX();
				posY = (currentViewTocross.getY() == 0) ? "" : ""
						+ currentViewTocross.getY();
				isDirty = "" + currentViewTocross.isDirty();
			} catch (Error e) {
				posX = "";
				posY = "";
				e.printStackTrace();
			} catch (Exception ex) {
				posX = "";
				posY = "";
				ex.printStackTrace();
			}
			String isLongClickable = "" + currentViewTocross.isLongClickable();
			String isClickable = "" + currentViewTocross.isClickable();

			String isFocusable = "" + currentViewTocross.isFocusable();
			String isFocus = "" + currentViewTocross.isFocused();
			String isInEditMode = "" + currentViewTocross.isInEditMode();
			String isVerticalScrollBarEnabled = ""
					+ currentViewTocross.isVerticalScrollBarEnabled();

			/**
			 * widgets properties
			 */
			int id = currentViewTocross.getId();
			String name = context.getResources().getResourceName(id);
			/**
			 * visible or not 0 8 4
			 */
			String visibility = "" + currentViewTocross.getVisibility();
			String statusEnable = "" + currentViewTocross.isEnabled();
			String statusPressed = "" + currentViewTocross.isPressed();
			String statusShown = "" + currentViewTocross.isShown();
			String statusSelection = "" + currentViewTocross.isSelected();

			/**
			 * Widget wig;
			 */
			String type = null;

			if (isEditText(currentViewTocross)) {
				type = EDITTEXT;
				Widget editText = new Widget(name, type, posX, posY,
						visibility, statusEnable, statusPressed, statusShown,
						statusSelection, EditText.class
								.cast(currentViewTocross).getText().toString(),
						isLongClickable, isClickable, isDirty, isFocusable,
						isFocus, isInEditMode, isVerticalScrollBarEnabled);

				editText.setInputType(""
						+ (EditText.class.cast(currentViewTocross)
								.getInputType()));
				return setted_state(state, editText);
			}

			/*
			 * handle ImageView
			 */
			if (isImageView(currentViewTocross)) {
				type = IMAGE;
				return setted_state(state, name, type, visibility,
						statusEnable, statusPressed, statusShown,
						statusSelection,
						""
								+ ImageView.class.cast(currentViewTocross)
										.getDrawingCacheBackgroundColor(),
						posX, posY, isLongClickable, isClickable, isDirty,
						isFocusable, isFocus, isInEditMode,
						isVerticalScrollBarEnabled);
			}

			/*
			 * handle CheckBox
			 */
			if (isCheckBox(currentViewTocross)) {
				type = CHEKBOX;
				return setted_state(state, name, type, visibility,
						statusEnable,
						"" + ((CheckBox) currentViewTocross).isChecked(),
						statusShown, statusSelection,
						""
								+ CheckBox.class.cast(currentViewTocross)
										.getContentDescription(), posX, posY,
						isLongClickable, isClickable, isDirty, isFocusable,
						isFocus, isInEditMode, isVerticalScrollBarEnabled);
			}

			/*
			 * handle button
			 */
			if (isRadioButtonView(currentViewTocross)) {
				type = RADIO_BUTTON;
				return setted_state(state, name, type, visibility,
						statusEnable, statusPressed, statusShown,
						"" + ((RadioButton) currentViewTocross).isChecked(),
						RadioButton.class.cast(currentViewTocross).getText()
								.toString(), posX, posY, isLongClickable,
						isClickable, isDirty, isFocusable, isFocus,
						isInEditMode, isVerticalScrollBarEnabled);
			}

			/*
			 * handle button
			 */
			if (isButtonView(currentViewTocross)) {
				type = BUTTON;
				return setted_state(state, name, type, visibility,
						statusEnable, statusPressed, statusShown,
						statusSelection, Button.class.cast(currentViewTocross)
								.getText().toString(), posX, posY,
						isLongClickable, isClickable, isDirty, isFocusable,
						isFocus, isInEditMode, isVerticalScrollBarEnabled);
			}

			/*
			 * handle ListView
			 */
			if (isListView(currentViewTocross)) {
				type = LIST;
				return setted_state(state, name, type, visibility,
						statusEnable, statusPressed, statusShown,
						statusSelection, "", posX, posY, isLongClickable,
						isClickable, isDirty, isFocusable, isFocus,
						isInEditMode, isVerticalScrollBarEnabled);
			}
			/*
			 * handle TextView
			 */
			if (isTextView(currentViewTocross)) {
				type = TEXTVIEW;
				return setted_state(state, name, type, visibility,
						statusEnable, statusPressed, statusShown,
						statusSelection, TextView.class
								.cast(currentViewTocross).getText().toString(),
						posX, posY, isLongClickable, isClickable, isDirty,
						isFocusable, isFocus, isInEditMode,
						isVerticalScrollBarEnabled);
			}

			/**
			 * case of custom View
			 */
			if (isClickable(currentViewTocross)) {
				type = CLICKABLEVIEW;
				return setted_state(state, name, type, visibility,
						statusEnable, statusPressed, statusShown,
						statusSelection, "", posX, posY, isLongClickable,
						isClickable, isDirty, isFocusable, isFocus,
						isInEditMode, isVerticalScrollBarEnabled);
			}
			/**
			 * else
			 */
			type = currentViewTocross.getClass().getSimpleName();
			return setted_state(state, name, type, visibility, statusEnable,
					statusPressed, statusShown, statusSelection, "", posX,
					posY, isLongClickable, isClickable, isDirty, isFocusable,
					isFocus, isInEditMode, isVerticalScrollBarEnabled);

		} catch (Error e) {
			if (Config.DEBUG) {
				Log.e("this", e.getMessage());
			}
		}
		return state;
	}

	public static String getValue(View currentViewTocross, Context context) {
		try {

			if (isEditText(currentViewTocross)) {
				return EditText.class.cast(currentViewTocross).getText()
						.toString();

			}
			if (isImageView(currentViewTocross))
				return "";

			if (isCheckBox(currentViewTocross)) {
				return CheckBox.class.cast(currentViewTocross).getText()
						.toString();

			}

			if (isRadioButtonView(currentViewTocross)) {
				return RadioButton.class.cast(currentViewTocross).getText()
						.toString();
			}

			if (isButtonView(currentViewTocross)) {
				return Button.class.cast(currentViewTocross).getText()
						.toString();
			}
			if (isListView(currentViewTocross)) {
				return "";
			}
			/*
			 * handle TextView
			 */
			if (isTextView(currentViewTocross)) {
				return TextView.class.cast(currentViewTocross).getText()
						.toString();
			}

		} catch (Error e) {
			if (Config.DEBUG) {
				Log.e("this", e.getMessage());
			}
		}
		return "";
	}

	/**
	 * Set the widget Property
	 * 
	 * @param state
	 * @param name
	 * @param type
	 * @param visibility
	 * @param statusEnable
	 * @param statusPressed
	 * @param statusShown
	 * @param statusSelection
	 * @param value
	 * @param posX
	 * @param posY
	 * @param isLongClickable
	 * @param isClickable
	 * @param isDirty
	 * @param isFocusable
	 * @param isFocus
	 * @param isInEditMode
	 * @param isVerticalScrollBarEnabled
	 * @return
	 */
	private static State setted_state(State state, String name, String type,
			String visibility, String statusEnable, String statusPressed,
			String statusShown, String statusSelection, String value,
			String posX, String posY, String isLongClickable,
			String isClickable, String isDirty, String isFocusable,
			String isFocus, String isInEditMode,
			String isVerticalScrollBarEnabled) {
		if (type != null) {
			state.setWidget(new Widget(name, type, posX, posY, visibility,
					statusEnable, statusPressed, statusShown, statusSelection,
					value, isLongClickable, isClickable, isDirty, isFocusable,
					isFocus, isInEditMode, isVerticalScrollBarEnabled));

		}
		return state;
	}

	/**
	 * 
	 * @paramS State state
	 * @param wig
	 *            to add into the State "state"
	 * @return
	 */
	private static State setted_state(State state, Widget wig) {
		state.setWidget(wig);
		return state;
	}

	public static ScenarioData addNewState(Activity mainActivity,
			ScenarioData scenarioData, ArrayList<View> current_views,
			Context context, String id) {
		State st = new State(mainActivity.getClass().getSimpleName(), id);
		if (!existInScenario(st, scenarioData)) {
			addWidgetsInState(st, current_views, context);
			scenarioData.setStates(st, false, false);
		}
		return scenarioData;
	}

	private static boolean existInScenario(State state,
			ScenarioData scenarioData) {
		for (State st : scenarioData.getStates()) {
			if (st.getName().equalsIgnoreCase(state.getName())) {
				return true;
			}
		}
		return false;
	}

	public static State addWidgetsInState(State state,
			ArrayList<View> currentViews, Context context) {

		for (int i = 0; i < currentViews.size(); i++) {
			View currentViewTocross = currentViews.get(i);
			try {
				state = addViewsIntoState(state, currentViewTocross, context);
			} catch (android.content.res.Resources.NotFoundException ns) {
				continue;
			}
		}
		return state;
	}

	public static ScenarioData setDefaultEndState(ScenarioData scenarioData) {
		State end = new State(Scenario.END, "0");
		end.setType(Scenario.DEST);
		scenarioData.setStates(end, false, true);
		return scenarioData;
	}

	public static boolean saveScenario(ScenarioGenerator scenarioGenerator,
			ScenarioData scenarioData) {
		return scenarioGenerator.generateXml(scenarioData);
	}

	public static ScenarioData endScenarioData(ScenarioData scenarioData) {
		if (scenarioData.getTransitions() == null
				|| scenarioData.getTransitions().isEmpty()) {
			return scenarioData;
		}
		Transition tr = scenarioData.getTransitions().get(
				scenarioData.getTransitions().size() - 1);
		scenarioData.getTransitions()
				.get(scenarioData.getTransitions().size() - 1)
				.setDest(new State(Scenario.END, false, true, tr.getId()));
		return scenarioData;
	}

	public static boolean isValidTransitonIn(ScenarioData scenario,
			Transition tr) {
		if (scenario == null) {
			throw new NullPointerException("ScenarioData is null");
		}
		for (Transition pathTransition : scenario.getTransitions()) {
			/**
			 * cas où les points de départs sont les même
			 */
			if (haveEndDest_and_SameSource(pathTransition, tr)) {
				// Check the actions
				// Empty action
				if (!isEqualState(scenario.getState(pathTransition.getId(),
						Scenario.SOURCE), scenario.getState(tr.getId(),
						Scenario.SOURCE))) {
					if (Config.DEBUG) {
						Log.d(TAG, "isValidTransitonIn : different source id ");
					}
					return true;
				}
				if (!transition_actions_is_empty(pathTransition)) {
					return compareEachActionInsideTransition(pathTransition, tr);
				}

			}
		}
		return true;
	}

	/**
	 * 
	 * @param pathTransition
	 * @param tr
	 * @return true if we don't have the same action otherWise return false
	 */
	private static boolean compareEachActionInsideTransition(
			Transition pathTransition, Transition tr) {
		if (!hasSameNumberOfAction(pathTransition.getAction(), tr.getAction())) {
			if (Config.DEBUG) {
				Log.d(TAG,
						"isValidTransition compareEachActionInsideTransition + Do not have the same number of action");
			}
			return true;
		}
		if (hasSameValue(pathTransition.getAction(), tr.getAction())) {
			if (Config.DEBUG) {
				Log.d(TAG,
						"isValidTransition compareEachActionInsideTransition +   Have the same value of action");
				Log.d(TAG, pathTransition.toString() + "  vs  " + tr.toString());

			}

			return false;
		}

		if (Config.DEBUG) {
			Log.d(TAG,
					"isValidTransition compareEachActionInsideTransition + do not Have the same value of action");
		}
		return true;
	}

	public static boolean hasSameValue(ArrayList<Action> actionPath,
			ArrayList<Action> action) {
		return isEqualActions(actionPath, action);
	}

	private static boolean hasSameNumberOfAction(ArrayList<Action> actionPath,
			ArrayList<Action> action) {
		return actionPath.size() == action.size();
	}

	private static boolean action_valueIsEqual(Action actTr, Action act) {

		if (!actTr.getName().equals(act.getName()))
			return false;
		if (!actTr.getWidget().getName()
				.equalsIgnoreCase(act.getWidget().getName()))
			return false;
		/**
		 * non editext widget
		 */
		if (actTr.getWidget().getValue() == null
				&& act.getWidget().getValue() == null) {
			return true;
		}
		/**
		 * editext widget ou textView
		 */
		if (Config.DEBUG) {
			System.out.println("1 " + actTr.getWidget().getValue());
			System.out.println("2 " + act.getWidget().getValue());
		}

		if (!actTr.getWidget().getValue()
				.equalsIgnoreCase(act.getWidget().getValue())) {
			return false;
		}

		return true;
	}

	private static boolean transition_actions_is_empty(Transition pathTransition) {
		return pathTransition.getAction().isEmpty();
	}

	private static boolean haveEndDest_and_SameSource(
			Transition pathTransition, Transition tr) {
		return pathTransition.getDest().getName()
				.equalsIgnoreCase(Scenario.END)
				&& pathTransition.getSource().getName()
						.equalsIgnoreCase(tr.getSource().getName());
	}

	public static String get_a_transition_id(ScenarioData tree, String taskID) {
		tree.setTaskId(taskID);

		return get_a_transition_id(tree);

	}

	public static String get_a_transition_id(ScenarioData tree) {
		if (Config.DEBUG) {
			System.err.println("get_a_transition_Id ");
		}
		/**
		 * get the task id
		 */
		String taskId = tree.getTaskId();
		if (taskId == null) {
			if (Config.DEBUG) {
				Log.e(TAG, "get_a_transition_Id ");
				Log.e(TAG, "null ");
			}
			taskId = "";
		}
		/**
		 * prendre les id des transitions
		 */
		HashSet<String> idLis = new HashSet<String>();
		for (Transition tr : tree.getTransitions()) {
			String id = tr.getId();
			/**
			 * prendre le dernier element apres Scenario.IDSEPARATOR
			 */
			StringTokenizer st = new StringTokenizer(id, Scenario.IDSEPARATOR);
			String value;
			if (!st.hasMoreTokens()) {
				value = id;
			}
			do {
				value = st.nextToken();

			} while (st.hasMoreTokens());

			idLis.add(value);
		}
		String id = generateId(idLis);
		// System.out.println("id generate:  " + id);

		return taskId + Scenario.IDSEPARATOR + id;
	}

	public static String get_a_tree_Id(ScenarioData mScenarioData,
			String tree_id) {
		/**
		 * prendre les id des transitions
		 */
		HashSet<String> idLis = new HashSet<String>();
		for (Tree tr : mScenarioData.getTrees()) {
			if (tr != null) {
				String id = tr.getId();
				StringTokenizer stk = new StringTokenizer(id, ",");
				String value = id;
				while (stk.hasMoreTokens()) {
					value = stk.nextToken();
				}
				idLis.add(value);
			}
		}
		String id = generateId(idLis);
		return (tree_id.equalsIgnoreCase("")) ? id : tree_id + "," + id;
	}

	public static String generateId(HashSet<String> ids) {

		if (ids.size() == 0) {
			return "" + 0;
		} else {
			// System.out.println("list of ids:  " + ids.toString());
			List<Double> ids_int = new ArrayList<Double>();
			for (String id : ids) {
				ids_int.add(Double.parseDouble(id));
			}

			int idgenerate = (int) (getMax(ids_int) + 1);
			return "" + idgenerate;
		}
	}

	public static Double getMax(List<Double> ids_int) {
		Double max = Double.MIN_VALUE;
		for (int i = 0; i < ids_int.size(); i++) {
			if (ids_int.get(i) > max) {
				max = ids_int.get(i);
			}
		}
		return max;
	}

	/**
	 * 
	 * @param mScenarioData
	 * @param currentViews
	 * @param mContext
	 * @param mainActivity
	 * @return
	 */
	public static boolean isInitialState(ScenarioData scen,
			ArrayList<View> currentViews, Context ctx, Activity mainActivity) {
		/**
		 * comparer le nom
		 */
		State initial = scen.getInitialState();
		return isEqualState(currentViews, ctx, mainActivity, initial);
	}

	public static boolean isEqualState(ArrayList<View> currentViews,
			Context ctx, Activity currentActivity, State st) {

		String name = currentActivity.getClass().getName();
		String shortName = currentActivity.getClass().getSimpleName();
		if (!st.getName().equalsIgnoreCase(name)) {
			if (!st.getName().equalsIgnoreCase(shortName))
				return false;
		}

		State temp = new State(name, "temp");
		/**
		 * comparer l'etat des widgets
		 */
		addWidgetsInState(temp, currentViews, ctx);
		boolean isContained = false;
		for (Widget wig : temp.getWidgets()) {
			/**
			 * comparable n'est pas tenue en compte (à faire element par
			 * element.
			 */
			for (Widget stWidget : st.getWidgets()) {
				if (stWidget.isEqualTo(wig)) {
					isContained = true;
				}
				if (isContained) {
					break;
				}

			}
			if (isContained) {
				isContained = false;
				continue;
			} else {
				return false;
			}
		}
		/**
		 * comparer l'environnemnt utilisateur
		 */
		return true;
	}

	public static boolean isTransitionExist(String id,
			ScenarioData mScenarioData) {
		for (Transition tr : mScenarioData.getTransitions()) {
			if (tr.getId() == id) {
				return true;
			}
		}
		return false;
	}

	public static State go_to_last_state(ScenarioData mScenarioData) {

		return null;

	}

	public static String getTreeIdOfTransition(ScenarioData scen,
			String transitionID) {
		if (scen == null) {
			return null;

		}
		for (Tree tree : scen.getTrees()) {
			for (Transition tr : tree.getTransitions()) {
				if (tr == null || tr.getId() == null || transitionID == null) {
					continue;
				}
				if (tr.getId().equalsIgnoreCase(transitionID)) {
					return tree.getId();
				}
			}

		}
		return null;
	}

	/**
	 * get the lis of new state between twoo trees
	 * 
	 * @param scen
	 *            : tree 1
	 * @param path
	 *            : tree 2
	 * @return
	 */
	public static HashSet<State> getNewStates(ScenarioData scen,
			ScenarioData path) {
		if (scen == null || path == null) {
			return null;
		}
		if (Config.DEBUG) {
			System.out.println("path states :");
			for (State st : scen.getStates())
				System.out.print(st.toString());

			System.out.println("scen states :");
			for (State st : path.getStates())
				System.out.print(st.toString());
		}

		HashSet<State> states = new HashSet<State>(path.getStates());
		Iterator<State> it = states.iterator();
		do {
			State currentPathstate = it.next();
			for (State st : scen.getStates()) {
				if (isEqualState(currentPathstate, st)) {
					it.remove();
					break;
				}

			}

		} while (it.hasNext());
		return states;
	}

	/**
	 * Generate a Path leading to a states
	 * 
	 * @param _New_state_toCrawl
	 * @param scen
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public static ScenarioData _getPath(State new_state_toCrawl,
			ScenarioData scen) throws CloneNotSupportedException {
		try {
			ScenarioData new_path = get_init_Tree(scen);
			State init_state = scen.getInitialState();
			System.out.println("initial state  " + init_state);

			/**
			 * solve transitions
			 */
			// path.addStates(scen.getInitialState());
			State init = new_state_toCrawl.clone();
			State finalstate = null;
			if (scen.getTransitions() == null
					|| scen.getTransitions().isEmpty()) {
				return scen;
			}
			boolean reached = false;
			do {
				Transition tr = null;
				if (init != null) {
					tr = get_input_transition(init, scen.getTransitions());
				} else {
					break;
				}
				if (tr == null) {
					new_path = null;
					break;
				}
				finalstate = getSource(tr.getId(), scen);// tr.getSource();
				if (finalstate == null) {
					new_path = null;
					break;

				}
				try {
					if (isEqualState(finalstate, init_state)) {
						new_path.addStates(init.clone());
						finalstate.setInit(true);
						tr.setSource(finalstate.clone());
						new_path.addStates(finalstate.clone());
						new_path.setTransitions(tr, true);
						reached = true;
						break;
					}
				} catch (NullPointerException nul) {
					nul.printStackTrace();
					System.err.println("initial state null:  "
							+ ((init == null) ? "null" : init.getName())
							+ "   ");
					break;
				}
				new_path.addStates(init.clone());
				new_path.setTransitions(tr.clone(), true);
				new_path.addStates(finalstate.clone());
				init = get_dest_state_equivalent(finalstate, scen.getStates());
			} while (!reached);
			/**
			 * gerer le cas où l'etat initial decrit n'est pas contenu dans une
			 * transition
			 */
			return new_path;
		} catch (NumberFormatException e) {
			/**
			 * les cas non corrigés
			 */
			return getConcretePath(new_state_toCrawl, scen);
		}
	}

	/**
	 * ex: 0_1 0_1_2 0_1 _2_4
	 * 
	 * @param new_state_toCrawl
	 * @param scen
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public static ScenarioData getConcretePath(State new_state_toCrawl,
			ScenarioData scen) throws CloneNotSupportedException {
		/**
		 * solve transitions
		 */
		if (scen.getTransitions() == null || scen.getTransitions().isEmpty()) {
			return scen;
		}
		if (new_state_toCrawl == null) {
			System.err.println("State to reach dos not exist");
			return null;
		}

		// path.addStates(scen.getInitialState());
		State state_to_reach = new_state_toCrawl.clone();
		ArrayList<String> path = solveReachability(state_to_reach, scen);
		if (path == null || path.isEmpty()) {
			System.err.println("State cannot be reached");
			return null;
		}
		ScenarioData new_path = get_init_Tree(scen, path.get(0));
		if (new_path == null) {
			System.err.println("State cannot be created");
			return null;
		}
		String target_id = path.get(0);
		if (target_id.equalsIgnoreCase(scen.getInitialState().getId())) {
			// return new_path;
		} else {
			new_path.remove_states(scen.getInitialState().getId());
		}

		/**
		 * list of initial state s0_x ?
		 */
		for (String pathId : path) {
			Transition curTransition = scen.getTransitions(pathId);
			if (curTransition == null) {
				System.err.println("intermediate transition  does not exist");
				return null;
			}
			new_path.addStates(curTransition.getSource().clone());
			new_path.addStates(curTransition.getDest().clone());
			new_path.set_unique_Transitions(curTransition);
		}
		for (String stateId : new_path.getStateIds()) {
			if (!new_path.getTransitionsId().contains(stateId)) {
				new_path.remove_states(stateId);
			}
		}

		return new_path;
	}

	private static ArrayList<String> solveReachability(State state_to_reach,
			ScenarioData all) {
		ArrayList<String> transitionid = new ArrayList<String>();
		ArrayList<String> allTr = all.getTransitionsId();
		transitionid = get_transition_id_from_state_id_(state_to_reach.getId());
		if (!isIn(transitionid, allTr)) {
			transitionid.clear();
		}

		return transitionid;
	}

	/**
	 * 
	 * @param transitionid
	 *            the tid to check
	 * @param allTr
	 *            all tr available in the sceanrio
	 * @return
	 */

	public static boolean isIn(ArrayList<String> transitionid,
			ArrayList<String> allTr) {
		for (String tr : transitionid) {
			if (!allTr.contains(tr))
				return false;

		}
		return true;

	}

	public static ArrayList<String> get_transition_id_from_state_id_(String idg) {

		StringTokenizer st = new StringTokenizer(idg, Scenario.IDSEPARATOR);
		int tr_number = st.countTokens() - 1;
		ArrayList<String> value = new ArrayList<String>(tr_number);
		String id_value = ((st.hasMoreTokens()) ? st.nextToken() : "");
		if (tr_number == 0) {
			value.add(id_value);
		}
		if (tr_number == 1) {
			value.add(id_value + Scenario.IDSEPARATOR + st.nextToken());
			// return value;
		}
		if (tr_number > 1) {
			do {
				id_value = id_value + Scenario.IDSEPARATOR + st.nextToken();
				value.add(id_value);
			} while (st.hasMoreElements());
		}
		for (String log : value) {
			System.out.println(log);
		}

		return value;
	}

	/**
	 * @param scen
	 * @return
	 */
	public static ScenarioData get_init_Tree(ScenarioData scen) {
		ScenarioData path = new ScenarioData();
		path.setAuthors(scen.getAuthors());
		path.setActions(scen.getActions());
		path.setVersion(scen.getVersion());
		State init_state = scen.getInitialState();
		init_state.setInit(true);
		try {
			path.addStates(init_state.clone());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return path;
	}

	/**
	 * impose an initial state
	 * 
	 * @param scen
	 * @param id
	 * @return
	 */

	private static ScenarioData get_init_Tree(ScenarioData scen, String id) {
		ScenarioData scen_to_build = get_init_Tree(scen);
		State init_state = scen.getState(id, Scenario.SOURCE);
		if (init_state == null) {
			return null;
		}
		init_state.setInit(true);
		try {
			scen_to_build.addStates(init_state.clone());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return scen_to_build;

	}

	/**
	 * @param id
	 * @param scen
	 * @return
	 */
	private static State getSource(String id, ScenarioData scen) {
		/*
		 * //for (State st : scen.getStates()) { // if
		 * (st.getId().equalsIgnoreCase(id) &&
		 * (st.getType().equalsIgnoreCase(Scenario.SRC) ||
		 * st.getType().equalsIgnoreCase( Scenario.SOURCE))) return st; }
		 */
		return scen.getState(id, Scenario.SOURCE);
	}

	/**
	 * 
	 * Get the state (type =dest) of the same state (type=source)
	 * 
	 * @param finalstate
	 * @param states
	 * @return
	 * @throws CloneNotSupportedException
	 */
	private static State get_dest_state_equivalent(State finalstate,
			HashSet<State> states) throws CloneNotSupportedException {
		finalstate.setType(Scenario.DEST);
		ArrayList<Integer> id = new ArrayList<Integer>();
		HashSet<State> same_states = new HashSet<State>();
		for (State st : states) {
			int idd = Integer.parseInt(st.getId());
			if (st.getType().equalsIgnoreCase(Scenario.DEST)
					&& !id.contains(idd) && isEqualState(st, finalstate)) {
				id.add(idd);
				same_states.add(st.clone());
			}

		}
		for (State st : same_states) {
			if ((is_closest_min_id(st.getId(), finalstate.getId(), id))) {
				finalstate.setType(Scenario.SOURCE);
				return st;
			}
		}
		finalstate.setType(Scenario.SOURCE);
		return null;
	}

	public static boolean isEqualState(State st1, State st2) {

		/**
		 * comparer les noms des activités
		 */
		if (!isEqualName(st1, st2)) {
			// System.out.println("Name is not Equal :" + st1.getName() + "  " +
			// st2.getName());
			return false;
		}
		/**
		 * comparer les proprietés des widgets
		 */
		if (st1.getWidgets().size() > st2.getWidgets().size()) {
			if (!isEqualWidget(st2, st1)) {
				return false;
			}
		} else {
			if (!isEqualWidget(st1, st2)) {
				return false;
			}
		}

		/**
		 * comparer l'environnemnt utilisateur
		 */
		if (!isEqualUserEnvironment(st1, st2)) {
			System.out.println("User environment is not Equal");
			return false;
		}

		return true;
	}

	public static boolean isStrictEqualState(State st1, State st2) {

		/**
		 * comparer les noms des activités
		 */
		if (!isEqualName(st1, st2)) {
			return false;
		}
		/**
		 * comparer les proprietés des widgets
		 */
		if (st1.getWidgets().size() > st2.getWidgets().size()) {
			if (!isEqualWidget(st2, st1)) {
				return false;
			}
		} else {
			if (!isEqualWidget(st1, st2)) {
				return false;
			}
		}
		/**
		 * comparer l'environnemnt utilisateur
		 */
		if (!isEqualUserEnvironment(st1, st2)) {
			System.out.println("User environment is not Equal");
			return false;
		}
		return true;
	}

	/**
	 * @param st1
	 * @param st2
	 * @return
	 */
	private static boolean isEqualUserEnvironment(State st1, State st2) {
		if (st1.get_user_environment() != (st2.get_user_environment())) {
			System.out.println("Global ue is not Equal");
			return false;
		}
		/**
		 * comparer chaque environnment utilisateur
		 */
		if (!isEqualUserEnvironments(st1.getUserEnvironments(),
				st2.getUserEnvironments())) {
			System.out.println("an environment is not Equal");
			return false;
		}
		return true;
	}

	private static boolean isEqualUserEnvironments(
			ArrayList<UserEnvironment> userEnvironments,
			ArrayList<UserEnvironment> userEnvironments2) {
		/**
		 * comparer chaque environment utilisateur CP
		 */
		if (userEnvironments.size() != userEnvironments2.size()) {
			return false;
		}
		boolean isContained = false;
		for (UserEnvironment ue1 : userEnvironments) {
			System.out.println(ue1);
			System.out.println(" vs ");
			for (UserEnvironment ue2 : userEnvironments2) {
				System.out.println(ue2);
				if (ue1.isEqualTo(ue2)) {
					isContained = true;
				}
				if (isContained) {
					break;
				}
			}
			System.out.println(" = " + isContained);
			if (isContained) {
				isContained = false;
				continue;
			} else {
				System.out.println(ue1.getName() + " differ ");
				return false;
			}
		}

		/**
		 * comparer chaque environment utilisateur Broadcast
		 */
		return true;
	}

	/**
	 * Ideally number of widget of st1 abd st2 must equal otherwise
	 * 
	 * @param st1
	 * 
	 * @param st2
	 * @return
	 */
	private static boolean isEqualWidget(State st1, State st2) {
		boolean isContained = false;
		/**
		 * nombre de widget facultatif
		 */

		if (st1.getWidgets().size() != st2.getWidgets().size()) { //
			System.out.println("  widget size: " + st1.getWidgets().size()
					+ "  not Equal to " + st2.getWidgets().size());
			System.out.println("  widget size is not equal ");
			return false;
		}

		for (Widget wig : st1.getWidgets()) {
			/**
			 * comparable n'est pas tenue en compte (à faire element par
			 * element.
			 */
			// System.out.println("wig _ to _ check :" + wig.toString());
			for (Widget stWidget : st2.getWidgets()) {

				/*
				 * if (Config.DEBUG) { System.out.println("isEqualWidget :");
				 * System.out.println("w1 :" + stWidget.toString());
				 * System.out.println("w2 :" + wig.toString()); }
				 */
				if (stWidget.isEqualTo(wig)) {
					isContained = true;
				}
				if (isContained) {
					// System.out.println("not Contained :");
					break;
				}

			}
			if (isContained) {
				isContained = false;
				continue;
			} else {
				// System.out.println("not included widget :" + wig.toString());
				return false;
			}
		}
		return true;
	}

	/**
	 * Compare all value
	 * 
	 * @param st1
	 * @param st2
	 * @return
	 */

	public static boolean isStrictEqualWidget(final State st1, final State st2) {
		boolean isContained = false;
		/**
		 * nombre de widget facultatif
		 */

		if (st1.getWidgets().size() != st2.getWidgets().size()) { //
			System.out.println("  widget size: " + st1.getWidgets().size()
					+ "  not Equal to " + st2.getWidgets().size());
			System.out.println("  widget size is not equal ");
			return false;
		}

		for (Widget wig : st1.getWidgets()) {
			/**
			 * comparable n'est pas tenue en compte (à faire element par
			 * element.
			 */
			// System.out.println("wig _ to _ check :" + wig.toString());
			for (Widget stWidget : st2.getWidgets()) {

				/*
				 * if (Config.DEBUG) { System.out.println("isEqualWidget :");
				 * System.out.println("w1 :" + stWidget.toString());
				 * System.out.println("w2 :" + wig.toString()); }
				 */
				if (stWidget.isStrictEqualTo(wig)) {
					isContained = true;
				}
				if (isContained) {
					// System.out.println("not Contained :");
					break;
				}

			}
			if (isContained) {
				isContained = false;
				continue;
			} else {
				// System.out.println("not included widget :" + wig.toString());
				return false;
			}
		}
		return true;
	}

	/**
	 * @param st1
	 * @param st2
	 * @return
	 */
	private static boolean isEqualName(State st1, State st2) {
		String name1 = st1.getName();
		String name2 = st2.getName();
		if (!name1.equalsIgnoreCase(name2)) {
			// System.out.println(name1 + "  not Equal to " + name2);
			return false;
		}
		return true;
	}

	/**
	 * @param init
	 * @param transitions
	 * @return
	 */
	private static Transition get_input_transition(State dest,
			ArrayList<Transition> transitions) {
		System.out.println("get_input_transition of dest: " + dest.toString());
		for (Transition tr : transitions) {
			if (tr != null && tr.getId().equals(dest.getId())) {
				if (tr.getDest().getName().equalsIgnoreCase(Scenario.END)) {
					dest.setInit(false, true);
					tr.setDest(dest);
				}
				if (Config.DEBUG) {
					System.out.println("value :" + tr.toString());
					System.out.println("is  dest InitFinal: " + dest.isFinal());
				}

				return tr;
			}
		}
		return null;
	}

	/**
	 * get the last state of a path
	 * 
	 * t1_2_3_4_5 1_2_3_4_5
	 * 
	 * @param path
	 * @return
	 */
	public static State get_last_state(ScenarioData path) {
		State dest = null;
		if (path == null || path.getTransitions().isEmpty()) {
			return dest;
		}
		try {

			State inital_state = path.getInitialState(true);

			do {
				dest = get_dest(inital_state, path);
				inital_state = get_source_state_equivalent(dest,
						path.getStates());
			} while (inital_state != null);
		} catch (NumberFormatException ne) {
			/***
			 * t1_2_3_4_5 1_2_3_4_5
			 */
			dest.setType(Scenario.DEST);
			dest = path.getState(get_highest_id(path.getTransitionsId()),
					Scenario.DEST);

		}
		if (Config.DEBUG) {
			System.out.println(TAG + "  LAST STATE ");
			if (dest != null) {

				System.out.println((TAG + "  " + dest.toString()));
			}

		}

		return dest;
	}

	public static String get_highest_id(ArrayList<String> transitionsId) {
		Iterator<String> idIterator = transitionsId.iterator();

		String highest = idIterator.next();
		if (!idIterator.hasNext()) {
			return highest;
		}
		String curent;
		do {
			curent = idIterator.next();
			if (highest.length() < curent.length()) {
				highest = curent;
			}
		} while (idIterator.hasNext());
		return highest;
	}

	/**
	 * get the dest of a source state in a path
	 * 
	 * @param inital_state
	 * @param scen
	 * @return
	 */
	public static State get_dest(State inital_state, ScenarioData scen) {
		if (inital_state == null) {
			return null;
		}
		Transition tr = scen.getTransitions(inital_state.getId());
		if (tr == null) {
			return null;
		}
		/**
		 * tr exist
		 */
		String dest_name = tr.getDest().getName();
		State dest = scen.getState(inital_state.getId(), Scenario.DEST,
				dest_name);
		return dest;
	}

	private static State get_source_state_equivalent(State dest,
			HashSet<State> states) {
		if (dest == null) {
			return null;
		}
		dest.setType(Scenario.SOURCE);
		ArrayList<Integer> state_id = new ArrayList<Integer>();
		for (State st : states) {
			if (isEqualState(st, dest))
				state_id.add(Integer.parseInt(st.getId()));
		}

		for (State st : states) {
			if (isEqualState(st, dest)
					&& is_closest_max_id(st.getId(), dest.getId(), state_id)) {
				dest.setType(Scenario.DEST);
				return st;
			}
		}
		dest.setType(Scenario.DEST);
		return null;
	}

	public static boolean is_closest_max_id(String id, String id2,
			ArrayList<Integer> state_id) {
		return is_closest_max_id(Integer.parseInt(id), Integer.parseInt(id2),
				state_id);
	}

	public static boolean is_closest_max_id(int id1, int id2,
			ArrayList<Integer> state_id) {
		return (id1 == get_closest_pred_sup(id2, state_id));
	}

	public static boolean is_closest_min_id(String id, String id2,
			ArrayList<Integer> state_id) {
		return is_closest_min_id(Integer.parseInt(id), Integer.parseInt(id2),
				state_id);
	}

	public static boolean is_closest_min_id(int id1, int id2,
			ArrayList<Integer> state_id) {
		return (id1 == get_closest_pred_inf(id2, state_id));
	}

	public static int get_closest_pred_inf(int value, List<Integer> list) {
		ArrayList<Integer> inf = new ArrayList<Integer>();
		for (int i : list) {
			if (i < value) {
				inf.add(i);
			}
		}
		Collections.sort(inf);
		if (!inf.isEmpty()) {
			return inf.get(inf.size() - 1);
		}
		return Integer.MAX_VALUE;

	}

	public static int get_closest_pred_sup(int value, List<Integer> list) {
		ArrayList<Integer> sup_list = new ArrayList<Integer>();
		for (int i : list) {
			if (i > value) {
				sup_list.add(i);
			}
		}
		Collections.sort(sup_list);
		if (!sup_list.isEmpty()) {
			return sup_list.get(0);
		}
		return Integer.MAX_VALUE;
	}

	/**
	 * get the sequence of edit text value in a transition
	 * 
	 * @param tr
	 * @return
	 */
	public static HashMap<String, String> get_ediText_sequence(Transition tr) {
		HashMap<String, String> seq = new HashMap<String, String>();
		for (Action action : tr.getAction()) {
			Widget wig = action.getWidget();
			if (wig.getType().equalsIgnoreCase(EditText.class.getSimpleName())) {
				String value = wig.getValue();
				if (!value.equalsIgnoreCase(""))
					seq.put(wig.getName(), value);
				else {
					/**
					 * Problem de completion de la proprieté value editText non
					 * resolu
					 */
					seq.put(wig.getName(), wig.getPosX());
				}
			}

		}
		return seq;
	}

	/**
	 * get the sequence of edit text value in a transition
	 * 
	 * @param tr
	 * @return
	 */
	public static ArrayList<Event> get_ediText_List(State st) {
		ArrayList<Event> seq = new ArrayList<Event>();
		System.out.println();
		for (Widget wig : st.getWidgets()) {
			if (wig.getType().equalsIgnoreCase(EditText.class.getName())) {
				Event ev = new Event(wig.getName(), wig.getType(),
						wig.getValue());
				if (Config.DEBUG) {
					System.out.println("events: " + ev.toString());
				}
				seq.add(ev);
			}
		}
		return seq;
	}

	/**
	 * get the sequence of edit text value in a state
	 * 
	 * @param tr
	 * @return
	 */
	public static Hashtable<String, String> get_ediText_sequence(State st) {
		Hashtable<String, String> seq = new Hashtable<String, String>();
		for (Widget wig : st.getWidgets()) {
			if (wig.getType().equalsIgnoreCase(EditText.class.getName())) {
				seq.put(wig.getName(), wig.getValue());
			}

		}
		return seq;
	}

	/**
	 * get edit text sequence from a state
	 * 
	 * @param source
	 * @param scen
	 * @return
	 */

	public static HashMap<String, String> get_ediText_sequence(State source,
			ScenarioData scen) {
		Transition tr = scen.getTransitions(source.getId());
		if (tr != null) {
			return get_ediText_sequence(tr);
		}

		return null;
	}

	public static HashMap<String, String> get_event(State source,
			ScenarioData scen) {
		if (source == null) {
			if (Config.DEBUG) {
				System.err
						.println("get_event() :  " + " Current state is null");
			}
			return null;
		}
		if (Config.DEBUG) {
			System.err.println("get_event() :  "
					+ " Current state is  not null");
		}
		Transition tr = scen.getTransitions(source.getId());
		if (tr != null) {
			return get_event(tr, source);
		}
		return null;
	}

	/**
	 * add eventable action, button, menu.
	 * 
	 * @param tr
	 * @return
	 */
	public static HashMap<String, String> get_event(Transition tr, State source) {
		HashMap<String, String> event = new HashMap<String, String>();
		for (Action action : tr.getAction()) {
			Widget wig = action.getWidget();
			for (Widget sourceWig : source.getWidgets()) {
				if (wig.getName().equalsIgnoreCase(sourceWig.getName())) {
					wig = sourceWig;
					break;
				}
			}

			if (isEvent(wig)) {
				event.put(wig.getName(), action.getName());
			} else {
				if (Config.DEBUG) {
					System.out.println(TAG + " Not event :  " + wig.getName());
				}
			}
		}
		if (Config.DEBUG) {
			System.out.println(TAG + " Obtained event :  " + event);
		}
		return event;

	}

	public static HashMap<String, String> get_event(Action act) {
		HashMap<String, String> event = new HashMap<String, String>();
		Widget wig = act.getWidget();
		if (isEvent(wig)) {
			event.put(wig.getName(), act.getName());
		}
		return event;
	}

	/**
	 * @param wig
	 * @return
	 */
	private static boolean isEvent(Widget wig) {
		boolean isImage = wig.getType().equalsIgnoreCase(
				ImageView.class.getSimpleName());
		boolean isClickable = isClickable(wig)
				|| wig.getType().equalsIgnoreCase(Button.class.getSimpleName())
				|| wig.getType().equalsIgnoreCase(
						RadioButton.class.getSimpleName())
				|| wig.getType().equalsIgnoreCase(
						CheckBox.class.getSimpleName())
				|| wig.getType().equalsIgnoreCase(CLICKABLEVIEW)
				|| wig.getType()
						.equalsIgnoreCase(ClickableView.class.getName());
		boolean isMenu = wig.getType().equalsIgnoreCase(
				MenuView.class.getSimpleName());
		boolean isListView = wig.getType().equalsIgnoreCase(
				ListView.class.getSimpleName());
		boolean isSpinner = wig.getType().equalsIgnoreCase(
				Spinner.class.getSimpleName());
		return isClickable || isMenu || isListView || isSpinner || isImage;
	}

	public static boolean test_event(HashMap<String, String> event, Solo solo) {
		// solo.getCurrentViews(classToFilterBy)

		return false;

	}

	/**
	 * @param the_path
	 */
	public static ScenarioData solve(ScenarioData the_path) {
		/*
		 * if (the_path != null) the_path = cler_repeated_States(the_path);
		 */

		if (the_path == null || the_path.getTransitions().isEmpty()) {
			return the_path;
		}

		/**
		 * supprimer les doublons de transition
		 */
		the_path = clear_repeated_transitions(the_path);
		/**
		 * verifier s'il existe des etats finaux sans transition
		 */
		ArrayList<String> id = new ArrayList<String>();
		for (Transition tr : the_path.getTransitions()) {
			if (tr.getDest().getName().equalsIgnoreCase(Scenario.END)) {
				id.add(tr.getId());
			}
		}
		for (String st : id) {
			State dest = the_path.getState(st, Scenario.DEST);
			if (dest == null) {
				/**
				 * creer l'etat end
				 */
				dest = new State(Scenario.END, false, true, st);
				the_path.addStates(dest);
			}
			Transition tr = the_path.getTransitions(st);
			if (tr == null) {
				continue;
			}
			tr.setDest(dest);
			the_path.setTransitions(tr, true);
			String tree_id = the_path.getTreeId(tr);
			Tree tree = the_path.getTree(tree_id);
			tree.setTransitions(tr, true);
			the_path.setTrees(tree, true);
		}
		return the_path;
	}

	/**
	 * @param the_path
	 * @return
	 */
	@SuppressWarnings("unused")
	private static ScenarioData cler_repeated_States(ScenarioData the_path) {
		System.out.println();
		HashMap<String, String> id_type = new HashMap<String, String>();
		Iterator<State> traIt = the_path.getStates().iterator();
		while (traIt.hasNext()) {
			State temp = traIt.next();
			if (id_type.containsKey(temp.getId())
					&& id_type.get(temp.getId()).equalsIgnoreCase(
							temp.getType())) {
				traIt.remove();
			} else {
				id_type.put(temp.getId(), temp.getType());
			}
		}
		return the_path;
	}

	/**
	 * @param the_path
	 * @return
	 */
	public static ScenarioData clear_repeated_transitions(ScenarioData the_path) {
		System.out.println("remove duplicated transition");
		ArrayList<Transition> transition = the_path.getTransitions();
		Iterator<Transition> tr_iterator = transition.iterator();
		if (!tr_iterator.hasNext()) {
			return the_path;
		}
		ArrayList<Transition> clone_transition = new ArrayList<Transition>();
		for (Transition tran : transition) {
			try {
				clone_transition.add(tran.clone());
			} catch (CloneNotSupportedException e) {
				return the_path;
			}
		}

		for (Transition tran : clone_transition) {
			int i = 0;
			tr_iterator = transition.iterator();
			do {
				Transition tran_temp = tr_iterator.next();
				if (isEqualTransition(tran_temp, tran)) {
					i++;
				}
				if (i == 2) {
					tr_iterator.remove();
					i--;
				}
			} while (tr_iterator.hasNext());

		}
		/*
		 * ArrayList<String> id = new ArrayList<String>(); Iterator<Transition>
		 * traIt = the_path.getTransitions().iterator(); while (traIt.hasNext())
		 * { Transition temp = traIt.next(); if (id.contains(temp.getId())) {
		 * traIt.remove(); } else { id.add(temp.getId()); } }
		 */
		return the_path;
	}

	/**
	 * @param currentViewTocross
	 * @return
	 */
	public static boolean is_notClickableView(View currentViewTocross) {

		if (isButtonView(currentViewTocross) || isCheckBox(currentViewTocross)
				|| isListElementView(currentViewTocross)
				|| isMenu(currentViewTocross)
				|| isImageView(currentViewTocross)) {// ||

			return false;
		}
		if (isClickableTextView(currentViewTocross)) {
			return false;
		}
		if (currentViewTocross.isClickable())
			return false;

		return true;
	}

	private static boolean isClickableTextView(View currentViewTocross) {
		if (isTextView(currentViewTocross) && currentViewTocross.isClickable()) {
			return true;
		}
		return false;
	}

	public static State get_source_state(ScenarioData scen) {
		if (Config.DEBUG) {
			System.out.println(TAG + "  get_source_state ");
		}
		int current_min_size = 0;
		// String current_id;
		String tr_id = null;
		List<String> id_value = new ArrayList<String>();
		for (Transition tr : scen.getTransitions()) {
			tr_id = tr.getId();
			if (!id_value.contains((tr_id))) {
				id_value.add((tr_id));
				if (current_min_size == 0) {
					current_min_size = tr_id.length();
					// current_id = tr_id;
					continue;
				} else {
					if (tr_id.length() < current_min_size) {
						current_min_size = tr_id.length();
						// current_id = tr_id;
					}
				}

			}
		}
		return scen.getState(tr_id, Scenario.SOURCE);
	}

	public static String getname(Context mContext, View v) {
		try {
			String name = mContext.getResources().getResourceName(v.getId());
			return name;
		} catch (Exception ex) {
			return "";
		}

	}

	public static boolean isEqualTransition(Transition t1, Transition t2) {
		System.out.print("check if equal transition: ");
		State source1 = t1.getSource();
		State source2 = t2.getSource();
		State dest1 = t1.getDest();
		State dest2 = t2.getDest();
		ArrayList<kit.Scenario.Action> act1 = t1.getAction();
		ArrayList<kit.Scenario.Action> act2 = t2.getAction();
		if (!isStrictEqualState(source1, source2)) {
			System.out.println("false");
			return false;
		}
		System.out.println("check");
		if (!isStrictEqualState(dest1, dest2)) {
			System.out.println("false");
			return false;
		}

		// if
		// (!(dest1.isFinal()&&dest2.isFinal())||(!dest1.isFinal()&&!dest2.isFinal())){
		// System.out.println("false");
		// return false;
		// }

		if (!hasSameNumberOfAction(act1, act2)) {
			System.out.println("false");
			return false;
		}

		if (!hasSame_action_value(act1, act2)) {
			System.out.println("false");
			return false;
		}
		System.out.println("true");
		return true;
	}

	private static boolean hasSame_action_value(ArrayList<Action> act1,
			ArrayList<Action> act2) {
		Iterator<Action> actIterator = act1.iterator();
		Iterator<Action> actIterator2 = act2.iterator();

		if (!actIterator.hasNext() && !actIterator2.hasNext()) {
			return true;
		}
		if (!actIterator.hasNext() && actIterator2.hasNext()) {
			return false;
		}
		if (actIterator.hasNext() && !actIterator2.hasNext()) {
			return false;
		}

		/*
		 * do { Action tocheck = actIterator.next(); for (Action act : act2) {
		 * if (action_valueIsEqual(tocheck, act)) { actIterator.remove(); break;
		 * } } } while (actIterator.hasNext()); if (act1.isEmpty()) { return
		 * true; }
		 */
		/**
		 * 
		 */
		return isEqualActions(act1, act2);
	}

	/**
	 * @param v
	 * @return
	 */
	public static boolean isMenu(View currentViewTocross) {
		if (MenuView.class.isAssignableFrom(currentViewTocross.getClass())) {
			Log.d("CurrentView", "isMenu item view index : "
					+ ((MenuView) currentViewTocross).getIndex());
			return true;
		}
		return false;
	}

	/**
	 * @param v
	 * @return
	 */
	public static boolean isListViewElement(View currentViewTocross) {
		if (ListViewElement.class.isAssignableFrom(currentViewTocross
				.getClass())) {
			Log.d("CurrentView", "isListViewElement item view index : "
					+ ((ListViewElement) currentViewTocross).getIndex());
			return true;
		}
		return false;
	}

	public static int isLogginState(State st, ArrayList<String> semantic) {
		int weight = 0;
		if (st == null || semantic == null) {
			return weight;
		}
		ArrayList<String> state_semantic = getState_Semantic(st);
		weight = semanticMatching(state_semantic, semantic, weight);
		return weight;
	}

	private static int semanticMatching(ArrayList<String> state_semantic,
			ArrayList<String> semantic, int weight) {
		for (String to_check : semantic) {
			for (String state_value_toCheck : state_semantic) {
				if (state_value_toCheck.contains(to_check)) {
					weight = weight + 1;
				}
			}
		}
		return weight;
	}

	private static ArrayList<String> getState_Semantic(State st) {
		ArrayList<String> state_semantic = new ArrayList<String>();
		state_semantic.add(st.getName());
		state_semantic.add(st.getShortName());
		for (Widget wig : st.getWidgets()) {
			/**
			 * les elements de widgets
			 */
			state_semantic.add(wig.getName());
			state_semantic.add(wig.getValue());
			state_semantic.add(wig.getType());
			String inputType = wig.getInputType();
			if (inputType != null) {
				state_semantic.add(inputType);
			}
		}
		return state_semantic;
	}

	/**
	 * @param scen
	 * @param st
	 * @return
	 */
	public static boolean is_source_equivalent_exist(ScenarioData scen, State st) {
		return get_source_state_equivalent(st, scen.getStates()) != null;
	}

	/**
	 * @param crash2
	 * @param tr
	 * @return
	 */
	public static boolean will_be_a_crash_transition_or_repeated_transition(
			ScenarioData crash, Transition tr) {
		ArrayList<Transition> trs = crash.getTransitions();
		if (crash == null || trs == null || trs.isEmpty()) {
			return false;
		}
		/**
		 * verification de la source
		 */
		for (Transition in_crash : trs) {
			if (!isEqualState(in_crash.getSource(), tr.getSource())) {
				continue;
			}
			System.out.println("is equalStateSource");

			if (will_be_crashed(tr.getAction(), in_crash.getAction())) {
				System.out.println("will_be_crashed");
				System.out
						.println(tr.toString() + " vs " + in_crash.toString());
				return true;
			}
			System.out.println("transition to Compare  " + tr.toString()
					+ " vs " + in_crash.toString());

			if (isEqualActions(tr.getAction(), in_crash.getAction())) {
				System.out.println("isEqualActions");
				System.out
						.println(tr.toString() + " vs " + in_crash.toString());
				return true;
			}
		}

		return false;
	}

	private static boolean will_be_crashed(ArrayList<Action> action_list1,
			ArrayList<Action> action_list2) {

		if (contains_error(action_list1) || contains_error(action_list2)) {
			/**
			 * et même action
			 */

			System.out.println("has error");

			for (Action action_list1_element : action_list1) {
				if (!actionIn_without_Error(action_list1_element, action_list2)) {
					return false;
				}
			}
			return true;
		}

		return false;
	}

	/**
	 * @param action
	 * @param action_list2
	 * @param crash_status
	 *            : check if crash must be tested
	 * @return
	 */
	public static boolean isEqualActions(ArrayList<Action> action_list1,
			ArrayList<Action> action_list2) {

		for (Action action_list1_element : action_list1) {
			if (!actionIn(action_list1_element, action_list2)) {
				return false;
			}
		}
		return true;
	}

	private static boolean actionIn(Action act, ArrayList<Action> action_list2) {
		for (Action actTr : action_list2) {
			if (action_valueIsEqual(actTr, act)
					&& hasBothErrorStatus(act, actTr)) {
				return true;
			}
		}

		return false;
	}

	private static boolean actionIn_without_Error(Action act,
			ArrayList<Action> action_list2) {
		for (Action actTr : action_list2) {
			if (action_valueIsEqual(actTr, act)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasBothErrorStatus(Action act, Action actTr) {
		if (act.getError() == null && actTr.getError() == null) {
			return true;
		}

		if (act.getError() != null && actTr.getError() != null) {
			return true;
		}
		if (act.getError() == null || actTr.getError() == null) {
			return false;
		}
		if (act.getError().equalsIgnoreCase("")
				&& actTr.getError().equalsIgnoreCase("")) {
			return true;
		}

		return false;
	}

	/**
	 * @param action1
	 * @return
	 */
	public static boolean contains_error(ArrayList<Action> action) {
		for (Action act : action) {
			if (act.getName().equalsIgnoreCase(Scenario.ERROR)) {
				return true;
			}
			if (act.getError() != null && !act.getError().equalsIgnoreCase("")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param pairWiseSequence
	 * @param currentViewTocross
	 * @param tr_clone
	 */
	public static Transition build_transition_with_sequence(
			String pairWiseSequence, View currentViewTocross, Transition tr,
			Solo solo, Context ctx) {
		if (tr == null) {
			return tr;
		}
		if (pairWiseSequence != null) {
			ArrayList<String> value = sequenceToList(pairWiseSequence);
			ArrayList<EditText> edits = solo.getCurrentViews(EditText.class);
			for (EditText edit : edits) {
				System.out.println("Builded ediText action: ");
				String value_to_insert = value.get(edits.indexOf(edit));
				try {
					addTransitionAction(edit, value_to_insert, tr, ctx);
				} catch (android.content.res.Resources.NotFoundException ns) {

				}
			}
		}

		/**
		 * tr = addTransitionAction(SgdViewFactory.createView(ctx,
		 * solo.getCurrentActivity(), currentViewTocross, tr, solo), "", tr,
		 * ctx);
		 */
		addTransitionAction(currentViewTocross, "", tr, ctx);
		System.out.println("Builded transion: ");
		System.out.println(tr.toString());
		for (Action act : tr.getAction()) {
			System.out.println(act.toString());
		}
		return tr;
	}

	/**
	 * @param v
	 * @param value
	 * @param tr
	 * @param mContext
	 * @param mSolo
	 * @return
	 */
	public static Transition addTransitionAction(Event event, Transition tr) {
		if (tr == null) {
			if (Config.DEBUG) {
				System.out.println("addTransitionAction");
				System.out.println("tr null");
			}
			return tr;
		}

		Action act = new Action("event " + event.getType());
		act.seWidget(new Widget(event.getName(), event.getType(), "", "", "",
				"", "", "", "", event.getValue(), "", "", "", "", "", "", ""));
		tr.setAction(act);
		tr.setWidgets(act.getWidget());
		return tr;
	}

	public static Transition addTransitionAction(View v, String value,
			Transition tr, Context ctx) {
		if (Config.DEBUG) {
			Log.d(TAG, "addTransitionAction ");
		}
		if (tr == null)
			return tr;

		if (Config.DEBUG) {
			Log.d(TAG, "Widget name : ");
		}
		if (isEditText(v)) {
			String name = ctx.getResources().getResourceName(v.getId());
			Action act = new Action(Scenario.EDITTEXT);
			Widget edit = new Widget(name, EditText.class.getSimpleName(),
					value, "", "", "", "", "", "", value, "", "", "", "", "",
					"", "");
			if (Config.DEBUG) {
				Log.d(TAG, "Value of editText : " + edit.getValue());
			}
			// edit.setValue(value);
			act.seWidget(edit);
			tr.setAction(act);
			return tr;
		}
		if (isListViewElement(v)) {
			Action act = new Action(Scenario.CLICK_LIST);
			act.seWidget(new Widget(((ListViewElement) v).getIndex(),
					ListViewElement.class.getSimpleName(), "", "", "", "", "",
					"", "", "", "", "", "", "", "", "", ""));
			tr.setAction(act);
			return tr;
		}
		if (isMenu(v)) {
			Action act = new Action(Scenario.MENU);
			String identifier = "" + ((MenuView) v).getIndex();
			act.seWidget(new Widget(identifier, MenuView.class.getSimpleName(),
					value, "", "", "", "", "", "", "", "", "", "", "", "", "",
					""));
			tr.setAction(act);
			return tr;
		}
		if (isCheckBox(v)) {
			Action act = new Action(Scenario.CHECK_BOX);
			act.seWidget(new Widget(ctx.getResources().getResourceName(
					v.getId()), CheckBox.class.getSimpleName(), "", "", "", "",
					"", "", "", "", "", "", "", "", "", "", ""));
			tr.setAction(act);
			return tr;
		}

		if (isRadioButtonView(v)) {
			Action act = new Action(Scenario.CLICK_RADIO_BUTTON);
			act.seWidget(new Widget(ctx.getResources().getResourceName(
					v.getId()), RadioButton.class.getSimpleName(), "", "", "",
					"", "", "", "", "", "", "", "", "", "", "", ""));
			tr.setAction(act);
			return tr;
		}

		if (isImageView(v)) {
			Action act = new Action(Scenario.CLICK_IMAGE);
			act.seWidget(new Widget(ctx.getResources().getResourceName(
					v.getId()), ImageView.class.getSimpleName(), "", "", "",
					"", "", "", "", "", "", "", "", "", "", "", ""));
			tr.setAction(act);
			return tr;

		}
		if (isButtonView(v)) {
			Action act = new Action(Scenario.CLICK_BUTTON);
			act.seWidget(new Widget(ctx.getResources().getResourceName(
					v.getId()), Button.class.getSimpleName(), "", "", "", "",
					"", "", "", "", "", "", "", "", "", "", ""));
			tr.setAction(act);
			return tr;

		}

		if (isListView(v)) {
			Action act = new Action(Scenario.CLICK_LIST);
			act.seWidget(new Widget(ctx.getResources().getResourceName(
					v.getId()), ListView.class.getSimpleName(), "", "", "", "",
					"", "", "", "", "", "", "", "", "", "", ""));
			tr.setAction(act);
			return tr;

		}

		if (isTextView(v)) {
			/**
			 * do nothing or get value
			 */
			Action act = new Action(Scenario.CLICK_TEXT);
			value = getTextValue(v);

			act.seWidget(new Widget(ctx.getResources().getResourceName(
					v.getId()), TextView.class.getName(), value, "", "", "",
					"", "", "", value, "", "", "", "", "", "", ""));
			tr.setAction(act);
			return tr;

		}

		if (isClickable(v)) {
			Action act = new Action(Scenario.CLICK_CUSTOMVIEW);
			act.seWidget(new Widget(ctx.getResources().getResourceName(
					v.getId()), CLICKABLEVIEW, "", "", "", "", "", "", "", "",
					"", "", "", "", "", "", ""));
			tr.setAction(act);
			return tr;

		}
		return tr;
	}

	/*
	 * get the textValue of a widget text value may be String, or
	 * SpannableString or SpannableStringBuilder
	 */

	private static String getTextValue(View v) {
		// String value = "";

		Object toread = (TextView.class.cast(v)).getText();

		if (toread instanceof SpannableString) {
			return ((SpannableString) toread).toString();
		}

		if (toread instanceof SpannableStringBuilder) {
			return ((SpannableStringBuilder) toread).toString();
		}

		return (String) toread;
	}

	/**
	 * @param ev
	 * @return
	 */
	public static Integer get_eventKey(String ev) {
		return Integer.parseInt(getToken(0, ev));
	}

	/**
	 * @param ev
	 * @return
	 */
	public static Event get_event(String ev) {
		return new Event(getToken(2, ev), getToken(3, ev), getToken(4, ev));
	}

	/**
	 * @param i
	 * @param ev
	 * @return
	 */
	private static String getToken(int i, String ev) {
		StringTokenizer st = new StringTokenizer(ev, Config.EVENTS_DELIMITERS);
		String id;
		if (i == 0) {
			id = st.nextToken();
			return id;
		}
		int j = 0;
		do {
			try {
				id = st.nextToken();
			} catch (NoSuchElementException e) {
				// cas espace
				id = " ";
			}
			j++;
		} while (j != i);
		return id;
	}

	/**
	 * @param event
	 * @return
	 */
	public static boolean isEditText(Event event) {
		return event.getType().equalsIgnoreCase(EDITTEXT)
				|| event.getType().equalsIgnoreCase(
						EditText.class.getSimpleName());
	}

	/**
	 * @param event
	 * @return
	 */
	public static boolean isMenu(Event event) {
		return event.getType().equalsIgnoreCase(MenuView.class.getName())
				|| event.getType().equalsIgnoreCase(
						MenuView.class.getSimpleName());
	}

	/**
	 * @param event
	 * @return
	 */
	public static boolean isListView(Event event) {
		return event.getType().equalsIgnoreCase(ListView.class.getSimpleName())
				|| event.getType().equalsIgnoreCase(ListView.class.getName());
	}

	/**
	 * @param event
	 * @return
	 */
	public static boolean isCheckBox(Event event) {
		return event.getType().equalsIgnoreCase(CheckBox.class.getName())
				|| event.getType().equalsIgnoreCase(
						CheckBox.class.getSimpleName());
	}

	/**
	 * @param event
	 * @return
	 */
	public static boolean isRadioButton(Event event) {
		return event.getType().equalsIgnoreCase(RadioButton.class.getName())
				|| event.getType().equalsIgnoreCase(
						RadioButton.class.getSimpleName());
	}

	/**
	 * @return
	 */
	public static ArrayList<Widget> get_menu_widgets() {
		ArrayList<Widget> wigs = new ArrayList<Widget>();
		for (int i = 0; i < AbstractCrawler.MENU_ITEM_SIZE; i++) {
			wigs.add(new Widget("" + i, MenuView.class.getName(), "", "", "",
					"", "", "", "", "", "", "", "", "", "", "", ""));
		}
		return wigs;
	}

	/**
	 * @return
	 */
	public static ArrayList<Widget> get_ListViewElements(State state) {
		ArrayList<Widget> wigs = new ArrayList<Widget>();
		if (!hasListView(state))
			return wigs;
		for (int i = 0; i < AbstractCrawler.LISTVIEW_SIZE; i++) {
			wigs.add(new Widget("" + i, ListView.class.getName(), "", "", "",
					"", "", "", "", "", "", "", "", "", "", "", ""));
		}
		return wigs;
	}

	/**
	 * @param state
	 * @return
	 */
	private static boolean hasListView(State state) {
		return hasView(state, ListView.class.getName())
				|| hasView(state, ListViewElement.class.getName());
	}

	/**
	 * @param state
	 * @return
	 */
	private static boolean hasView(State state, String ViewName) {
		for (Widget wig : state.getWidgets()) {
			if (wig.getType().equalsIgnoreCase(ViewName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param stateWidgets
	 * @return
	 */
	public static Widget get_a_random_widget(ArrayList<Widget> stateWidgets) {
		return stateWidgets.get(getRandomView(stateWidgets.size()));
	}

	/**
	 * @param mTestData
	 * @return
	 */
	public static String get_a_random_testData(ArrayList<String> mTestData) {
		return random(mTestData);
	}

	public static boolean path_contains_error(ScenarioData scen) {
		if (scen == null) {
			return false;
		}
		for (Transition tr : scen.getTransitions()) {
			if (contains_error(tr.getAction())) {
				System.out.println("=======> transition error ID :"
						+ tr.getId());
				return true;
			}
		}
		return false;
	}

	public static ArrayList<Transition> get_out_transitions(State sourceState,
			ScenarioData tree) {
		ArrayList<Transition> listTransition = new ArrayList<Transition>();
		for (Transition tr : tree.getTransitions()) {
			if (isEqualState(tr.getSource(), sourceState)) {
				listTransition.add(tr);
			}
		}

		return listTransition;
	}

	public static ArrayList<Transition> get_in_transitions(State destState,
			ScenarioData tree) {
		ArrayList<Transition> listTransition = new ArrayList<Transition>();
		for (Transition tr : tree.getTransitions()) {
			if (isEqualState(tr.getDest(), destState)) {
				listTransition.add(tr);
			}
		}
		return listTransition;
	}

	/**
	 * @param mStates
	 * @param id
	 * @return
	 */
	public static State get_a_valid_state(HashSet<State> mStates,
			ArrayList<String> id) {
		for (State state : mStates) {
			if (id.contains(state.getId())) {
				if (!state.isDest() && state.getType() != null
						&& state.getWidgets().size() > 0) {
					return state;
				}
			}
		}
		return null;
	}

	public static String shortName(String mName) {
		StringTokenizer stk = new StringTokenizer(mName, ".");
		String value = mName;
		while (stk.hasMoreTokens()) {
			value = stk.nextToken();
		}
		return value;
	}

	/**
	 * @param mLListUri
	 */
	public static void plot(ArrayList<Uri> list) {
		if (Config.DEBUG) {
			for (Uri obj : list) {
				System.out.println(obj.toString());
			}
		}
	}

	/**
	 * @param targetState
	 * @return
	 */
	public static boolean hasEditText(State targetState) {
		for (Widget wig : targetState.getWidgets()) {
			if (wig.getType().equalsIgnoreCase(EditText.class.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param wig
	 * @return
	 */
	public static boolean isEditText(Widget wig) {

		return wig.getType().equalsIgnoreCase(EditText.class.getName());
	}

	/**
	 * @param wig
	 * @return
	 */
	public static boolean isTextView(Widget wig) {
		return wig.getType().equalsIgnoreCase(TextView.class.getName());
	}

	/**
	 * ================================= test
	 * Methods==========================================================
	 * 
	 */
	/**
	 * 
	 * @param solo
	 * @param event
	 *            to stimulate
	 */
	public static void trigger(Solo solo, Event event, Context context) {
		if (Config.DEBUG) {
			Log.d(TAG, "trigger " + event.getName() + " " + event.getType()
					+ "  " + event.getValue());
		}
		// cas editText ?
		if (isEditText(event)) {
			fillEditText(event, solo, context);
			return;

		}
		// cas cas menu ?
		if (isMenu(event)) {
			clickMenu(event, solo);
			return;

		}
		// cas listView ?
		if (isListView(event)) {
			clickList(event, solo, context);
			return;
		}

		View view = solo.getView(getId(event.getName(), context));
		if (Config.DEBUG) {
			Log.d(TAG,
					" before trigger " + event.getName() + " "
							+ event.getType() + "  " + event.getValue());
		}
		solo.clickOnView(view, true);
	}

	private static void clickList(Event event, Solo solo, Context context) {
		if (Config.DEBUG) {
			Log.d(TAG, "clickList ");
		}
		int max = solo.getCurrentViews(ListView.class).size();
		if (max > 0) {
			// solo.clickOnView(solo.getView(getId(event.getName(), context)),
			// true);
			int index = Integer.parseInt(event.getName());
			if (index > max - 1) {
				index = max - 1;
			}
			solo.clickInList(0, index);
		}

	}

	/**
	 * @param event
	 */
	private static void clickMenu(Event event, Solo solo) {
		if (Config.DEBUG) {
			Log.d(TAG, "clickMenu ");
		}
		try {
			solo.pressMenuItem(Integer.parseInt(event.getName()));
		} catch (AssertionError assertionError) {
			assertionError.printStackTrace();
		}
	}

	private static int getId(String wig_name, Context context) {
		int id = context.getResources().getIdentifier(wig_name, null, null);
		return id;
	}

	/**
	 * @param event
	 */
	private static void fillEditText(Event event, Solo solo, Context context) {
		if (Config.DEBUG) {
			Log.d(TAG, "fillEditText ");
		}
		View view = solo.getView(getId(event.getName(), context));
		EditText edit = EditText.class.cast(view);
		solo.clearEditText(edit);
		solo.enterText(edit, event.getValue());
	}

	/**
	 * @param content1
	 * @param content2
	 * @return
	 */
	public static boolean equal(ArrayList<Object> content1,
			ArrayList<Object> content2) {
		if (content1.size() != content2.size())
			return false;
		if (!in(content1, content2))
			return false;
		if (!in(content2, content1))
			return false;
		return true;
	}

	/**
	 * content1 is a subset of content 2
	 * 
	 * @param content2
	 * @param content1
	 */
	public static boolean in(ArrayList<Object> content2,
			ArrayList<Object> content1) {
		for (Object con1 : content2) {
			if (!content1.contains(con1))
				return false;
		}
		return true;
	}

	/**
	 * elt is a subset of content 2
	 * 
	 * @param content2
	 * @param content1
	 */
	public static boolean in(Object elt, ArrayList<Object> content1) {
		return content1.contains(elt);
	}

	/**
	 * ∃v ∈ UIV alue ∧ v ∈ TestTableContentk
	 * 
	 * @param Value
	 * @param TestTable
	 */
	public static boolean inElement(ArrayList<Object> Value,
			ArrayList<Object> TestTable) {
		for (Object con1 : Value) {
			if (TestTable.contains(con1))
				return true;
		}
		return false;
	}

	/**
	 * add error into the last transition (by default)
	 * 
	 * @param scen
	 * @param out2
	 *            the error description
	 */

	public static void addError(Transition tr, String message) {
		Action act = new Action(Scenario.ERROR);
		act.setError(message);
		tr.setAction((act));
	}

	public static ArrayList<Transition> contains_error(ScenarioData scen) {
		ArrayList<Transition> trs = new ArrayList<Transition>();
		for (Transition tr : scen.getTransitions()) {
			if (contains_error(tr.getAction())) {
				trs.add(tr);
			}
		}
		return trs;
	}

	/**
	 * @param scen
	 * @return
	 */
	public static String get_error(ScenarioData scen) {
		for (Transition tr : scen.getTransitions()) {
			ArrayList<Action> action = tr.getAction();
			for (Action act : action) {
				if (act.getName().equalsIgnoreCase(Scenario.ERROR)) {
					return act.getName() + "  " + act.getError();
				}
			}
		}
		return null;
	}

	/**
	 * @param scen
	 * @return
	 */
	public static HashSet<String> get_error_number(ScenarioData scen) {
		HashSet<String> transitionIds = new HashSet<String>();
		for (Transition tr : scen.getTransitions()) {
			if (contains_error(tr.getAction()))
				transitionIds.add(tr.getId());

		}
		return transitionIds;
	}

	public static int get_end_id(String id) {
		System.err.println("get_end_id");
		StringTokenizer st = new StringTokenizer(id, Scenario.IDSEPARATOR);
		if (!st.hasMoreTokens()) {

			return 0;
		}
		if (st.countTokens() == 1) {
			try {
				return Integer.parseInt(st.nextToken());
			} catch (NumberFormatException numb) {
				numb.printStackTrace();
				return 0;
			}
		}
		String valueString;
		do {
			valueString = st.nextToken();
			System.err.println(valueString);
		} while (st.hasMoreTokens());

		try {
			return Integer.parseInt(valueString);
		} catch (NumberFormatException numb) {
			numb.printStackTrace();
			return 0;
		}
	}

	public static String getTransitionError(Transition transitions) {
		for (Action act : transitions.getAction()) {
			if (act.getError() != null && !act.getError().equalsIgnoreCase("")) {
				return act.getError();
			}
		}
		return null;
	}

	public static String get_action_type(Event event) {

		/**
		 * set actions (List of all possible actions)
		 */
		if (isEditText(event)) {
			return (Scenario.EDITTEXT);
		}
		if (isListView(event)) {
			return (Scenario.CLICK_LIST);
		}

		if (isMenu(event)) {
			return (Scenario.MENU);
		}
		return Scenario.CLICK;

	}

	public static boolean isClickable(Widget wig) {
		return (wig.getIsClickable().equalsIgnoreCase("true") || wig
				.getIsLongClickable().equalsIgnoreCase("true"));
	}

	public static boolean isListView(Widget wig) {
		return wig.getType().equalsIgnoreCase(ListView.class.getName());
	}

	public static HashMap<String, String> get_event(Transition tr) {
		State source = tr.getSource();
		return get_event(tr, source);
	}

}
