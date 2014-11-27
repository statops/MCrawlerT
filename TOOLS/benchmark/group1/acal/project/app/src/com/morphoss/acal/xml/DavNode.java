package com.morphoss.acal.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import com.morphoss.acal.Constants;

public abstract class DavNode {
	
		protected abstract List<? extends DavNode> getChildren();
		protected abstract boolean removeChild(DavNode node);
		
		public abstract String getTagName();
		public abstract DavNode getParent();
		public abstract String getNameSpace();	
		public abstract String getText();
		public abstract boolean hasAttribute(String key);
		public abstract String getAttribute(String key);
		
		/**
		 * Returns the nodes which match the path below the current node, so if this node
		 * is the ROOT then getNodesFromPath("multistatus/response") will return all the
		 * response nodes.  With one of those response nodes getNodesFromPath("propstat/prop/getetag")
		 * would return all getetag nodes (within all props, within all propstats).
		 * 
		 * If a segment of the path is '*' that will match all response nodes present at that point
		 * so that getNodesFromPath("propstat/prop/*") will return all of the nodes within propstat/prop.
		 * 
		 * @param path
		 * @return A list of matching DavNodes, or null if path is null.
		 */
		public List<DavNode> getNodesFromPath(String path) {
			String[] tokens = path.split("/");
			if (tokens.length == 0) return null;

			ArrayList<DavNode> ret = new ArrayList<DavNode>();
			for (DavNode dn : getChildren()) {
				ret.addAll(dn.findNodesFromPath(tokens,0));
			}
			return ret;
		} 
		
		public List<DavNode> findNodesFromPath(String[] path, int curIndex) {
			if (!path[curIndex].equals("*") && !path[curIndex].equals(getTagName())) return new ArrayList<DavNode>();
			ArrayList<DavNode> ret = new ArrayList<DavNode>();
			if (curIndex == path.length-1) {
				ret.add(this);
				return ret;
			}
			for (DavNode dn : getChildren()) {
				ret.addAll(dn.findNodesFromPath(path,curIndex+1));
			}
			return ret;
		}
		
		/**
		 * When given an explicit path matching will return the text value of the first node
		 * matching the path.  Or null if no nodes match the path.
		 * @param path
		 * @return The segment name, or null if no such path existed.
		 */
		public String getFirstNodeText(String path) {
			List<DavNode> textNode = getNodesFromPath(path);
			if ( textNode.isEmpty() ) return null;

			return textNode.get(0).getText();
		}
		
		/**
		 * When given an explicit path matching will return the 'segment' (last part of path)
		 * for the href element at the end of the path.
		 * @param path
		 * @return The segment name, or null if no such path existed.
		 */
		public String segmentFromFirstHref(String path) {
			String name = getFirstNodeText(path);
			if ( name == null ) return null;

			Matcher m = Constants.matchSegmentName.matcher(name);
			if (m.find()) name = m.group(1);
			return name;
		}
		
		public void removeSubTree(DavNode node) {
			for (DavNode dn : getChildren()) {
				if (dn == node) {
					removeChild(node);
					return;
				}
				dn.removeSubTree(node);
			}
		}
}
