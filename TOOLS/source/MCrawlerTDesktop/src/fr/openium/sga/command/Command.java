package fr.openium.sga.command;

import java.io.File;

import fr.openium.automaticOperation.AntManager;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.emmatest.Emma;

public class Command {

	public Command() {

	}

	public static String generateReport(String sdkPath, String[] fileTomerge) {
		/**
		 * java -cp /Users/Stassia/android-sdks/tools/lib/emma.jar emma report
		 * -r html -in
		 * ../../sgdExample/bin/coverage.em,../../sgdExample/bin/coverage.ec
		 */
		String javaEmma = ConfigApp.JAVA_CP + sdkPath + ConfigApp.EMMAJARPATH + File.separator + ConfigApp.EMMAXMLREPORT + fileTomerge[0];
		Emma.info("[Command]: " + javaEmma);
		for (int i = 1; i < fileTomerge.length; i++) {
			javaEmma = javaEmma + "," + fileTomerge[i];
		}
		Emma.info("[Command]: " + javaEmma);
		AntManager antManager = new AntManager();
		return antManager.exec(javaEmma);

	}

	public class CommandLine extends AntManager {
		public CommandLine() {
			super();
		}

		public String execute(String[] args) {
			String commandLine = args[0];
			for (int i = 1; i < args.length; i++) {
				commandLine = commandLine + " " + args[i];
			}
			return super.exec(commandLine);
		}
	}

	public static String push(String adb, String desk, String device) {

		String[] args = new String[] { adb, desk, device };
		String commandLine = args[0];
		for (int i = 1; i < args.length; i++) {
			commandLine = commandLine + " " + args[i];
		}
		return new AntManager().exec(commandLine);
	}

	public static String pull(String adb, String desk, String device) {

		String[] args = new String[] { adb, desk, device };
		String commandLine = args[0];
		for (int i = 1; i < args.length; i++) {
			commandLine = commandLine + " " + args[i];
		}
		return new AntManager().exec(commandLine);
	}

}
