package fr.openium.taskPool;

import java.util.concurrent.ThreadFactory;

public class TaskPoolThreadFactory implements ThreadFactory {
	ITaskManager<AbstractMobileCrawler> mTaskManager;

	public TaskPoolThreadFactory(ITaskManager<AbstractMobileCrawler> taskManager) {
		mTaskManager = taskManager;
	}

	@Override
	public Thread newThread(Runnable task) {
		return new TaskPoolThread(mTaskManager, task);
	}
}
