package com.antbrains.urlcrawler.crawler;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.urlcrawler.db.CrawlTask;
import com.google.gson.Gson; 

public class Fetcher extends Thread{
	protected static Logger logger=Logger.getLogger(Fetcher.class);
	HttpClientFetcher fetcher;
	BlockingQueue<CrawlTask> taskQueue;
	BlockingQueue<CrawlTask> resultQueue; 
	FetcherAndExtractor fae;
	public Fetcher(HttpClientFetcher fetcher, BlockingQueue<CrawlTask> taskQueue,
			BlockingQueue<CrawlTask> resultQueue, FetcherAndExtractor fae){
		this.fetcher=fetcher;
		this.taskQueue=taskQueue;
		this.resultQueue=resultQueue; 
		this.fae=fae;
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
				logger.debug("task: "+task.crawlUrl);
				this.doWork(task, 0);
			} catch (InterruptedException e) {
			}
		}
		
	}

	private void doWork(CrawlTask task, int depth){
	    if(depth>=2){
	        logger.warn("depth="+depth);
	        task.status=CrawlTask.STATUS_FAILED;
	        task.failReason="depth>=2";
	        return;
	    }
	    List<CrawlTask> newTasks=fae.processTask(fetcher, task);
		try {
			this.resultQueue.put(task);
		} catch (InterruptedException e) {
		}
		if(newTasks!=null){
		    for(CrawlTask newTask:newTasks){
		        doWork(newTask, depth+1);
		    }
		}
	}
	
}
