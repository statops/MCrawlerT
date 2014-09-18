package fr.openium.sga.strategy;

import java.io.File;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.ThreadSleeper;
import fr.openium.sga.Utils.Utils;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;

public class Emulator_checker extends Thread {
	private IEmulateur_Client mThreadClient;
	private SgdEnvironnement mSgdEnvironnement;

	public Emulator_checker(IEmulateur_Client androidCrawler,
			SgdEnvironnement sgd) {
		mThreadClient = androidCrawler;
		mSgdEnvironnement = sgd;
	}

	@Override
	public void run() {
		Emma.info("Check if sga test runner has finished ...");
		String outDirectory = mSgdEnvironnement.getOutDirectory();
		File ok = new File(outDirectory + ConfigApp.OkPath);
		do {
			try {
				Utils.pull(ConfigApp.OkPath, mSgdEnvironnement);
				sleep(ThreadSleeper.LONG);
			} catch (InterruptedException e) {
				Emma.info(getName() + " Thread interrupted");
			}
			System.out.print(".");
			if (mThreadClient.remoteTestState()) {
				break;
			}
		} while (!ok.exists());

		System.out.println("ok file is available or test is finished");
		mThreadClient.update_state(true);
	}

}
