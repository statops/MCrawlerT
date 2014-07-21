package fr.openium.taskPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import kit.Config.Config;

public class TaskManagerRunnable<T extends AbstractMobileCrawler> implements
		ITaskManager<T> {

	private List<T> mTaches = new ArrayList<T>();
	private List<T> futures = new ArrayList<T>();
	private HashSet<String> availableEmulateur = new HashSet<String>();
	private final int numberOfThread;
	private int remaining_futur = 0;
	private ThreadPoolExecutor executor;
	private boolean mAllResultReceived;
	private TaskComparator<T> mComparator;
	private EmulatorManager mEmulatorManager;

	public TaskManagerRunnable(ArrayList<T> tasks, int number_of_thread,
			TaskComparator<T> comparator, String adb) {
		mEmulatorManager = new EmulatorManager(adb, null,
				EmulatorManager.GET_AVAILABLE_EMULATEUR_LIST, null);
		init(tasks);
		numberOfThread = number_of_thread;
		mComparator = comparator;

	}

	private void init(ArrayList<T> tasks) {
		for (T tl : tasks) {
			addTasks(tl);
			futures.add(tl);
		}
		Collections.sort(futures, mComparator);
		initEmulator();
	}

	private void initEmulator() {
		availableEmulateur
				.addAll(mEmulatorManager.getList_Available_Emulator());
		if (availableEmulateur.isEmpty()) {
			throw new IllegalStateException("no avalaible emulator");
		} else
			System.out.println("emulateur list: "
					+ availableEmulateur.toString());
	}

	void init(List<T> initTask) {
		System.out.println("init mTaches addAll");
		mTaches.addAll(initTask);
	}

	public void reset() {
		System.out.println("Reset");
		mTaches.clear();
	}

	public void setTaches(List<T> listT) {
		System.out.println("setTaches");
		mTaches.addAll(listT);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute() {
		executor = new PriorityExecutor(numberOfThread, mComparator, this);
		runTasksPool();
		shutdownExecutorIfNoMoreTaskQueued();
	}

	private void shutdownExecutorIfNoMoreTaskQueued() {
		System.out
				.println("sutdownExecutorIfNoMoreTaskQueued  ; remaining_futur= "
						+ remaining_futur);
		if (this.remaining_futur <= 0
				&& (this.executor.getCompletedTaskCount() == this.executor
						.getTaskCount() || this.executor
						.getCompletedTaskCount() == this.executor
						.getTaskCount() - 1)) {
			mAllResultReceived = true;
			executor.shutdown();
		}
		System.out
				.println(String
						.format("[Threapool monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s",

						this.executor.getPoolSize(),

						this.executor.getCorePoolSize(),

						this.executor.getActiveCount(),

						this.executor.getCompletedTaskCount(),

						this.executor.getTaskCount(),

						this.executor.isShutdown(),

						this.executor.isTerminated()));

	}

	/**
	 * 
	 * @return the number of tasks launched
	 */
	private int launchTasks() {

		System.out.println("TaskManagerRunnable.launchTasks()");
		int taskNumber = mTaches.size();
		try {
			Iterator<T> taskIterator = mTaches.iterator();
			while (taskIterator.hasNext()) {
				T taskToPerform = taskIterator.next();
				executor.execute(taskToPerform);
			}
			System.out.println("clear launch task ");
			mTaches.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return taskNumber;

	}

	private void addTasks(T taskToPerform) {
		System.out.println("addTask mTaches.add");
		if (taskToPerform.getTaskManager() == null) {
			taskToPerform.setTaskManager(this);
		}
		mTaches.add(taskToPerform);
		Collections.sort(mTaches, mComparator);
		remaining_futur++;
		System.out.println("remainFutur: " + remaining_futur);
	}

	private void runTasksPool() {
		System.out.println("runTasksPool");
		launchTasks();
		/**
		 * wait for result
		 */
	}

	public List<T> getResult() {
		return futures;
	}

	@Override
	public synchronized void update(List<?> tasks) {
		System.out.print("update remain_futur:");
		updateResults((List<T>) tasks);
		if (executor.isShutdown()) {
			System.out.print("ShutDown:");
			return;
		}
		if (tasks == null) {
			shutdownExecutorIfNoMoreTaskQueued();
			return;
		}

		for (T task : (List<T>) tasks) {
			addTasks(task);
		}
		runTasksPool();
		shutdownExecutorIfNoMoreTaskQueued();
	}

	private void updateResults(List<T> tasks) {
		remaining_futur--;
		System.out.println("remaining_futur  :" + remaining_futur);
		if (tasks == null) {
			return;
		}
		for (T task : tasks) {
			futures.add(task);
		}
	}

	public boolean isAllResultReceived() {
		return mAllResultReceived;
	}

	@Override
	public void endEmulator(String emulateurValue) {
		if (executor.isShutdown()) {
			return;
		}
		synchronized (availableEmulateur) {
			availableEmulateur.add(emulateurValue);
		}
		System.out.println("TaskManagerRunnable.endEmulator() : "
				+ availableEmulateur.toString());

	}

	@Override
	public synchronized String getAvailableEmulator(long id) {
		synchronized (availableEmulateur) {
			if (!availableEmulateur.isEmpty()) {
				String emulateurValue = availableEmulateur.iterator().next();
				availableEmulateur.remove(emulateurValue);
				if (Config.DEBUG) {
					System.out.println("getAvailableEmulator");
					System.out
							.println("TaskManagerRunnable.getAvailableEmulator() ; affecteed to id ="
									+ id + "  value:" + emulateurValue);
					System.out.println("removed: " + emulateurValue);
				}
				return emulateurValue;
			} else {
				return null;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openium.taskPool.ITaskManager#stop()
	 */
	@Override
	public void stop() {
		if (executor.isShutdown()) {
			return;
		}
		// executor.
		executor.getQueue().clear();
		// executor.shutdownNow();
		executor.shutdown();
		// mAllResultReceived = true;
	}

	public boolean isTerminated() {
		return executor.isTerminated();
	}

}
