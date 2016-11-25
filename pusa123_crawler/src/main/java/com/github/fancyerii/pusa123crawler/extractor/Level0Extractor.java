package com.github.fancyerii.pusa123crawler.extractor;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.extractor.BasicInfoExtractor;
import com.antbrains.sc.tools.UrlUtils;

public class Level0Extractor extends BasicInfoExtractor {
	protected static Logger logger = Logger.getLogger(Level0Extractor.class);
	
	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		// TODO Auto-generated method stub
		List<Block> blocks = new ArrayList<>(1);
		List<String> urls = new ArrayList<>();
		List<String> anchors = new ArrayList<>();
		
		NodeList aList = parser.selectNodes("//SPAN[@class='hot']/A");
		for (int i = 0; i < aList.getLength(); i++) {
			Node a = aList.item(i);
			String anchor = a.getTextContent().trim();
			String href = parser.getNodeText("./@href", a);
			String url = UrlUtils.getAbsoluteUrl(webPage.getUrl(), href);
			if (url.equals("http://www.pusa123.com/pusa/news/"))
				continue;
			urls.add(url);
			anchors.add(anchor);
		}
		blocks.add(super.addChild(urls, anchors, webPage.getDepth() + 1));
		return blocks;
	}

	@Override
	public void extractProps(WebPage arg0, NekoHtmlParser arg1, HttpClientFetcher arg2, String arg3, Archiver arg4,
			String arg5) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean needUpdate(WebPage arg0) {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public boolean needAddChildren2FrontierIfNotUpdate(WebPage webPage) {
		return true;
	}
	
	public static void main(String[] args) {
		String[] urls = new String[] { "http://www.pusa123.com/pusa/news/", };
		Level0Extractor ext = new Level0Extractor();
		for (String url : urls) {
			ext.testUrl(url);
		}
	}

}
