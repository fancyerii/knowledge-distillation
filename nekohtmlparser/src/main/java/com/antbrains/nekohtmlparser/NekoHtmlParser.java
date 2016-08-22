package com.antbrains.nekohtmlparser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xpath.XPathAPI;
import org.cyberneko.html.filters.ElementRemover;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class NekoHtmlParser {

	private Document document;

	public NodeList selectNodes(String path) {
		try {
			return XPathAPI.selectNodeList(document, path);
		} catch (Exception e) {
			return null;
		}
	}

	public Node selectSingleNode(String path) {
		try {
			return XPathAPI.selectSingleNode(document, path);
		} catch (Exception e) {
			return null;
		}
	}

	public static Node getChildByTag(Node parent, String tagName) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (((Element) child).getTagName().equalsIgnoreCase(tagName)) {
					return child;
				}
			}
		}
		return null;
	}

	public static List<Node> getChildrenByTag(Node parent, String tagName) {
		NodeList children = parent.getChildNodes();
		List<Node> result = new ArrayList<Node>(children.getLength());
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (((Element) child).getTagName().equalsIgnoreCase(tagName)) {
					result.add(child);
				}
			}
		}
		return result;
	}

	public NodeList selectNodes(String path, Node node) {
		try {
			return XPathAPI.selectNodeList(node, path);
		} catch (Exception e) {
			return null;
		}
	}

	public Node selectSingleNode(String path, Node node) {
		try {
			return XPathAPI.selectSingleNode(node, path);
		} catch (Exception e) {
			return null;
		}
	}

	public Object eval(String path, Node node) throws TransformerException {

		return XPathAPI.eval(node, path);

	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public Document getDocument() {
		return this.document;
	}

	public boolean load(String content, String charSet) throws Exception {
		return load(content, charSet, false);
	}

	private void addRemover(DOMParser parser) throws Exception {
		ElementRemover remover = new MdrElementRemover();
		remover.removeElement("script");
		parser.setProperty("http://cyberneko.org/html/properties/filters", new XMLDocumentFilter[] { remover });
	}

	public boolean load(String content, String charSet, boolean removeScript) throws Exception {
		DOMParser parser = new DOMParser();
		XMLInputSource source = new XMLInputSource(null, "xpath-wrapper", null, new StringReader(content), charSet);

		parser.setFeature("http://xml.org/sax/features/namespaces", false);
		parser.setProperty("http://cyberneko.org/html/properties/default-encoding", charSet);
		if (removeScript) {
			addRemover(parser);
		}
		parser.parse(source);
		document = parser.getDocument();
		return true;
	}

	public static String printTree(Node n) {
		StringBuilder sb = new StringBuilder();

		recurPrint(n, sb, 0);
		return sb.toString();
	}

	private static void recurPrint(Node n, StringBuilder sb, int indent) {
		if (n instanceof Element) {
			String tag = ((Element) n).getTagName();
			for (int i = 0; i < indent; i++) {
				sb.append("\t");
			}
			sb.append("<" + tag + ">").append("\n");
			NodeList children = n.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				recurPrint(children.item(i), sb, indent + 1);
			}
			for (int i = 0; i < indent; i++) {
				sb.append("\t");
			}
			sb.append("</" + tag + ">").append("\n");
		} else {
			for (int i = 0; i < indent; i++) {
				sb.append("\t");
			}
			String ss = n.getTextContent();
			if (ss.length() > 50) {
				ss = ss.substring(0, 50) + "...";
			}
			sb.append("[" + ss + "]" + "\n");
		}
	}

	public boolean load(byte[] content, String charSet) throws Exception {
		return load(content, charSet, false);
	}

	public boolean load(byte[] content, String charSet, boolean removeScript) throws Exception {
		DOMParser parser = new DOMParser();

		InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(content), charSet);
		InputSource source = new InputSource(reader);
		parser.setFeature("http://xml.org/sax/features/namespaces", false);
		parser.setProperty("http://cyberneko.org/html/properties/default-encoding", charSet);
		if (removeScript) {
			addRemover(parser);
		}
		parser.parse(source);
		document = parser.getDocument();

		return true;
	}

	public boolean load(File file, String charSet) throws Exception {
		return load(file, charSet, false);
	}

	public boolean load(File file, String charSet, boolean removeScript) throws Exception {
		DOMParser parser = new DOMParser();

		InputStreamReader reader = new InputStreamReader(new FileInputStream(file), charSet);
		InputSource source = new InputSource(reader);
		parser.setFeature("http://xml.org/sax/features/namespaces", false);
		if (removeScript) {
			addRemover(parser);
		}
		parser.parse(source);
		document = parser.getDocument();

		return true;
	}

	public String getNodeText(String xpath) {
		Node node = selectSingleNode(xpath);
		if (node == null)
			return "";
		return node.getTextContent();
	}

	public String getNodeText(String xpath, Node sourceNode) {
		Node node = selectSingleNode(xpath, sourceNode);
		if (node == null)
			return "";
		return node.getTextContent();
	}

}

class MdrElementRemover extends ElementRemover {
	protected int fNonElementDepth = 0;

	// since Xerces-J 2.2.0

	/** Start prefix mapping. */
	public void startPrefixMapping(String prefix, String uri, Augmentations augs) throws XNIException {
		fNonElementDepth++;
	}

	/** End prefix mapping. */
	public void endPrefixMapping(String prefix, Augmentations augs) throws XNIException {
		fNonElementDepth--;
	}

	//
	// Protected methods
	//

	/** Start element. */
	public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
		if (fElementDepth <= fRemovalElementDepth && handleOpenTag(element, attributes)) {
			super.startElement(element, attributes, augs);
		}
		fElementDepth++;
	}

	/** Empty element. */
	public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
		if (fElementDepth <= fRemovalElementDepth && handleOpenTag(element, attributes)) {
			super.emptyElement(element, attributes, augs);
		}
	}

	/** Comment. */
	public void comment(XMLString text, Augmentations augs) throws XNIException {
	}

	/** Processing instruction. */
	public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
	}

	/** Characters. */
	public void characters(XMLString text, Augmentations augs) throws XNIException {
		if (fElementDepth <= fRemovalElementDepth && fNonElementDepth <= 0) {
			if (text.toString().trim().length() > 0) {
				super.characters(text, augs);
			}
		}
	}

	/** Ignorable whitespace. */
	public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
	}

	/** Start general entity. */
	public void startGeneralEntity(String name, XMLResourceIdentifier id, String encoding, Augmentations augs)
			throws XNIException {
		fNonElementDepth++;
	}

	/** Text declaration. */
	public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
	}

	/** End general entity. */
	public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
		fNonElementDepth--;
	}

	/** Start CDATA section. */
	public void startCDATA(Augmentations augs) throws XNIException {
		fNonElementDepth++;
	}

	/** End CDATA section. */
	public void endCDATA(Augmentations augs) throws XNIException {
		fNonElementDepth--;
	}

	/** End element. */
	public void endElement(QName element, Augmentations augs) throws XNIException {
		if (fElementDepth <= fRemovalElementDepth && elementAccepted(element.rawname)) {
			super.endElement(element, augs);
		}
		fElementDepth--;
		if (fElementDepth == fRemovalElementDepth) {
			fRemovalElementDepth = Integer.MAX_VALUE;
		}
	}

	/** Returns true if the specified element is accepted. */
	protected boolean elementAccepted(String element) {
		return true;
	}

	/** Returns true if the specified element should be removed. */
	protected boolean elementRemoved(String element) {
		Object key = element.toLowerCase();
		if (key.equals("style") || key.equals("script")) {
			return true;
		} else {
			return false;
		}
	}

	/** Handles an open tag. */
	protected boolean handleOpenTag(QName element, XMLAttributes attributes) {
		if (elementRemoved(element.rawname)) {
			fRemovalElementDepth = fElementDepth;
			return false;
		}
		return true;
	}
}
