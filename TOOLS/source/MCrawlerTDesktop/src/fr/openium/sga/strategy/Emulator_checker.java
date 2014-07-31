package fr.openium.sga.strategy;

import java.io.File;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.ThreadSleeper;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;

public class Emulator_checker extends Thread {
	private IEmulateur_Client mThreadClient;
	private SgdEnvironnement mSgdEnvironnement;

	public Emulator_checker(IEmulateur_Client androidCrawler, SgdEnvironnement sgd) {
		mThreadClient = androidCrawler;
		mSgdEnvironnement = sgd;
	}

	@Override
	public void run() {
		Emma.info("Check if sga test runner has finished");
		String outDirectory = mSgdEnvironnement.getOutDirectory();
		File ok = new File(outDirectory + ConfigApp.OkPath);
		do {
			try {
				Emma.pull(ConfigApp.OkPath, mSgdEnvironnement);
				sleep(ThreadSleeper.LONG);
			} catch (InterruptedException e) {
				Emma.info(getName() + " Thread interrupted");
			}
		} while (!ok.exists());

		Emma.info("ok file is available");
		mThreadClient.update_state(true);
	}

}
