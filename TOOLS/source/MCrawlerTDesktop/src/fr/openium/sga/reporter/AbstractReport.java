package fr.openium.sga.reporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

abstract class AbstractReport {

	protected final File outpuFile;
	protected final static String SEPARATOR = ": ";
	public static final String CODE_COVERAGE = "Code coverage";
	public static final String LIST_EVENT_DONE = "List of executed  events ";
	public static final String VULNERABILTY_STATUS = "Vulnerability status ";
	protected static final String ISVULNERABLE = "is vulnerable";
	protected static final String ISNOT_VULNERABLE = "is not vulnerable";
	protected static final String ERROR_DESCRIPTION = "Error description ";
	public static final String STATE_NAME = "Target location ";

	public static final String PATH_TO_REACH_LOCATION = "Path to reach the target location ";

	public AbstractReport(File output) throws IOException {
		outpuFile = output;
		if (!outpuFile.exists()) {
			outpuFile.createNewFile();
		}
	}

	public abstract void generate() throws FileNotFoundException, SAXException,
			IOException, ParserConfigurationException,
			CloneNotSupportedException, kit.Intent.StreamException;

}
