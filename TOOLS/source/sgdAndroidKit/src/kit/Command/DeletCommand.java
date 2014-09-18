package kit.Command;

import java.io.IOException;
import java.util.ArrayList;

public class DeletCommand extends AbstractCommand implements ICommand {

	public DeletCommand(String adb, String device, String fileToDelete) {
		mArgs = new String[] { adb, "-s", device, "shell", "rm", fileToDelete };
	}

	@Override
	public boolean execute() {
		ProcessBuilder pull_files = new ProcessBuilder(mArgs);
		try {
			Process p = pull_files.start();
			System.out.println("DeleteCommand.execute() :" + p.toString());
			return translateOutput(p);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	protected boolean translateOutput(Process p) throws IOException {
		ArrayList<String> output = readProcessOutput(p);
		System.out.println("DleteCommand.output :");
		for (String debugOutput : output) {
			System.out.println(debugOutput);
		}
		return true;
	}

}
