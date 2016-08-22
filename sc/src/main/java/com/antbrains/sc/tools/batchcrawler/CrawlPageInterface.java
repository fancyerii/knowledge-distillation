package com.antbrains.sc.tools.batchcrawler;

import com.antbrains.httpclientfetcher.HttpClientFetcher;

public interface CrawlPageInterface {
	public String crawl(String url, HttpClientFetcher fetcher);
}
