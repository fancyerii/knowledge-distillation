package com.antbrains.httpclientfetcher;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;

public class MyHttpRoutePlanner2 extends MyHttpRoutePlanner {

	public MyHttpRoutePlanner2() {
		super(null, null);
	}

	private MyHttpRoutePlanner2(SchemePortResolver schemePortResolver) {
		super(schemePortResolver, null);
	}

	private HttpHost proxy;

	public HttpHost getProxy() {
		return proxy;
	}

	public void setProxy(HttpHost proxy) {
		this.proxy = proxy;
	}

	@Override
	protected HttpHost determineProxy(final HttpHost target, final HttpRequest request, final HttpContext context)
			throws HttpException {
		if (proxy == null)
			return null;
		else
			return proxy;
	}
}