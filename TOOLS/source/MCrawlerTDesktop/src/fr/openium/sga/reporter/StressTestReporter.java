package fr.openium.sga.reporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import kit.Config.Config;
import kit.Scenario.Action;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Stress.Event;
import kit.Utils.SgUtils;
import kit.Utils.TransitionIdComparator;

import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager.SPReport;

public class StressTestReporter extends AbstractReport {

	protected StringBuffer contents = new StringBuffer();
	public static final String NUMBER_OF_VULNERABLE_COMPONENT_CRASH = "Number of crash detected ";
	public static final String NUMBER_OF_TESTED_STATE = "Number of tested location ";
	public static final String STRESSREPORTFILE = "ReadableStressReport";

	protected final ArrayList<SPReport> crashReport;

	public StressTestReporter(File output, ArrayList<SPReport> stressReport)
			throws IOException {
		super(output);
		crashReport = stressReport;
	}

	@Override
	public void generate() throws IOException {

		/**
		 * number of vulnerable
		 */
		add(NUMBER_OF_TESTED_STATE, "" + crashReport.size());
		add(NUMBER_OF_VULNERABLE_COMPONENT_CRASH, ""
				+ getVulnerableComponentNumber());

		newSection();
		for (SPReport report : crashReport) {

			add(STATE_NAME, report.getTargetState().getShortName());
			add(CODE_COVERAGE, "" + report.getCode_coverage());
			add(VULNERABILTY_STATUS, getVulnerabilityStatus(report));
			add(ERROR_DESCRIPTION, report.getMessage());
			add(PATH_TO_REACH_LOCATION);
			add(report.getPath());
			add(LIST_EVENT_DONE);
			add(report.getExecutedEvents());
			nextLine();

		}

		FileUtils.writeStringToFile(outpuFile, contents.toString(),
				kit.Config.Config.UTF8, true);
	}

	protected void add(ArrayList<String> executedEvents) {
		if (executedEvents == null)
			return;
		for (String events : executedEvents) {
			add(SgUtils.get_event(events));
		}

	}

	protected void add(HashMap<String, String> events) {
		for (String key : events.keySet()) {
			add(key + Config.EVENTS_DELIMITERS
					+ SgUtils.get_event(events.get(key)));
		}

	}

	protected void add(Event event) {
		if (SgUtils.get_action_type(event).equalsIgnoreCase(Scenario.EDITTEXT)) {
			add(SgUtils.get_action_type(event) + " with value "
					+ event.getValue() + " in the field " + event.getName());
			return;

		}

		if (SgUtils.get_action_type(event)
				.equalsIgnoreCase(Scenario.CLICK_LIST)) {
			add(SgUtils.get_action_type(event) + " element number : "
					+ event.getName());
			return;

		}

		if (SgUtils.get_action_type(event).equalsIgnoreCase(Scenario.MENU)) {
			add(SgUtils.get_action_type(event) + " element number : "
					+ event.getName());
			return;

		}

		else {

			add(SgUtils.get_action_type(event) + "  the widget "
					+ event.getName());

		}

	}

	protected String getVulnerabilityStatus(SPReport report) {
		if (report.isVulnerable()) {
			return ISVULNERABLE;
		}
		return ISNOT_VULNERABLE;
	}

	protected void add(ScenarioData path) {
		Collections.sort(path.getTransitions(), new TransitionIdComparator());
		for (int i = path.getTransitions().size() - 1; i >= 0; i--) {
			this.add(Scenario.SOURCE, path.getTransitions().get(i).getSource()
					.getName());
			for (Action act : path.getTransitions().get(i).getAction()) {
				this.add(act.getName() + "  " + act.getWidget().getName() + " "
						+ act.getWidget().getValue());
				if (act.getError() != null) {
					add("error?", act.getError());
				}
			}
		}

	}

	protected void newSection() {
		contents.append("=======================================================================================================");
		nextLine();
	}

	protected int getVulnerableComponentNumber() {

		int vulNumber = 0;
		for (SPReport report : crashReport) {
			if (report.isVulnerable()) {
				vulNumber++;
			}

		}
		return vulNumber;

	}

	protected void add(String tag, String value) {
		contents.append(tag + SEPARATOR + value);
		nextLine();

	}

	protected void add(String tag) {
		contents.append(tag);
		nextLine();

	}

	protected void nextLine() {
		contents.append("\n");
	}

}
