package kit.Intent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;

/*
 * generate intent.xml file for testing  for an activity from
 * to push on device
 */
public class IntentXmlGenerator {
	private final MCrawlerTIntent mIntent;
	private final File mOutFile;

	public IntentXmlGenerator(MCrawlerTIntent intent, File out) {
		mIntent = intent;
		mOutFile = out;

	}

	public boolean generateXml() {

		try {
			TransformerHandler handler = ((SAXTransformerFactory) SAXTransformerFactory
					.newInstance()).newTransformerHandler();
			handler.getTransformer()
					.setOutputProperty(OutputKeys.INDENT, "yes");
			handler.setResult(new StreamResult(new BufferedWriter(
					new FileWriter(mOutFile))));
			new IntentXmlGeneratorHandler(handler).generate(mIntent);
		} catch (TransformerConfigurationException e) {
			System.out.println("Transformer Configuration Exception: "
					+ e.getMessage());
			return false;
		} catch (IOException e) {
			System.out.println("IO Exception: " + e.getMessage());
			return false;
		} catch (SAXException e) {

			e.printStackTrace();
			return false;
		}

		return true;
	}
}
