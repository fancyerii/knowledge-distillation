package com.antbrains.ifengcrawler.extractor;

import java.text.SimpleDateFormat;
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

import com.google.gson.JsonParser;
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

public class ListPageExtractor extends IfengBasicInfoExtractor {
	protected static Logger logger = Logger.getLogger(ListPageExtractor.class);
	public static int maxPage=20;
	public static String pubTimeBound="2018-01-01";
	public static int maxCrawlThread=1;
	
	private CrawlPageInterface cpi=new BasicCrawlPage(3);
	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
	}

	public List<ListPageItem> extractItems(String url, NekoHtmlParser parser, Archiver archiver, String taskId,
			String content) {
		List<ListPageItem> items = new ArrayList<>(10);
		NodeList divs = parser.selectNodes("//DIV[@class='col_L']/DIV[1]/DIV");
		if (divs == null) {
			archiver.saveLog(taskId, "ListPageExtractor.extractItems", "divs==null", new Date(),
					GlobalConstants.LOG_LEVEL_WARN, url + "##" + content);
		} else {
			for (int i = 0; i < divs.getLength(); i++) {
				Node div = divs.item(i);
				String href = parser.getNodeText("./H2/A/@href", div);
				String itemUrl = UrlUtils.getAbsoluteUrl(url, href);
				String title = parser.getNodeText("./H2/A", div);
				String pubTime = parser.getNodeText("./DIV[contains(@class,'box_txt')]/SPAN", div);
				ListPageItem listPageItem = new ListPageItem(title, itemUrl, pubTime);
				items.add(listPageItem);
			}
		}
		return items;
	}
	
	private Pattern pattern=Pattern.compile("(.*\\/)1(\\/.*)");
	private String[] parseListUrl(String url){
		Matcher m=pattern.matcher(url);
		if(!m.matches()) return null;
		return new String[]{
			m.group(1),
			m.group(2)
		};
	}
	
	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		List<Block> blocks = new ArrayList<>(1);
		List<String> urls = new ArrayList<>();
		List<String> anchors = new ArrayList<>();
		
		String timeBound=pubTimeBound;
		if(webPage.getLastFinishTime()!=null){
			timeBound = DateTimeTools.formatDate(webPage.getLastFinishTime());
			logger.info("useLastFinishTime: "+timeBound+" for: "+webPage.getUrl());
		}
		//http://fo.ifeng.com/listpage/119/1/list.shtml
		String url=webPage.getUrl();
		String[] urlLeftRightPart=this.parseListUrl(url);
		if(urlLeftRightPart==null){
			archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "urlLeftRightPart", new Date(),
					GlobalConstants.LOG_LEVEL_WARN, url);
			return blocks;
		}
		
		//process firstPage
		boolean skip=false;
		List<ListPageItem> allItems=this.extractItems(url, parser, archiver, taskId, content);
		if(timeBound!=null){
			for(ListPageItem item:allItems){
				if(item.getPubTime().compareTo(timeBound)<0){
					logger.info("found old item so skip: "+item.toString());
					skip=true;
					break;
				}
			}
		}
		for(int i=2;!skip && i<=maxPage;i+=maxCrawlThread){
			logger.info("crawl page "+i+" of "+url);
			int startPage=i;
			int endPage=Math.min(maxPage, i+maxCrawlThread);
			List<String> batchUrls=new ArrayList<>(maxCrawlThread);
			for(int pg=startPage;pg<endPage;pg++){
				String pageUrl=urlLeftRightPart[0]+pg+urlLeftRightPart[1];
				batchUrls.add(pageUrl);
			}
			List<String[]> batchResults=BatchCrawler.crawler(batchUrls, maxCrawlThread, fetcher, cpi);
			boolean needContinue=true;
			for(String[] pair:batchResults){
				if(pair[1]==null){
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
				for(ListPageItem item:items){
					if(needContinue && item.getPubTime().compareTo(timeBound)<0){
						logger.info("skip older: "+item);
						needContinue=false;
					}
				}
			}
			
			if(!needContinue) break;
		}
		
		Collections.sort(allItems, new Comparator<ListPageItem>(){
			@Override
			public int compare(ListPageItem o1, ListPageItem o2) {
				return o2.getPubTime().compareTo(o1.getPubTime());
			}	
		});
		for(ListPageItem item:allItems){
			urls.add(item.getUrl());
			anchors.add(item.getTitle());
		}
		//update finish time
		if(allItems.size()>0){
			String putTime=allItems.iterator().next().getPubTime();
			Date d=DateTimeTools.parseDate(putTime);
			if(d==null){
				archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "parseDate", new Date(),
						GlobalConstants.LOG_LEVEL_WARN, url);
			}else{
				Calendar c = Calendar.getInstance();
				c.setTime(d);
				//minus one hour to avoid same time in ifeng(it's yyyy-MM-dd HH:mm)
				c.add(Calendar.HOUR_OF_DAY, -1);
				d=c.getTime();
				//webPage.setLastFinishTime(d);
				archiver.updateFinishTime(webPage, d);
				logger.info("setLastFinish: "+webPage.getUrl()+"\t"+DateTimeTools.formatDate(d));
			}
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
		String[] urls = new String[] { "http://fo.ifeng.com/listpage/8537/1/list.shtml", };
		ListPageExtractor ext = new ListPageExtractor();
		ext.pubTimeBound="2018-07-01";
		ext.maxCrawlThread=1;
		for (String url : urls) {
			ext.testUrl(url);
		}
	}
}
