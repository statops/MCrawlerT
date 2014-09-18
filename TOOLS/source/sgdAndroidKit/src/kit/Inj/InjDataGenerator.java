/**
 * 
 */
package kit.Inj;

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
public class InjDataGenerator {
	@SuppressWarnings("unused")
	private static final String TAG = InjDataGenerator.class.getName();
	private final State mTargetState;

	public InjDataGenerator(State targetState, ArrayList<String> testData,
			double maxEventNumber) {
		mTargetState = targetState;
	}

	public boolean generateInjectionTestData(File outputFile) {
		ArrayList<String> generated = new ArrayList<String>();
		ArrayList<Widget> stateWidgets = mTargetState.getWidgets();
		ArrayList<Widget> listWidgets = SgUtils
				.get_ListViewElements(mTargetState);
		/**
		 * remove editText
		 */
		stateWidgets = removeEditText_and_List(stateWidgets);
		/**
		 * remove textView
		 */
		stateWidgets = removeText(stateWidgets);
		stateWidgets.addAll(SgUtils.get_menu_widgets());

		stateWidgets.addAll(listWidgets);

		for (int i = 0; i < stateWidgets.size(); i++) {
			Widget randomWidget = stateWidgets.get(i);
			String value = "no String value";
			String sequence = build_sequence_(i, randomWidget, value);
			if (sequence.equalsIgnoreCase("")) {
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

	/**
	 * @param stateWidgets
	 * @return
	 */
	private ArrayList<Widget> removeText(ArrayList<Widget> stateWidgets) {
		ArrayList<Widget> FinalstateWidgets = new ArrayList<Widget>();
		for (Widget wig : stateWidgets) {
			if (!SgUtils.isTextView(wig) && SgUtils.isClickable(wig)) {
				FinalstateWidgets.add(wig);
			}
		}
		return FinalstateWidgets;
	}

	/**
	 * @param stateWidgets
	 * @return
	 */
	private ArrayList<Widget> removeEditText_and_List(
			ArrayList<Widget> stateWidgets) {
		ArrayList<Widget> FinalstateWidgets = new ArrayList<Widget>();
		for (Widget wig : stateWidgets) {
			if (!SgUtils.isEditText(wig) && !SgUtils.isListView(wig)) {
				FinalstateWidgets.add(wig);
			}
		}
		return FinalstateWidgets;
	}

	/**
	 * @param i
	 * @param randomWidget
	 * @param value
	 * @return
	 */
	private String build_sequence_(int i, Widget randomWidget, String value) {

		return "" + i + Config.EVENTS_DELIMITERS + randomWidget.getName()
				+ Config.EVENTS_DELIMITERS + randomWidget.getType()
				+ Config.EVENTS_DELIMITERS + value;
	}
}