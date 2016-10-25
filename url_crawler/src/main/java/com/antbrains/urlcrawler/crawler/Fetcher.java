package com.antbrains.urlcrawler.crawler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.urlcrawler.db.CrawlTask; 

public class Fetcher extends Thread{
	protected static Logger logger=Logger.getLogger(Fetcher.class);
	HttpClientFetcher fetcher;
	BlockingQueue<CrawlTask> taskQueue;
	BlockingQueue<CrawlTask> resultQueue; 
	public Fetcher(HttpClientFetcher fetcher, BlockingQueue<CrawlTask> taskQueue,
			BlockingQueue<CrawlTask> resultQueue){
		this.fetcher=fetcher;
		this.taskQueue=taskQueue;
		this.resultQueue=resultQueue; 
	}
	
	private volatile boolean bStop;
	public void stopMe(){
		bStop=true;
	}
	
	@Override
	public void run(){
		while(!bStop){
			CrawlTask task;
			try {
				task = this.taskQueue.poll(3, TimeUnit.SECONDS);
				if(task==null) continue;
				this.doWork(task);
			} catch (InterruptedException e) {
			}
		}
		
	}
	
	private String getHtml(String url){
		try {
			return fetcher.httpGet(url, 3);
		} catch (Exception e) {
		}
		return null;
	}
	
	private void doWork(CrawlTask task){
		String html=getHtml(task.url);
		if(html==null){
			logger.warn("getFail: "+task.url);
			task.failCount++;
			task.status=CrawlTask.STATUS_FAILED;
			task.failReason=CrawlTask.FAIL_REASON_NETWORK;
		}else{
			task.status=CrawlTask.STATUS_SUCC;
			task.otherInfo=html;
		}
		try {
			this.resultQueue.put(task);
		} catch (InterruptedException e) {
		}
	}
	
}
