package com.antbrains.reviewcrawler.fetcher;

public interface Fetcher {
	public String httpGet(String url, int retry);
}
