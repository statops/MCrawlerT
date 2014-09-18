/**
 * 
 */
package kit.Utils;

import java.io.File;
import java.io.IOException;

import kit.Config.Config;
import kit.Stress.Event;

import org.apache.commons.io.FileUtils;

/**
 * @author Stassia
 * 
 */
public class WriterEvent extends Thread {
	/**
	 * 
	 */
	Event mEvent;
	File mfileToSaveEvent;

	public WriterEvent(String name, Event ev, File toSave) {
		super(name);
		mEvent = ev;
		mfileToSaveEvent = toSave;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try {
			FileUtils.writeStringToFile(mfileToSaveEvent, getName()
					+ Config.EVENTS_DELIMITERS + mEvent.toString() + "\n",
					Config.UTF8, true);
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
}
