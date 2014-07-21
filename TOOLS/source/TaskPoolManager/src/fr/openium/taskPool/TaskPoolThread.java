package fr.openium.taskPool;

import kit.Config.Config;
import fr.openium.taskPool.AbstractTaskPoolRunnable;

public class TaskPoolThread extends Thread {
	public ITaskManager<AbstractMobileCrawler> mTaskManager;
	public String emulateurValue;
	public Runnable mTask;

	public TaskPoolThread(ITaskManager<AbstractMobileCrawler> taskManager,
			Runnable run) {
		mTaskManager = taskManager;
		System.err.println(run.getClass());
		if (run instanceof Runnable) {
			System.err.println("is Runnable");
		}
		if (run instanceof AbstractMobileCrawler) {
			System.err.println("is AbstractMobileCrawler");
		}
		if (run instanceof AbstractTaskPoolRunnable) {
			System.err.println("is AbstractTaskPoolRunnable");
		}
		mTask = run;
	}

	@Override
	public void run() {
		emulateurValue = null;
		int n = 0;
		do {
			System.out.println("waiting for emulator " + (n++) + "secondes");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			emulateurValue = mTaskManager.getAvailableEmulator(getId());
		} while (emulateurValue == null);

		if (Config.DEBUG) {
			System.out.print("======id  :" + getId());
			System.out.println("  emu  :" + emulateurValue + "===");
			;
		}
		mTask.run();
		mTaskManager.endEmulator(emulateurValue);
	}

	public String getUsedEmulator() {
		return emulateurValue;
	}

	@Override
	public void interrupt() {
		mTaskManager.endEmulator(emulateurValue);
		super.interrupt();
	}
}
