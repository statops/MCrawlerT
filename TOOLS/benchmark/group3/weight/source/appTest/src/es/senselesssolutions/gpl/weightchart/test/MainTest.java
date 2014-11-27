
package es.senselesssolutions.gpl.weightchart.test;

import kit.TestRunner.SgdFourmyStrategy;

public class MainTest
    extends SgdFourmyStrategy
{

    private static String PAIR_SEQUENCE = "1";
    private final static String TARGET_PACKAGE_ID = "es.senselesssolutions.gpl.weightchart";
    private final static String LAUNCHER_ACTIVITY_FULL_CLASSNAME = "es.senselesssolutions.gpl.weightchart.ChartActivity";
    private static String EXPECTED_INITIAL_STATE_ACTIVITY = "es.senselesssolutions.gpl.weightchart.ChartActivity";
    static {try {Main = Class.forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);} catch (ClassNotFoundException e) {throw new RuntimeException(e);}}public MainTest() {super(TARGET_PACKAGE_ID, Main);} protected void setUp() throws Exception {PACKAGE_ID=TARGET_PACKAGE_ID;INITIAL_STATE_ACTIVITY_FULL_CLASSNAME = EXPECTED_INITIAL_STATE_ACTIVITY;super.setPairwiseSequenceNumber(""+ PAIR_SEQUENCE );super.setUp();} 
}
