package com.morphoss.acal.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.morphoss.acal.Constants;

public class SaxDavNode extends DavNode {

	private static final String TAG = "acal SaxDavNode";

	//vars that define THIS Node
	private String tagName;
	private HashMap<String,String> attributes;;
	private String text;
	private ArrayList<SaxDavNode> children;
	private SaxDavNode parent;
	
	public SaxDavNode() {
		this.children = new ArrayList<SaxDavNode>();
		this.attributes = new HashMap<String,String>();
		this.tagName = "ROOT";
		this.parent = null;
		if (Constants.LOG_VERBOSE && Constants.debugSaxParser ) {
			Log.v(TAG,"Created ROOT Node");
		}
	}
	
	private SaxDavNode(String tag, Attributes attributes, SaxDavNode parent) {
		this.tagName = tag;
		this.attributes = new HashMap<String,String>();
		this.parent = parent;
		this.children = new ArrayList<SaxDavNode>();
		for (int i = 0; i<attributes.getLength(); i++) {
			String name = attributes.getLocalName(i);
			if (name == null || name.equals("")) name = attributes.getQName(i);
			this.attributes.put(name, attributes.getValue(i));
		}
		if (Constants.LOG_VERBOSE && Constants.debugSaxParser) {
			Log.v(TAG,"Created Child Node: "+tag);
		}
	}
	
	public SaxDavHandler getHandler() {
		return new SaxDavHandler();
	}

	@Override
	protected List<? extends DavNode> getChildren() {
		return Collections.unmodifiableList(children);
	}

	@Override
	public String getNameSpace() {
		throw new UnsupportedOperationException("SAX Method does not support name spaces.");
	}

	@Override
	public DavNode getParent() {
		return this.parent;
	}

	@Override
	public String getTagName() {
		return this.tagName;
	}

	@Override
	public String getText() {
		return this.text;
	}

	@Override
	protected boolean removeChild(DavNode node) {
		return children.remove(node);
	}
	
	@Override
	public String getAttribute(String key) {
		return attributes.get(key);
	}

	@Override
	public boolean hasAttribute(String key) {
		return attributes.containsKey(key);
	}
	
	private class SaxDavHandler extends DefaultHandler {
		
		private StringBuffer textbuffer; //store chars here
		
		//If we are in a child, we pass handler calls to the child
		private SaxDavNode child;
		private SaxDavHandler childHandler;
		private boolean inChild = false;
		
		public void startDocument() throws SAXException {
			//Not Used
	    }

	    public void endDocument() throws SAXException {
	        //Not Used
	    }

	    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
	    	if (inChild) {
	    		childHandler.startElement(uri, localName, qName, attributes);
	    		return;
	    	}
	    	String name = localName;
	    	if (name == null || name.equals("")) name = qName;
	    	child = new SaxDavNode(name, attributes, SaxDavNode.this);
	    	inChild = true;
	    	childHandler = child.getHandler();
   	    }

	    public void endElement(String uri, String localName, String qName) throws SAXException {
	    	if (inChild) {
	    		if (childHandler.inChild) {
	    			childHandler.endElement(uri, localName, qName);
	    		} else if (child.tagName.equals(qName) || child.tagName.equals(localName)){
	    			if (childHandler.textbuffer != null) {
	    				child.text = childHandler.textbuffer.toString();
	    				childHandler.textbuffer = null;
	    			}
	    			inChild = false;
	    	        SaxDavNode.this.children.add(child);
	    	        child = null;		
	    		} else {
	    			throw new SAXException("Malformed xml? Closing tag did not match opening tag");
	    		}
	    	}
	    }

	    public void characters(char ch[], int start, int length) throws SAXException {
	        if (inChild) childHandler.characters(ch, start, length);
	        else {
	        	if (textbuffer == null) {
	        		textbuffer = new StringBuffer(new String(ch,start,length));
	        	} else {
	        		textbuffer.append(new String(ch,start,length));
	        	}
	        }
	    }
	}
}
