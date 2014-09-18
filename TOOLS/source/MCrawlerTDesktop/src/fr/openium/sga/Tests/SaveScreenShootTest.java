/**
 * 
 */
package fr.openium.sga.Tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import kit.Config.Config;

import org.junit.Test;
import org.xml.sax.SAXException;

import fr.openium.sga.emmatest.SgdEnvironnement;

/**
 * @author Stassia
 * 
 */
public class SaveScreenShootTest {
	//private static final String TAG = SaveScreenShootTest.class.getName();

	@Test
	public void test2() throws SAXException, IOException, ParserConfigurationException {
		SgdEnvironnement env = new SgdEnvironnement();
		String out = Config.CURRENT_DIRECTORY + File.separator + "TEST" + File.separator + "OUTSCREENSHOOT";
		File outdir = new File(out);
		if (!outdir.exists()) {
			outdir.mkdir();
		}
		env.setOutDirectory(out);
		env.setDevice("304D19930672257E");
		env.setTestSdk(ConfigTest.androidHome);
		env.saveScreenShot();
		assertTrue(outdir.listFiles().length > 0);
		

	}
}
