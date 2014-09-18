package fr.openium.sga.Tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import fr.openium.sga.Tests.ForSgaKit.GoToLastStateTest;

import fr.openium.sga.Tests.ForSgaKit.ScennarioParserTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({ ActivityCoverageUtilTest.class, AndroidCrawlerTest.class,
		CommandTest.class, CrawlResultTest.class, CrawlThreadTest.class,
		EmmaParserTest.class, RandomValueParserTest.class,
		SaveScreenShootTest.class, ScenarioCodeGenTest.class,
		ScenarioGeneratorTest.class, ScenarioToDotTest.class,
		SgUtilsTest.class, TreeOperationTest.class, GoToLastStateTest.class,
		ScenarioToDotTest.class, ScennarioParserTest.class })
/**
 * Non ajoute
 * ModelReporterTest.class (fichier dans ubuntu)
 *INJManagerTest.class,
 *INJRandomManagerTest.class,
 *DisplayerTest.class,
 *FourmystrategyTest.class,
 *StressTestManagerTest.class
 */
public class ASGDTestSuites {

}
