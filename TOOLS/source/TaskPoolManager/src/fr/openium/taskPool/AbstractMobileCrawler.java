package fr.openium.taskPool;




public abstract class AbstractMobileCrawler extends AbstractTaskPoolRunnable implements Priority, Comparable<AbstractMobileCrawler>{
	protected final ITask mTask;
	protected ITaskManager<?> mTaskManager;

	public AbstractMobileCrawler(ITask tasks) {
		mTask = tasks;
	}

	public AbstractMobileCrawler(ITask tasks, ITaskManager<?> manager) {
		mTask = tasks;
		mTaskManager = manager;
	}

	@Override
	public int compareTo(AbstractMobileCrawler other) {
		return new TaskComparator().compare(this, other);
	}

	@Override
	public int getPriority() {
		return getRank();
	}

	public abstract int getRank();

	public ITaskManager getTaskManager() {
		return mTaskManager;
	}

	public void setTaskManager(ITaskManager manager) {
		mTaskManager = manager;
	}
}
