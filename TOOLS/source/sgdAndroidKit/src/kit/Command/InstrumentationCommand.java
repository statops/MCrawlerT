package kit.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import kit.Config.Config;

public class InstrumentationCommand extends AbstractCommand implements ICommand {
	private String mStdOut = null;
	private String mStdErr = null;

	private String command;

	public InstrumentationCommand(String ant, String testProjectPath, String device, String pmd,
			String sdkPath) {
		command = "ant -buildfile " + testProjectPath + Config.BUILDXML + " -Dadb.device.arg=\"-s " + device
				+ "\" " + "-Dsdk.dir=\"" + sdkPath + "\"" + " -Dpmd.dir=\"" + pmd + "\"" + " "
				+ Config.ANT_EMMA_COMMAND;

	}

	@Override
	public boolean execute() {
		Process process;
		try {
			process = Runtime.getRuntime().exec(command);
			InputStreamReader r = new InputStreamReader(process.getInputStream());
			getOut(process, r);
			getError(process, r);
		} catch (Exception ex) {

		}
		if (Config.DEBUG) {
			System.out.println(mStdOut.toString());
		}
		return mStdErr == null ? true : false;
	}

	private void getOut(Process process, InputStreamReader r) throws IOException {
		StringBuilder stdOut = new StringBuilder();
		final char buf[] = new char[1024];
		int read = 0;
		while ((read = r.read(buf)) != -1) {
			if (stdOut != null) {
				stdOut.append(buf, 0, read);
			}
		}
		try {
			process.waitFor();
		} catch (InterruptedException ne) {
			ne.printStackTrace();
		}
		mStdOut = stdOut.toString();
	}

	private void getError(Process p, InputStreamReader r) throws IOException {
		StringBuilder stdErr = new StringBuilder();
		final char buf[] = new char[1024];
		r = new InputStreamReader(p.getErrorStream());
		int read = 0;
		while ((read = r.read(buf)) != -1) {
			if (stdErr != null)
				stdErr.append(buf, 0, read);
		}

		if (stdErr.length() != 0) {
			mStdErr = mStdErr + (stdErr.toString());
		}

	}

	protected boolean translateOutput(Process p) throws IOException {
		ArrayList<String> output = readProcessOutput(p);
		for (String debugOutput : output) {
			System.out.println("Instrumentation.execute() :" + debugOutput);
		}
		if (p.getErrorStream() != null) {
			ArrayList<String> Bline = new ArrayList<String>();
			InputStream is = p.getErrorStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String ligne;
			while ((ligne = br.readLine()) != null) {
				Bline.add(ligne);
				System.out.println("Instrumentation.execute():" + ligne);
			}
			if (!Bline.isEmpty())
				throw new IllegalStateException();
			// return false;

		}

		return true;
	}

}
