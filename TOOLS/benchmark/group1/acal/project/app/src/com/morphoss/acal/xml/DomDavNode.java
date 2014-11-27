/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.morphoss.acal.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DomDavNode extends DavNode {

	private final static Pattern splitNsTag = Pattern.compile("^(.*):([^:]+)$"); 
	private String tagName;
	private HashMap<String,String> attributes;;
	private String text="";
	private String nameSpace;
	private ArrayList<DomDavNode> children;
	private Map<String,String> nameSpaces;
	private DavNode parent;

	public DomDavNode() {
		this.children = new ArrayList<DomDavNode>();
		this.tagName = "ROOT";
		this.nameSpace = null;
		this.attributes = new HashMap<String,String>();
		this.parent = null;
	}

	public DomDavNode(Node n, String ns, Map<String,String> nameSpaces, DomDavNode parent) {
		this.parent = parent;
		this.nameSpaces = nameSpaces;
		this.nameSpace = ns;
		this.tagName = n.getNodeName();
		this.children = new ArrayList<DomDavNode>();
		
		//Check for name space modifier in tagname
		Matcher m = splitNsTag.matcher(tagName);
		if ( m.matches() ) {
			tagName = m.group(2); 
			nameSpace = nameSpaces.get(m.group(1));
		}

		attributes = new HashMap<String,String>();
		NodeList nl = n.getChildNodes();
		for (int i = 0; i<nl.getLength(); i++) {
			Node item = nl.item(i);
			if (item.getNodeType() == Node.TEXT_NODE || item.getNodeType() == Node.CDATA_SECTION_NODE )
				text += item.getNodeValue();
		}
		NamedNodeMap attr = n.getAttributes();
		for (int i = 0; i<attr.getLength(); i++) {
			Node item = attr.item(i);
			if (item.getNodeType() == Node.ATTRIBUTE_NODE) {

				if (item.getNodeName().length() >= 6 && item.getNodeName().substring(0,6).equalsIgnoreCase("xmlns:")) {
					this.nameSpaces.put(item.getNodeName().substring(6),item.getNodeValue().toLowerCase().trim());
				} else {
					attributes.put(item.getNodeName().toLowerCase().trim(),item.getNodeValue().toLowerCase().trim());
				}
			}
		}
		if (attributes.containsKey("xmlns")) this.nameSpace = attributes.get("xmlns").toLowerCase().trim();
	}

	public String getText() {
		return this.text;
	}

	public String getNameSpace() {
		return this.nameSpace;
	}
	
	public boolean hasAttribute(String key) {
		return attributes.containsKey(key.toLowerCase());
	}
	public String getAttribute(String key) {
		return attributes.get(key.toLowerCase());
	}
	
	
	public void addChild(DomDavNode dn) {
		children.add(dn);
	}

	public DavNode getParent() {
		return this.parent;
	}

	@Override
	protected List<? extends DavNode> getChildren() {
		return Collections.unmodifiableList(children);
	}

	@Override
	public String getTagName() {
		return this.tagName;
	}

	@Override
	protected boolean removeChild(DavNode node) {
		return children.remove(node);		
	}
}
