/**
 * 
 */
package fr.openium.sga.Tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import kit.Intent.StreamException;

import org.junit.Test;
import org.xml.sax.SAXException;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.Utils.ActivityCoverageUtils;

/**
 * @author Stassia
 * 
 */
public class ActivityCoverageUtilTest {

	@Test
	public void test() throws FileNotFoundException, SAXException, IOException,
			StreamException, ParserConfigurationException {
		File manifest = new File(ConfigApp.CURRENT_DIRECTORY
				+ "/src/fr/openium/sga/Tests/AndroidManifest.xml");

		File scen = new File(ConfigApp.CURRENT_DIRECTORY
				+ "/TEST/ActivityCoverage/scen.xml");
		ActivityCoverageUtils util = new ActivityCoverageUtils(manifest, scen);
		Double expected = 42.0;
		Double obtained = util.getActivityCoverage();
		System.out.println("obtained " + obtained);
		System.out.println("expected" + expected);
		assertEquals(expected, obtained);
		// assertEquals(expected, obtained);

	}
}
