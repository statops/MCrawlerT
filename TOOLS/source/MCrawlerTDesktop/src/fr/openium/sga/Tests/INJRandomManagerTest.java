/**
 * 
 */
package fr.openium.sga.Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import kit.Config.Config;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager.SPReport;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTestTask;
import fr.openium.sga.SecurityTesting.INJTest.INJRandomTaskManager;
import fr.openium.sga.SecurityTesting.INJTest.INJTask;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;

/**
 * @author Stassia
 * 
 */
public class INJRandomManagerTest {
	private static final String TAG = INJRandomManagerTest.class.getName();

	@Test
	public void testInMethod() throws Exception {

		String[] params = new String[] { "inpuFile",
				"/Users/Stassia/Documents/Scen-genWorkSpace/sgd/input_file/NotePad/injRandomTest" };

		List<String> param = FileUtils.readLines(new File(params[1]));
		params = null;
		ArrayList<String> attr = new ArrayList<String>();
		for (String paramElt : param) {
			StringTokenizer st = new StringTokenizer(paramElt);
			while (st.hasMoreTokens()) {
				attr.add(st.nextToken());
			}
		}

		params = attr.toArray(new String[0]);
		ScenarioData model = ScenarioParser.parse(new File(Config.CURRENT_DIRECTORY + File.separator
				+ "TEST/injTask/out.xml"));
		assertNotNull(model);
		SgdEnvironnement env = Emma.init_environment_(params);
		AbstractTaskManager inj = new INJRandomTaskManager(model, new File(env.getAllTestDataPath()), env);
		HashSet<AbstractTestTask> tasks = (HashSet<AbstractTestTask>) inj.getTasks();
		for (AbstractTestTask task : tasks) {
			System.out.println(((INJTask) task).toString());
			assertTrue(SgUtils.hasEditText(task.getTargetState()));
			assertTrue(task.getTargetState().isInit() || !task.getPath().getTransitions().isEmpty());
			assertTrue(((INJTask) task).getSuccessor() != null);
			System.out.println("\n Successor: " + ((INJTask) task).getSuccessor());
		}
		assertEquals(3, tasks.size());
		
		ArrayList<SPReport> reports = inj.launchSecurityTestJob();
		for (SPReport rep : reports) {
			System.out.println(rep.toString());
		}

		if (ConfigApp.DEBUG) {
			System.out.println("end test");
		}

	}
}
