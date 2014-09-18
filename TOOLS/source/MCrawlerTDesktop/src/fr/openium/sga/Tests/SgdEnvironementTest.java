package fr.openium.sga.Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import kit.Command.PullCommand;
import kit.Command.PushCommand;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;

public class SgdEnvironementTest {

	@Test
	public void testInitRemoteDirectory() throws IOException {

		String[] params = new String[] { "explore", "-p",
				"/Users/Stassia/Documents/Scen-genWorkSpace/sgdExample", "-tp",
				"/Users/Stassia/Documents/Scen-genWorkSpace/SgdExampleTestWithoutEmma", "-coverage", "70",
				"-tpackage", "fr.openium.example.exampleforsgd.test", "-sdk", "/Users/Stassia/android-sdks",
				"-out", new File("").getAbsolutePath() + "/out", "-arv",
				new File("").getAbsolutePath() + "\\TestData\\testData.xml", "-strategy", "1", "-thread",
				"2", "-emu", "emulator-5554", "-tpackage", "package", "-strategy", "0", "-thread", "10",
				"-class", "class", "-stopError", "false" };

		SgdEnvironnement model = Emma.init_environment_(params);
		assertNotNull("enter the required parameter", model);
		if (ConfigApp.DEBUG) {
			System.out.println("debug");
		}
		File temp = new File(model.getOutDirectory() + File.separator + "temp");

		if (!temp.exists()) {
			temp.mkdir();
		} else {
			FileUtils.deleteDirectory(temp);
			assertTrue(!temp.exists());
			temp.mkdir();
		}
		/**
		 * pusher quelque chose
		 */

		File toPush = new File(temp.getPath() + File.separator + "temp");
		toPush.createNewFile();
		new PushCommand(model.getAdb(), model.getDevice(), toPush.getPath(), ConfigApp.DEVICECOVERAGE)
				.execute();
		toPush.delete();
		/**
		 * method to test
		 */
		model.initRemoteDirectory();

		PullCommand pull = new PullCommand(model.getAdb(), model.getDevice(), ConfigApp.DEVICECOVERAGE,
				temp.getPath());
		pull.execute();
		/**
		 * temp must be null
		 */
		assertTrue(temp.exists());
		assertEquals(0, temp.listFiles().length);
		FileUtils.deleteDirectory(temp);
	}

	@Test
	public void testClone() throws IOException, CloneNotSupportedException {

		String[] params = new String[] { "explore", "-p",
				"/Users/Stassia/Documents/Scen-genWorkSpace/sgdExample", "-tp",
				"/Users/Stassia/Documents/Scen-genWorkSpace/SgdExampleTestWithoutEmma", "-coverage", "70",
				"-tpackage", "fr.openium.example.exampleforsgd.test", "-sdk", "/Users/Stassia/android-sdks",
				"-out", new File("").getAbsolutePath() + "/out", "-arv",
				new File("").getAbsolutePath() + "\\TestData\\testData.xml", "-strategy", "1", "-thread",
				"2", "-emu", "emulator-5554", "-tpackage", "package", "-strategy", "0", "-thread", "10",
				"-class", "class", "-stopError", "false" };
		SgdEnvironnement model = Emma.init_environment_(params);
		assertNotNull("enter the required parameter", model);
		SgdEnvironnement modelbis = model.clone();
		assertEquals(model.getAdb(), modelbis.getAdb());
		assertEquals(model.getDevice(), modelbis.getDevice());

	}
}
