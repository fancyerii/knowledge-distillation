package com.antbrains.nekohtmlparser;

import org.junit.Test;
import org.w3c.dom.Node;

import com.antbrains.nekohtmlparser.NekoHtmlParser;

import static org.junit.Assert.*;

public class TestNekoHtmlParser {

	@Test
	public void testParse() throws Exception {
		NekoHtmlParser parser = new NekoHtmlParser();
		String content = "<html><body><h1>H1</h1><DIV></DIV><DIV class='abc'><SPAN>span</SPAN></DIV><script>i++</script></body></html>";
		parser.load(content, "UTF-8", true);
		String s = parser.getNodeText("/HTML");
		assertTrue(!s.contains("i++"));
		String span = parser.getNodeText("//DIV[@class='abc']/SPAN");
		assertEquals(span, "span");
		Node div = parser.selectSingleNode("//DIV[2]");
		assertNotNull(div);
		String span2 = parser.getNodeText("./SPAN", div);
		assertEquals(span2, "span");
	}

	@Test
	public void testParse2() throws Exception {
		NekoHtmlParser parser = new NekoHtmlParser();
		String content = "<html><body><h1>H1</h1><DIV></DIV><DIV class='abc'><SPAN>span</SPAN></DIV><script>i++</script></body></html>";
		parser.load(content, "UTF-8", false);
		String s = parser.getNodeText("/HTML");
		assertTrue(s.contains("i++"));
		String ss = NekoHtmlParser.printTree(parser.selectSingleNode("/HTML"));
		System.out.println(ss);
	}
}
