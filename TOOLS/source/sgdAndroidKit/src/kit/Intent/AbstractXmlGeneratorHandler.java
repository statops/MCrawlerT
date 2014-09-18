package kit.Intent;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public abstract class AbstractXmlGeneratorHandler {
	public AbstractXmlGeneratorHandler(TransformerHandler handler2) {
		mHandler = handler2;
	}

	protected final static Attributes EMPTY_ATTRS = new AttributesImpl();
	protected TransformerHandler mHandler;
	protected final static String CDATA = "CDATA";

	protected void addElement(String element, String name, ElementAttribute atr)
			throws SAXException {
		AttributesImpl ageAttrs = new AttributesImpl();
		ageAttrs.addAttribute("", atr.getKey(), atr.getKey(), CDATA,
				atr.getValue());
		mHandler.startElement("", element, element, ageAttrs);
		if (name != null) {
			mHandler.characters(name.toCharArray(), 0, name.length());
		}
		mHandler.endElement("", element, element);
	}

	protected class ElementAttribute {
		private final String key;
		private final String value;

		public ElementAttribute(String name, String val) {
			key = name;
			value = val;
		}

		/**
		 * @return the QName (key)
		 */
		public String getKey() {
			if (key == null)
				return "";

			return key;
		}

		/**
		 * @return the value of the attributes
		 */
		public String getValue() {
			if (value == null)
				return "";
			return value;
		}

	}

}
