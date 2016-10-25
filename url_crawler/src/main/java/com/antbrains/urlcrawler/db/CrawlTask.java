package com.antbrains.urlcrawler.db;

import java.util.Date;

public class CrawlTask {
	public static final int STATUS_NOT_CRAWLED=0;
	public static final int STATUS_CRAWLING=1;
	public static final int STATUS_FAILED=2;
	public static final int STATUS_SUCC=3;
	
	public static final String FAIL_REASON_NETWORK="network";
	
	public String url;
	public int failCount;
	public String failReason;
	public int status;
	public Date lastUpdate;
	
	public String otherInfo;
	
	@Override
	public String toString(){
		return url+"\tfailCount: "+failCount+"\tfailReason: "+failReason+"\tstatus: "+status;
	}
	
	public CrawlTask copy(){
		CrawlTask task=new CrawlTask();
		task.url=url;
		task.failCount=failCount;
		task.failReason=failReason;
		task.status=status;
		task.lastUpdate=(Date) lastUpdate.clone();
		task.otherInfo=otherInfo;
		return task;
	}
}
