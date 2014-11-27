package com.morphoss.acal.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import android.util.Log;

public class SaxDavXmlTreeBuilder  {

	public static final String TAG = "acal SaxDavXmlTreebuilder";
	
	public static SaxDavNode getXmlTree(InputStream xml) throws IOException {
		  SAXParserFactory factory = SAXParserFactory.newInstance();
		  SaxDavNode root = new SaxDavNode();
		  try {
		        SAXParser saxParser = factory.newSAXParser();
		        saxParser.parse( xml, root.getHandler() );

		  } catch (ParserConfigurationException e) {
			  //Error parsing the document;
			  Log.w(TAG, "Error setting up parser: "+e);
		  } catch (SAXException e) {
			  //Error parsing the document;
			  Log.i(TAG, "Error parsing xml document: "+e);
		  } 
		  
		  return root;
	}
}
