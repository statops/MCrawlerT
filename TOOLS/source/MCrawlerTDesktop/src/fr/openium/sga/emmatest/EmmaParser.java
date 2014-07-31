package fr.openium.sga.emmatest;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class EmmaParser {
	private final static int LEVEL_CLASS = 10;
	private final static int LEVEL_METHOD = 11;
	private final static int LEVEL_BLOCK = 12;
	private final static int LEVEL_LINE = 13;

	public interface EmmaParserErrorHandler extends ErrorHandler {
		void handleError(Exception exception, int lineNumber);

	}

	private final static SAXParserFactory sParserFactory;

	static {
		sParserFactory = SAXParserFactory.newInstance();
		sParserFactory.setNamespaceAware(true);
	}

	public String parse(File emmaFile, EmmaParserErrorHandler errorHandler) throws SAXException, IOException,
			ParserConfigurationException {
		if (emmaFile != null) {
			SAXParser parser = sParserFactory.newSAXParser();
			EmmaHandler manifestHandler = new EmmaHandler(emmaFile, errorHandler);
			parser.parse(emmaFile, manifestHandler);
			return getClassCoverage();
		}
		return null;
	}

	private static String mClassCoverage;
	private static String mMethodCoverage;
	private static String mBlockCoverage;
	private static String mLineCoverage;

	public String getClassCoverage() {
		return mClassCoverage.trim();
	}

	public static void setClassCoverage(String cov) {
		mClassCoverage = cov;
	}

	public String getMethodCoverage() {
		return mMethodCoverage.trim();
	}

	public static void setMethodCoverage(String mthodCoverage) {
		mMethodCoverage = mthodCoverage;
	}

	public String getBlockCoverage() {
		return mBlockCoverage.trim();
	}

	public static void setBlockCoverage(String bCoverage) {
		mBlockCoverage = bCoverage;
	}

	public String getLineCoverage() {
		return mLineCoverage;
	}

	public static void setLineCoverage(String lCoverage) {
		mLineCoverage = lCoverage;
	}

	private static class EmmaHandler extends DefaultHandler {
		private final EmmaParserErrorHandler mErrorHandler;
		private int mCurrentLevel = 1;
		private int mLevel = 1;

		public EmmaHandler(File emmaFile, EmmaParserErrorHandler errorHandler) {
			super();
			mErrorHandler = errorHandler;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			try {
				String type = "";
				if (localName.equals("coverage")) {
					type = getAttributeValue(attributes, "type", true);
				}

				if (mLevel == mCurrentLevel) {
					switch (mLevel) {
					case LEVEL_LINE:
						if (type.contains("line")) {
							setLineCoverage(getAttributeValue(attributes, "value", true));
							// coverageRetrieved = true;
						}
						mLevel++;
						break;

					case LEVEL_BLOCK:
						if (type.contains("block")) {
							setBlockCoverage(getAttributeValue(attributes, "value", true));
							// coverageRetrieved = true;
						}
						mLevel++;
						break;

					case LEVEL_METHOD:
						if (type.contains("method")) {
							setMethodCoverage(getAttributeValue(attributes, "value", true));
							// coverageRetrieved = true;
						}
						mLevel++;
						break;
					case LEVEL_CLASS:// all
						if (type.contains("class")) {
							setClassCoverage(getAttributeValue(attributes, "value", true));
							// coverageRetrieved = true;
						}

						mLevel++;
						break;
					default:
						mLevel++;
						break;

					}
				}
				mCurrentLevel++;
			} finally {
				super.startElement(uri, localName, qName, attributes);
			}
		}

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			try {

			} finally {
				super.endElement(uri, localName, name);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException
		 * )
		 */
		@Override
		public void error(SAXParseException e) {
			if (mErrorHandler != null) {
				mErrorHandler.handleError(e, e.getLineNumber());

			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.
		 * SAXParseException)
		 */
		@Override
		public void fatalError(SAXParseException e) {
			if (mErrorHandler != null) {
				mErrorHandler.handleError(e, e.getLineNumber());
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException
		 * )
		 */
		@Override
		public void warning(SAXParseException e) throws SAXException {
			if (mErrorHandler != null) {
				mErrorHandler.warning(e);
			}
		}

		private String getAttributeValue(Attributes attributes, String attributeName, boolean hasNamespace) {
			int count = attributes.getLength();
			for (int i = 0; i < count; i++) {
				if (attributeName.equals(attributes.getLocalName(i)) && ((hasNamespace))
						|| (hasNamespace == false && attributes.getURI(i).length() == 0)) {
					return attributes.getValue(i);
				}
			}

			return null;
		}

	}

}
