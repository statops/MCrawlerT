package fr.openium.sga.Tests.MCrawlerTExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import kit.Config.Config;
import kit.Intent.AndroidManifestComponent;
import kit.Intent.AndroidManifestParser;
import kit.Intent.IntentXmlGenerator;
import kit.Intent.MCrawlerTIntent;
import kit.Intent.ManifestData;
import kit.Intent.StartingIntentGenerator;
import kit.Intent.StreamException;

import org.junit.Test;
import org.xml.sax.SAXException;

public class ManifestParserTest {
	@Test
	public void testManifestParse() throws SAXException, IOException,
			ParserConfigurationException, StreamException {

		File manifest = new File(Config.CURRENT_DIRECTORY
				+ "/TEST/extension/manifest/AndroidManifest.xml");
		assertTrue(manifest.exists());
		ManifestData data = AndroidManifestParser.parse(new FileInputStream(
				manifest));
		assertNotNull(data);
		assertEquals(37, data.getComponents().size());
		assertEquals("com.eleybourn.bookcatalogue", data.getPackage());
		for (AndroidManifestComponent component : data.getComponents()) {
			if (component.getName().contains("StartupActivity")) {
				assertEquals(1, component.getIntent().size());
				assertEquals("android.intent.action.MAIN", component
						.getIntent().get(0).getActions().iterator().next());
				assertEquals("android.intent.category.LAUNCHER", component
						.getIntent().get(0).getCategories().iterator().next());

			}

			if (component.getName().contains("GoodreadsAuthorizationActivity")) {
				assertEquals(9, component.getIntent().size());
				Iterator<String> cat = component.getIntent().get(0)
						.getCategories().iterator();

				assertEquals(1, component.getIntent().get(0).getActions()
						.size());
				assertEquals(3, component.getIntent().get(0).getCategories()
						.size());

				assertEquals("android.intent.action.VIEW", component
						.getIntent().get(0).getActions().iterator().next());

				assertEquals("android.intent.category.DEFAULT", cat.next());

				// assertEquals("android.intent.category.BROWSABLE",
				// cat.next());
				assertEquals("android.intent.category.BROWSABLE", component
						.getIntent().get(2).getCategories().iterator().next());

				assertEquals(1, component.getIntent().get(0).getIntentData()
						.size());
				assertEquals("goodreadsauth", component.getIntent().get(0)
						.getIntentData().get(0).getHost());
				assertEquals("http", component.getIntent().get(0)
						.getIntentData().get(0).getScheme());

			}

			if (component.getName().contains(".AdministrationLibraryThing")) {
				assertEquals(0, component.getIntent().size());
				assertEquals(component.getPackage()
						+ ".AdministrationLibraryThing", component.getName());
				assertTrue(!component.isExported());

			}
			if (component.getName().contains(".BookCatalogueClassic")) {

				assertTrue(component.isExported());

			}

		}

	}

	@Test
	public void testIntentGenerator() throws SAXException, IOException,
			ParserConfigurationException, StreamException {

		File manifest = new File(Config.CURRENT_DIRECTORY
				+ "/TEST/extension/manifest/AndroidManifest.xml");
		assertTrue(manifest.exists());
		ManifestData data = AndroidManifestParser.parse(new FileInputStream(
				manifest));
		assertNotNull(data);
		assertEquals(37, data.getComponents().size());
		StartingIntentGenerator intGgen = new StartingIntentGenerator(data);
		ArrayList<MCrawlerTIntent> _ints = new ArrayList<MCrawlerTIntent>();
		_ints = intGgen.generate();
		assertTrue(_ints.size() > 0);
		assertEquals(_ints.size(), 18);
		for (int i = 0; i < _ints.size(); i++) {
			IntentXmlGenerator gen = new IntentXmlGenerator(_ints.get(i),
					new File(Config.CURRENT_DIRECTORY
							+ "/TEST/extension/manifest/get" + i + ".xml"));
			gen.generateXml();
			assertTrue(new File(Config.CURRENT_DIRECTORY
					+ "/TEST/extension/manifest/get" + i + ".xml").exists());

		}
	}

	@Test
	public void testIntentGeneratorExemples() throws SAXException, IOException,
			ParserConfigurationException, StreamException {

		File manifest = new File(Config.CURRENT_DIRECTORY
				+ "/TEST/extension/eij/AndroidManifest.xml");
		assertTrue(manifest.exists());
		ManifestData data = AndroidManifestParser.parse(new FileInputStream(
				manifest));
		assertNotNull(data);
		assertEquals(7, data.getComponents().size());
		StartingIntentGenerator intGgen = new StartingIntentGenerator(data);
		ArrayList<MCrawlerTIntent> _ints = new ArrayList<MCrawlerTIntent>();
		_ints = intGgen.generate();
		for (int i = 0; i < _ints.size(); i++) {
			IntentXmlGenerator gen = new IntentXmlGenerator(_ints.get(i),
					new File(Config.CURRENT_DIRECTORY
							+ "/TEST/extension/eij/intent" + i + ".xml"));
			gen.generateXml();
			assertTrue(new File(Config.CURRENT_DIRECTORY
					+ "/TEST/extension/eij/intent" + i + ".xml").exists());

		}
	}

}
