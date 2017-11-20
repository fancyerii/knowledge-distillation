package com.antbrains.urlcrawler.crawler;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.urlcrawler.db.CrawlTask;
import com.google.gson.Gson;

public class BasicFetcherAndExtractor implements FetcherAndExtractor{
    protected static Logger logger=Logger.getLogger(BasicFetcherAndExtractor.class);
    private Gson gson=new Gson();
    private String getHtml(HttpClientFetcher fetcher, String url){
        try {
            return fetcher.httpGet(url, 3);
        } catch (Exception e) {
        }
        return null;
    }
    @Override
    public void processTask(HttpClientFetcher fetcher, CrawlTask task) {
        String html=getHtml(fetcher, task.crawlUrl);
        if(html==null){
            logger.warn("getFail: "+task.crawlUrl);
            task.failCount++;
            task.status=CrawlTask.STATUS_FAILED;
            task.failReason=CrawlTask.FAIL_REASON_NETWORK;
        }else{
            task.status=CrawlTask.STATUS_SUCC;
            HashMap<String,String> map=new HashMap<>(1);
            map.put("html", html);
            task.json=gson.toJson(map);
        }
    }

}
