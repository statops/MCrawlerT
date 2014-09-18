package fr.openium.sga.codegen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import kit.Intent.AndroidManifestComponent;
import kit.Intent.AndroidManifestParser;
import kit.Intent.MCrawlerTIntent;
import kit.Intent.ManifestData;
import kit.Intent.StreamException;
import kit.Utils.SgUtils;

import org.xml.sax.SAXException;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.SecurityTesting.BruteForceTest.BruteForceManager;
import fr.openium.sga.SecurityTesting.INJTest.INJRandomTaskManager;
import fr.openium.sga.SecurityTesting.INJTest.INJRequestTaskManager;
import fr.openium.sga.SecurityTesting.StressTest.StressTaskManager;
import fr.openium.sga.strategy.AbstractStrategy;
import fr.openium.sga.strategy.IntentLauncherStrategy;
import fr.openium.sga.strategy.NormalStrategy;

public class ScenarioCodeGen {
	private ManifestData mManifestdata = null;
	private String pairwiseSequence = "" + 1;
	private String strategy = "" + 1;
	private final File out;
	private String mExpectedActivity = null;
	/**
	 * template
	 */
	// private final String TEMPLATE_PATH = ConfigApp.CURRENT_DIRECTORY +
	// "/codegen/unitTestTemplate";
	private final String TEMPLATE = "static {try {Main = Class.forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);} catch (ClassNotFoundException e) {throw new RuntimeException(e);}}"
			+ "public %s() {super(TARGET_PACKAGE_ID, Main);} protected void setUp() throws Exception {PACKAGE_ID=TARGET_PACKAGE_ID;INITIAL_STATE_ACTIVITY_FULL_CLASSNAME = EXPECTED_INITIAL_STATE_ACTIVITY;super.setPairwiseSequenceNumber(\"\"+ PAIR_SEQUENCE );super.setUp();} ";

	public ScenarioCodeGen(File manifestXml, File output) {
		setManifestdata(manifestXml);
		this.out = output;
	}

	private void setManifestdata(File manifestXml) {
		if (ConfigApp.DEBUG) {
			System.out.println("Manifest Path" + manifestXml.getPath());
		}
		try {
			InputStream manifestStream = new FileInputStream(manifestXml);
			mManifestdata = AndroidManifestParser.parse(manifestStream);
		} catch (SAXException e) {
		} catch (IOException e) {
			e.printStackTrace();
		} catch (StreamException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	/***
	 * generate the unit test for ManifestDara and Manifest File
	 * 
	 */
	private static final String MAIN = "android.intent.action.MAIN";
	private static final String LAUNCHER = "android.intent.category.LAUNCHER";

	public void generate() throws JClassAlreadyExistsException, IOException {
		/**
		 * the package name
		 */
		String packageName = mManifestdata.getPackage();
		/**
		 * get the Launcher activity
		 */
		String ActivityName = null;

		if (mLauncherActivity != null) {
			ActivityName = mLauncherActivity;
		} else
			for (AndroidManifestComponent act : mManifestdata.getComponents()) {
				for (MCrawlerTIntent intent : act.getIntent()) {
					if (intent.getActions().contains(MAIN)
							&& intent.getCategories().contains(LAUNCHER)) {
						ActivityName = act.getName();
						break;
					}

				}
				if (ActivityName != null)
					break;

			}
		if (ActivityName == null)
			throw new NullPointerException("There is no Launcher Activity");

		/**
		 * read the code Template
		 */
		// StringBuilder unitTestTemplate = new apkJTc().getText(new
		// File(TEMPLATE_PATH));

		JCodeModel cm = new JCodeModel();
		JDefinedClass dclass;
		addImportedClass(cm);

		String testName = null;
		;
		JClass SgdTestCase = null;
		switch (Integer.parseInt(strategy)) {
		case NormalStrategy.DFS_STRATEGY_ID:
			SgdTestCase = cm.ref(kit.TestRunner.SgdTestCase.class);
			testName = ".test.MainTest";
			break;
		case NormalStrategy.FOURMY_STRATEGY_ID:
			SgdTestCase = cm.ref(kit.TestRunner.SgdFourmyStrategy.class);
			testName = ".test.MainTest";
			break;
		case AbstractStrategy.LOGGING_STRATEGY_ID:
			SgdTestCase = cm.ref(kit.TestRunner.SgdFourmyStrategy.class);
			testName = ".test.MainTest";
			break;
		case StressTaskManager._STRATEGY_ID:
			SgdTestCase = cm.ref(kit.Stress.StressRunner.class);
			testName = ".test.StressTest";
			break;
		case INJRequestTaskManager._STRATEGY_ID:
			SgdTestCase = cm.ref(kit.Inj.SQLRequestInjectionTestRunner.class);
			testName = ".test.InjTest";
			break;
		case INJRandomTaskManager._STRATEGY_ID:
			SgdTestCase = cm.ref(kit.Inj.SQLRandomDataInjectionRunner.class);
			testName = ".test.InjRandomTest";
			break;
		case BruteForceManager._STRATEGY_ID:
			SgdTestCase = cm.ref(kit.BruteForce.BruteForceRunner.class);
			testName = ConfigApp.BRUTEFORCE_TEST;
			break;
		case IntentLauncherStrategy._STRATEGY_ID:
			SgdTestCase = cm.ref(kit.TestRunner.IntentRunner.class);
			testName = ConfigApp.INTENTCRAWLER_TEST;
			break;
		default:
			throw new NullPointerException(
					"Please choose a correct strategy type");
		}
		if (ConfigApp.DEBUG) {
			System.out.println("Class name " + packageName + testName);
		}
		dclass = cm._class(packageName + testName);
		JClass mextends = cm.ref(SgdTestCase.fullName());
		dclass._extends(mextends);
		/**
		 * PairWise number
		 */
		JVar PAIRWISENUMBER = dclass.field(JMod.PRIVATE + JMod.STATIC, String,
				"PAIR_SEQUENCE");
		PAIRWISENUMBER.init(JExpr.lit(pairwiseSequence));

		/**
		 * the target package ID
		 */
		JVar TARGET_PACKAGE_ID = dclass.field(JMod.PRIVATE + JMod.STATIC
				+ JMod.FINAL, String, "TARGET_PACKAGE_ID");
		TARGET_PACKAGE_ID.init(JExpr.lit(packageName));
		// private final static String
		// EXPECTED_INITIAL_STATE_ACTIVITY_FULL_CLASSNAME =
		// "fr.openium.converterexample.MainActivity";
		/**
		 * the LauncherActivity
		 */
		JVar LAUNCHER_ACTIVITY_FULL_CLASSNAME = dclass.field(JMod.PRIVATE
				+ JMod.STATIC + JMod.FINAL, String,
				"LAUNCHER_ACTIVITY_FULL_CLASSNAME");
		JVar EXPECTED_INITIAL_STATE_ACTIVITY = dclass.field(JMod.PRIVATE
				+ JMod.STATIC, String, "EXPECTED_INITIAL_STATE_ACTIVITY");
		LAUNCHER_ACTIVITY_FULL_CLASSNAME.init(JExpr.lit(ActivityName));
		if (mExpectedActivity != null)
			EXPECTED_INITIAL_STATE_ACTIVITY.init(JExpr.lit(mExpectedActivity));

		dclass.direct(java.lang.String.format(TEMPLATE, shortname(testName)));
		// dclass.direct(unitTestTemplate.toString());
		cm.build(out);

	}

	/**
	 * @param testName
	 * @return
	 */
	private String shortname(java.lang.String testName) {
		return SgUtils.shortName(testName);
	}

	/***
	 * all used class
	 */
	JClass String;
	private java.lang.String mLauncherActivity;

	private void addImportedClass(JCodeModel cm) {
		String = cm.ref(String.class.getName());

	}

	public void setPairwiseSequence(String mPairwiseSequenceNumber) {
		pairwiseSequence = mPairwiseSequenceNumber;
	}

	public void setStrategyType(String mStrategy) {
		strategy = mStrategy;

	}

	public String getExpectectedActivity() {
		return mExpectedActivity;
	}

	public void setExpectectedActivity(String ExpectectedActivity) {
		mExpectedActivity = ExpectectedActivity;
	}

	public void setLauncherActivity(String launchActivity) {
		mLauncherActivity = launchActivity;
	}

}
