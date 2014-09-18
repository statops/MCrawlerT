package fr.openium.sga.strategy;

import java.io.File;
import kit.Scenario.ScenarioData;
import kit.Scenario.State;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.ThreadSleeper;
import fr.openium.sga.Utils.Utils;
import fr.openium.sga.bissimulation.SgaGraph;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;

public class NormalStrategy extends AbstractStrategy implements IStrategy,
		IEmulateur_Client {

	private static final int MAX_TREE_SIZE = 100;
	private boolean remoteTestState;

	public NormalStrategy(SgdEnvironnement environment) {
		super(environment);
	}

	private static ThreadSleeper mSleeper;
	private static boolean testIsfinshed = false;
	public static boolean sgaIsfinshed = false;
	public static boolean sgaIsRunning = false;
	private static int mOccurrence = 0;

	@Override
	public CrawlResult getResult() throws Exception {
		mResult = new CrawlResult(
				new File(mSgdEnvironnement.getOutDirectory()), this);
		/**
		 * Creation d'un seul thread, un runnable
		 */

		/**
		 * init sleeper
		 */
		mSleeper = new ThreadSleeper();
		mSgdEnvironnement.init();

		if (!ConfigApp.ISTEST) {
			String results = mSgdEnvironnement.installSga();
			info("Install sgDevice status : " + results);
			info("TEST NUMBER " + (mOccurrence++));
			mSgdEnvironnement.instrument_with_Emma_and_install();
		}

		/**
		 * launch t estRunner (en boucle)
		 */
		testIsfinshed = true;
		Long initTime = System.currentTimeMillis();
		synchronized (mSgdEnvironnement) {
			do {
				if (mSgdEnvironnement.isCodeCoverageLimitReached()) {
					break;
				}
				if (mSgdEnvironnement.isTestDataCoverageLimitReached()) {
					break;
				}

				if (mResult != null
						&& mResult.getScenarioData() != null
						&& !mResult.getScenarioData().isEmpty()
						&& mResult.getScenarioData().getTrees().size() > MAX_TREE_SIZE) {
					break;
				}
				if (mResult != null && mResult.getScenarioData() != null
						&& !mResult.getScenarioData().isEmpty()
						&& mResult.getListNewStates().size() == 0) {
					break;
				}

				if (testIsfinshed) {
					final_operation(mSgdEnvironnement);
					info("TEST NUMBER " + mOccurrence++);
					Emma.delete_File_onDevice(ConfigApp.OkPath,
							mSgdEnvironnement, mSleeper);
					mSgdEnvironnement.launchSga_class_defined();
					remoteTestState = true;
					set_sga_is_finished(false);
					checkIfTestOnDeviceIsFinished(mSgdEnvironnement);
				}
				/**
				 * couverture mis à jour toutes les 10 secondes
				 */
				mSleeper.sleepLong();
				if (Utils.limit_time_isReached(initTime, Emma.TIME_LIMITE)) {
					pull(ConfigApp.OutXMLPath, mSgdEnvironnement);
					// mResult.setGraph(final_operation(mSgdEnvironnement));
					mSgdEnvironnement.pullCoverageAndGenerateReport();
					break;
				}

			} while (mSgdEnvironnement.getCoverageLimit() > mSgdEnvironnement
					.getCurrentCoverage());

		}
		// mResult.setScenarioData(mScenarioData)
		mResult.setGraph(final_operation(mSgdEnvironnement));
		info("End of test " + mSgdEnvironnement.getCurrentCoverage() + "%");
		// Pull les resultats de l'interaction avec le systeme
		pullMobileSystemObservers(mSgdEnvironnement);
		if (!ConfigApp.ISTEST) {
			mSgdEnvironnement.finish();
		}
		return mResult;
	}

	private synchronized SgaGraph final_operation(SgdEnvironnement env) {
		pullScenario(env);
		try {
			SgaGraph graphe = refine_save_Scenario(env.getOutDirectory());
			return graphe;
		} catch (NullPointerException e) {
			info("ERROR: " + e.getMessage());
		}
		return null;
	}

	protected SgaGraph refine_save_Scenario(String outDirectory) {
		File Outdirectory = new File(outDirectory);
		File ScenarioDirectory = new File(Outdirectory + ConfigApp.SCENARII);
		File[] scenarioDataFiles = ScenarioDirectory.listFiles();
		if (scenarioDataFiles.length == 0) {
			return null;
		}
		/*
		 * Refinement refinement_operation = new Refinement(scenarioDataFiles);
		 * SgaGraph[] result = refinement_operation.computeBissModel();
		 * result[1].display(); result[0].display();
		 */
		// mResult.setScenarioData(refinement_operation.getScenarioData());
		/**
		 * enregistrer sous forme dot
		 */
		/*
		 * try { FileUtils.writeStringToFile(new File(Outdirectory +
		 * ConfigApp.SCENARIO_REFINED_FILE), result[1].toDot(), Config.UTF8,
		 * false); } catch (IOException e) { e.printStackTrace(); }
		 */
		save(new File(Outdirectory + ConfigApp.SCENARIO_REFINED_FILE),
				new File(Outdirectory + ConfigApp.DOT_DIRECTORY), ".dot");

		// return result[1];
		return null;

	}

	private void checkIfTestOnDeviceIsFinished(SgdEnvironnement env) {
		Emulator_checker checker = new Emulator_checker(this, mSgdEnvironnement);
		checker.run();
	}

	protected synchronized static void set_sga_is_finished(boolean b) {
		testIsfinshed = b;

	}

	/**
	 * Not used
	 */
	@Override
	public boolean updateResult(Object result) {
		return false;
	}

	/**
	 * performed when ok file is available
	 */
	@Override
	public void update_state(boolean status) {
		Emma.info("Ok file is Present");
		set_sga_is_finished(status);
		/**
		 * delete OKFile on device
		 */
		Emma.delete_File_onDevice(ConfigApp.OkPath, mSgdEnvironnement, mSleeper);
		/** save ok file */
		File ok = new File(mSgdEnvironnement.getOutDirectory()
				+ ConfigApp.OkPath);
		save_ok(mSgdEnvironnement, ok);
		/** save rv_done file file */
		save_rv_done_directory(mSgdEnvironnement);
		/**
		 * save time of each test
		 */
		save_time(mSgdEnvironnement);
		/**
		 * refine and save scenario
		 */
		mResult.setGraph(final_operation(mSgdEnvironnement));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openium.sga.strategy.IStrategy#getRank(kit.Scenario.State,
	 * kit.Scenario.ScenarioData)
	 */
	@Override
	public int getRank(State st, ScenarioData path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean remoteTestState() {
		return remoteTestState;
	}

}
