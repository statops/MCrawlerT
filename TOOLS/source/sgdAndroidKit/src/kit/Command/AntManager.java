package kit.Command;

import java.io.InputStreamReader;

public class AntManager {

	public String debug(String buildpath, String sdkpath) {

		String commandLine = "ant debug -buildfile " + buildpath
				+ " -Dsdk.dir=" + sdkpath;
		return exec(commandLine);

	}

	private String mStdOut = null;
	private String mStdErr = null;

	/**
	 * launch ant installd
	 * 
	 * @param the
	 *            build.xml path the android sdk path
	 * @return the Std output
	 * */
	public String installd(String buildpath, String sdkpath) {
		mStdErr = null;
		String commandLine = "ant installd -buildfile " + buildpath
				+ " -Dsdk.dir=" + sdkpath;
		return exec(commandLine);
	}

	/**
	 * launch ant clean
	 * 
	 * @param the
	 *            build.xml path the android sdk path
	 * @return the Std output
	 * */
	public String clean(String buildpath, String sdkpath) {
		String commandLine = "ant clean -buildfile " + buildpath
				+ " -Dsdk.dir=" + sdkpath;
		return exec(commandLine);

	}

	/**
	 * launch command line
	 * 
	 * @param the
	 *            command line to execute
	 * 
	 * @return the Std output
	 * */
	public String exec(String commandLine) {

		Process mExec;
		StringBuilder stdOut = new StringBuilder();
		StringBuilder stdErr = new StringBuilder();
		try {
			mExec = Runtime.getRuntime().exec(commandLine);
			InputStreamReader r = new InputStreamReader(mExec.getInputStream());
			final char buf[] = new char[1024];
			int read = 0;
			while ((read = r.read(buf)) != -1) {
				if (stdOut != null)
					stdOut.append(buf, 0, read);
			}
			try {
				mExec.waitFor();
				if (mExec.exitValue() != 255) {
				} else {
				}

			} catch (InterruptedException ne) {
				ne.printStackTrace();
			}
			log("stdOut:  " + stdOut.toString());
			mStdOut = stdOut.toString();
			r = new InputStreamReader(mExec.getErrorStream());
			read = 0;
			while ((read = r.read(buf)) != -1) {
				if (stdErr != null)
					stdErr.append(buf, 0, read);
			}

			if (stdErr.length() != 0) {
				setStdErr(stdErr.toString());
				log("stdErr:  " + stdErr.toString());
			}
		} catch (Exception ex) {
			if (stdErr != null)
				stdErr.append("\n" + ex);
		}

		return mStdOut;
	}

	static void log(String log) {
		System.out.println(log);
	}

	public String getStdErr() {
		return mStdErr;
	}

	public void setStdErr(String mStdErr) {
		this.mStdErr = mStdErr;
	}

	public String clean_debug_Install(String buildpath, String sdkpath) {
		String commandLine = "ant clean debug install -buildfile " + buildpath
				+ " -Dsdk.dir=" + sdkpath;
		return exec(commandLine);
	}

}
