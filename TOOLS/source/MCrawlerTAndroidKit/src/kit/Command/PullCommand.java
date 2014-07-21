package kit.Command;

import java.io.IOException;
import java.util.ArrayList;

public class PullCommand extends AbstractCommand implements ICommand {

	public PullCommand(String adb, String emulateur, String fileToPull, String fileDestination) {
		mArgs = new String[] { adb, "-s", emulateur, "pull", fileToPull, fileDestination };
	}

	private Process pull;

	@Override
	public boolean execute() {
		/**
		 * stop pull after 10000ms
		 */

		ProcessBuilder pull_files = new ProcessBuilder(mArgs);

		Thread stopPull = new Thread("pullstop") {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Thread#run()
			 */
			@Override
			public void run() {
				int n = 1;
				do {

					try {
						Thread.sleep(5000);
						n = n + 5;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				} while (n < 30);
				if (pull != null)
					pull.destroy();

			}
		};

		stopPull.start();

		try {
			pull = pull_files.start();

			// System.out.println("PullCommand.execute() :" + mArgs.toString());

			return translateOutput(pull);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	protected boolean translateOutput(Process p) throws IOException {
		ArrayList<String> output = readProcessOutput(p);
		// System.out.println("PullCommand.execute() :");
		for (@SuppressWarnings("unused")
		String debugOutput : output) {
			// System.out.println(debugOutput);
		}
		return true;
	}

}
