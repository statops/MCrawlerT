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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

import com.morphoss.acal.Constants;

public class DomDavXmlTreeBuilder  {

	private DomDavNode root;
	public static final String TAG = "aCal DavXMLTreeBuilder";

	public DomDavXmlTreeBuilder (Document dom) {
		try {
			NodeList nl = dom.getChildNodes();
			HashMap<String, String> nameSpaces = new HashMap<String, String>();
			root = new DomDavNode();
			for (int i = 0; i < nl.getLength(); i++) {
				Node item = nl.item(i);
				if ( item.getNodeType() == Node.ELEMENT_NODE ) root.addChild(getSubTree(item, null, nameSpaces, root));
			}
		}
		catch ( Exception e ) {
			Log.e(TAG, "Error occured creating XML tree." + e);
		}
	}

	public DomDavNode getRoot() {
		return this.root;
	}

	public DomDavNode getSubTree(Node n, String ns, Map<String,String> spaces, DomDavNode parent) {
		try {
		DomDavNode root = new DomDavNode(n,ns,spaces,parent);
		if (root.getNameSpace() != null) ns = root.getNameSpace();
		NodeList nl = n.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node item = nl.item(i);
			if (item.getNodeType() == Node.ELEMENT_NODE) root.addChild(getSubTree(item,ns,spaces,root));
		}
		return root;
		} catch (Exception e) {
			Log.e(TAG, "Error occured creating XML tree."+e);
			return null;
		}
	}

	public static DomDavNode buildTreeFromXml(InputStream in) {
		long start = System.currentTimeMillis();

		if ( Constants.LOG_VERBOSE )
			Log.v(TAG,"Building DOM from XML input stream");

		try {
			//Build XML Tree
			DocumentBuilderFactory dof = DocumentBuilderFactory.newInstance();
			DocumentBuilder dob = dof.newDocumentBuilder();
			Document dom = dob.parse(in);
			DomDavXmlTreeBuilder dxtb = new DomDavXmlTreeBuilder(dom);
			DomDavNode root = dxtb.getRoot();
			if (Constants.LOG_VERBOSE)
				Log.v(TAG,"Build DOM from XML completed in "+(System.currentTimeMillis()-start)+"ms");
			return root;

		} catch (Exception e) {
			Log.d(TAG,Log.getStackTraceString(e));
		}
		return null;
		
	}
}
