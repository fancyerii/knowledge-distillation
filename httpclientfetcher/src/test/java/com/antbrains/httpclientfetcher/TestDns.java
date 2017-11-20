package com.antbrains.httpclientfetcher;

import java.io.File;


public class TestDns {

	public static void main(String[] args) throws Exception{
		HttpClientFetcher fetcher=new HttpClientFetcher("");
		fetcher.setDnsFile(new File("/home/mc/dnsFile"));
		fetcher.setKeepAlive(false);
		fetcher.init();
		String word = "色差计";

		//String html = fetcher.httpGet("http://dict.youdao.com/w/" + UrlTools.encodeParams(word));
		String url="http://dict.youdao.com/w/" + java.net.URLEncoder.encode(word,"UTF8");
		System.out.println(url);
		String html=fetcher.httpGet(url);
		System.out.println(html);
		html=fetcher.httpGet(url);
		System.out.println(html);
		fetcher.close();
	}

}
