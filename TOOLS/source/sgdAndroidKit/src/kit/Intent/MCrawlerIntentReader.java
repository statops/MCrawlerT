package kit.Intent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import kit.Intent.MCrawlerTIntent.IntentData;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MCrawlerIntentReader {

	public static MCrawlerTIntent parse(File file) {

		try {
			InputStream inputStream = new FileInputStream(file);
			Reader reader = new InputStreamReader(inputStream, "UTF-8");
			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");

			SAXParserFactory fact = SAXParserFactory.newInstance();
			MCrawlerTIntent scen = new MCrawlerTIntent();

			SAXParser parser = fact.newSAXParser();
			DefaultHandler handler = new McrawlerIntentdHandler(file, scen);
			System.out.println("parse the scenario file: " + file.toString());
			parser.parse(is, handler);// (file, handler);
			System.out.println("Results: ");
			return scen;

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return null;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		} catch (SAXException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	public static class McrawlerIntentdHandler extends DefaultHandler {
		private MCrawlerTIntent mIntentCrawlerTData;
		private int mElementLevel = 0;
		private int mValidLevel = 0;
		private IntentData currentIntentData;
		private String host;
		private String scheme;
		private String path;
		private String type;
		private boolean isAction;
		boolean isCategory;
		boolean isComponent;
		private String component;

		private final static int LEVEL_INTENT = 0;
		/**
		 * action,category,data
		 * 
		 */
		private final static int LEVEL_INTENT_ELEMENT = 1;

		/**
		 * data elements: uri, type,scheme
		 * 
		 */
		private final static int LEVEL_DATA_ELEMENT = 2;

		public McrawlerIntentdHandler(File specAddXmlFile,
				MCrawlerTIntent intent) {
			mIntentCrawlerTData = intent;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
		 */
		@Override
		public void endDocument() throws SAXException {
			super.endDocument();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
		 * java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {

			if (mValidLevel == mElementLevel) {
				mValidLevel--;
			}
			mElementLevel--;

			if (mValidLevel == mElementLevel) {

				switch (mValidLevel) {

				case LEVEL_INTENT_ELEMENT:
					currentIntentData = new IntentData(host, scheme, path, type);
					break;

				case LEVEL_INTENT:
					mIntentCrawlerTData.setIntentData(currentIntentData);
					break;

				default:
					break;
				}

			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
		 */
		@Override
		public void startDocument() throws SAXException {
			super.startDocument();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
		 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (mValidLevel == mElementLevel) {
				switch (mValidLevel) {
				case LEVEL_INTENT:
					if (MCrawlerTIntent.INTENT.equals(qName)) {
						mValidLevel++;
					}
					break;
				case LEVEL_INTENT_ELEMENT:
					/**
					 * handle alphabet
					 */
					if (MCrawlerTIntent.ActionElements.ACTION.equals(qName)) {
						isAction = true;
						mValidLevel++;
					}

					if (MCrawlerTIntent.CategoryElements.CATEGORY.equals(qName)) {
						isCategory = true;
						mValidLevel++;
					}

					if (MCrawlerTIntent.DataElements.DATA.equals(qName)) {
						mValidLevel++;
					}

					if (MCrawlerTIntent.ComponentElements.COMPONONENT
							.equals(qName)) {
						isComponent = true;
						mValidLevel++;
					}

					break;
				case LEVEL_DATA_ELEMENT:

					if (MCrawlerTIntent.DataElements.HOST.equals(qName)) {
						host = getAttributeValue(attributes,
								MCrawlerTIntent.DataElements.HOST);
					}
					if (MCrawlerTIntent.DataElements.SCHEME.equals(qName)) {
						scheme = getAttributeValue(attributes,
								MCrawlerTIntent.DataElements.SCHEME);
					}
					if (MCrawlerTIntent.DataElements.PATH.equals(qName)) {
						path = getAttributeValue(attributes,
								MCrawlerTIntent.DataElements.PATH);
					}
					if (MCrawlerTIntent.DataElements.TYPE.equals(qName)) {
						type = getAttributeValue(attributes,
								MCrawlerTIntent.DataElements.TYPE);
					}
					if (MCrawlerTIntent.DataElements.URI.equals(qName)) {
						uri = getAttributeValue(attributes,
								MCrawlerTIntent.DataElements.SCHEME);
					}

					if (MCrawlerTIntent.ActionElements.NAME.equals(qName)
							&& isAction) {
						mIntentCrawlerTData
								.addAction(getAttributeValue(attributes,
										MCrawlerTIntent.ActionElements.NAME));
						isAction = false;
					}

					if (MCrawlerTIntent.CategoryElements.NAME.equals(qName)
							&& isCategory) {
						mIntentCrawlerTData.addCategory(getAttributeValue(
								attributes,
								MCrawlerTIntent.CategoryElements.NAME));
						isCategory = false;

					}
					if (MCrawlerTIntent.ComponentElements.NAME.equals(qName)
							&& isComponent) {
						mIntentCrawlerTData.setComponentName(getAttributeValue(
								attributes,
								MCrawlerTIntent.ComponentElements.NAME));
						isComponent = false;

					}

					break;

				}

			}
			mElementLevel++;
		}

		private String getAttributeValue(Attributes attributes,
				String attributeName) {
			int count = attributes.getLength();
			for (int i = 0; i < count; i++) {
				String localName = attributes.getQName(i);
				int uri = attributes.getURI(i).length();
				if (attributeName.equals(localName) && uri == 0) {
					return attributes.getValue(i);
				}
			}

			return null;
		}

	}

}
