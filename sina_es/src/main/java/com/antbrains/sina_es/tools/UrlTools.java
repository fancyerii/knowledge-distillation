package com.antbrains.sina_es.tools;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class UrlTools {
	public static String getDomainName(String url) throws URISyntaxException {
	    URI uri = new URI(url);
	    String host = uri.getHost();
	    return host;
	}
	public static String getAbsoluteUrl(String baseUrl, String url) {
		try {
			URL u = new URL(baseUrl);
			URL uu = new URL(u, url);
			return uu.toString();
		} catch (MalformedURLException e) {
			return null;
		}

	}
}
