package com.antbrains.urlcrawler.db;

import java.util.Date;

public class CrawlTask {
	public static final int STATUS_NOT_CRAWLED=0;
	public static final int STATUS_CRAWLING=1;
	public static final int STATUS_FAILED=2;
	public static final int STATUS_SUCC=3;
	
	public static final String FAIL_REASON_NETWORK="network";
	
	public String pk;
	public String crawlUrl;
	public int failCount;
	public String failReason;
	public int status;
	public Date lastUpdate;
	
	public String json;
	
	@Override
	public String toString(){
		return crawlUrl+"\tfailCount: "+failCount+"\tfailReason: "+failReason+"\tstatus: "+status;
	}
	
	public CrawlTask copy(){
		CrawlTask task=new CrawlTask();
		task.pk=pk;
		task.crawlUrl=crawlUrl;
		task.failCount=failCount;
		task.failReason=failReason;
		task.status=status;
		task.lastUpdate=(Date) lastUpdate.clone();
		task.json=json;
		return task;
	}
}
