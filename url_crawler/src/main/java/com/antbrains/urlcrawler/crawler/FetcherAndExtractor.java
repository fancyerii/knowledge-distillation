package com.antbrains.urlcrawler.crawler;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.urlcrawler.db.CrawlTask;

public interface FetcherAndExtractor {
    public void processTask(HttpClientFetcher fetcher, CrawlTask task);
}
