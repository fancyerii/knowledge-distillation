package com.antbrains.httpclientfetcher;

import com.antbrains.httpclientfetcher.HttpClientFetcher;

public class Test {

	public static void main(String[] args) throws Exception {
		HttpClientFetcher fetcher = new HttpClientFetcher("test");
		fetcher.init();

		// String[]
		// arr=fetcher.httpGetReturnRedirect("http://music.baidu.com/song/116208330/51076ed32ca085440b3b4");
		// System.out.println(arr[0]);
		// System.out.println(arr[1]);
		Object[] array = fetcher
				.httpGetReturnRedirectAndCode("http://music.baidu.com/song/116208330/51076ed32ca085440b3b4");
		System.out.println(array[0].toString());
		System.out.println(array[1]);
		System.out.println(array[2]);
		fetcher.close();
	}

}
