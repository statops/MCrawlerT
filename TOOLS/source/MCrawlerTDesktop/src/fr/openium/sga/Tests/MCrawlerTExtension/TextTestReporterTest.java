package fr.openium.sga.Tests.MCrawlerTExtension;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;

import org.junit.Test;
import org.xml.sax.SAXException;

import fr.openium.sga.reporter.TextTestReporter;

public class TextTestReporterTest {
	@Test
	public void testReport() throws SAXException, IOException,
			ParserConfigurationException {
		ScenarioData value = ScenarioParser.parse(new File((new File("")
				.getAbsolutePath() + "/TEST/extension/jv/out.xml")));
		assertNotNull(value);
		File out = new File(
				(new File("").getAbsolutePath() + "/TEST/extension/jv/test.iosts"));
		TextTestReporter report=new TextTestReporter(value, out);
		report.generate();
		assertTrue(report.done);

	}
}
