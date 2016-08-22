package com.antbrains.ifengcrawler.extractor;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.JsonParser;
import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.extractor.BasicInfoExtractor;
import com.antbrains.sc.tools.UrlUtils;

public class Level0Extractor extends IfengBasicInfoExtractor {
	protected static Logger logger = Logger.getLogger(Level0Extractor.class);

	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {

	}

	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		List<Block> blocks = new ArrayList<>(1);
		List<String> urls = new ArrayList<>();
		List<String> anchors = new ArrayList<>();

		NodeList aList = parser.selectNodes("//DIV[@id='col_wbf']//LI/A");
		for (int i = 0; i < aList.getLength(); i++) {
			Node a = aList.item(i);
			String anchor = a.getTextContent().trim();
			String href = parser.getNodeText("./@href", a);
			String url = UrlUtils.getAbsoluteUrl(webPage.getUrl(), href);
			if (url.equals("http://fo.ifeng.com/"))
				continue;
			urls.add(url);
			anchors.add(anchor);
		}

		blocks.add(super.addChild(urls, anchors, webPage.getDepth() + 1));
		return blocks;
	}

	@Override
	public boolean needUpdate(WebPage webPage) {
		return true;
	}

	@Override
	public boolean needAddChildren2FrontierIfNotUpdate(WebPage webPage) {
		return true;
	}

	public static void main(String[] args) {
		String[] urls = new String[] { "http://fo.ifeng.com/", };
		Level0Extractor ext = new Level0Extractor();
		for (String url : urls) {
			ext.testUrl(url);
		}
	}
}
