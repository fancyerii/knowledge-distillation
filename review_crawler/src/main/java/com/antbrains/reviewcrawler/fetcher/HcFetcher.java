package com.antbrains.reviewcrawler.fetcher;

import org.apache.log4j.Logger;

import com.antbrains.httpclientfetcher.HttpClientFetcher;

public class HcFetcher implements Fetcher{
	protected static Logger logger=Logger.getLogger(HcFetcher.class);
	
	HttpClientFetcher fetcher;
	public HcFetcher(HttpClientFetcher fetcher){
		this.fetcher=fetcher;
	}
	@Override
	public String httpGet(String url, int retry) {
		try {
			return fetcher.httpGet(url, retry);
		} catch (Exception e) {
			logger.warn(e.getMessage());
			return null;
		}
	}

}
