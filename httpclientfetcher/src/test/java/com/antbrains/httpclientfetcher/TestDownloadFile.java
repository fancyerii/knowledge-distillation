package com.antbrains.httpclientfetcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.httpclientfetcher.HttpTime;
import com.antbrains.httpclientfetcher.MyHttpRoutePlanner;
import com.antbrains.httpclientfetcher.ProxyManager;

public class TestDownloadFile {

	public static void main(String[] args) {
		List<HttpHost> proxys = new ArrayList<>();

		HttpHost hh = new HttpHost("111.161.126.101", 80);
		proxys.add(hh);
		ProxyManager pm = new ProxyManager(proxys,
				"http://file.qianqian.com/data2/music/41580711/41580711.mp3?xcode=187acb1013ec9036f984af40205610a3782d5985a473e045",
				"", 300_000, 2, 120_000, 24 * 3600 * 1000, true, 1, 1, 6 * 3600 * 1000, 1, false, 4054727, false, 10000,
				0.5);
		if (pm.getGoodProxySize() == 0) {
			System.out.println("bad proxy");

			return;
		}
		HttpClientFetcher hc = new HttpClientFetcher("Test", null);
		MyHttpRoutePlanner routePlanner = new MyHttpRoutePlanner(pm);
		hc.setConnectTimeout(30_000);
		hc.setReadTimeout(60_000);
		hc.setRoutePlanner(routePlanner);
		hc.setDoSweep(true);
		hc.setMonitorTimeout(900_000);
		hc.init();
		InputStream is = null;
		BufferedInputStream bis = null;
		OutputStream output = null;
		HttpResponse response = null;
		HttpTime ht = null;
		boolean succ = false;
		HttpContext ctx = null;
		try {
			Object[] arr = hc.httpGetStream(
					"http://file.qianqian.com/data2/music/41580711/41580711.mp3?xcode=187acb1013ec9036f984af40205610a3782d5985a473e045");
			response = (HttpResponse) arr[0];
			ht = (HttpTime) arr[1];
			ctx = (HttpContext) arr[2];
			is = response.getEntity().getContent();
			bis = new BufferedInputStream(is);
			output = new BufferedOutputStream(new FileOutputStream("/Users/lili/test.mp3"));
			byte[] buffer = new byte[1024];
			int read = 0;
			int total = 0;
			while ((read = bis.read(buffer)) > 0) {
				output.write(buffer, 0, read);
				total += read;
			}
			System.out.println("total: " + total);
			if (total == 4054727) {
				succ = true;
			} else {
				succ = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			hc.updateStatus(ctx, ht, succ);
		}
		System.out.println("succ: " + succ);
		hc.close();
		pm.stopValidateThread();
	}

}
