
package org.liberty.android.fantastischmemo.test;

import kit.TestRunner.IntentRunner;

public class IntentCrawler
    extends IntentRunner
{

    private static String PAIR_SEQUENCE = "3";
    private final static String TARGET_PACKAGE_ID = "org.liberty.android.fantastischmemo";
    private final static String LAUNCHER_ACTIVITY_FULL_CLASSNAME = "org.liberty.android.fantastischmemo.ui.AnyMemo";
    private static String EXPECTED_INITIAL_STATE_ACTIVITY = "org.liberty.android.fantastischmemo.ui.AnyMemo";
    static {try {Main = Class.forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);} catch (ClassNotFoundException e) {throw new RuntimeException(e);}}public IntentCrawler() {super(TARGET_PACKAGE_ID, Main);} protected void setUp() throws Exception {PACKAGE_ID=TARGET_PACKAGE_ID;INITIAL_STATE_ACTIVITY_FULL_CLASSNAME = EXPECTED_INITIAL_STATE_ACTIVITY;super.setPairwiseSequenceNumber(""+ PAIR_SEQUENCE );super.setUp();} 
}
