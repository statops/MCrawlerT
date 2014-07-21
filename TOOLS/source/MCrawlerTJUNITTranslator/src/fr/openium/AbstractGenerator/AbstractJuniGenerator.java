/**
 * 
 */
package fr.openium.AbstractGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;

/**
 * @author Stassia
 * 
 */
public abstract class AbstractJuniGenerator {
	private static final String TAG = AbstractJuniGenerator.class.getName();

	/**
	 * 
	 * @param packagename
	 * @param test_name
	 * @param ActivityName
	 * @param out
	 * @return the list of state
	 */
	public abstract HashSet<java.lang.String> generate(String packagename,
			String test_name, String ActivityName, File out);

	/**
	 * input Path xml , output JUNIT
	 */
	protected JClass String;

	protected JClass State;
	protected JClass ScenarioData;
	protected JClass MCrawlerFunctions;

	protected JClass MCrawlerVariable;

	protected ArrayList<JClass> classToImport = new ArrayList<JClass>();

	protected final String TEMPLATE = "static {try {Main = Class.forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);} catch (ClassNotFoundException e) {throw new RuntimeException(e);}}public "
			+ "%s"
			+ "() {super(TARGET_PACKAGE_ID, Main);} protected void setUp() throws Exception {INITIAL_STATE_ACTIVITY_FULL_CLASSNAME=EXPECTED_INITIAL_STATE_ACTIVITY;PACKAGE_ID=TARGET_PACKAGE_ID;super.setUp();} ";

	protected void addImportedClass(JCodeModel cm) {
		String = cm.ref(String.class.getName());
		MCrawlerVariable = cm.ref(fr.openium.variable.MCrawlerVariable.class
				.getName());
		MCrawlerFunctions = cm.ref(fr.openium.function.MCrawlerFunctions.class
				.getName());
		State = cm.ref(kit.Scenario.State.class.getName());
		ScenarioData = cm.ref(kit.Scenario.ScenarioData.class.getName());
		classToImport.add(MCrawlerVariable);
		classToImport.add(MCrawlerFunctions);
		classToImport.add(State);
		classToImport.add(ScenarioData);

	}

	protected java.lang.String getTestClassName(java.lang.String packagename,
			java.lang.String test_name) {
		return packagename + "." + test_name;
	}

}
