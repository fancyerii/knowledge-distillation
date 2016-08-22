package com.antbrains.httpclientfetcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.antbrains.nekohtmlparser.NekoHtmlParser;

public class ProxyDiscover {
	public static void main(String[] args) {

		LinkedHashSet<HttpHost> proxys = ProxyDiscover.findProxys();
		System.out.println(proxys.size());
		for (HttpHost p : proxys) {
			String s = p.toHostString();
			System.out.println(s);
		}

	}

	public static LinkedHashSet<HttpHost> findProxys() {
		// Will not work on windows!!!
		String home = System.getProperty("user.home");
		String proxyFile = home + "/.proxys";
		return findProxys(proxyFile);
	}

	public static LinkedHashSet<HttpHost> findProxys(String dataFile) {
		LinkedHashSet<HttpHost> proxys = new LinkedHashSet<HttpHost>();
		if (dataFile != null) {
			List<String> lines = null;
			try {
				lines = FileTools.readFile2List(dataFile, "UTF8");
			} catch (Exception e) {

			}

			if (lines != null) {
				for (String line : lines) {
					String[] arr = line.split("\\s+");
					if (arr.length < 2)
						continue;
					try {
						HttpHost hh = new HttpHost(arr[0], Integer.valueOf(arr[1]));
						proxys.add(hh);
					} catch (Exception e) {
					}
				}
			}
		}

		HttpClientFetcher fetcher = new HttpClientFetcher(ProxyDiscover.class.getName());
		fetcher.init();
		proxys.addAll(findProxy1(fetcher));

		if (dataFile != null) {
			StringBuilder sb = new StringBuilder();
			for (HttpHost hh : proxys) {
				sb.append(hh.getHostName() + "\t" + hh.getPort() + "\n");
			}

			try {
				FileTools.writeFile(dataFile, sb.toString(), "UTF8");
			} catch (IOException e) {
				System.err.println("can't save to " + dataFile);
			}
		}
		if (fetcher != null) {
			fetcher.close();
		}
		return proxys;
	}

	private static void extractProxy1(NekoHtmlParser parser, List<HttpHost> proxys) {
		NodeList tables = parser.selectNodes("//TABLE");
		Node tb = null;
		for (int i = 0; i < tables.getLength(); i++) {
			NodeList tds = parser.selectNodes("./TBODY/TR[1]//TD", tables.item(i));
			int tdCount = tds.getLength();
			if (tdCount == 5) {
				tb = tables.item(i);
				break;
			}
		}

		if (tb != null) {
			NodeList trs = parser.selectNodes("./TBODY/TR", tb);
			for (int i = 1; i < trs.getLength(); i++) {
				Node tr = trs.item(i);
				String host = parser.getNodeText(".//TD[2]", tr);
				String port = parser.getNodeText(".//TD[3]", tr);
				try {
					int p = Integer.valueOf(port);
					proxys.add(new HttpHost(host, p));
				} catch (Exception e) {

				}
			}
		}
	}

	private static List<HttpHost> findProxy1(HttpClientFetcher fetcher) {
		List<HttpHost> proxys = new ArrayList<>();
		String url = "http://proxy.com.ru/";
		String s = null;
		try {
			s = fetcher.httpGet(url);
		} catch (Exception e) {

		}
		if (s == null)
			return proxys;
		NekoHtmlParser parser = new NekoHtmlParser();
		try {
			parser.load(s, "UTF8");
		} catch (Exception e) {
			return proxys;
		}

		extractProxy1(parser, proxys);
		Pattern ptn = Pattern.compile("共(\\d+)页");
		Matcher m = ptn.matcher(s);
		int total = 1;
		if (m.find()) {
			total = Integer.valueOf(m.group(1));
		}

		for (int i = 2; i <= total; i++) {
			url = "http://proxy.com.ru/list_" + i + ".html";
			try {
				s = fetcher.httpGet(url);
				if (s == null)
					continue;
				parser.load(s, "UTF8");
				extractProxy1(parser, proxys);
			} catch (Exception e) {

			}
		}

		return proxys;
	}
}
