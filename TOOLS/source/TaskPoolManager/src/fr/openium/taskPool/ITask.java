package fr.openium.taskPool;

public interface ITask {
	public int getRank();

	/**
	 * @param i
	 *            : incrementation of rank
	 */
	public void setRank(int i);
}
