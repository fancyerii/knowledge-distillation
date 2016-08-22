package com.antbrains.sc.tools;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

public class UrlUtils {
	public static String getAbsoluteUrl(String baseUrl, String url) {
		try {
			URL u = new URL(baseUrl);
			URL uu = new URL(u, url);
			return uu.toString();
		} catch (MalformedURLException e) {
			return null;
		}

	}

	private static boolean isChinese(char ch) {
		return (ch >= '\u4E00' && ch <= '\u9FA5') || (ch >= '\uF900' && ch <= '\uFA2D');
	}

	public static String encodeChinese(String url) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < url.length(); i++) {
			char ch = url.charAt(i);
			if (isChinese(ch)) {
				try {
					sb.append(java.net.URLEncoder.encode(ch + "", "UTF8"));
				} catch (UnsupportedEncodingException e) {

				}
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	public static String removeAnchor(String url) {
		try {
			URL u = new URL(url);
			return removeAnchor(u);
		} catch (MalformedURLException e) {
			return url;
		}
	}

	public static String removeAnchorAndQuery(String url) {
		try {
			URL u = new URL(url);
			return removeAnchorAndQuery(u);
		} catch (MalformedURLException e) {
			return url;
		}
	}

	public static String removeAnchorAndQuery(URL u) {

		// pre-compute length of StringBuffer
		int len = u.getProtocol().length() + 1;
		if (u.getAuthority() != null && u.getAuthority().length() > 0)
			len += 2 + u.getAuthority().length();
		if (u.getPath() != null) {
			len += u.getPath().length();
		}
		if (u.getQuery() != null) {
			len += 1 + u.getQuery().length();
		}
		if (u.getRef() != null)
			len += 1 + u.getRef().length();

		StringBuffer result = new StringBuffer(len);
		result.append(u.getProtocol());
		result.append(":");
		if (u.getAuthority() != null && u.getAuthority().length() > 0) {
			result.append("//");
			result.append(u.getAuthority());
		}
		if (u.getPath() != null) {
			result.append(u.getPath());
		}

		return result.toString();
	}

	public static String encodeCn(String url, String encoding)
			throws MalformedURLException, UnsupportedEncodingException {
		URL u = new URL(url);
		return encodeCn(u, encoding);
	}

	public static String encodeCn(URL u, String encoding) throws UnsupportedEncodingException {
		// pre-compute length of StringBuffer
		int len = u.getProtocol().length() + 1;
		if (u.getAuthority() != null && u.getAuthority().length() > 0)
			len += 2 + u.getAuthority().length();
		if (u.getPath() != null) {
			len += u.getPath().length();
		}
		if (u.getQuery() != null) {
			len += 1 + u.getQuery().length();
		}
		if (u.getRef() != null)
			len += 1 + u.getRef().length();

		StringBuffer result = new StringBuffer(len);
		result.append(u.getProtocol());
		result.append(":");
		if (u.getAuthority() != null && u.getAuthority().length() > 0) {
			result.append("//");
			result.append(u.getAuthority());
		}
		if (u.getPath() != null) {

			result.append(java.net.URLEncoder.encode(u.getPath(), "UTF8"));
		}
		if (u.getQuery() != null) {
			result.append('?');
			result.append(java.net.URLEncoder.encode(u.getQuery(), "UTF8"));
		}

		return result.toString();
	}

	public static String removeAnchor(URL u) {

		// pre-compute length of StringBuffer
		int len = u.getProtocol().length() + 1;
		if (u.getAuthority() != null && u.getAuthority().length() > 0)
			len += 2 + u.getAuthority().length();
		if (u.getPath() != null) {
			len += u.getPath().length();
		}
		if (u.getQuery() != null) {
			len += 1 + u.getQuery().length();
		}
		if (u.getRef() != null)
			len += 1 + u.getRef().length();

		StringBuffer result = new StringBuffer(len);
		result.append(u.getProtocol());
		result.append(":");
		if (u.getAuthority() != null && u.getAuthority().length() > 0) {
			result.append("//");
			result.append(u.getAuthority());
		}
		if (u.getPath() != null) {
			result.append(u.getPath());
		}
		if (u.getQuery() != null) {
			result.append('?');
			result.append(u.getQuery());
		}

		return result.toString();
	}

	public static void main(String[] args) throws Exception {
		String base = "http://www.baidu.com";
		String rel1 = "a.html";
		System.out.println(getAbsoluteUrl(base, rel1));
		String rel2 = "http://www.sina.com.cn/index.html";
		System.out.println(getAbsoluteUrl(base, rel2));

		String url = "http://t.miyou.cc/sDd6eGdu/恰恰舞曲/[QQ]2017-恰恰、小小情歌[曾春年].mp3";
		url = encodeCn(url, "UTF8");
		System.out.println(url);
	}
}
