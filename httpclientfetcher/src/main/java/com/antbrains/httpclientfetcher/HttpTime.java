package com.antbrains.httpclientfetcher;

import org.apache.http.client.methods.HttpGet;

public class HttpTime {
	public volatile HttpGet get;
	public volatile long startTime;
	public volatile boolean finished = false;

	public HttpTime(HttpGet get, long startTime) {
		this.get = get;
		this.startTime = startTime;
	}
}
