package com.antbrains.sc.tools;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlParser {

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
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			factory.setIgnoringElementContentWhitespace(true);

			DocumentBuilder db = factory.newDocumentBuilder();
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
}
