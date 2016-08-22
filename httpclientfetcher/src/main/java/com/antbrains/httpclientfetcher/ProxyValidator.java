package com.antbrains.httpclientfetcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

public class ProxyValidator implements Closeable {
	HttpClientFetcher fetcher;
	private String validateUrl;
	private long maxLatency;
	private String validateContent;
	MyHttpRoutePlanner2 rp;
	Integer validFileSize;

	public ProxyValidator(String validateUrl, long maxLatency, String validateContent, Integer validFileSize) {
		fetcher = new HttpClientFetcher(this.getClass().getName());
		rp = new MyHttpRoutePlanner2();
		fetcher.setRoutePlanner(rp);
		fetcher.setConnectTimeout((int) maxLatency);
		fetcher.setReadTimeout((int) maxLatency);
		fetcher.init();
		this.validateUrl = validateUrl;
		this.maxLatency = maxLatency;
		this.validateContent = validateContent;
		this.validFileSize = validFileSize;
	}

	private boolean validByStringContent() {
		try {
			long start = System.currentTimeMillis();
			String s = fetcher.httpGet(validateUrl);
			if (System.currentTimeMillis() - start > maxLatency) {
				return false;
			}
			if (s != null && s.contains(validateContent)) {
				return true;
			}
		} catch (Exception e) {

		}
		return false;
	}

	private boolean validBySize() {
		HttpResponse response = null;
		HttpTime ht = null;
		BufferedInputStream bis = null;
		HttpContext ctx = null;
		boolean succ = false;
		try {
			long start = System.currentTimeMillis();
			Object[] arr = fetcher.httpGetStream(validateUrl);
			response = (HttpResponse) arr[0];
			ht = (HttpTime) arr[1];
			ctx = (HttpContext) arr[2];
			InputStream is = response.getEntity().getContent();
			bis = new BufferedInputStream(is);
			byte[] buffer = new byte[10240];
			int read = 0;
			int totalBytes = 0;
			while ((read = bis.read(buffer)) > 0) {

				totalBytes += read;
			}
			if (totalBytes == this.validFileSize.intValue()) {
				succ = true;
			} else {
				succ = false;
			}
			if (System.currentTimeMillis() - start > maxLatency) {
				return false;
			}

			return true;
		} catch (Exception e) {

		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
				}
			}

			fetcher.updateStatus(ctx, ht, succ);
		}
		return false;
	}

	public boolean isGood(HttpHost proxy) {
		try {
			rp.setProxy(proxy);
			if (this.validFileSize == null) {
				return this.validByStringContent();
			} else {
				return this.validBySize();
			}
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
	}

	public static void main(String[] args) {

	}

	@Override
	public void close() {
		this.fetcher.close();
	}

}
