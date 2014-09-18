package fr.openium.sga.emmatest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;

import javax.xml.parsers.ParserConfigurationException;

import kit.Command.AntManager;
import kit.Command.DeletCommand;
import kit.Command.InstallCommand;
import kit.Command.InstrumentationCommand;
import kit.Command.PullCommand;
import kit.Command.PushCommand;
import kit.Command.UninstallCommand;
import kit.Config.Config;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.ThreadSleeper;
import fr.openium.sga.Utils.Utils;
import fr.openium.sga.command.Command;
import fr.openium.sga.datamanagement.Datamanager;
import fr.openium.sga.launchSga.sga;
import fr.openium.sgaDesktop.Sga;

public class SgdEnvironnement implements Cloneable, Serializable, Observer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7373417659579130909L;
	private String mTestProjectPath;
	private String mTestProjectPackage;
	private String mProjectPath;
	private String mSdkPath;
	private String mPmd;
	private String mOutDirectory = null;
	/**
	 * will be static path for all jobs
	 */
	private String mCoveragePath;
	/**
	 * by default 10
	 */
	private Long mCoverageLimit = 10L;
	private Datamanager mDatamanager;
	/*
	 * test data path
	 */
	private String mAllTestDataPath = null;
	private String mTestedDataPath = null;
	private String mDevice = null;
	private String mScenarioDirectory;
	private String mTargetClass;
	private String mStopErrorCondition;
	private String mMAxEvent = "100"; // by default
	private ScenarioData mModel;
	private String mDicoPath;

	public SgdEnvironnement() {
		mSleeper = new ThreadSleeper();
	}

	private static ThreadSleeper mSleeper;

	/**
	 * lancer sga dans le cas de crawler simple
	 */
	/*
	 * public void launchSga_simple_crawler() { String[] test = { "-tp",
	 * mTestProjectPackage, "-n", "1", "-adb", mSdkPath + File.separator +
	 * ConfigApp.ADBPATH, "-emu", mDevice };
	 * Emma.info(" Launch sga test Runner..."); Emma.info("parameters {" +
	 * test.toString() + "}"); mSleeper.sleepShort(); pushTestDataIntoDevice();
	 * sga.launchTestOnDevice(test);
	 * 
	 * }
	 */

	/**
	 * lancer sga dans le cas o� on donne le de la classe � tester et la
	 * condition d'arr�t en cas de crash
	 */
	public synchronized void launchSga_class_defined() {
		String[] test = { "-tp", mTestProjectPackage, "-n", "1", "-adb",
				mSdkPath + File.separator + ConfigApp.ADBPATH, "-emu", mDevice,
				"-class", mTargetClass, "-stopError", mStopErrorCondition };
		launchSga_class_defined(test);
	}

	public void setTestSdk(String string) {
		mSdkPath = string;
		Emma.info("Sdk: " + mSdkPath);

	}

	public void setTestProjectPackage(String string) {
		mTestProjectPackage = string;
		Emma.info("Test Project Package: " + mTestProjectPackage);

	}

	public void setTestProjectPath(String string) {
		mTestProjectPath = string;
		Emma.info("Test Project path: " + mTestProjectPath);

	}

	public void setProjectPath(String string) {
		mProjectPath = string;
		Emma.info("Project path: " + mProjectPath);

	}

	public boolean checkParameters() {
		if (mSdkPath == null || !new File(mSdkPath).exists()) {
			throw new NullPointerException("please check the sdk information");
		}
		if (mTestProjectPath == null || !new File(mTestProjectPath).exists()) {
			throw new NullPointerException(
					"please check the test project information");
		}

		if (mProjectPath == null || !new File(mProjectPath).exists()) {
			throw new NullPointerException(
					"please check the project information");
		}

		if (mDevice == null) {
			throw new NullPointerException("please add emulator argument -emu");
		}
		return true;
	}

	public String getTestProjectPath() {
		return mTestProjectPath;
	}

	public String getTestProjectPackage() {
		return mTestProjectPackage;
	}

	public Long getCoverageLimit() {
		return mCoverageLimit;
	}

	public void setCoverageLimit(Long coverageLimit) {
		this.mCoverageLimit = coverageLimit;
		Emma.info("Coverage Limit " + mCoverageLimit);
	}

	public Datamanager getDatamanager() {
		return mDatamanager;
	}

	public void setDatamanager(Datamanager mDatamanager) {
		this.mDatamanager = mDatamanager;
	}

	public String getProjectPath() {
		return mProjectPath;
	}

	public String getSdkPath() {
		return mSdkPath;
	}

	public String getOutDirectory() {
		if (mOutDirectory != null)
			return mOutDirectory;
		else
			return getDefaultOutDirectory();
	}

	public void setOutDirectory(String out) {
		mOutDirectory = out;

	}

	public String getAllTestDataPath() {
		return mAllTestDataPath;
	}

	public void setAllTestDataPath(String mAllTestDataPath) {
		this.mAllTestDataPath = mAllTestDataPath;
		Emma.info("mAllTestDataPath " + mAllTestDataPath);
	}

	public String getTestedDataPath() {
		return mTestedDataPath;
	}

	public void setTestedDataPath(String mTestedDataPath) {
		this.mTestedDataPath = mTestedDataPath;
		Emma.info("mTestedDataPath " + mTestedDataPath);
	}

	/**
	 * init scenario, time,ok,coverage directories
	 * 
	 * @throws IOException
	 */
	public void initOutDirectories() throws IOException {

		if (mOutDirectory == null) {
			mOutDirectory = getDefaultOutDirectory();
			Emma.info("OutDirectory is not setted");
		}
		if (!new File(mOutDirectory).exists()) {
			if (!new File(mOutDirectory).mkdirs()) {
				throw new IOException(mOutDirectory + "cannot be created");
			}
		}
		Emma.info("OutDirectory is avalaible on : " + mOutDirectory);
		/**
		 * create scenario drectory
		 */
		initScenarioDirectory();
		initCoverageDirectory();
		initOkDirectory();
		initTimeDirectory();
		for (File currentFile : (new File(mOutDirectory)).listFiles()) {
			if (!currentFile.isDirectory()) {
				if (ConfigApp.DEBUG) {
					System.out.println("delete  :" + currentFile.getName());
				}
				currentFile.delete();
			}
		}
		/**
		 * init Remote directory
		 */
		if (mDevice != null)
			initRemoteDirectory();
	}

	/**
	 * @return
	 */
	private String getDefaultOutDirectory() {
		return ConfigApp.CURRENT_DIRECTORY + ConfigApp.OUTPATH;
	}

	/**
	 * remove /sdcard/coverage/*
	 * 
	 * remove /sdcard/testResults/*
	 */
	public void initRemoteDirectory() {
		initRemoteDirectory(ConfigApp.DEVICETESTRESULTS + "/*");
		initRemoteDirectory(ConfigApp.DEVICECOVERAGE + "/*");
	}

	private void initRemoteDirectory(String directory_to_delete) {
		DeletCommand deleter = new DeletCommand(getAdb(), mDevice,
				directory_to_delete);
		if (ConfigApp.DEBUG) {
			System.out.println("initRemoteDirectory  " + deleter.toString());
		}
		deleter.execute();
	}

	private void initScenarioDirectory() throws IOException {
		if (mOutDirectory == null) {
			getDefaultOutDirectory();
		}
		mScenarioDirectory = mOutDirectory + ConfigApp.SCENARII;
		initDirectory(mScenarioDirectory);

	}

	private void initTimeDirectory() throws IOException {
		initDirectory(mOutDirectory + ConfigApp.TimeDirectory);

	}

	private void initOkDirectory() throws IOException {
		initDirectory(mOutDirectory + ConfigApp.OkDirectory);

	}

	private void initCoverageDirectory() throws IOException {
		initDirectory(mOutDirectory + File.separator + ConfigApp.COVERAGE);
	}

	private void initDirectory(String fileName) throws IOException {
		File directory = new File(fileName);
		if (directory.exists()) {
			for (File file : directory.listFiles()) {
				file.delete();
			}
			directory.delete();
		}

		if (!directory.mkdirs()) {
			throw new IOException(directory.getAbsolutePath()
					+ "   cannot be created");
		}
		Emma.info("directory created : " + directory.getAbsolutePath());
	}

	/**
	 * set DataManager file
	 */
	public void initTestData() {

		mDatamanager = Datamanager.getInstance();
		if (mAllTestDataPath == null) {
			Emma.info("test data path is not setted");
			Emma.info("use default path:" + ConfigApp.CURRENT_DIRECTORY
					+ ConfigApp.TESTDATA_ALL_RV);
			mDatamanager.setRVFile(new File(ConfigApp.CURRENT_DIRECTORY
					+ ConfigApp.TESTDATA_ALL_RV));
		} else {
			Emma.info("test data path: " + mAllTestDataPath);
			mDatamanager.setRVFile(new File(mAllTestDataPath));
		}

		if (mTestedDataPath == null) {
			Emma.info("test data path is not setted");
			Emma.info("use default path:" + ConfigApp.CURRENT_DIRECTORY
					+ ConfigApp.TESTDATA_TESTED_RV);
			mDatamanager.setTestedRVFile(new File(ConfigApp.CURRENT_DIRECTORY
					+ ConfigApp.TESTDATA_TESTED_RV));
		} else {
			mDatamanager.setTestedRVFile(new File(mTestedDataPath));
		}
	}

	public String installSga() {
		Emma.info("install sga");
		if (!new File(ConfigApp.CURRENT_DIRECTORY + File.separator
				+ ConfigApp.SGAAPK).exists()) {
			throw new NullPointerException(
					"sga.apk is not in the current directory : "
							+ ConfigApp.CURRENT_DIRECTORY + File.separator
							+ ConfigApp.SGAAPK);
		}
		Emma.info(mSdkPath + File.separator + ConfigApp.ADBPATH + " -s "
				+ mDevice + " install " + ConfigApp.CURRENT_DIRECTORY
				+ File.separator + ConfigApp.SGAAPK);

		return new AntManager().exec(mSdkPath + File.separator
				+ ConfigApp.ADBPATH + " -s " + mDevice + " install "
				+ ConfigApp.CURRENT_DIRECTORY + File.separator
				+ ConfigApp.SGAAPK);

	}

	public void instrument_with_Emma_and_install() throws Exception {
		Emma.info("Instrumentation with Emma of " + mProjectPath);
		Emma.info("First Install of " + mTestProjectPath);
		long a = System.currentTimeMillis();
		ant();
		Emma.info("END of test  {" + (System.currentTimeMillis() - a) / 1000
				+ " secondes}");
		Emma.info("Waiting for Coverage ...");
		// Pull scenario
	}

	private void pushTestDataIntoDevice() {
		/**
		 * version ant�rieure
		 */
		// pushTextData();
		/**
		 * version actuelle
		 */
		pushStructuredData();
		pushCPData();

	}

	/**
	 * 
	 */
	private void pushCPData() {
		if (mCP == null) {
			Emma.info("No CP List");
			return;
		}
		Emma.info("Push CP Data :" + mSdkPath + File.separator
				+ ConfigApp.ADBPATH + "   " + mCP + "   "
				+ ConfigApp.DEVICESDCARD);
		PushCommand push = new PushCommand(mSdkPath + File.separator
				+ ConfigApp.ADBPATH, getDevice(), mCP, ConfigApp.DEVICESDCARD);
		push.execute();
	}

	private void pushStructuredData() {
		Emma.info("Push Test Data :" + mSdkPath + File.separator
				+ ConfigApp.ADBPATH + "   " + getDevice() + "   "
				+ mDatamanager.getTestDatafile() + "   "
				+ ConfigApp.DEVICETESTRESULTS);
		PushCommand push = new PushCommand(mSdkPath + File.separator
				+ ConfigApp.ADBPATH, getDevice(),
				mDatamanager.getTestDatafile(), ConfigApp.DEVICETESTRESULTS);
		push.execute();
	}

	/**
	 * Launch test With Emma (in a thread)
	 * 
	 * "ant -buildfile buildPAth clean emma debug install test"
	 * 
	 * @throws Exception
	 */
	private void ant() throws Exception {
		String command = "ant -buildfile " + mTestProjectPath
				+ ConfigApp.BUILDXML + " -Dadb.device.arg=\"-s " + mDevice
				+ "\" " + "-Dsdk.dir=\"" + mSdkPath + "\"" + " -Dpmd.dir=\""
				+ mPmd + "\"" + " " + ConfigApp.ANT_EMMA_COMMAND;
		Emma.info(command);
		InstrumentationCommand instr = new InstrumentationCommand(null,
				mTestProjectPath, mDevice, mPmd, mSdkPath);
		// AntManager ant = new AntManager();
		// String out = ant.exec(command);
		// Emma.info(out);
		instr.execute();
		// checkError(ant);
	}

	public void pullCoverageAndGenerateReport() {
		File coverage = new File(mOutDirectory
				+ ConfigApp.DESKTOP_COVERAGE_DIRECTORY);
		if (!coverage.exists())
			coverage.mkdirs();
		/**
		 * d'un seul emulateur
		 */
		delete(coverage);
		// Emma.info("Pull coverage :" + mSdkPath + File.separator +
		// ConfigApp.ADBPATH + " " + mDevice + " "
		// + ConfigApp.DEVICECOVERAGE + " " + coverage.getPath());
		PullCommand pull = new PullCommand(mSdkPath + File.separator
				+ ConfigApp.ADBPATH, mDevice, ConfigApp.DEVICECOVERAGE,
				coverage.getPath());
		pull.execute();
		mSleeper.sleepLong();
		/**
		 * launch coverage generation
		 */
		if (coverage.exists()) {
			File[] f = coverage.listFiles();

			/**
			 * copier dans coverage principale
			 */
			File principal_coverage = new File(mCoveragePath);
			if (!principal_coverage.exists())
				principal_coverage.mkdirs();
			if (!coverage.getPath().equalsIgnoreCase(
					principal_coverage.getPath())) {
				for (int i = 0; i < f.length; i++) {
					Utils.savegeneric_file(f[i], principal_coverage, null);
				}
				f = principal_coverage.listFiles();
			}
			if (f.length > 0) {
				generateXmlCoverageFile(f);
			}
		}
	}

	/**
	 * read the coverage of the
	 * 
	 * @param f
	 */
	protected synchronized void generateXmlCoverageFile(File[] f) {
		String[] s = new String[f.length + 1];
		/**
		 * add coverage.em path
		 */
		if (new File(mProjectPath + File.separator + ConfigApp.COVERAGEEM)
				.exists())
			s[0] = mProjectPath + File.separator + ConfigApp.COVERAGEEM;
		else {
			throw new NullPointerException(" File does not exist : "
					+ mProjectPath + File.separator + ConfigApp.COVERAGEEM);
		}
		for (int i = 0; i < f.length; i++) {
			s[i + 1] = f[i].getPath();
		}
		Emma.info("Generate the Coverage Report");
		Command.generateReport(mSdkPath, s);
	}

	private void delete(File file_to_delete) {
		if (file_to_delete.isDirectory()) {
			if (file_to_delete.exists()) {
				File[] f = file_to_delete.listFiles();
				if (f.length > 0) {
					for (int i = 0; i < f.length; i++) {
						f[i].delete();
					}
				}
			}
			return;
		}
		if (file_to_delete.isFile()) {
			file_to_delete.delete();
		}
	}

	protected void reportError(Exception e) {
		System.out.println("[error] :" + e.getMessage());
		e.printStackTrace();
	}

	/**
	 * read Emma results
	 */
	public boolean isCodeCoverageLimitReached() {
		pullCoverageAndGenerateReport();
		File coverage = new File(ConfigApp.CURRENT_DIRECTORY + File.separator
				+ ConfigApp.COVERAGEXML);
		Emma.info("Check if coverage is available in : " + coverage.getPath());
		if (coverage.exists())
			readEmma(coverage);
		else {
			Emma.info(ConfigApp.COVERAGEXML + "  does not exist");
		}
		if (mCoverageLimit == null || mCurrentEmmaValue == null) {
			return false;
		}
		if (mCoverageLimit < mCurrentEmmaValue) {
			return true;
		}
		return false;
	}

	private Long mCurrentEmmaValue = 0L;
	Long previousClassCoverage = 0L;
	Long previousLineCoverage = 0L;

	protected void readEmma(File coverage) {
		EmmaParser pa = new EmmaParser();
		try {
			pa.parse(coverage, null);
			Emma.info("[info] : Coverage detail");
			Emma.info("Class : " + pa.getClassCoverage());
			Emma.info("Method : " + pa.getMethodCoverage());
			Emma.info("Line : " + pa.getLineCoverage());
			Emma.info("Block : " + pa.getBlockCoverage());
			mCurrentEmmaValue = Long.parseLong(pa.getLineCoverage().substring(
					0, pa.getLineCoverage().indexOf("%") - 1));
			Emma.info("EMMA value = " + mCurrentEmmaValue);
		} catch (SAXException e) {
			reportError(e);
		} catch (IOException e) {
			reportError(e);
		} catch (ParserConfigurationException e) {
			reportError(e);
		}
		/**
		 * save in the main repertory
		 * 
		 */
		if (coverage.exists()) {
			Utils.savegeneric_file(coverage.getPath(), getCoveragePath(), null);
		}
		Emma.info("Delete  " + coverage.getPath() + "  " + coverage.delete());
	}

	public String readCoverage() throws SAXException, IOException,
			ParserConfigurationException {
		/**
		 * read coverag
		 */

		if (Config.DEBUG) {
			System.out.println("read coverage:");
			System.out.println(mCoveragePath);

		}
		EmmaParser pa = new EmmaParser();

		/**
		 * parser toutes les fichiers qui commencent par cov
		 */
		FilenameFilter covFilter = new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {

				return (arg1.startsWith("cov") && arg1.contains(".xml"));
			}
		};
		File[] listFile = new File(mCoveragePath).listFiles(covFilter);
		String _coverage_value = "cannot read";
		if (listFile.length > 0) {
			if (Config.DEBUG) {
				System.out.println("read coverage file: ");
				// System.out.println(listFile[0]);
			}

			pa.parse(listFile[0], null);
			previousClassCoverage = Long.parseLong(pa.getClassCoverage()
					.substring(0, pa.getClassCoverage().indexOf("%") - 1));
			previousLineCoverage = Long.parseLong(pa.getLineCoverage()
					.substring(0, pa.getLineCoverage().indexOf("%") - 1));
			_coverage_value = " class coverage  " + (pa.getClassCoverage())
					+ " line coverage  " + (pa.getLineCoverage()) + "\n";
		}

		for (File coverage : listFile) {
			if (Config.DEBUG) {
				System.out.println("read coverage file: ");
				System.out.println(coverage);
			}

			pa.parse(coverage, null);
			if (solveCoverage(pa)) {
				_coverage_value = " class coverage  " + (pa.getClassCoverage())
						+ " line coverage  " + (pa.getLineCoverage()) + "\n";
			}

		}

		System.out.println(_coverage_value);
		return _coverage_value;

	}

	private boolean solveCoverage(EmmaParser pa) {

		mCurrentEmmaValue = Long.parseLong(pa.getLineCoverage().substring(0,
				pa.getLineCoverage().indexOf("%") - 1));
		if (Config.DEBUG) {
			System.out.println("LineCoverage() " + mCurrentEmmaValue);
		}
		if (mCurrentEmmaValue > previousLineCoverage) {
			previousLineCoverage = mCurrentEmmaValue;
			return true;
		}
		mCurrentEmmaValue = Long.parseLong(pa.getClassCoverage().substring(0,
				pa.getClassCoverage().indexOf("%") - 1));
		if (Config.DEBUG) {
			System.out.println("ClassCoverage()" + mCurrentEmmaValue);
		}
		if (mCurrentEmmaValue > previousClassCoverage) {
			previousClassCoverage = mCurrentEmmaValue;
			return true;
		}
		return false;
	}

	public boolean isTestDataCoverageLimitReached() {
		/**
		 * Calculer le poucentatge de donn�e de test utilis�
		 */
		mDatamanager.getTestDataCoverage(mOutDirectory);
		return false;
	}

	@SuppressWarnings("unused")
	private void pushTextData() {
		System.out.println("[info]: Push test Data");
		String testResult = ConfigApp.DEVICETESTRESULTS;
		/**
		 * Read random value (Limit � definir)
		 */
		ArrayList<String> rv = new ArrayList<String>();
		for (int i = 0; i < 30; i++) {
			String value = mDatamanager.get_A_rtestv(rv);
			if (value != null)
				rv.add(value);
		}
		rv.removeAll(Collections.singleton(null));
		if (rv.isEmpty()) {
		}

		Emma.info("Number of pushed data " + rv.size());
		/**
		 * create a temporary file containing random value to push
		 */
		File testDataDirectory = new File(new File("").getAbsolutePath()
				+ ConfigApp.TESTDATA);
		if (!testDataDirectory.exists()) {
			testDataDirectory.mkdir();
		}
		File testDataFile = new File(ConfigApp.CURRENT_DIRECTORY
				+ ConfigApp.TESTDATA_TO_PUSH);
		if (testDataFile.exists())
			testDataFile.delete();
		try {
			testDataFile.createNewFile();
			FileUtils.writeLines(testDataFile, "UTF-8", rv);
		} catch (IOException e) {
			throw new Error("push operation failed");
		}
		Emma.info("[push]: " + mSdkPath + File.separator + ConfigApp.ADBPATH
				+ "," + testDataFile.getPath() + "," + testResult);
		new PushCommand(mSdkPath + File.separator + ConfigApp.ADBPATH,
				getDevice(), testDataFile.getPath(), testResult);

	}

	public void setDevice(String device) {
		mDevice = device;

	}

	public String getDevice() {
		return mDevice;
	}

	public String getmPmd() {
		return mPmd;
	}

	public void setPmdDir(String mPmd) {
		this.mPmd = mPmd;
		Emma.info("pmd " + mPmd);
	}

	public Long getCurrentCoverage() {
		return mCurrentEmmaValue;
	}

	public void finish() {
		/**
		 * uninstall sga.apk
		 */
		uninstall(sga.SGA_PACKAGE);
		/**
		 * uninstall test project
		 */
		uninstall(mTestProjectPackage);

	}

	private void uninstall(String _package) {
		if (mDevice == null) {
			return;
		}
		Emma.info("uninstall :" + _package);
		if (_package == null) {
			if (ConfigApp.DEBUG) {
				System.out.println("uninstall package is null ");
			}
			return;
		}
		new UninstallCommand(getAdb(), mDevice, _package).execute();
		// new AntManager().exec(mSdkPath + File.separator + ConfigApp.ADBPATH +
		// " -s " + mDevice
		// + " uninstall " + _package);

	}

	private int mStrategyType;
	private String mProjectPackage;

	public void setStrategyType(String id) {
		mStrategyType = Integer.parseInt(id);

	}

	public int getStrategyType() {
		return mStrategyType;
	}

	public void init() throws IOException {
		initOutDirectories();
		// ***
		// set Instance of dataManager and retrieve test Data
		initTestData();
		// **
		// Premi�re Lancement des test pour instrumenter
		// *
		/***
		 * push data
		 */
		if (mDevice != null) {
			pushTestDataIntoDevice();
			uninstall(getProjectPackape());
			installProjectApk();
		}

	}

	/**
	 * @return
	 */
	public String getProjectPackape() {
		if (mProjectPackage == null) {
			if (Config.DEBUG) {
				System.out.println("getProjectPackape ");
				System.out.println("project package is null ");
			}
		}

		return mProjectPackage;
	}

	public void setProjectPackage(String package_) {
		mProjectPackage = package_;
	}

	/**
	 * 
	 */
	private void installProjectApk() {
		if (mProjectApk == null) {
			return;
		}
		InstallCommand install = new InstallCommand(getAdb(), getDevice(),
				getProjectApk().getPath());
		install.execute();
	}

	public String getAdb() {
		return mSdkPath + File.separator + ConfigApp.ADBPATH;
	}

	private int mThread_number = 1;// by default equal 1
	private File mProjectApk;
	private String mLauncherActivityOfAppUnderTest;

	public int getThread_number() {
		return mThread_number;
	}

	public void setThread_number(int thread_number) {
		if (thread_number == 0) {
			throw new IllegalStateException("thread_number must be > 0");
		}
		mThread_number = thread_number;
	}

	/**
	 * init the environment with a thread id
	 * 
	 * @param id
	 *            : - the id og the thread
	 * @throws IOException
	 */
	public void init(long id) throws IOException {
		/**
		 * generer un fichier de sortie pour chaque thread
		 */
		generateOutDirectory("" + id);
		init();

	}

	public void init(String id) throws IOException {
		generateOutDirectory(id);
		init();
	}

	private void generateOutDirectory(String id) {
		/**
		 * generer un fichier de sortie pour chaque thread
		 */
		File thread_file = new File((mOutDirectory != null ? getOutDirectory()
				: getDefaultOutDirectory()) + id);
		if (thread_file.exists()) {
			thread_file.mkdirs();
		}
		setOutDirectory(thread_file.getPath());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public SgdEnvironnement clone() throws CloneNotSupportedException {
		SgdEnvironnement sgdEnv = null;
		try {
			sgdEnv = (SgdEnvironnement) super.clone();
			if (mDatamanager != null)
				sgdEnv.mDatamanager = mDatamanager.clone();
			else {
				sgdEnv.mDatamanager = null;
			}
		} catch (CloneNotSupportedException cnse) {
			cnse.printStackTrace(System.err);
		}
		return sgdEnv;
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public String getScenarioDirectory() throws IOException {
		if (mScenarioDirectory == null) {
			initScenarioDirectory();
		}
		return mScenarioDirectory;
	}

	public String getScenarioDirectoryTemp() {
		File temp = new File(mScenarioDirectory + "_" + ConfigApp.TEMP);
		temp.mkdirs();
		return temp.getPath();
	}

	public String getPathDirectory() {
		if (mOutDirectory == null) {
			return getDefaultOutDirectory() + File.separator + ConfigApp.PATH;
		}
		return mOutDirectory + File.separator + ConfigApp.PATH;
	}

	public String getTimeDirectory() {
		if (mOutDirectory == null) {
			return getDefaultOutDirectory() + ConfigApp.TimeDirectory;
		}
		return mOutDirectory + ConfigApp.TimeDirectory;

	}

	/**
	 * @param string
	 */
	public void setProjectApk(String string) {
		mProjectApk = new File(string);
		if (!mProjectApk.exists()) {
			mProjectApk = null;
		}

	}

	/**
	 * @return
	 */
	public File getProjectApk() {
		return mProjectApk;
	}

	public void setTargetClass(String targetClass) {
		mTargetClass = targetClass;
	}

	public String getTargetClass() {
		return mTargetClass;
	}

	public void setErrorIndicator(String error) {
		mStopErrorCondition = error;
	}

	public String getErrorIndicator() {
		return mStopErrorCondition;
	}

	/**
	 * @return
	 */
	public String getLauncherActivityOfAppUnderTest() {
		return mLauncherActivityOfAppUnderTest;
	}

	public void setLauncherActivityOfAppUnderTest(String activity) {
		mLauncherActivityOfAppUnderTest = activity;
	}

	public double getStressMaxEvent() {
		return Double.parseDouble(mMAxEvent);
	}

	public void setStressMaxEvent(String event) {
		mMAxEvent = event;
	}

	public File getStressEventFile() {
		File eventFile = new File(getOutDirectory() + File.separator
				+ Config.EVENTS);
		System.out.println("event file" + eventFile.getPath());
		return eventFile;
	}

	public File getBruteEventFile() {
		return getStressEventFile();
	}

	public File getSuccTransitionFile() {
		File eventFile = new File(getOutDirectory() + File.separator
				+ Config.TR_OUT);
		System.out.println("event file" + eventFile.getPath());
		return eventFile;
	}

	public ScenarioData getModel() {
		return mModel;
	}

	public void setModel(ScenarioData mModel) {
		this.mModel = mModel;
	}

	/**
	 * @param string
	 */
	public void setModel(String pathToModel) {
		setModel(ScenarioParser.parse(new File(pathToModel)));
	}

	/**
	 * @param usedEmulator
	 */
	public void launchSga_class_defined(String usedEmulator) {
		String[] test = { "-tp", mTestProjectPackage, "-n", "1", "-adb",
				mSdkPath + File.separator + ConfigApp.ADBPATH, "-emu",
				usedEmulator, "-class", mTargetClass, "-stopError",
				mStopErrorCondition };
		launchSga_class_defined(test);
	}

	/**
	 * @param test
	 */
	private void launchSga_class_defined(String[] test) {
		if (mTargetClass == null) {
			throw new NullPointerException(
					"the target test Class is not defined \n please define -class parameters thank you");
		}
		Emma.info(" Launch sga test Runner...");
		Emma.info("parameters {" + test + "}");
		mSleeper.sleepShort();
		pushTestDataIntoDevice();
		/**
		 * lancement de sga
		 */
		Sga sga_launcher;
		try {
			sga_launcher = new Sga(test, mOutDirectory,
					Config.DEVICETESTRESULTS);
			sga_launcher.launchTest();
		} catch (Exception e) {
			e.printStackTrace();
		}

		pushTempFile();
		// sga.launchTestOnDevice(test);
	}

	/**
	 * 
	 */
	private void pushTempFile() {
		Emma.info("Push temp File:");
		PushCommand push = new PushCommand(mSdkPath + File.separator
				+ ConfigApp.ADBPATH, getDevice(), mOutDirectory
				+ File.separator + "temp", ConfigApp.DEVICETESTRESULTS);
		push.execute();
	}

	private String mCP;

	/**
	 * @param string
	 */
	public void setCP(String cp) {
		mCP = cp;
	}

	/**
	 * @return
	 */
	public String getCp() {
		return mCP;
	}

	/**
	 * path to put the name of database
	 */
	private File dbPath;

	/**
	 * @param string
	 */
	public void setDbName(String db) {
		dbPath = new File(db);
		if (!dbPath.exists()) {
			throw new NullPointerException(" db path does not exist");
		}

	}

	/**
	 * @return
	 */
	public String getDbPath() {
		return dbPath.getPath();
	}

	/**
	 * @return
	 */
	public File getDbFile() {
		return dbPath;
	}

	public String getCoveragePath() {
		if (mCoveragePath == null) {
			/**
			 * set�CoverageDirectory
			 */
			setCoveragePath(mOutDirectory + Config.DESKTOP_COVERAGE_DIRECTORY);

		}
		return mCoveragePath;
	}

	public void setCoveragePath(String covPath) {
		this.mCoveragePath = covPath;
	}

	public void setCoveragePath() {
		setCoveragePath(mOutDirectory + Config.DESKTOP_COVERAGE_DIRECTORY);
	}

	private long mMaxTime = 0L;

	/**
	 * @param string
	 */
	public void setMaxTime(String time) {
		if (time != null & time != "")
			mMaxTime = Long.parseLong(time);
	}

	/**
	 * @return
	 */
	public long getMaxTime() {
		return mMaxTime;
	}

	/**
	 * @return
	 */
	public String getManifestfilePath() {
		return mProjectPath + File.separator + "AndroidManifest.xml";
	}

	/**
	 * @return
	 */
	public String getFinalModelPath() {
		return mOutDirectory + File.separator + "out.xml";
	}

	/**
	 * @throws IOException
	 * 
	 */
	public void saveScreenShot() throws IOException {
		// pull in Robotium directory
		// must be renamed during tree completion
		File screenShootTemp = new File(mOutDirectory + File.separator
				+ Config.ROBOTIUM_SCREENSHOTS);
		System.out.println(screenShootTemp);
		if (!screenShootTemp.exists()) {
			screenShootTemp.mkdir();
		}
		PullCommand pull = new PullCommand(mSdkPath + File.separator
				+ ConfigApp.ADBPATH, mDevice, ConfigApp.DEVICESDCARD + "/"
				+ Config.ROBOTIUM_SCREENSHOTS, screenShootTemp.getPath());

		System.out.println(ConfigApp.DEVICESDCARD + "/"
				+ Config.ROBOTIUM_SCREENSHOTS);
		pull.execute();
		mSleeper.sleepLong();
		screenShootTemp.deleteOnExit();

		/**
		 * copier dans out
		 */
		// File screenShoot = new File(mOutDirectory + File.separator +
		// Config.ROBOTIUM_SCREENSHOTS);
		// for (File shot : screenShootTemp.listFiles()) {
		// Emma.savegeneric_file(shot, screenShoot, ".jpg");
		// FileUtils.forceDelete(shot);
		// }

	}

	public String getDicoPath() {
		return mDicoPath;
	}

	public void setDicoPath(String dicoPath) throws FileNotFoundException {
		if (!new File(dicoPath).exists()) {
			throw new FileNotFoundException(dicoPath + " does not exist");
		}
		this.mDicoPath = dicoPath;
	}

	private boolean mSga = false;

	@Override
	public void update(Observable arg0, Object arg1) {
		mSga = true;

	}

}
