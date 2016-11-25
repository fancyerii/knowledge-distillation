package com.github.fancyerii.pusa123crawler.extractor;

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

public class Level1Extractor extends BasicInfoExtractor {
	protected static Logger logger = Logger.getLogger(Level1Extractor.class);
	public static int maxPage = 1000;
	public static String pubTimeBound = "2015-01-01";
	public static int maxCrawlThread = 3;

	private CrawlPageInterface cpi = new BasicCrawlPage(3);

	// private Pattern pattern = Pattern.compile("(.*\\/)1(\\/.*)");
	private Pattern pattern = Pattern.compile("(.*\\/list_\\d*_)\\d*(\\..*)");

	private String[] parseListUrl(String url) {
		Matcher m = pattern.matcher(url);
		if (!m.matches())
			return null;
		return new String[] { m.group(1), m.group(2) };
	}

	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		// TODO Auto-generated method stub
		List<Block> blocks = new ArrayList<>(1);
		List<String> urls = new ArrayList<>();
		List<String> anchors = new ArrayList<>();

		String timeBound = pubTimeBound;
		if (webPage.getLastFinishTime() != null) {
			timeBound = DateTimeTools.formatDate(webPage.getLastFinishTime());
			logger.info("useLastFinishTime: " + timeBound + " for: " + webPage.getUrl());
		}
		// http://www.pusa123.com/pusa/news/fo/list_2_1.shtml

		List<ListPageItem> allItems = this.extractItems(webPage.getUrl(), parser, archiver, taskId, content);
		String url = webPage.getUrl();

		// get max page numbers
		Node pageInfo = parser.selectSingleNode("//SPAN[@class='pageinfo']/STRONG");
		if (pageInfo == null) {
			logger.info("Error while getting page info for: " + webPage.getUrl());
			return blocks;
		}

		int lastpage = Integer.parseInt(pageInfo.getTextContent());
		if (lastpage <= 1) {
			return blocks;
		}

		// DIV[@id='pagelist']/A[@href]
		Node pageWithHref = parser.selectNodes("//DIV[@id='pagelist']/A[@href]").item(0);
		String fullurl = parser.getNodeText("./@href", pageWithHref);
		fullurl = UrlUtils.getAbsoluteUrl(url, fullurl);

		String[] urlLeftRightPart = this.parseListUrl(fullurl);
		if (urlLeftRightPart == null) {
			archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "urlLeftRightPart", new Date(),
					GlobalConstants.LOG_LEVEL_WARN, fullurl);
			return blocks;
		}

		for (int i = 2; i <= lastpage; i += maxCrawlThread) {
			int startPage = i;
			int endPage = Math.min(lastpage, i + maxCrawlThread);
			List<String> batchUrls = new ArrayList<>(maxCrawlThread);
			for (int pg = startPage; pg < endPage; pg++) {
				String pageUrl = urlLeftRightPart[0] + pg + urlLeftRightPart[1];
				batchUrls.add(pageUrl);
			}
			List<String[]> batchResults = BatchCrawler.crawler(batchUrls, maxCrawlThread, fetcher, cpi);
			boolean needContinue = true;
			for (String[] pair : batchResults) {
				if (pair[1] == null) {
					archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "batchResults", new Date(),
							GlobalConstants.LOG_LEVEL_WARN, pair[0]);
					continue;
				}
				try {
					parser.load(pair[1], "UTF8");
				} catch (Exception e) {
					archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "parser.load", new Date(),
							GlobalConstants.LOG_LEVEL_WARN, pair[0]);
					continue;
				}
				List<ListPageItem> items = this.extractItems(pair[0], parser, archiver, taskId, pair[1]);
				allItems.addAll(items);
				for (ListPageItem item : items) {
					if (needContinue && item.getPubTime().compareTo(timeBound) < 0) {
						logger.info("skip older: " + item);
						needContinue = false;
					}
				}
			}
			if (!needContinue)
				break;
		}
		Collections.sort(allItems, new Comparator<ListPageItem>() {
			@Override
			public int compare(ListPageItem o1, ListPageItem o2) {
				return o2.getPubTime().compareTo(o1.getPubTime());
			}
		});
		for (ListPageItem item : allItems) {
			urls.add(item.getUrl());
			anchors.add(item.getTitle());
		}

		// update finish time
		if (allItems.size() > 0) {
			String putTime = allItems.iterator().next().getPubTime();
			Date d = DateTimeTools.parseDate(putTime);
			if (d == null) {
				archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "parseDate", new Date(),
						GlobalConstants.LOG_LEVEL_WARN, url);
			} else {
				Calendar c = Calendar.getInstance();
				c.setTime(d);
				// minus one hour to avoid same time (it's yyyy-MM-dd
				// HH:mm)
				c.add(Calendar.HOUR_OF_DAY, -1);
				d = c.getTime();
				// webPage.setLastFinishTime(d);
				archiver.updateFinishTime(webPage, d);
				logger.info("setLastFinish: " + webPage.getUrl() + "\t" + DateTimeTools.formatDate(d));
			}
		}
		blocks.add(super.addChild(urls, anchors, webPage.getDepth() + 1));
		return blocks;
	}

	private List<ListPageItem> extractItems(String url, NekoHtmlParser parser, Archiver archiver, String taskId,
			String content) {
		// TODO Auto-generated method stub
		List<ListPageItem> items = new ArrayList<>(18);
		NodeList liList = parser.selectNodes("//UL[@id='listbox']/LI");
		for (int i = 0; i < liList.getLength(); i++) {
			Node li = liList.item(i);
			Node titleNode = parser.selectSingleNode("./H3/A", li);
			String title = titleNode.getTextContent().trim();
			String href = parser.getNodeText("./@href", titleNode);
			String itemurl = UrlUtils.getAbsoluteUrl(url, href);
			String pubtime = parser.getNodeText(".//DIV[@class='grid-12']", li).trim();
			pubtime = pubtime.split(":")[1];
			ListPageItem listPageItem = new ListPageItem(title, itemurl, pubtime);
			items.add(listPageItem);
		}
		return items;
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
		String[] urls = new String[] { "http://www.pusa123.com/pusa/vod/", };
		Level1Extractor ext = new Level1Extractor();
		ext.pubTimeBound = "2016-10-20";
		ext.maxCrawlThread = 1;
		for (String url : urls) {
			ext.testUrl(url);
		}
	}

}
