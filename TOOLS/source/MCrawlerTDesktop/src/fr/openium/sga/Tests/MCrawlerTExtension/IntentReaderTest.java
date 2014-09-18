package fr.openium.sga.Tests.MCrawlerTExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import kit.Config.Config;
import kit.Intent.AndroidManifestParser;
import kit.Intent.IntentXmlGenerator;
import kit.Intent.MCrawlerIntentReader;
import kit.Intent.MCrawlerTIntent;
import kit.Intent.ManifestData;
import kit.Intent.StartingIntentGenerator;
import kit.Intent.StreamException;

import org.junit.Test;
import org.xml.sax.SAXException;

public class IntentReaderTest {

	@Test
	public void testIntentReader() throws SAXException, IOException,
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
			assertNotNull(_ints.get(i).getComponentName());
			IntentXmlGenerator gen = new IntentXmlGenerator(_ints.get(i),
					new File(Config.CURRENT_DIRECTORY
							+ "/TEST/extension/manifest/get" + i + ".xml"));
			gen.generateXml();
			assertTrue(new File(Config.CURRENT_DIRECTORY
					+ "/TEST/extension/manifest/get" + i + ".xml").exists());

		}

		/**
		 * 
		 */
		File toRead = new File(Config.CURRENT_DIRECTORY
				+ "/TEST/extension/manifest/get" + 11 + ".xml");
		MCrawlerTIntent _int = MCrawlerIntentReader.parse(toRead);
		assertNotNull(_int);
		assertNotNull(_int.getComponentName());
		assertEquals(1, _int.getActions().size());
		assertEquals(_int.getCategories().size(), 1);
		assertEquals(_int.getIntentData().size(), 1);
	}
}
