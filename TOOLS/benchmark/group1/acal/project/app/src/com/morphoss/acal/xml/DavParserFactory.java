package com.morphoss.acal.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

public class DavParserFactory {
	
	public static final String TAG = "aCal DavParserFactory";
	
	public enum PARSEMETHOD { DOM, SAX };
	
	public static DavNode buildTreeFromXml(PARSEMETHOD method, InputStream in) {
		if (in == null) return null;
		DavNode root = null;
		try {
			switch (method) {
				case SAX: 	root = SaxDavXmlTreeBuilder.getXmlTree(in);
							break;
				case DOM :
				default: 	root = DomDavXmlTreeBuilder.buildTreeFromXml(in);
			}
		in.close();
		} catch (IOException e) {
			Log.e(TAG,"IOException when parsing XML:\n"+Log.getStackTraceString(e));
		}
		
		return root;
		
	}
	
	//Probably only used for debugging
	public static DavNode buildTreeFromXml(PARSEMETHOD method, String xml) {
		InputStream in = null;
		DavNode root = null;
		in = new ByteArrayInputStream(xml.getBytes());
		root = buildTreeFromXml(method,in);
		return root;
	}
}
