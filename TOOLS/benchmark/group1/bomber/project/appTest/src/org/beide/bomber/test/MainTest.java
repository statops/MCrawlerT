
package org.beide.bomber.test;

import kit.TestRunner.SgdFourmyStrategy;

public class MainTest
    extends SgdFourmyStrategy
{

    private static String PAIR_SEQUENCE = "2";
    private final static String TARGET_PACKAGE_ID = "org.beide.bomber";
    private final static String LAUNCHER_ACTIVITY_FULL_CLASSNAME = "org.beide.bomber.BomberActivity";
    private static String EXPECTED_INITIAL_STATE_ACTIVITY = "org.beide.bomber.BomberActivity";
    static {try {Main = Class.forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);} catch (ClassNotFoundException e) {throw new RuntimeException(e);}}public MainTest() {super(TARGET_PACKAGE_ID, Main);} protected void setUp() throws Exception {PACKAGE_ID=TARGET_PACKAGE_ID;INITIAL_STATE_ACTIVITY_FULL_CLASSNAME = EXPECTED_INITIAL_STATE_ACTIVITY;super.setPairwiseSequenceNumber(""+ PAIR_SEQUENCE );super.setUp();} 
}
