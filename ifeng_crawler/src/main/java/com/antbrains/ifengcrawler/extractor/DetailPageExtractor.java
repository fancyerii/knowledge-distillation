package com.antbrains.ifengcrawler.extractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.JsonParser;
import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage; 

public class DetailPageExtractor extends IfengBasicInfoExtractor {
	protected static Logger logger = Logger.getLogger(DetailPageExtractor.class);

	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		String title=parser.getNodeText("//H1");
		Map<String, String> attrs = webPage.getAttrs();
		if(attrs==null){
			attrs = new HashMap<>();
			webPage.setAttrs(attrs);
		}
		attrs.put("#title#", title);
	}

	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		return null;
	}

	@Override
	public boolean needUpdate(WebPage webPage) {
		return super.needUpdate(webPage.getLastVisitTime(), 15, 0, 0, 0);
	}

	@Override
	public boolean needAddChildren2FrontierIfNotUpdate(WebPage webPage) {
		return false;
	}

	public static void main(String[] args) {
		String[] urls = new String[] { "http://fo.ifeng.com/a/20160729/44429366_0.shtml", };
		DetailPageExtractor ext = new DetailPageExtractor();
		for (String url : urls) {
			ext.testUrl(url);
		}
	}
}
