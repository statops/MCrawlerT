package fr.openium.sga.Tests.ForSgaKit;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import kit.Command.DeletCommand;
import kit.Command.InstrumentationCommand;
import kit.Command.PullCommand;
import kit.Command.PushCommand;
import kit.Config.Config;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.Tests.ConfigTest;

public class CommandTest {
	@Test
	public void testPull() throws IOException {
		File dest = new File(Config.CURRENT_DIRECTORY
				+ "src/fr/openium/sga/Tests/ForSgaKit/temp");
		if (dest.exists()) {
			FileUtils.deleteDirectory(dest);
		}
		dest.mkdirs();
		PullCommand com = new PullCommand(ConfigTest.androidHome
				+ "/platform-tools/adb", "emulator-5554",
				"/mnt/sdcard/testResults", dest.toString());
		com.execute();
		assertTrue(dest.exists());
		assertTrue(dest.listFiles().length > 0);
		FileUtils.deleteDirectory(dest);
		// dest.deleteOnExit();
	}

	@Test
	public void testDelete() throws IOException {
		String adb = ConfigTest.androidHome + "/platform-tools/adb";
		String emu = "emulator-5554";
		String remoteFile = "/mnt/sdcard/testResults/TreeOut.xml";
		File fileToPush = new File(ConfigApp.CURRENT_DIRECTORY
				+ "/src/fr/openium/sga/Tests/ForSgaKit/TreeOut.xml");
		PushCommand com = new PushCommand(adb, emu, fileToPush.toString(),
				"/mnt/sdcard/testResults");
		com.execute();
		File dest = new File(ConfigApp.CURRENT_DIRECTORY + "/out/TreeOut.xml");
		if (dest.exists()) {
			dest.delete();
		}
		PullCommand com_pull = new PullCommand(adb, emu, remoteFile,
				dest.toString());
		com_pull.execute();
		assertTrue(dest.exists());
		dest.delete();

		DeletCommand deleter = new DeletCommand(adb, emu, remoteFile);
		deleter.execute();

		com_pull.execute();
		assertTrue(!dest.exists());

	}

	@Test
	public void testPush() {
		File fileToPush = new File(ConfigApp.CURRENT_DIRECTORY
				+ "/Tests/ForSgaKit/TreeOut.xml");

		PushCommand com = new PushCommand(ConfigTest.androidHome
				+ "s/platform-tools/adb", "emulator-5554",
				fileToPush.toString(), "/mnt/sdcard/testResults");
		com.execute();
		File dest = new File(ConfigApp.CURRENT_DIRECTORY + "/out/TreeOut.xml");
		if (dest.exists()) {
			dest.delete();
		}
		PullCommand com_pull = new PullCommand(ConfigTest.androidHome
				+ "/platform-tools/adb", "emulator-5554",
				"/mnt/sdcard/testResults/TreeOut.xml", dest.toString());
		com_pull.execute();
		assertTrue(dest.exists());
		dest.delete();
	}

	/**
	 * marche seulement sur ma machine windows
	 */
	/*
	 * @Test public void testInstr() { String ant = "ant";//
	 * System.getenv("ANT"); System.out.print(ant); InstrumentationCommand com =
	 * new InstrumentationCommand( ant,
	 * "/Users/Stassia/Documents/Scen-genWorkSpace/SgdExampleTestWithoutEmma",
	 * "emulator-5554", null, ConfigTest.androidHome); com.execute();
	 * 
	 * }
	 */
}
