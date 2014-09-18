package kit.RandomValue;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RandomValueParser {
	private static HashSet<RandomValueData> mRandomValueDatas = new HashSet<RandomValueData>();

	public HashSet<RandomValueData> parse(File file) {
		if (!file.exists()){
			System.out.println("file  does not exist :"+file.getPath());
			return null;
		}
		SAXParserFactory fact = SAXParserFactory.newInstance();
		try {
			SAXParser parser = fact.newSAXParser();
			DefaultHandler handler = new RandomValuedHandler(mRandomValueDatas);
			parser.parse(file, handler);
			return mRandomValueDatas;
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

	public static class RandomValuedHandler extends DefaultHandler {

		private HashSet<RandomValueData> mRandomValue = new HashSet<RandomValueData>();
		RandomValueData randomType;
		private int mElementLevel = 0;
		private int mValidLevel = 0;
		private StringBuffer mValue;

		private final static int LEVEL_RV = 0;
		/**
		 * authors, alphabet,states,or transitions
		 * 
		 */
		private final static int LEVEL_TYPE = 1;

		/**
		 * state,action,transition
		 * 
		 */
		private final static int LEVEL_VALUE = 2;

		public RandomValuedHandler(HashSet<RandomValueData> rv) {
			mRandomValue = rv;
		}

		@Override
		public void startDocument() throws SAXException {
			super.startDocument();
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (mValidLevel == mElementLevel) {
				switch (mValidLevel) {
				case LEVEL_RV:
					if (RandomValue.RANDOM_VALUE.equals(qName)) {
						mValidLevel++;
					}
					break;
				case LEVEL_TYPE:
					randomType = new RandomValueData(qName);
					mValidLevel++;
					break;
				case LEVEL_VALUE:
					mValue = new StringBuffer();
					mValidLevel++;
					break;
				}
			}
			mElementLevel++;
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {

			if (mValidLevel == mElementLevel) {
				mValidLevel--;
			}
			mElementLevel--;

			if (mValidLevel == mElementLevel) {

				switch (mValidLevel) {
				case LEVEL_VALUE:
					randomType.setValue(mValue.toString());
				case LEVEL_TYPE:
					mRandomValue.add(randomType);
					break;
				default:
					break;
				}

			}

		}

		@Override
		public void endDocument() throws SAXException {
			super.endDocument();
		}

		public void characters(char[] ch, int start, int length)
				throws SAXException {
			String lecture = new String(ch, start, length);
			if (mValue != null)
				mValue.append(lecture);
		}
	}

}
