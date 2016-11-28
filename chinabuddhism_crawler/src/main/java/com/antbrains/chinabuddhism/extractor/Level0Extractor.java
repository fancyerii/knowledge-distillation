package com.antbrains.chinabuddhism.extractor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.JsonArray;
import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.extractor.BasicInfoExtractor;
import com.antbrains.sc.tools.DateTimeTools;
import com.antbrains.sc.tools.GlobalConstants;
import com.antbrains.sc.tools.UrlUtils;
import com.antbrains.sc.tools.batchcrawler.BasicCrawlPage;
import com.antbrains.sc.tools.batchcrawler.BatchCrawler;
import com.antbrains.sc.tools.batchcrawler.CrawlPageInterface;

public class Level0Extractor extends BasicInfoExtractor{
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

		NodeList aList = parser.selectNodes("//LI[@class='title_lir']//A");
		for (int i = 0; i < aList.getLength(); i++) {
			Node a = aList.item(i);
			String anchor = a.getTextContent().trim();
			String href = parser.getNodeText("./@href", a);
			String url = UrlUtils.getAbsoluteUrl(webPage.getUrl(), href);
			if (url.equals("http://www.chinabuddhism.com.cn/e/action/ListInfo/?classid=7"))
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
		String[] urls = new String[] { "http://www.chinabuddhism.com.cn/e/action/ListInfo/?classid=7", };
		Level0Extractor ext = new Level0Extractor();
		for (String url : urls) {
			ext.testUrl(url);
		}
	}
}
