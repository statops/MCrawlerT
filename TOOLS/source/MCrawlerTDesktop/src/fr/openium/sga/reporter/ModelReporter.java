package fr.openium.sga.reporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import javax.annotation.Generated;
import javax.xml.parsers.ParserConfigurationException;

import kit.Config.Config;
import kit.Scenario.Action;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Utils.SgUtils;
import kit.Utils.TransitionIdComparator;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import fr.openium.sga.Utils.ActivityCoverageUtils;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.EmmaParser;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;
import fr.openium.specification.xml.StreamException;

/**
 * Write all output from Model
 * 
 * @author Stassia
 * 
 */
public class ModelReporter extends AbstractReport {

	public final static String ACTIVITY_COVERAGE = "Activity coverage";

	public static final String PACKAGE = "Application package";
	public static final String TIME = "Execution time";
	public static final String TEST_NUMBER = "Number of test";
	public static final String ERROR_NUMBER = "Number of detected bugs";
	public static final String ERROR_REPORT = "Reported error";
	public static final String ERROR_DETAIL = "Path (s) to reach the error";
	public static final String MODEL_REPORT_NAME = "modelReport";
	private ArrayList<String> contents = new ArrayList<String>();
	private final SgdEnvironnement env;
	private final CrawlResult result;
	private final String exec_time;

	public ModelReporter(File output, SgdEnvironnement environment,
			CrawlResult resultOfModel, String final_exec_time)
			throws IOException {
		super(output);
		env = environment;
		result = resultOfModel;
		exec_time = final_exec_time;
	}

	public ModelReporter(SgdEnvironnement environment,
			CrawlResult resultOfModel, String final_exec_time)
			throws IOException {
		super(new File(environment.getOutDirectory() + File.separator
				+ MODEL_REPORT_NAME));
		env = environment;
		result = resultOfModel;
		exec_time = final_exec_time;
	}

	public ModelReporter(File output) throws IOException {
		super(output);
		env = null;
		result = null;
		exec_time = null;
	}

	/**
	 * generate a report from a model path and coverage path
	 * 
	 * @param outXmlPath
	 * @param coveragePath
	 * @throws ParserConfigurationException
	 * @throws StreamException
	 * @throws IOException
	 * @throws SAXException
	 * @throws FileNotFoundException
	 * @throws CloneNotSupportedException
	 */
	public void generate(String applicationPackage, String outXmlPath,
			String coveragePath, String androidManifestPath)
			throws FileNotFoundException, SAXException, IOException,
			StreamException, ParserConfigurationException,
			CloneNotSupportedException {
		/**
		 * add packageName
		 * 
		 */
		add(ModelReporter.PACKAGE, applicationPackage);
		ScenarioData model = ScenarioParser.parse(new File(outXmlPath));
		this.add(ModelReporter.TEST_NUMBER, ""
				+ (model.getTransitions() == null ? "" + 1 : ""
						+ (model.getTransitions().size() + 1)));
		this.add(ModelReporter.CODE_COVERAGE, "" + getCoverage(coveragePath));
		ActivityCoverageUtils actCov = new ActivityCoverageUtils(
				androidManifestPath, outXmlPath);
		this.add(ModelReporter.ACTIVITY_COVERAGE, actCov.getActivityCoverage()
				+ " %");
		HashSet<String> errorNumber = SgUtils.get_error_number(model);
		this.add(ModelReporter.ERROR_NUMBER, "" + errorNumber.size());
		this.newSection();
		Iterator<String> errors = errorNumber.iterator();
		if (errors.hasNext()) {
			String currentErrors;
			do {
				currentErrors = errors.next();
				this.add(ModelReporter.ERROR_REPORT, SgUtils
						.getTransitionError(result.getScenarioData()
								.getTransitions(currentErrors)));
				/**
				 * add path
				 */
				ScenarioData path = SgUtils.getConcretePath(
						model.getState(currentErrors, Scenario.DEST), model);
				if (path == null || path.getTransitions() == null
						|| path.getTransitions().isEmpty()) {
					continue;
				}

				this.add(ModelReporter.ERROR_DETAIL);

				Collections.sort(path.getTransitions(),
						new TransitionIdComparator());
				for (int i = path.getTransitions().size() - 1; i >= 0; i--) {
					this.add(Scenario.SOURCE, path.getTransitions().get(i)
							.getSource().getName());
					for (Action act : path.getTransitions().get(i).getAction()) {
						this.add(act.getName() + "  "
								+ act.getWidget().getName() + " "
								+ act.getWidget().getValue());
					}

				}
				this.newSection();
			} while (errors.hasNext());
		}
		this.finish();

	}

	private String getCoverage(String coveragePath) throws SAXException,
			IOException, ParserConfigurationException {
		EmmaParser pa = new EmmaParser();
		String _coverage_value = "\n";
		pa.parse(new File(coveragePath), null);
		_coverage_value = " " + _coverage_value + " class coverage  "
				+ (pa.getClassCoverage()) + " line coverage  "
				+ (pa.getLineCoverage()) + "\n";
		return _coverage_value;
	}

	/**
	 * 
	 * @param tag
	 * @param value
	 */
	public void add(String tag, String value) {
		contents.add(tag + SEPARATOR + value);
	}

	public void add(String value) {
		contents.add(value);

	}

	public void finish() throws IOException {
		newSection();
		FileUtils.writeLines(outpuFile, Config.UTF8, contents, true);

	}

	public void newSection() {
		contents.add("===========================================================================================================");

	}

	@Override
	public String toString() {
		String content = "";
		for (String contElt : contents) {
			content = content + contElt + "\n";
		}
		return content;
	}

	public void generate() throws FileNotFoundException, SAXException,
			IOException, StreamException, ParserConfigurationException,
			CloneNotSupportedException {

		/**
		 * add packageName
		 * 
		 */
		add(ModelReporter.PACKAGE, env.getProjectPackape());

		this.add(ModelReporter.TIME, "" + exec_time);
		if (result == null || result.getScenarioData() == null) {
			this.newSection();
			return;
		}
		this.add(ModelReporter.TEST_NUMBER, ""
				+ (result.getScenarioData().getTransitions() == null ? "" + 1
						: ""
								+ (result.getScenarioData().getTransitions()
										.size() + 1)));

		this.add(ModelReporter.CODE_COVERAGE, "" + env.readCoverage());
		ActivityCoverageUtils actCov = new ActivityCoverageUtils(
				env.getManifestfilePath(), env.getFinalModelPath());
		this.add(ModelReporter.ACTIVITY_COVERAGE, actCov.getActivityCoverage()
				+ " %");
		HashSet<String> errorNumber = SgUtils.get_error_number(result
				.getScenarioData());
		this.add(ModelReporter.ERROR_NUMBER, "" + errorNumber.size());
		this.newSection();
		Iterator<String> errors = errorNumber.iterator();
		if (errors.hasNext()) {
			String currentErrors;
			do {
				currentErrors = errors.next();
				this.add(ModelReporter.ERROR_REPORT, SgUtils
						.getTransitionError(result.getScenarioData()
								.getTransitions(currentErrors)));
				/**
				 * add path
				 */
				ScenarioData path = SgUtils.getConcretePath(
						result.getScenarioData().getState(currentErrors,
								Scenario.DEST), result.getScenarioData());
				if (path == null || path.getTransitions() == null
						|| path.getTransitions().isEmpty()) {
					continue;
				}

				this.add(ModelReporter.ERROR_DETAIL);

				Collections.sort(path.getTransitions(),
						new TransitionIdComparator());
				for (int i = path.getTransitions().size() - 1; i >= 0; i--) {
					this.add(Scenario.SOURCE, path.getTransitions().get(i)
							.getSource().getName());
					for (Action act : path.getTransitions().get(i).getAction()) {
						this.add(act.getName() + "  "
								+ act.getWidget().getName() + " "
								+ act.getWidget().getValue());
					}

					// report.add("\n");
				}
				this.newSection();
			} while (errors.hasNext());
		}
		this.finish();
	}

	public static void main(String[] args) throws SAXException, IOException,
			StreamException, ParserConfigurationException,
			CloneNotSupportedException {
		SgdEnvironnement env = Emma.init_environment_(args);
		if (env == null) {
			return;
		}
		ScenarioData model = env.getModel();
		CrawlResult result = new CrawlResult(new File(env.getOutDirectory()),
				null);
		result.setScenarioData(model);
		ModelReporter reporter = new ModelReporter(env, result, null);
		reporter.generate();
	}

}
