package com.antbrains.sc.tools.batchcrawler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.antbrains.httpclientfetcher.HttpClientFetcher;

public class BatchCrawler {
	protected static Logger logger=Logger.getLogger(BatchCrawler.class);
	public static List<String[]> crawler(List<String> urls, int numThread, HttpClientFetcher fetcher, CrawlPageInterface cpi){
		return crawler(urls, numThread, fetcher, cpi, Long.MAX_VALUE);
	}
	public static List<String[]> crawler(List<String> urls, int numThread, HttpClientFetcher fetcher, CrawlPageInterface cpi, long timeOut){
		List<String[]> result=new ArrayList<>(urls.size());
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThread);
		List<Future<String[]>> futures = new ArrayList<>();
		
		for(String url:urls){
			futures.add(executor.submit(new CrawlCallable(url, cpi, fetcher)));
		}
		
		for(Future<String[]> future:futures){
			try {
				String[] urlHtml=future.get(timeOut, TimeUnit.MILLISECONDS);
				result.add(urlHtml);
			} catch(Exception e){
				
			}
		}
		executor.shutdownNow();
		return result;
	}
	
	public static void main(String[] args) {
		HttpClientFetcher fetcher =new HttpClientFetcher("");
		fetcher.init();
		CrawlPageInterface cpi=new BasicCrawlPage(3);
		String[] urls=new String[]{
				"http://fo.ifeng.com/a/20160814/44436705_0.shtml",
				"http://fo.ifeng.com/listpage/119/2/list.shtml",
				"http://fo.ifeng.com/listpage/119/3/list.shtml",
				"http://fo.ifeng.com/listpage/119/4/list.shtml",
				"http://fo.ifeng.com/listpage/119/5/list.shtml",
				"http://fo.ifeng.com/listpage/119/6/list.shtml"
		};
		List<String[]> result=BatchCrawler.crawler(Arrays.asList(urls), 2, fetcher, cpi, Long.MAX_VALUE);
		for(String[] pair:result){
			if(pair[1]==null){
				System.out.println(pair[0]+"\tnull");
			}else{
				System.out.println(pair[0]+"\t"+pair[1].substring(0, 100));
			}
		}
		fetcher.close();
	}

}

class CrawlCallable implements Callable<String[]>{
	private String url;
	private CrawlPageInterface cpi;
	private HttpClientFetcher fetcher;
	public CrawlCallable(String url, CrawlPageInterface cpi, HttpClientFetcher fetcher){
		this.url=url;
		this.cpi=cpi;
		this.fetcher=fetcher;
	}
	@Override
	public String[] call() throws Exception {
		return new String[]	{url, cpi.crawl(url, fetcher)};
	}
	
}
