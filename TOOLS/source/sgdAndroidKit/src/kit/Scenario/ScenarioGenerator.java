package kit.Scenario;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;

public class ScenarioGenerator {
	private static String OUTPUT_FILE;

	public ScenarioGenerator(String outputPath) {
		OUTPUT_FILE = outputPath;
	}

	public boolean generateXml(ScenarioData scenario) {

		try {

			StreamResult result = new StreamResult(new BufferedWriter(
					new FileWriter(new File(OUTPUT_FILE))));
			SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory
					.newInstance();
			TransformerHandler handler = tf.newTransformerHandler();
			Transformer transformer = handler.getTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			handler.setResult(result);
			new ScenarioXmlGeneratorHandler(handler).generate(scenario);
			System.out.println("DONE");
		} catch (TransformerConfigurationException e) {
			System.out.println("Transformer Configuration Exception: "
					+ e.getMessage());
			return false;
		} catch (IOException e) {
			System.out.println("IO Exception: " + e.getMessage());
			return false;
		} catch (SAXException e) {
			return false;
		}

		return true;
	}
}
