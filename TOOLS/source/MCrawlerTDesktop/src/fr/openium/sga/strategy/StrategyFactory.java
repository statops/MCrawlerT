package fr.openium.sga.strategy;

import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.richModel.LauncherMultipleIntent;

public class StrategyFactory {
	protected static AbstractStrategy mcurrent_strategy;

	public AbstractStrategy getStrategy(AbstractStrategy strategy) {
		return strategy;

	}

	/**
	 * @param env
	 * @return
	 */
	public static AbstractStrategy getNewStrategy(SgdEnvironnement env) {
		switch (env.getStrategyType()) {
		case AbstractStrategy.DFS_STRATEGY_ID:
			mcurrent_strategy = new NormalStrategy(env);
			break;
		case AbstractStrategy.FOURMY_STRATEGY_ID:
			mcurrent_strategy = new FourmiStrategy(env, null);
			break;
		case AbstractStrategy.LOGGING_STRATEGY_ID:
			mcurrent_strategy = new SemanticStrategy(env, null);
			break;
		case IntentLauncherStrategy._STRATEGY_ID:
			mcurrent_strategy = new LauncherMultipleIntent(env);
			break;
		default:
			throw new IllegalStateException("unknown strategy");
		}
		return mcurrent_strategy;
	}
}
