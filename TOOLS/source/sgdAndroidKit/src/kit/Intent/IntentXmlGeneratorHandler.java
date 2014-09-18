package kit.Intent;

import javax.xml.transform.sax.TransformerHandler;

import kit.Intent.MCrawlerTIntent.IntentData;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class IntentXmlGeneratorHandler extends AbstractXmlGeneratorHandler {

	public IntentXmlGeneratorHandler(TransformerHandler handler) {
		super(handler);
	}

	public void generate(MCrawlerTIntent intent) throws SAXException {
		mHandler.startDocument();
		/**
		 * root Element
		 */
		AttributesImpl rootAtrr = new AttributesImpl();
		mHandler.startElement("", MCrawlerTIntent.INTENT,
				MCrawlerTIntent.INTENT, rootAtrr);

		/**
		 * Action element
		 */
		mHandler.startElement("", MCrawlerTIntent.ActionElements.ACTION,
				MCrawlerTIntent.ActionElements.ACTION, EMPTY_ATTRS);
		for (String action : intent.getActions()) {
			addElement(MCrawlerTIntent.ActionElements.NAME, null,
					new ElementAttribute(MCrawlerTIntent.ActionElements.NAME,
							action));

		}
		mHandler.endElement("", MCrawlerTIntent.ActionElements.ACTION,
				MCrawlerTIntent.ActionElements.ACTION);

		/**
		 * component element
		 */
		mHandler.startElement("",
				MCrawlerTIntent.ComponentElements.COMPONONENT,
				MCrawlerTIntent.ComponentElements.COMPONONENT, EMPTY_ATTRS);
		addElement(MCrawlerTIntent.ComponentElements.NAME, null,
				new ElementAttribute(MCrawlerTIntent.ComponentElements.NAME,
						intent.getComponentName()));
		mHandler.endElement("", MCrawlerTIntent.ComponentElements.COMPONONENT,
				MCrawlerTIntent.ComponentElements.COMPONONENT);

		/**
		 * Catégory element
		 */
		mHandler.startElement("", MCrawlerTIntent.CategoryElements.CATEGORY,
				MCrawlerTIntent.CategoryElements.CATEGORY, EMPTY_ATTRS);
		for (String action : intent.getCategories()) {
			addElement(MCrawlerTIntent.CategoryElements.NAME, null,
					new ElementAttribute(MCrawlerTIntent.CategoryElements.NAME,
							action));

		}
		mHandler.endElement("", MCrawlerTIntent.CategoryElements.CATEGORY,
				MCrawlerTIntent.CategoryElements.CATEGORY);

		/**
		 * Intent element
		 */
		for (IntentData action : intent.getIntentData()) {
			mHandler.startElement("", MCrawlerTIntent.DataElements.DATA,
					MCrawlerTIntent.DataElements.DATA, EMPTY_ATTRS);
			addElement(MCrawlerTIntent.DataElements.URI, null,
					new ElementAttribute(MCrawlerTIntent.DataElements.URI,
							action.toString()));
			addElement(MCrawlerTIntent.DataElements.SCHEME, null,
					new ElementAttribute(MCrawlerTIntent.DataElements.SCHEME,
							action.getScheme()));
			addElement(MCrawlerTIntent.DataElements.HOST, null,
					new ElementAttribute(MCrawlerTIntent.DataElements.HOST,
							action.getHost()));
			addElement(MCrawlerTIntent.DataElements.PATH, null,
					new ElementAttribute(MCrawlerTIntent.DataElements.PATH,
							action.getPath()));
			addElement(MCrawlerTIntent.DataElements.TYPE, null,
					new ElementAttribute(MCrawlerTIntent.DataElements.TYPE,
							action.getMimeType()));
			mHandler.endElement("", MCrawlerTIntent.DataElements.DATA,
					MCrawlerTIntent.DataElements.DATA);
		}

		/**
		 * end root
		 */
		mHandler.endElement("", MCrawlerTIntent.INTENT, MCrawlerTIntent.INTENT);
		mHandler.endDocument();

	}
}
