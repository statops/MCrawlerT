package kit.Command;

import java.io.IOException;
import java.util.ArrayList;

public class UninstallCommand extends AbstractCommand implements ICommand {

	public UninstallCommand(String adb, String emulateur, String _package) {
		mArgs = new String[] { adb, "-s", emulateur, "uninstall", _package };
	}

	@Override
	public boolean execute() {
		ProcessBuilder pull_files = new ProcessBuilder(mArgs);
		System.out.println("Uninstall :" + mArgs.toString());
		try {
			Process p = pull_files.start();
			return translateOutput(p);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	protected boolean translateOutput(Process p) throws IOException {
		ArrayList<String> output = readProcessOutput(p);
		for (String debugOutput : output) {
			System.out.println("Uninstall.execute() :" + debugOutput);
		}
		return true;
	}

}
