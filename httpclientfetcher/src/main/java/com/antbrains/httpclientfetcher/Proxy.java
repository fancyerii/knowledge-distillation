package com.antbrains.httpclientfetcher;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpHost;

public class Proxy {
	public Proxy(HttpHost host, int id) {
		this.host = host;
		this.id = id;
	}

	public HttpHost host;
	public int id;
	public AtomicLong totalTime = new AtomicLong();
	public AtomicInteger succ = new AtomicInteger();
	public AtomicInteger fail = new AtomicInteger();
	public AtomicInteger totalFail = new AtomicInteger();
	public AtomicInteger totalSucc = new AtomicInteger();
	public volatile long lastUsedTime = 0;
	public volatile double weight = 1.0;
	public int badTimes;
	public long avgTime;

	public void clear() {
		succ.set(0);
		fail.set(0);
		avgTime = 0;
		totalTime.set(0);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Proxy))
			return false;
		Proxy p = (Proxy) o;
		return host.equals(p.host);

	}

	@Override
	public int hashCode() {
		return host.hashCode();
	}

	@Override
	public String toString() {
		if (host == null)
			return "";
		else
			return host.toHostString();
	}
}
