package com.antbrains.nekohtmlparser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.parsers.AbstractSAXParser;
import org.cyberneko.html.HTMLConfiguration;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class AnchorParser extends AbstractSAXParser {
	private MyHandler handler;

	public AnchorParser() {
		super(new HTMLConfiguration());
		handler = new MyHandler();
		super.setContentHandler(handler);
	}

	public void parse(String html) throws SAXException, IOException {
		StringReader sr = new StringReader(html);
		InputSource is = new InputSource(sr);
		this.parse(is);
	}

	public List<ExtractedUrlAnchorPair> getAnchors() {
		return this.handler.getOutgoingUrls();
	}

	public String getBody() {
		return this.handler.getBody();
	}

	public String getTitle() {
		return this.handler.getTitle();
	}

	public List<ExtractedUrlAnchorPair> parseAnchors(String html) throws SAXException, IOException {
		StringReader sr = new StringReader(html);
		InputSource is = new InputSource(sr);
		this.parse(is);
		return this.handler.getOutgoingUrls();
	}

	public static void main(String[] args) throws SAXException, IOException {
		AnchorParser ap = new AnchorParser();
		{
			String html = "<html><head><title>tt1</title></head><body><title>tt2</title><div><a href='a.html'>aa<em>bb</em>&nbsp;cc</a>汉字</div><script>JavaScript</script></body></html>";
			List<ExtractedUrlAnchorPair> list = ap.parseAnchors(html);
			for (ExtractedUrlAnchorPair euap : list) {
				System.out.println(euap.getHref() + " " + euap.getAnchor());
			}
			System.out.println(ap.getBody());
			System.out.println(ap.getTitle());
		}

		{
			String html = "<html><body><div><a href='http://www.baidu.com/a.html'>aa<em><a href='a.html'>bad</a>cc</em>cc</a>汉字</div></body></html>";
			List<ExtractedUrlAnchorPair> list = ap.parseAnchors(html);
			for (ExtractedUrlAnchorPair euap : list) {
				System.out.println(euap.getHref() + " " + euap.getAnchor());
			}
			System.out.println(ap.getBody());
		}
	}

}

class MyHandler implements ContentHandler {
	private final int MAX_ANCHOR_LENGTH = 500;
	private boolean anchorFlag = false;
	private StringBuilder anchorText = new StringBuilder();
	private ExtractedUrlAnchorPair curUrl;
	private List<ExtractedUrlAnchorPair> outgoingUrls;
	private boolean isWithinBodyElement = false;
	private boolean isWithinScript = false;
	private boolean isWithinTitle = false;
	private boolean isWithinHead = false;
	private boolean isWithinStyle = false;// css
	private StringBuilder bodyText = new StringBuilder("");
	private StringBuilder titleText = new StringBuilder("");

	public String getBody() {
		return bodyText.toString();
	}

	public String getTitle() {
		return titleText.toString();
	}

	public MyHandler() {
		outgoingUrls = new ArrayList<>();
	}

	public List<ExtractedUrlAnchorPair> getOutgoingUrls() {
		return outgoingUrls;
	}

	public void clear() {
		anchorFlag = false;
		anchorText.setLength(0);
		curUrl = null;
		outgoingUrls.clear();
		isWithinBodyElement = false;
		isWithinScript = false;
		bodyText.setLength(0);
		isWithinTitle = false;
		titleText.setLength(0);
		isWithinHead = false;
		isWithinStyle = false;
	}

	@Override
	public void setDocumentLocator(Locator locator) {

	}

	@Override
	public void startDocument() throws SAXException {
		this.clear();
	}

	@Override
	public void endDocument() throws SAXException {

	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {

	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (localName.equalsIgnoreCase("BODY")) {
			isWithinBodyElement = true;
		} else if (localName.equalsIgnoreCase("SCRIPT")) {
			isWithinScript = true;
		} else if (localName.equalsIgnoreCase("A")) {
			String href = atts.getValue("href");
			if (href != null) {
				anchorFlag = true;
				curUrl = new ExtractedUrlAnchorPair();
				curUrl.setHref(href);
				outgoingUrls.add(curUrl);
			}
		} else if (localName.equalsIgnoreCase("TITLE")) {
			if (isWithinHead) {
				isWithinTitle = true;
			}
		} else if (localName.equalsIgnoreCase("HEAD")) {
			isWithinHead = true;
		} else if (localName.equalsIgnoreCase("STYLE")) {
			isWithinStyle = true;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (localName.equalsIgnoreCase("A")) {
			anchorFlag = false;
			if (curUrl != null) {
				String anchor = anchorText.toString().replaceAll("\n", " ").replaceAll("\t", " ").trim();
				if (!anchor.isEmpty()) {
					if (anchor.length() > MAX_ANCHOR_LENGTH) {
						anchor = anchor.substring(0, MAX_ANCHOR_LENGTH) + "...";
					}
					curUrl.setAnchor(anchor);
				}
				anchorText.setLength(0);
			}
			curUrl = null;
		} else if (localName.equalsIgnoreCase("SCRIPT")) {
			isWithinScript = false;
		} else if (localName.equalsIgnoreCase("BODY")) {
			isWithinBodyElement = false;
		} else if (localName.equalsIgnoreCase("TITLE")) {
			isWithinTitle = false;
		} else if (localName.equalsIgnoreCase("HEAD")) {
			isWithinHead = false;
		} else if (localName.equalsIgnoreCase("STYLE")) {
			isWithinStyle = false;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (isWithinBodyElement) {
			if (!isWithinScript && !isWithinStyle) {
				bodyText.append(ch, start, length);
			}
			if (anchorFlag) {
				anchorText.append(ch, start, length);
			}
		} else if (isWithinTitle) {
			titleText.append(ch, start, length);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {

	}

	@Override
	public void skippedEntity(String name) throws SAXException {
	}

}