package kit.Command;

import java.io.IOException;
import java.util.ArrayList;

public class PushCommand extends AbstractCommand implements ICommand {

	public PushCommand(String adb, String emulateur, String fileToPush, String fileDestination) {
		mArgs = new String[] { adb, "-s", emulateur, "push", fileToPush, fileDestination };
	}

	@Override
	public boolean execute() {
		ProcessBuilder pull_files = new ProcessBuilder(mArgs);
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
			System.out.println("PushCommand.execute() :" + debugOutput);
		}
		return true;
	}

}
