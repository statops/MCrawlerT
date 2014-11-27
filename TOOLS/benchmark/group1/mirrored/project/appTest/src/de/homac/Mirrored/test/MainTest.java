
package de.homac.Mirrored.test;

import kit.TestRunner.SgdFourmyStrategy;

public class MainTest
    extends SgdFourmyStrategy
{

    private static String PAIR_SEQUENCE = "1";
    private final static String TARGET_PACKAGE_ID = "de.homac.Mirrored";
    private final static String LAUNCHER_ACTIVITY_FULL_CLASSNAME = "de.homac.Mirrored.ArticlesList";
    private static String EXPECTED_INITIAL_STATE_ACTIVITY = "de.homac.Mirrored.ArticlesList";
    static {try {Main = Class.forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);} catch (ClassNotFoundException e) {throw new RuntimeException(e);}}public MainTest() {super(TARGET_PACKAGE_ID, Main);} protected void setUp() throws Exception {PACKAGE_ID=TARGET_PACKAGE_ID;INITIAL_STATE_ACTIVITY_FULL_CLASSNAME = EXPECTED_INITIAL_STATE_ACTIVITY;super.setPairwiseSequenceNumber(""+ PAIR_SEQUENCE );super.setUp();} 
}
