package fr.openium.sga.Tests.MCrawlerTExtension;

import static org.junit.Assert.assertTrue;

import java.io.File;

import kit.Config.Config;

import org.junit.Test;

public class MultipleModelReporterTest {
	@Test
	public void testModelReportGenerator() throws Exception {

		String[] params = getUe();
		Long value = (long) 1;
		Long currentvalue = (long) 2;
		assertTrue(currentvalue > value);

	}

	private String[] getUe() {
		return new String[] {
				"explore",
				"-p",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/project/paypal",
				"-manifest",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/project/paypal/app/AndroidManifest.xml",
				"-tp",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/project/paypalTest",
				"-tpackage",
				"com.paypal.here.test",
				"-pPackage",
				"com.paypal.here",
				"-pApk",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/project/paypal/appsigned.apk",
				"-sdk",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/ANDROID_SDK/adt-bundle-mac-x86_64-20140321/sdk",
				"-arv",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/input/testData.xml",
				"-strategy",
				"500",
				"-thread",
				"1",
				"-launcherActivity",
				"com.paypal.here.StartUpActivity ",
				"-maxEvent",
				"50",
				"-class",
				"com.example.loggingproject.test.Maintest",
				"-stopError",
				"true",
				"-maxTime",
				"140000",
				"-emu",
				"null",
				"-cp",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/input/cp",
				"-db",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/input/db",
				"-out",
				"/Users/Stassia/Documents/Scen-genWorkSpace/AutomatisationScript/projects/OpenSourceProject/2013/PAYPAL/out/paypalWIKOOutput",
				"-dico",
				new File(Config.CURRENT_DIRECTORY + File.separator
						+ "TEST/bruteForce/dico").getPath() };

	}

}
