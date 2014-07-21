package kit.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public abstract class AbstractCommand {
	protected String[] mArgs;

	protected abstract boolean translateOutput(Process p) throws IOException;

	protected ArrayList<String> readProcessOutput(Process p) throws IOException {
		ArrayList<String> Bline = new ArrayList<String>();
		InputStream is = p.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String ligne;
		while ((ligne = br.readLine()) != null) {
			Bline.add(ligne);
		}
		return Bline;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return mArgs.toString();
	}

}
