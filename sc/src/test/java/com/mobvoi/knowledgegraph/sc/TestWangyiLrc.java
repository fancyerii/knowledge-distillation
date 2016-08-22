package com.mobvoi.knowledgegraph.sc;

import com.antbrains.httpclientfetcher.HttpClientFetcher;

public class TestWangyiLrc {

	public static void main(String[] args) throws Exception {
		HttpClientFetcher fetcher = new HttpClientFetcher("");

		fetcher.setReferer("http://music.163.com/");
		fetcher.init();
		String s = fetcher.httpGet("http://music.163.com/api/song/lyric?os=pc&id=29431066&lv=-1&kv=-1&tv=-1");
		System.out.println(s);
		fetcher.close();
	}

}
