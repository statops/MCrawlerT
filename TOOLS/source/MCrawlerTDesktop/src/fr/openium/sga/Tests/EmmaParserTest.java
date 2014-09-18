package fr.openium.sga.Tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import fr.openium.sga.emmatest.EmmaParser;

public class EmmaParserTest {

	@Test
	public void test() throws SAXException, IOException,
			ParserConfigurationException {
		EmmaParser pa = new EmmaParser();
		pa.parse(new File(new File("").getAbsolutePath()
				+ "/TEST/coverage/coverage.xml"), null);

		String pourcentage = pa.getClassCoverage();
		pourcentage = pourcentage.substring(0, pourcentage.indexOf("%") - 1);
		assertEquals(27, Long.parseLong(pourcentage));
		pourcentage = pa.getMethodCoverage();
		pourcentage = pourcentage.substring(0, pourcentage.indexOf("%") - 1);
		assertEquals(15, Long.parseLong(pourcentage));
		pourcentage = pa.getBlockCoverage();
		pourcentage = pourcentage.substring(0, pourcentage.indexOf("%") - 1);
		assertEquals(9, Long.parseLong(pourcentage));
		pourcentage = pa.getLineCoverage();
		pourcentage = pourcentage.substring(0, pourcentage.indexOf("%") - 1);
		assertEquals(10, Long.parseLong(pourcentage));

	}
}
