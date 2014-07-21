package fr.openium.taskPool;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PriorityExecutor<T extends Runnable> extends ThreadPoolExecutor {

	public PriorityExecutor(int threadNumber, Comparator<? super Runnable> comparator,
			ITaskManager<AbstractMobileCrawler> taskManager) {
		super(threadNumber, threadNumber, 600L, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>(11,
				comparator), new TaskPoolThreadFactory(taskManager));

	}
}
