package com.antbrains.sc.data;

public class CrawlTask {
	public String url;
	public int depth;
	public long lastVisit;
	public long lastFinish;
	public int priority;
	public int failCount;
	public int id;
	public String redirectedUrl;
}
