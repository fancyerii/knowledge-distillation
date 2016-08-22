package com.antbrains.httpclientfetcher;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

public class MyHttpRoutePlanner extends DefaultRoutePlanner {
	protected static Logger logger = Logger.getLogger(MyHttpRoutePlanner.class);
	private ProxyManager pm;

	public ProxyManager getPm() {
		return pm;
	}

	public MyHttpRoutePlanner(ProxyManager pm) {
		this(null, pm);
	}

	protected MyHttpRoutePlanner(SchemePortResolver schemePortResolver, ProxyManager pm) {
		super(schemePortResolver);
		this.pm = pm;

	}

	@Override
	protected HttpHost determineProxy(final HttpHost target, final HttpRequest request, final HttpContext context)
			throws HttpException {
		Proxy oldProxy = (Proxy) context.getAttribute("proxy");
		if (oldProxy != null)
			return oldProxy.host;
		Proxy proxy = pm.getProxy();
		while (proxy == null) {
			logger.error("no proxy, so checkBadProxy");
			pm.checkBadProxys(true);
			proxy = pm.getProxy();
		}
		context.setAttribute("startTime", System.currentTimeMillis());
		context.setAttribute("proxy", proxy);
		return proxy.host;
	}

}
