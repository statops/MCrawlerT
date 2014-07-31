package fr.openium.sga;

import java.io.File;

public class ConfigApp {
	public static final String CURRENT_DIRECTORY = System.getProperty("user.dir");
	public static final String OkPath = "/OK";
	public static final String TimeDirectory = File.separator + "TimeDirectory";
	public static final String TIME = "time";
	public static final String TIME_PATH = "/time";
	public static final String OkDirectory = File.separator + "OKdirectory";
	public static final String OutXMLPath = "/out.xml";
	public static final String SCENARII = File.separator + "Scenario";
	public static final String OUTPATH = File.separator + "out";
	public static final String ACTION = "sga.intent.action.LAUNCHTEST";
	public static final String ACTIVITYLAUNCHER = "fr.openium.sga/.MainActivity";
	public static final String SERVICELAUNCHER = "fr.openium.sga/.CoverageService";
	public static final String TESTRESULTS = File.separator + "testResults";
	public static String DEVICETESTRESULTS = "/mnt/sdcard/testResults";
	public static String DEVICECOVERAGE = "/mnt/sdcard/coverage";
	public static String DEVICESDCARD = "/mnt/sdcard";

	public static final String DEVICE_SGD_OBSERVERS = "/mnt/sdcard/SgdObservationReport";
	public static final String COVERAGEPATH = File.separator + "bin/coverage.xml";
	public static final String COVERAGEXML = "coverage.xml";
	public static final String BUILDXML = File.separator + "build.xml";
	public static final String RV = "/testResults/rv";
	public static final String TESTDATA = File.separator + "TestData";
	public static final String TESTDATA_ALL_RV = File.separator + "TestData/all_rv";
	public static final String TESTDATA_TESTED_RV = File.separator + "TestData/tested_rv";
	public static final String TESTDATA_TO_PUSH = File.separator + "TestData/rv";
	public static final String EMMAJAR = "emma.jar";
	public static final String ADBPATH = "platform-tools/adb";
	public static final String EMMAJARPATH = File.separator + "tools/lib/emma.jar";
	public static final String JAVA = "java";
	public static final String JAVA_CP = "java -cp ";
	public static final String EMMA = " emma";
	public static final String COMA = ",";
	public static final String EMMAXMLREPORT = EMMA + " report -r xml,html -in ";
	public static final String COVERAGE = "coverage";
	public static final String DEVICE_COVERAGE_DIRECTORY = DEVICETESTRESULTS + File.separator + COVERAGE;
	public static final String DESKTOP_COVERAGE_DIRECTORY = File.separator + COVERAGE;
	public static final String DEVICE_SGDCONTENT_OBSERVERS_DIRECTORY = DEVICE_SGD_OBSERVERS;
	public static final String COVERAGEEM = "bin/coverage.em";
	public static final String TESTDATA_Directory = File.separator + "TestData";
	public static final String RVDONE = "/rv_done";
	public static final String SGA = "fr.openium.sga";
	public static final String SGAAPK = "sga.apk";
	public static final boolean DEBUG = true;
	public static final String SGD_OBSERVATION_REPORT_DIRECTORY = "SgdObservationReport";
	public static final String DOT_DIRECTORY = File.separator + "dot";
	public static final String SCENARIO_REFINED_FILE = File.separator + "r_scen.dot";
	public static final String ANT_EMMA_COMMAND = "clean emma debug install";// test";
	public static final String PATH = "PATH";
	public static final String RECEIVED_PATH = "RECEIVED_PATH";
	public static final String SEMANTIC = "semantic";

	public static boolean ISTEST = true;
	public static String TEMP = "temp";
	
	/**
	 * List of options
	 */
	
	
	/**
	 * List of testName
	 */

	public static final String BRUTEFORCE_TEST = ".test.BruteTest";
	
}
