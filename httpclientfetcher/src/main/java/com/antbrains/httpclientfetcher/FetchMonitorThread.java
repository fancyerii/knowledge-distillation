package com.antbrains.httpclientfetcher;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;

public class FetchMonitorThread extends Thread {
	protected static Logger logger = Logger.getLogger(FetchMonitorThread.class);
	private long timeOut;
	private long sweepInterval;

	private ConcurrentLinkedQueue<HttpTime> requestQueue;

	public FetchMonitorThread(long timeOut, long sweepInterval, ConcurrentLinkedQueue<HttpTime> requestQueue,
			String creator) {
		this.timeOut = timeOut;
		this.sweepInterval = sweepInterval;
		this.requestQueue = requestQueue;
		super.setName(creator);
	}

	private volatile boolean bStop = false;

	public void stopMe() {
		bStop = true;
	}

	@Override
	public void run() {
		while (!bStop) {
			try {
				Thread.sleep(sweepInterval);
				long curr = System.currentTimeMillis();
				while (true) {
					HttpTime ht = requestQueue.peek();
					if (ht == null)
						break;
					if (ht.finished) {
						requestQueue.poll();
						continue;
					}
					if (curr - ht.startTime > timeOut) {
						String url = ht.get.getURI().toString();
						logger.warn("sweeped 'cause timeOut: " + url + " start: " + ht.startTime + " curr: " + curr);
						HttpGet get = ht.get;
						if (get != null) {
							get.abort();
						}
						requestQueue.poll();
					} else {
						break;
					}
				}
			} catch (InterruptedException e) {
			}

		}

		logger.info("I am stopped");
	}
}
