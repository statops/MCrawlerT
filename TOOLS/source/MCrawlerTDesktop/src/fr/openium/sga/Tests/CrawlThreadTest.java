package fr.openium.sga.Tests;

import junit.framework.TestCase;
import kit.Config.Config;
import android.app.Activity;

public class CrawlThreadTest extends TestCase {
	@Override
	protected void setUp() throws Exception {

		super.setUp();
		i = 0;
		j = 5;
	}

	public void testCrawl() {
		CrawlRecursiveMethod();

	}

	private static int i = 0;
	private static int j = 5;

	private void CrawlRecursiveMethod() {

		do {
			if (i > 5) {
				System.out.println("========================>" + "i=5");
				break;
			}
			CrawlChild crossChild = new CrawlChild();
			crossChild.start();

			if (Config.DEBUG) {
				System.out.println("CrawlChild" + " thread " + (i - 1)
						+ " before join");
			}

			;
			if (Config.DEBUG) {
				System.out.println("CrawlChild" + ": reprise de thread " + j--
						+ " /after join");
			}

		} while (i < 5);

		assertTrue("i:"+i,12 >= i);
		assertTrue("j:"+j,(-6) <= j);

	}

	public class CrawlChild extends Thread {
		Activity currentActivity;
		Activity mainActivity;

		public CrawlChild(String name, Activity current, Activity main) {
			super(name);
			setParameters(current, main);
		}

		public CrawlChild() {

		}

		public void setParameters(Activity current, Activity main) {

			currentActivity = current;
			mainActivity = main;

		}

		@Override
		protected void finalize() throws Throwable {
			notify();
			super.finalize();
		}

		public void run() {
			if (Config.DEBUG) {
				System.out.println("CrawlChild" + "run " + i++);
			}
			CrawlRecursiveMethod();
		};
	}
}
