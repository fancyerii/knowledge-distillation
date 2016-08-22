package com.antbrains.httpclientfetcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

//ThreadSafe
public class ProxyManager2 extends ProxyManager {
	protected static Logger logger = Logger.getLogger(ProxyManager2.class);
	private volatile ArrayList<Proxy> proxyList;
	private LinkedList<Proxy> candidatesList;

	private AtomicInteger pt = new AtomicInteger();
	private long maxLatency;
	private int maxFailCount;

	private ValidateThread vt;
	private int maxProxyRequried = 0;

	public ProxyManager2(List<HttpHost> proxys, boolean itself, long checkInterval, long maxLatency, int maxFailCount,
			int maxProxyRequried) {
		this.maxFailCount = maxFailCount;
		this.maxLatency = maxLatency;
		this.maxProxyRequried = maxProxyRequried;
		candidatesList = new LinkedList<>();
		this.proxyList = new ArrayList<>(proxys.size() + 1);
		if (itself) {
			Proxy p = new Proxy(null, 0);
			this.proxyList.add(p);
		}

		for (HttpHost hh : proxys) {
			Proxy p = new Proxy(hh, proxys.size());
			if (proxyList.size() < maxProxyRequried) {
				this.proxyList.add(p);
			} else {
				this.candidatesList.add(p);
			}
		}
		vt = new ValidateThread(this, checkInterval, 10000, 24 * 3600 * 1000);
		vt.start();
		logger.info("proxyList: " + proxyList.size());
		logger.info("candidatesList: " + candidatesList.size());
	}

	public void stopValidateThread() {
		vt.stopMe();
		vt.interrupt();

	}

	public void waitValidateThread(long waitTime) {
		vt.waitFinish(waitTime);
	}

	public void updateProxyStatus(Proxy p, long timeUsed, boolean succ) {
		p.totalTime.addAndGet(timeUsed);
		if (succ) {
			p.succ.incrementAndGet();
		} else {
			p.fail.incrementAndGet();
			p.totalFail.incrementAndGet();
		}
	}

	/**
	 * 定期从文件和网络发现新的proxy
	 */
	public synchronized void addProxys() {

	}

	private synchronized void addProxyFromCandidate() {
		if (candidatesList.size() == 0) {
			logger.warn("no proxy available");
			return;
		}
		final ArrayList<Proxy> validList = new ArrayList<>(this.proxyList);
		while (validList.size() < this.maxProxyRequried && !candidatesList.isEmpty()) {
			Proxy p = this.candidatesList.pop();
			validList.add(p);
		}
		if (validList.size() == 0) {
			logger.warn("no proxy");

		}
		this.proxyList = validList;
	}

	public synchronized int getGoodProxySize() {
		return this.proxyList.size();
	}

	public synchronized void checkBadProxys(boolean noProxyCheck) {
		this.addProxyFromCandidate();
	}

	public synchronized void validateProxys() {
		final ArrayList<Proxy> validList = new ArrayList<>();
		for (Proxy proxy : proxyList) {
			int fail = proxy.fail.get();

			if (fail > maxFailCount) {
				logger.warn("too many fail: " + fail + " (ms) move to badList: " + proxy);

			} else {
				// check speed
				long timeUsed = proxy.totalTime.get();
				int succ = proxy.succ.get();
				int total = fail + succ;

				long avgTime = 0;
				if (total > 0) {
					avgTime = timeUsed / (fail + succ);
				}
				if (avgTime > this.maxLatency) {
					logger.warn("too slow: " + avgTime + " (ms/url) move to badList: " + proxy);

				} else {
					validList.add(proxy);
				}
				// clear
				// not thread safe, but we can tolerant this
			}

		}
		// logger.info("good: "+validList.size());
		boolean changed = false;
		while (validList.size() < this.maxProxyRequried && !candidatesList.isEmpty()) {
			Proxy p = this.candidatesList.pop();
			logger.info("add proxy: " + p);
			validList.add(p);
			changed = true;
		}
		if (changed) {
			StringBuilder sb = new StringBuilder("");
			for (Proxy p : validList) {
				sb.append(p).append(",");
			}
			logger.info("proxys: " + sb.toString());
		}
		// switch
		this.proxyList = validList;

	}

	private long avgTime(int succ, int fail, long totalTime) {

		long avgTime = 0;
		int total = succ + fail;
		if (total > 0) {
			avgTime = totalTime / (succ + fail);
		}
		return avgTime;
	}

	public Proxy getProxy() {
		// 那一个引用，这样validate线程切换时不会出问题
		ArrayList<Proxy> ref = this.proxyList;
		int size = ref.size();
		if (size == 0)
			return null;
		int idx = pt.getAndIncrement() % size;

		Proxy p = ref.get(idx);
		if (p != null) {

		} else {
			logger.info("no proxy");
			this.addProxyFromCandidate();
		}
		return p;
	}
}
