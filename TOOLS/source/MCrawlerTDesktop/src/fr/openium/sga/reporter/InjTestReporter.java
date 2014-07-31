package fr.openium.sga.reporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import kit.Scenario.Transition;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager.SPReport;

public class InjTestReporter extends StressTestReporter {

	public static final String INJREPORT_FILE = "InjectionTestReport";
	public static final String NUMBER_OF_VULNERABLE_COMPONENT_INJECTION = "Number of detected injection vulnerability";
	private static final String VULNERABLE_TRANSITION = "Vulnerable transition";
	private static final String TESTED_TRANSITION = "Tested transition";
	private static final String TEST_NUMBER = "Test Number";

	public InjTestReporter(File output, ArrayList<SPReport> reports)
			throws IOException {
		super(output, reports);

	}

	public void generate() throws FileNotFoundException, IOException {
		/**
		 * number of vulnerable
		 */
		add(NUMBER_OF_TESTED_STATE, "" + crashReport.size());
		add(NUMBER_OF_VULNERABLE_COMPONENT_INJECTION, ""
				+ getVulnerableComponentNumber());

		newSection();
		int testNumber = 1;
		for (SPReport report : crashReport) {

			add(TEST_NUMBER, "" + testNumber++);
			add(STATE_NAME, report.getTargetState().getShortName());
			add(CODE_COVERAGE, "" + report.getCode_coverage());
			add(VULNERABILTY_STATUS, getVulnerabilityStatus(report));
			nextLine();
			add(PATH_TO_REACH_LOCATION);
			add(report.getPath());
			nextLine();
			/**
			 * la transition testée
			 */
			if (report.getTestedTransition() != null
					&& report.getTestedTransition().getTransitions() != null
					&& !report.getTestedTransition().getTransitions().isEmpty()) {
				add(TESTED_TRANSITION, report.getTestedTransition()
						.getTransitions().get(0).getId());
				/*
				 * for (Transition tr : report.getTestedTransition()
				 * .getTransitions()) { add(SgUtils.get_event(tr));
				 * 
				 * }
				 */
				add("error?", SgUtils.get_error((report.getTestedTransition())));

			}

			/**
			 * la transition vulnerable si existe
			 */
			if (report.getVulnerableTransition() != null) {
				nextLine();
				add(VULNERABLE_TRANSITION);
				// HashMap<String, String> events = SgUtils.get_event(report
				// .getVulnerableTransition());
				for (Transition vul : report.getVulnerableTransition()) {
					add(vul.toString());
					nextLine();
				}

				// add(ERROR_DESCRIPTION, report.getMessage());
			}
			nextLine();
			add(LIST_EVENT_DONE);
			add(report.getExecutedEvents());
			newSection();

		}

		FileUtils.writeStringToFile(outpuFile, contents.toString(),
				kit.Config.Config.UTF8, true);

	}
}
