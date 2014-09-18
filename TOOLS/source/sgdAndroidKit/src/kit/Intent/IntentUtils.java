package kit.Intent;

import java.util.ArrayList;
import java.util.Collection;

import kit.Intent.MCrawlerTIntent.IntentData;

public class IntentUtils {

	public static Collection<? extends MCrawlerTIntent> getAllPossibleIntent(
			MCrawlerTIntent intent) {
		ArrayList<MCrawlerTIntent> intents = new ArrayList<MCrawlerTIntent>();
		for (String action : intent.getActions()) {
			if (intent.getCategories().isEmpty()) {
				if (intent.getIntentData().isEmpty()) {
					MCrawlerTIntent _int = new MCrawlerTIntent(action);
					_int.setComponentName(intent.getComponentName());
					intents.add(_int);
					continue;
				} else {
					for (IntentData data : intent.getIntentData()) {
						MCrawlerTIntent _int = new MCrawlerTIntent(action);
						_int.setComponentName(intent.getComponentName());
						_int.setIntentData(data);
						intents.add(_int);
					}

				}
				continue;
			}
			for (String category : intent.getCategories()) {
				if (intent.getIntentData().isEmpty()) {
					MCrawlerTIntent _int = new MCrawlerTIntent(action);
					_int.setComponentName(intent.getComponentName());
					_int.addCategory(category);
					intents.add(_int);
					continue;
				}
				for (IntentData data : intent.getIntentData()) {
					MCrawlerTIntent _int = new MCrawlerTIntent(action);
					_int.setComponentName(intent.getComponentName());
					_int.addCategory(category);
					_int.setIntentData(data);
					intents.add(_int);
				}
			}

		}
		return intents;
	}
}
