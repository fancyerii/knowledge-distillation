package com.antbrains.urlcrawler.crawler;

import java.util.List;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.urlcrawler.db.CrawlTask;

public interface FetcherAndExtractor {
    public List<CrawlTask> processTask(HttpClientFetcher fetcher, CrawlTask task);
}
