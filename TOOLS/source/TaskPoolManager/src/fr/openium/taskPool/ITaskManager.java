package fr.openium.taskPool;

import java.util.List;

public interface ITaskManager<T extends AbstractMobileCrawler> {

	public void update(List<?> Tasks);

	public void execute();

	public List<T> getResult();

	public void endEmulator(String emulateurValue);

	public String getAvailableEmulator(long threadId);

	/**
	 * 
	 */
	public void stop();

	

}
