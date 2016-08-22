package com.antbrains.httpclientfetcher;

import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;

import com.antbrains.httpclientfetcher.HttpClientFetcher;

public class TestProxy {

	public static void main(String[] args) throws Exception {
		HttpClientFetcher fetcher = new HttpClientFetcher("test");
		fetcher.setProxy(new HttpHost("58.220.10.7", 80));
		fetcher.setCs(CookieSpecs.BROWSER_COMPATIBILITY);
		fetcher.init();
		//
		// http://www.dianping.com/shop/8114003/review_more
		String s = fetcher.httpGet("http://www.mafengwo.cn/poi/5844.html");
		System.out.println(s);
		fetcher.close();
	}

}
