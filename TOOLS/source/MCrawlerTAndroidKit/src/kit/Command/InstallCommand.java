/**
 * 
 */
package kit.Command;

import java.io.IOException;
import java.util.ArrayList;

import kit.Config.Config;

/**
 * @author Stassia
 * 
 */
public class InstallCommand extends AbstractCommand implements ICommand {
	@SuppressWarnings("unused")
	private static final String TAG = InstallCommand.class.getName();

	/**
	 * 
	 * @param adb
	 * @param emulateur
	 * @param apk
	 */
	public InstallCommand(String adb, String emulateur, String apk) {
		mArgs = new String[] { adb, "-s", emulateur, "install", apk };
	}

	@Override
	public boolean execute() {
		ProcessBuilder install_apk = new ProcessBuilder(mArgs);
		try {
			Process p = install_apk.start();
			System.out.println("Install.execute() :" + p.toString());
			return translateOutput(p);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	protected boolean translateOutput(Process p) throws IOException {
		ArrayList<String> output = readProcessOutput(p);
		for (String debugOutput : output) {
			if (Config.DEBUG) {
				System.out.println("Install.execute() :" + debugOutput);
			}

		}
		return true;
	}

}
