package com.antbrains.nekohtmlparser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlParser {
	public static void main(String[] args) throws Exception {
		String html = "<html><body><div><ul><li class='cls'><a href='a.html'>aa</a></li><li><a href='a.html'>aa</a></li><li><a href='a.html'>aa</a></li></ul></div></body></html>";
		NekoHtmlParser parser = new NekoHtmlParser();
		parser.load(html, "UTF8");
		Node body = parser.selectSingleNode("//BODY");
		System.out.println(XmlParser.printTree(body));
	}

	public Node selectSingleNode(String express) {
		return selectSingleNode(express, doc.getDocumentElement());
	}

	public Node selectSingleNode(String express, Object source) {
		Node result = null;
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		try {
			result = (Node) xpath.evaluate(express, source, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}

		return result;
	}

	public String selectSingleNodeValue(String express, Object source) {
		Node n = this.selectSingleNode(express, source);
		if (n == null)
			return null;
		else
			return n.getTextContent();
	}

	public String selectSingleNodeValue(String express) {
		Node n = this.selectSingleNode(express);
		if (n == null)
			return null;
		else
			return n.getTextContent();
	}

	public NodeList selectNodes(String express) {
		return selectNodes(express, doc.getDocumentElement());
	}

	public NodeList selectNodes(String express, Object source) {
		NodeList result = null;
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		try {
			result = (NodeList) xpath.evaluate(express, source, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}

		return result;
	}

	public Element getRoot() {
		return doc.getDocumentElement();
	}

	private Document doc;

	public boolean load(String content) {
		return load(content, false);
	}

	public boolean load(String content, boolean ignoreDTD) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			factory.setIgnoringElementContentWhitespace(true);

			DocumentBuilder db = factory.newDocumentBuilder();
			if (ignoreDTD) {
				db.setEntityResolver(new EntityResolver() {
					@Override
					public InputSource resolveEntity(String publicId, String systemId)
							throws SAXException, IOException {
						return new InputSource(new StringReader(""));
					}
				});
			}
			doc = db.parse(new InputSource(new StringReader(content)));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;

	}

	public boolean load(File file) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			factory.setIgnoringElementContentWhitespace(true);

			DocumentBuilder db = factory.newDocumentBuilder();
			doc = db.parse(new InputSource(new FileInputStream(file)));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static String toXml(Node node) {
		if (node == null)
			return null;
		TransformerFactory transFactory = TransformerFactory.newInstance();
		try {
			Transformer transformer = transFactory.newTransformer();
			transformer.setOutputProperty("encoding", "UTF-8");
			transformer.setOutputProperty("indent", "yes");

			DOMSource source = new DOMSource();
			source.setNode(node);
			StreamResult result = new StreamResult();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			result.setOutputStream(baos);

			transformer.transform(source, result);
			return baos.toString("UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getInnerXML(Element elem) {
		return getInnerXML(elem, false, true);
	}

	public static String getInnerXML(Element elem, boolean omitDeclaration, boolean omitDocType) {
		OutputFormat of = new OutputFormat("XML", "UTF-8", true);
		of.setIndent(1);
		of.setIndenting(true);
		of.setOmitXMLDeclaration(omitDeclaration);
		of.setOmitDocumentType(omitDocType);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		XMLSerializer serializer = new XMLSerializer(bos, of);
		try {
			serializer.asDOMSerializer();

			serializer.serialize(elem);
			return bos.toString("UTF-8");
		} catch (Exception e) {
			return null;
		}

	}

	public static String printTree(Node n) {
		StringBuilder sb = new StringBuilder();
		printTree(n, 0, sb);
		return sb.toString();
	}

	private static void printTree(Node n, int depth, StringBuilder sb) {
		if (n.getNodeType() == Node.TEXT_NODE) {
			printTab(sb, depth);
			sb.append(n.getTextContent().replaceAll("\n", "<BR>")).append("\n");
		} else if (n.getNodeType() == Node.ELEMENT_NODE) {
			Element e = (Element) n;
			String tag = e.getTagName();
			printTab(sb, depth);
			sb.append("<").append(tag).append(" ");
			NamedNodeMap attrs = e.getAttributes();
			for (int i = 0; i < attrs.getLength(); i++) {
				Node attr = attrs.item(i);
				sb.append("" + attr.getNodeName().replaceAll("['\n]", " ") + "");
				sb.append(" = ");
				sb.append("'" + attr.getNodeValue().replaceAll("['\n]", " ") + "'");
				sb.append(" ");
			}
			sb.append(">\n");
			NodeList children = e.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				printTree(children.item(i), depth + 1, sb);
			}
			printTab(sb, depth);
			sb.append("</").append(tag).append(">\n");

		}
	}

	private static void printTab(StringBuilder sb, int tabNum) {
		for (int i = 0; i < tabNum; i++) {
			sb.append("\t");
		}
	}

}
