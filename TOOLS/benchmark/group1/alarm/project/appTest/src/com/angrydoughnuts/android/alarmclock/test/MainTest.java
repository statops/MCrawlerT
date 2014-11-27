
package com.angrydoughnuts.android.alarmclock.test;

import kit.TestRunner.SgdFourmyStrategy;

public class MainTest
    extends SgdFourmyStrategy
{

    private static String PAIR_SEQUENCE = "2";
    private final static String TARGET_PACKAGE_ID = "com.angrydoughnuts.android.alarmclock";
    private final static String LAUNCHER_ACTIVITY_FULL_CLASSNAME = "com.angrydoughnuts.android.alarmclock.ActivityAlarmClock";
    private static String EXPECTED_INITIAL_STATE_ACTIVITY = "com.angrydoughnuts.android.alarmclock.ActivityAlarmClock";
    static {try {Main = Class.forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);} catch (ClassNotFoundException e) {throw new RuntimeException(e);}}public MainTest() {super(TARGET_PACKAGE_ID, Main);} protected void setUp() throws Exception {PACKAGE_ID=TARGET_PACKAGE_ID;INITIAL_STATE_ACTIVITY_FULL_CLASSNAME = EXPECTED_INITIAL_STATE_ACTIVITY;super.setPairwiseSequenceNumber(""+ PAIR_SEQUENCE );super.setUp();} 
}
