/**
 * 
 */
package fr.openium.sga.strategy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import kit.RandomValue.RandomValueData;
import kit.RandomValue.RandomValueParser;
import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Utils.SgUtils;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;
import fr.openium.sga.semantic.LoggingSemantic;

/**
 * @author Stassia
 * 
 */
public class SemanticStrategy extends AntStrategy {

	/**
	 * @param env
	 * @param object
	 */
	public SemanticStrategy(SgdEnvironnement env, String initactivity) {
		super(env, initactivity);
		mResult = new CrawlResult(
				new File(mSgdEnvironnement.getOutDirectory()), this);
	}

	public SemanticStrategy(SgdEnvironnement env) {
		super(env, null);
		mResult = new CrawlResult(
				new File(mSgdEnvironnement.getOutDirectory()), this);
	}

	public static LoggingSemantic getLoggingSemantic(String testData) {
		System.out.println(" is in LoggingSemantic: ");
		ArrayList<String> semantic = new ArrayList<String>();
		HashSet<RandomValueData> data = new HashSet<RandomValueData>();
		/**
		 * � lire � partir d'un fichier
		 */
		RandomValueParser parser = new RandomValueParser();
		data = parser.parse(new File(testData));
		if (data == null || data.isEmpty()) {
			System.out.println("data==null");
			semantic.add("file");
			semantic.add("SignIn");
			semantic.add("Sign");
			semantic.add("sign");
			semantic.add("sign in");
			semantic.add("ID");
			semantic.add("user ID");
		} else {
			System.out.println("data!=null:");
			semantic = SgUtils.getTestDataSet(data, ConfigApp.SEMANTIC);
		}
		LoggingSemantic sem = new LoggingSemantic(semantic);
		System.out.println("semantics: " + sem);
		return sem;
	}

	private static LoggingSemantic getLoggingSemantic() {
		return getLoggingSemantic(mSgdEnvironnement.getAllTestDataPath());
	}

	public int getRank(State st, ScenarioData path) {
		System.out.println("get rank in a semantic :");
		int defaultRank = DEFAULT_RANK;
		if (st != null) {
			for (State state : path.getStates()) {
				/**
				 * add semantic_weight
				 */
				int weight = SgUtils.isLogginState(state, getLoggingSemantic()
						.getValueSet());
				if (weight > 0) {
					defaultRank = weight + MAX_RANK;
					System.out.println(" is in LoggingSemantic");
					System.out.println(" state: " + state.getShortName());
					System.out.println(" rank: " + defaultRank);
				} else {
					defaultRank = defaultRank + DEFAULT_RANK;
				}

			}
		}
		System.out.println("rank " + defaultRank);
		return defaultRank;
	}

}
