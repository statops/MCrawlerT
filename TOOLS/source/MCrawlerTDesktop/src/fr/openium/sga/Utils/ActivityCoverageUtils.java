/**
 * 
 */
package fr.openium.sga.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;

import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;

import org.xml.sax.SAXException;

import fr.openium.components.ActivityComponent;
import fr.openium.specification.xml.AndroidManifestParser;
import fr.openium.specification.xml.ManifestData;
import fr.openium.specification.xml.StreamException;

/**
 * @author Stassia
 * 
 */
public class ActivityCoverageUtils {
	private static final String TAG = ActivityCoverageUtils.class.getName();

	private final ManifestData mManifestData;
	private final ScenarioData mScenarioData;

	public ActivityCoverageUtils(File manifestFile, File out)
			throws FileNotFoundException, SAXException, IOException,
			StreamException, ParserConfigurationException {
		mManifestData = AndroidManifestParser.parse(new FileInputStream(
				manifestFile));
		mScenarioData = ScenarioParser.parse(out);

	}

	public ActivityCoverageUtils(ManifestData manifestData,
			ScenarioData scenData) {
		mManifestData = manifestData;
		mScenarioData = scenData;
	}

	/**
	 * @param manifestfilePath
	 * @param finalModelPath
	 * @throws ParserConfigurationException
	 * @throws StreamException
	 * @throws IOException
	 * @throws SAXException
	 * @throws FileNotFoundException
	 */
	public ActivityCoverageUtils(String manifestfilePath, String finalModelPath)
			throws FileNotFoundException, SAXException, IOException,
			StreamException, ParserConfigurationException {
		this(new File(manifestfilePath), new File(finalModelPath));

	}

	public Double getActivityCoverage() {
		if (getTotalActivity() == 0) {
			return (double) 0;
		}
		return (double) (((getExploredActivity() * 100) / getTotalActivity()));
	}

	/**
	 * @return
	 */
	private int getTotalActivity() {
		System.out.println("Total activity "
				+ mManifestData.getActivities().size() + " :");
		for (ActivityComponent act : mManifestData.getActivities()) {
			System.out.println(act.getName());
		}

		return mManifestData.getActivities().size();
	}

	/**
	 * @return
	 */
	private int getExploredActivity() {
		HashSet<String> activities = new HashSet<String>();
		System.out.println("explored activity  :");
		for (State st : mScenarioData.getStates()) {
			if (!activities.contains(st.getName())
					&& !activities.contains(st.getShortName())
					&& !st.getShortName().equals(Scenario.END)) {
				System.out.println("activity :" + st.getName());
				activities.add(st.getName());
			}
		}
		System.out.println("Total explored activity  :" + activities.size());
		return activities.size();
	}
}
