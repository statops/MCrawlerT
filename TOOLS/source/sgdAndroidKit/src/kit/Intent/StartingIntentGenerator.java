package kit.Intent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import kit.Config.Config;

import org.xml.sax.SAXException;

public class StartingIntentGenerator {

	private final ManifestData mManifestData;

	public StartingIntentGenerator(ManifestData manifestData) {
		mManifestData = manifestData;
	}

	public StartingIntentGenerator(File manifestFile)
			throws FileNotFoundException, SAXException, IOException,
			StreamException, ParserConfigurationException {

		mManifestData = getManifest(manifestFile);
	}

	private ManifestData getManifest(File manifestFile)
			throws FileNotFoundException, SAXException, IOException,
			StreamException, ParserConfigurationException {

		return AndroidManifestParser.parse(new FileInputStream(manifestFile));

	}

	public StartingIntentGenerator(String manifestfilePath)
			throws FileNotFoundException, SAXException, IOException,
			StreamException, ParserConfigurationException {
		mManifestData = getManifest(new File(manifestfilePath));
	}

	public ArrayList<MCrawlerTIntent> generate() {
		ArrayList<MCrawlerTIntent> intents = new ArrayList<MCrawlerTIntent>();
		for (AndroidManifestComponent component : mManifestData.getComponents()) {
			/*
			 * cas activité
			 */
			if (Config.DEBUG) {
				System.out.println(component.getName());
			}
			if (component.getType().equalsIgnoreCase(
					AndroidManifestComponent.typeElement.ACTIVITY)) {
				intents.addAll(generateIntentsForActivity(component));
			}

		}

		return intents;
	}

	private ArrayList<MCrawlerTIntent> generateIntentsForActivity(
			AndroidManifestComponent component) {
		ArrayList<MCrawlerTIntent> intents = new ArrayList<MCrawlerTIntent>();
		for (MCrawlerTIntent intent : component.getIntent()) {
			intents.addAll(IntentUtils.getAllPossibleIntent(intent));
		}
		return intents;
	}

}
