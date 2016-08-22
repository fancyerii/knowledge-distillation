package com.antbrains.sc.tools.batchcrawler;

import com.antbrains.httpclientfetcher.HttpClientFetcher;

public class BasicCrawlPage implements CrawlPageInterface{
	int retry;
	public BasicCrawlPage(int retry){
		this.retry=retry;
	}
	@Override
	public String crawl(String url, HttpClientFetcher fetcher) {
		try {
			String s = fetcher.httpGet(url, retry);
			return s;
		} catch (Exception e) {
		}
		return null;
	}

}
