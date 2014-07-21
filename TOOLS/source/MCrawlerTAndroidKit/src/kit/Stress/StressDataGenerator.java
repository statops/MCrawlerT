/**
 * 
 */
package kit.Stress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import kit.Config.Config;
import kit.Scenario.State;
import kit.Scenario.Widget;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

/**
 * @author Stassia
 * 
 */
public class StressDataGenerator {
	// private static final String TAG = StressDataGenerator.class.getName();
	private final State mTargetState;
	private final ArrayList<String> mTestData;
	private final double eventNumber;

	public StressDataGenerator(State targetState, ArrayList<String> testData, double maxEventNumber) {
		mTargetState = targetState;
		mTestData = testData;
		eventNumber = maxEventNumber;
	}

	public boolean generateStressTest(File outputFile) {
		ArrayList<String> generated = new ArrayList<String>();
		ArrayList<Widget> stateWidgets = mTargetState.getWidgets();
		stateWidgets.addAll(SgUtils.get_menu_widgets());
		stateWidgets.addAll(SgUtils.get_ListViewElements(mTargetState));
		/**
		 * get List of EditText
		 */
		ArrayList<Widget> EditTextWidgets = getEditext(stateWidgets);
		String value;
		String sequence;
		Widget randomWidget;

		for (int i = 0; i < eventNumber; i++) {
			if (!EditTextWidgets.isEmpty() && (i % 5) == 0) {
				/**
				 * apply pairwise here
				 */
				for (Widget wig : EditTextWidgets) {
					value = SgUtils.get_a_random_testData(mTestData);
					sequence = build_sequence_(i, wig, value);
					if (sequence.equalsIgnoreCase("") || sequence.equalsIgnoreCase(" ")
							|| sequence.equalsIgnoreCase("\n")) {
						continue;
					}
					generated.add(sequence);
				}
			}
			randomWidget = SgUtils.get_a_random_widget(stateWidgets);
			value = SgUtils.get_a_random_testData(mTestData);
			sequence = build_sequence_(i, randomWidget, value);
			if (sequence.equalsIgnoreCase("") || sequence.equalsIgnoreCase(" ")
					|| sequence.equalsIgnoreCase("\n")) {
				continue;
			}
			generated.add(sequence);
		}
		try {
			FileUtils.writeLines(outputFile, generated);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private ArrayList<Widget> getEditext(ArrayList<Widget> stateWidgets) {
		ArrayList<Widget> editTextWidgets = new ArrayList<Widget>();
		for (Widget wig : stateWidgets)
			if (SgUtils.isEditText(wig)) {
				editTextWidgets.add(wig);
			}
		System.err.println("edidtText set number: " + editTextWidgets.size());
		return editTextWidgets;
	}

	/**
	 * @param i
	 * @param randomWidget
	 * @param value
	 * @return
	 */
	private String build_sequence_(int i, Widget randomWidget, String value) {
		if (randomWidget == null) {
			return "";
		}

		return "" + i + Config.EVENTS_DELIMITERS + randomWidget.getName() + Config.EVENTS_DELIMITERS
				+ randomWidget.getType() + Config.EVENTS_DELIMITERS + value;
	}
}
