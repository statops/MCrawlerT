package fr.openium.sga.Tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;


import org.apache.commons.io.FileUtils;
import org.junit.Test;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.command.Command;

public class CommandTest {

	private String androidHome = System.getenv("HOME") + "/android-sdks";

	@Test
	public void test_for_experimental_results() {
		File coverage = new File(ConfigApp.CURRENT_DIRECTORY + "/TEST/coverage");
		if (coverage.exists()) {
			File[] f = coverage.listFiles();
			if (f.length > 0) {

				String[] s = new String[f.length + 1];
				/**
				 * add coverage.em path
				 */
				File em = new File(ConfigApp.CURRENT_DIRECTORY
						+ "/TEST/coverage/coverage.em");
				if (em.exists())
					s[0] = em.getPath();
				else {
					throw new NullPointerException(" File does not exist"
							+ em.getPath());
				}

				for (int i = 0; i < f.length; i++) {
					if (f[i].isDirectory()) {
						s[i + 1] = em.getPath();
						continue;
					}
					s[i + 1] = f[i].getPath();

				}
				Command.generateReport(androidHome, s);
				assertTrue("xml file exist", new File(
						ConfigApp.CURRENT_DIRECTORY
								+ "/coverage.xml").exists());
				assertTrue("html file exist", new File(
						ConfigApp.CURRENT_DIRECTORY
								+ "/coverage/index.html").exists());
				 new File(
							ConfigApp.CURRENT_DIRECTORY
									+ "/coverage.xml").deleteOnExit();
				 new File(
							ConfigApp.CURRENT_DIRECTORY
									+ "/coverage/index.html").deleteOnExit();

			}
		}

	}

	@Test
	public void test() throws IOException {
		String[] s = new String[] {
				ConfigApp.CURRENT_DIRECTORY + "/TEST/coverage/coverage.em",
				ConfigApp.CURRENT_DIRECTORY + "/TEST/coverage/coverage.ec",
				ConfigApp.CURRENT_DIRECTORY + "/TEST/coverage/coverage.ec",
				ConfigApp.CURRENT_DIRECTORY + "/TEST/coverage/mcoverage1ec" };
		Command.generateReport(androidHome, s);
		assertTrue(new File(ConfigApp.CURRENT_DIRECTORY + "/coverage.xml")
				.exists());
		assertTrue(new File(ConfigApp.CURRENT_DIRECTORY + "/coverage").exists());
		new File(ConfigApp.CURRENT_DIRECTORY + "/coverage.xml").delete();
		FileUtils.deleteDirectory(new File(ConfigApp.CURRENT_DIRECTORY
				+ "/coverage"));
		// Emma.isCodeCoverageLimitReached();

	}

	

	@Test
	public void testCoverageFileUnit() {
		File coverage = new File(ConfigApp.CURRENT_DIRECTORY
				+ "/TEST/coverage/coverage.ec");
		if (coverage.exists()) {

			String[] s = new String[2];
			/**
			 * add coverage.em path
			 */
			File em = new File(ConfigApp.CURRENT_DIRECTORY
					+ "/TEST/coverage/coverage.em");
			if (em.exists())
				s[0] = em.getPath();
			else {
				throw new NullPointerException(" File does not exist"
						+ em.getPath());
			}
			s[1] = coverage.getPath();
			Command.generateReport(androidHome, s);
			assertTrue("xml file exist", new File(
					ConfigApp.CURRENT_DIRECTORY
							+ "/coverage.xml").exists());
			assertTrue("html file exist", new File(
					ConfigApp.CURRENT_DIRECTORY
							+ "/coverage/index.html").exists());
			 new File(
						ConfigApp.CURRENT_DIRECTORY
								+ "/coverage.xml").deleteOnExit();
			 new File(
						ConfigApp.CURRENT_DIRECTORY
								+ "/coverage/index.html").deleteOnExit();
		}
		

	}

}
