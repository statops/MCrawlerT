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

			File f = new File(OUTPUT_FILE);
			FileWriter fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);
			StreamResult result = new StreamResult(bw);
			
			SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
			TransformerHandler handler = tf.newTransformerHandler();
			Transformer transformer = handler.getTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			handler.setResult(result);
			new ScenarioXmlGeneratorHandler(handler).generate(scenario);
			System.out.println("DONE");
		} catch (TransformerConfigurationException e) {
			System.out.println("Transformer Configuration Exception: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO Exception: " + e.getMessage());
		} catch (SAXException e) {
		}

		return true;
	}
}
