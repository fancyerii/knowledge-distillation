package com.antbrains.fjnet_crawler.extractor;

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
	
	public static int maxCrawlThread=1;
	public static String pubTimeBound="2000-01-01";
	private CrawlPageInterface cpi=new BasicCrawlPage(3);
	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		
	}

	private int parseInt(String s){
		return Integer.parseInt(s);
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
		
		List<ListPageItem> allItems=this.extractItems(webPage.getUrl(), parser, archiver, taskId, content);
		String url=webPage.getUrl();
		NodeList aList=parser.selectNodes("//DIV[@id='t_content_right']//DIV[4]//A");//"//DIV[@class='listnum']//A");
		NodeList divList=parser.selectNodes("//DIV[@class='t_content_d']//LI");
		int len=aList.getLength();
		int len2=divList.getLength();
		Node lastPageAnchor=aList.item(aList.getLength()-1);
		String s=parser.getNodeText("./@href", lastPageAnchor);//lastPageAnchor.getTextContent().trim();
		s=s.substring(8, 9).trim();
		int lastPage=this.parseInt(s);
		
		
		for(int i=2;i<=lastPage;i+=maxCrawlThread){
			int startPage=i;
			int endPage=Math.min(lastPage, i+maxCrawlThread);
			List<String> batchUrls=new ArrayList<>(maxCrawlThread);
			for(int pg=startPage;pg<endPage;pg++){
				String pageUrl=webPage.getUrl()+"/default_"+pg+".htm";
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
	
	public List<ListPageItem> extractItems(String url, NekoHtmlParser parser, Archiver archiver, String taskId,
			String content) {
		List<ListPageItem> items = new ArrayList<>(40);
		NodeList divList=parser.selectNodes("//DIV[@class='t_content_d']//LI");
		NodeList aList=parser.selectNodes("//DIV[@id='t_content_right']//DIV[4]//A");
		int len=aList.getLength();
		for(int i=0;i<divList.getLength();i++){
			Node div=divList.item(i);
			Node anchor=parser.selectSingleNode("./A", div);
			String href=parser.getNodeText("./@href", anchor); 
			String itemUrl = UrlUtils.getAbsoluteUrl(url, href);
			String anchorText=anchor.getTextContent().trim();
			String pubTime=parser.getNodeText("./EM", div).trim();
			ListPageItem listPageItem = new ListPageItem(anchorText, itemUrl, pubTime);
			items.add(listPageItem);
		}
		
		return items;
	}

	@Override
	public boolean needUpdate(WebPage webPage) {
		return true;
	}
	
	
	public static void main(String[] args) {
		String[] urls = new String[] { "http://www.fjnet.com/yw/", };
		Level0Extractor ext = new Level0Extractor();
		ext.pubTimeBound="2016-11-01";
		ext.maxCrawlThread=1;
		for (String url : urls) {
			ext.testUrl(url);
		}
	}


}
