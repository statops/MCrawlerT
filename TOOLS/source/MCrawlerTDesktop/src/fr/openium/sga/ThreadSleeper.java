package fr.openium.sga;

public class ThreadSleeper {
	public static final int LONG = 6000;
	public static final int SHORT = 1000;
	public static final int MEDIUM = 3000;

	public void sleepLong() {
		sleep(LONG);
	}

	private void sleep(int timeOfStop) {
		try {
			Thread.sleep(timeOfStop);
		} catch (InterruptedException not_Handle) {
		}

	}

	public void sleepShort() {
		sleep(SHORT);
	}

	public void sleepMedium() {
		sleep(MEDIUM);
	}

}
