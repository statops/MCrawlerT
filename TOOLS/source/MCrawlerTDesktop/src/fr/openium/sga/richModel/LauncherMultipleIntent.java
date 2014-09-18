package fr.openium.sga.richModel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import kit.Config.Config;
import kit.Intent.MCrawlerTIntent;
import kit.Intent.StartingIntentGenerator;
import kit.Intent.StreamException;
import kit.Scenario.Action;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Widget;

import org.xml.sax.SAXException;

import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;
import fr.openium.sga.strategy.AbstractStrategy;
import fr.openium.sga.strategy.IntentLauncherStrategy;

public class LauncherMultipleIntent extends AbstractStrategy {
	/**
	 * List of Results
	 * 
	 * @param arg
	 */
	HashMap<MCrawlerTIntent, CrawlResult> mListResults = new HashMap<MCrawlerTIntent, CrawlResult>();
	private String mOutdirectory;

	public LauncherMultipleIntent(SgdEnvironnement env) {
		super(env);
		mOutdirectory = "" + env.getOutDirectory();
	}

	@Override
	public int getRank(State st, ScenarioData path) {
		return 0;
	}

	/**
	 * combine AllResults in a CrawlerResults
	 */
	@Override
	public CrawlResult getResult() throws Exception {

		for (MCrawlerTIntent launcherIntent : generateListOfInitialState()) {
			launchEachIntent(launcherIntent);
		}
		/**
		 * combine results
		 */
		mResult = mergeResults();
		return mResult;
	}

	private CrawlResult mergeResults() {
		// TODO Auto-generated method stub
		/**
		 * pour le moment un seul
		 */
		for (MCrawlerTIntent key : mListResults.keySet()) {
			if (mListResults.get(key) != null
					&& mListResults.get(key).getScenarioData() != null) {
				mResult = mListResults.get(key);
				break;
			}

		}
		return mResult;
	}

	private void launchEachIntent(MCrawlerTIntent launcherIntent)
			throws Exception {
		/**
		 * create a new report environment for each intent
		 */
		SgdEnvironnement env = mSgdEnvironnement.clone();
		env.setOutDirectory(mOutdirectory + mListResults.size() + "_");
		env.initOutDirectories();
		IntentLauncherStrategy strategy = new IntentLauncherStrategy(env, null,
				launcherIntent);
		CrawlResult result = buildIntentTransition(launcherIntent,
				strategy.getResult());
		// generate the out.xml here
		new ScenarioGenerator(env.getOutDirectory() + Config.OutXMLPath)
				.generateXml(result.getScenarioData());
		mListResults.put(launcherIntent, result);
	}

	private ArrayList<MCrawlerTIntent> generateListOfInitialState()
			throws FileNotFoundException, SAXException, IOException,
			StreamException, ParserConfigurationException {

		StartingIntentGenerator gen = new StartingIntentGenerator(
				mSgdEnvironnement.getManifestfilePath());
		return gen.generate();
	}

	public HashMap<MCrawlerTIntent, CrawlResult> getListResults()
			throws Exception {
		getResult();
		return mListResults;
	}

	public CrawlResult buildIntentTransition(MCrawlerTIntent intent,
			CrawlResult result) {
		ScenarioData scen = new ScenarioData();
		scen.setVersion("" + 0);
		scen.setActions(new Action("intent"));
		State source = buildState(intent.getComponentName(), ""
				+ (Integer.MAX_VALUE - mListResults.size()));
		source.setType(Scenario.SOURCE);
		source.setCalledWithIntent("" + true);
		State dest = buildState(Scenario.END, ""
				+ (Integer.MAX_VALUE - mListResults.size()));
		dest.setType(Scenario.DEST);
		if (result.getScenarioData() == null) {
			scen.setStates(source, true, false);
			scen.setStates(dest, false, true);
			buildTransitions(scen, source, dest, intent);
			result.setScenarioData(scen);
			return result;
		}

		result.getScenarioData().setStates(source, false, false);
		// result.getScenarioData().setStates(dest, false, true);
		try {
			dest = result.getScenarioData().getInitialState().clone();
			dest.setType(Scenario.DEST);
			dest.setId(source.getId());
			result.getScenarioData().setStates(dest, false, true);
		} catch (CloneNotSupportedException e) {
		}
		buildTransitions(result.getScenarioData(), source, dest, intent);
		return result;

	}

	private void buildTransitions(ScenarioData scen, State source, State dest,
			MCrawlerTIntent intent) {
		Action action = new Action("intent", new Widget("", "", "", "", "", "",
				"", "", "", intent.toString(), "", "", "", "", "", "", ""));
		action.updateProperties(MCrawlerTIntent.ActionElements.ACTION, intent
				.getActions().iterator().next());
		action.updateProperties(MCrawlerTIntent.CategoryElements.CATEGORY,
				intent.getCategories() == null
						|| intent.getCategories().isEmpty() ? "" : intent
						.getCategories().iterator().next());
		action.updateProperties(MCrawlerTIntent.DataElements.DATA,
				intent.getIntentData() == null
						|| intent.getIntentData().isEmpty() ? "" : intent
						.getIntentData().iterator().next().toString());

		scen.setTransitions_with_newID(new Transition(source, action, dest,
				dest.getId()), dest.getId());

	}

	private State buildState(String name, String id) {
		return new State(name, id);
	}
}
