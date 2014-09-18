package fr.openium.sga.reporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.Transition;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import android.widget.EditText;

import fr.openium.sga.ConfigApp;

/***
 * Generate all executed test in text
 * 
 * @author Stassia
 * 
 */
public class TextTestReporter extends AbstractReport {

	public static final String IOSTS = "tests.iosts";
	private final ScenarioData mScen;
	public boolean done = false;

	private Vector<String> test = new Vector<String>();

	public TextTestReporter(ScenarioData scen, File outPutfile)
			throws IOException {
		super(outPutfile);
		mScen = scen;

	}

	public void generate() {
		// get all transition
		for (Transition tr : mScen.getTransitions()) {
			generateExecutedTest(tr);
			test.add("   ");

		}
		test.add("number of tests:   " + test.size());
		try {
			FileUtils.writeLines(outpuFile, test);
			done = true;
		} catch (IOException e) {
			e.printStackTrace();

		}
		System.out.println("iost test report is available in " + outpuFile.getPath());
	}

	private void generateExecutedTest(Transition tr) {
		ArrayList<String> ids = SgUtils.get_transition_id_from_state_id_(tr
				.getId());
		for (String to_execute : ids) {
			addTestInList(to_execute);
		}

	}

	private void addTestInList(String to_execute) {
		String source = getSourceState(to_execute);
		String dest = getDestState(to_execute);
		String action = getAction(to_execute);
		to_execute = to_execute + " : " + "(" + source + ") ==> " + "  ("
				+ dest + ")" + "[" + action + "]";
		if (ConfigApp.DEBUG) {
			System.out.println(to_execute);
		}
		test.add(to_execute);

	}

	private String getDestState(String to_execute) {
		return mScen.getState(to_execute, Scenario.DEST).getShortName()
				+ ","
				+ "Nwig="
				+ mScen.getState(to_execute, Scenario.DEST).getWidgets().size()
				+ ","
				+ "Nue="
				+ mScen.getState(to_execute, Scenario.DEST)
						.getUserEnvironments().size()
				+ ","
				+ "Nb="
				+ mScen.getState(to_execute, Scenario.DEST)
						.getBroadCastEnvironments().size();
	}

	private String getSourceState(String to_execute) {
		return mScen.getState(to_execute, Scenario.SOURCE).getShortName()
				+ ","
				+ "Nwig="
				+ mScen.getState(to_execute, Scenario.SOURCE).getWidgets()
						.size()
				+ ","
				+ "Nue="
				+ mScen.getState(to_execute, Scenario.SOURCE)
						.getUserEnvironments().size()
				+ ","
				+ "Nb="
				+ mScen.getState(to_execute, Scenario.SOURCE)
						.getBroadCastEnvironments().size();
	}

	private String getAction(String to_execute) {
		String action = "";
		for (kit.Scenario.Action act : mScen.getTransitions(to_execute)
				.getAction()) {
			String wig = "wig="
					+ act.getWidget().getName()
					+ (act.getWidget().getType()
							.equalsIgnoreCase(EditText.class.getSimpleName()) ? ",value='"
							+ act.getWidget().getValue() + "'"
							: "");
			String op = "event=" + act.getName();
			String error = "error=" + (act.getError());
			if (act.getError() == null || act.getError().equalsIgnoreCase("")) {
				action = action + op + "," + wig + ",";
			} else {
				action = action + op + "," + wig + "," + error + ",";
			}

		}

		return action.substring(0, action.length() - 1);
	}
}
