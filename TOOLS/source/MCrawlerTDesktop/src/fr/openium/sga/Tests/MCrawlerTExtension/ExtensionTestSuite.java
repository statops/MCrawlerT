package fr.openium.sga.Tests.MCrawlerTExtension;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ ManifestParserTest.class, ScenarioTest.class,
		IntentReaderTest.class, IntentCrawlerCodeGenerationTest.class })
public class ExtensionTestSuite {

}
