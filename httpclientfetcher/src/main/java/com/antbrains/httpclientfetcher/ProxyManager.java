package com.antbrains.httpclientfetcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
public class ProxyManager {
	protected static Logger logger = Logger.getLogger(ProxyManager.class);
	private volatile ArrayList<Proxy> proxyList;
	private ArrayList<Proxy> badList;
	private ProxyValidator pv;
	private AtomicInteger pt = new AtomicInteger();
	private long maxLatency;
	private int maxFailCount;
	private int maxTotalFailCount;
	private double maxFailRatio;
	private long checkInterval;
	private ValidateThread vt;
	private int minThreshold;
	private int expectedProxy;
	private int checkThreadsCount;

	private String validUrl;
	private String validContent;
	private Integer validFileSize;
	private boolean doInitCheck;

	public ProxyManager(List<HttpHost> proxys, String validUrl, String validContent, long maxLatency, int maxFailCount,
			long checkInterval, int checkBadTimes, boolean discardBadHost, int initCheckThreads, int minThreshold,
			long checkNewInterval, int expectedProxy, int maxTotalFailCount) {
		this(proxys, validUrl, validContent, maxLatency, maxFailCount, checkInterval, checkBadTimes, discardBadHost,
				initCheckThreads, minThreshold, checkNewInterval, expectedProxy, true, null, true, maxTotalFailCount,
				0.5);
	}

	public ProxyManager() {

	}

	public ProxyManager(List<HttpHost> proxys, String validUrl, String validContent, long maxLatency, int maxFailCount,
			long checkInterval, int checkBadTimes, boolean discardBadHost, int initCheckThreads, int minThreshold,
			long checkNewInterval, int expectedProxy, boolean addItself, Integer validFileSize, boolean doInitCheck,
			int maxTotalFailCount, double maxFailRatio) {
		pv = new ProxyValidator(validUrl, maxLatency, validContent, validFileSize);
		this.maxTotalFailCount = maxTotalFailCount;
		this.maxLatency = maxLatency;
		this.maxFailCount = maxFailCount;
		this.checkInterval = checkInterval;
		this.minThreshold = minThreshold;
		this.checkThreadsCount = initCheckThreads;
		this.validContent = validContent;
		this.validUrl = validUrl;
		this.expectedProxy = expectedProxy;
		this.validFileSize = validFileSize;
		this.doInitCheck = doInitCheck;
		this.maxFailRatio = maxFailRatio;
		proxyList = new ArrayList<>();
		badList = new ArrayList<>();
		if (addItself) {
			Proxy p = new Proxy(null, 0);
			proxyList.add(p);
		}
		logger.info("starting validate proxys...");
		logger.info("total to validate: " + proxys.size());
		long start = System.currentTimeMillis();
		InitValidateThread[] checkThreads = new InitValidateThread[initCheckThreads];

		final BlockingQueue<Proxy> queues = new LinkedBlockingQueue<>(10_000);
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i] = new InitValidateThread(proxyList, queues, validUrl, maxLatency, validContent,
					discardBadHost, badList, Integer.MAX_VALUE, validFileSize, doInitCheck);
			checkThreads[i].start();
		}

		for (HttpHost host : proxys) {
			Proxy proxy = new Proxy(host, proxyList.size());
			try {
				queues.put(proxy);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}

		while (queues.size() > 0) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {

			}
		}
		logger.info("finishedPut");
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i].stopMe();
		}
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i].waitFinish();
		}
		logger.info("stopAllInitThread");
		logger.info("initValidateTime: " + (System.currentTimeMillis() - start) + " ms");
		logger.info("good proxys: " + proxyList.size());
		logger.info("bad proxys: " + badList.size());
		vt = new ValidateThread(this, this.checkInterval, checkBadTimes, checkNewInterval);
		vt.start();
	}

	public void stopValidateThread() {
		vt.stopMe();
		vt.interrupt();
		pv.close();
	}

	public void waitValidateThread(long waitTime) {
		vt.waitFinish(waitTime);
	}

	public void updateProxyStatus(Proxy p, long timeUsed, boolean succ) {
		p.totalTime.addAndGet(timeUsed);
		if (succ) {
			p.succ.incrementAndGet();
			p.totalSucc.incrementAndGet();
		} else {
			p.fail.incrementAndGet();
			p.totalFail.incrementAndGet();
		}
	}

	/**
	 * 定期从文件和网络发现新的proxy
	 */
	public synchronized void addProxys() {
		Set<HttpHost> newProxys = ProxyDiscover.findProxys();
		logger.info("starting find new proxys...");
		Set<Proxy> proxys = new HashSet<>();
		int total = proxyList.size() + badList.size();
		for (HttpHost pp : newProxys) {
			if (!this.exist(pp)) {
				proxys.add(new Proxy(pp, total));
				total++;
			}
		}
		logger.info("total to validate: " + proxys.size());
		long start = System.currentTimeMillis();
		InitValidateThread[] checkThreads = new InitValidateThread[this.checkThreadsCount];

		final BlockingQueue<Proxy> queues = new LinkedBlockingQueue<>(10_000);
		ArrayList<Proxy> newGoodList = new ArrayList<>();
		ArrayList<Proxy> newBadList = new ArrayList<>();
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i] = new InitValidateThread(newGoodList, queues, validUrl, maxLatency, validContent, true,
					newBadList, Integer.MAX_VALUE, validFileSize, this.doInitCheck);
			checkThreads[i].start();
		}

		for (Proxy host : proxys) {
			try {
				queues.put(host);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}

		while (queues.size() > 0) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {

			}
		}
		logger.info("finishedPut");
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i].stopMe();
		}
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i].waitFinish();
		}
		logger.info("stopAllInitThread");
		logger.info("initValidateTime: " + (System.currentTimeMillis() - start) + " ms");
		logger.info("good proxys: " + newGoodList.size());
		logger.info("bad proxys: " + newBadList.size());
		this.proxyList.addAll(newGoodList);
	}

	private boolean exist(HttpHost hh) {
		for (Proxy p : this.proxyList) {
			if (hh.equals(p.host))
				return true;
		}
		for (Proxy p : this.badList) {
			if (hh.equals(p.host))
				return true;
		}

		return false;
	}

	public synchronized int getGoodProxySize() {
		return this.proxyList.size();
	}

	public synchronized void validateProxys() {
		final ArrayList<Proxy> validList = new ArrayList<>();
		for (Proxy proxy : proxyList) {
			int fail = proxy.fail.get();
			int succ = proxy.succ.get();
			int totalFail = proxy.totalFail.get();
			int totalSucc = proxy.totalSucc.get();
			long totalTime = proxy.totalTime.get();
			long avgTime = this.avgTime(succ, fail, totalTime);
			proxy.avgTime = avgTime;
			if (avgTime > this.maxLatency) {
				logger.warn("too slow avg: " + avgTime + " (ms) move to badList: " + proxy);
				proxy.badTimes++;
				badList.add(proxy);
			} else {
				if (totalFail > maxTotalFailCount) {
					if (1.0 * totalFail / (totalFail + totalSucc) > this.maxFailRatio) {
						logger.warn("too many total fail: " + fail + " (ms) move to badList: " + proxy);
						proxy.badTimes++;
						badList.add(proxy);
					} else {
						logger.info("totalFail: " + totalFail + "\ttotalSucc: " + totalSucc);
						proxy.totalFail.set(0);
						proxy.totalSucc.set(0);
					}
				} else {
					if (fail > maxFailCount) {
						logger.warn("too many fail: " + fail + " (ms) move to badList: " + proxy);
						proxy.badTimes++;
						badList.add(proxy);
					} else {
						validList.add(proxy);
						// clear
						// not thread safe, but we can tolerant this
						proxy.fail.set(0);
						proxy.succ.set(0);
						proxy.totalTime.set(0);
					}
				}
			}
		}
		logger.info("good: " + validList.size());
		// switch
		this.proxyList = validList;

		if (validList.size() < this.minThreshold) {
			this.addBack(expectedProxy - validList.size());
		}
	}

	private long avgTime(int succ, int fail, long totalTime) {

		long avgTime = 0;
		int total = succ + fail;
		if (total > 0) {
			avgTime = totalTime / (succ + fail);
		}
		return avgTime;
	}

	private void addBack(int count) {
		logger.info("try to addBack: " + count);

		Collections.sort(badList, new Comparator<Proxy>() {
			@Override
			public int compare(Proxy o1, Proxy o2) {
				// 代理总共用过的次数越少越好
				if (o1 == null)
					return -1;
				if (o2 == null)
					return 1;
				int tryTimes1 = o1.badTimes;
				int tryTimes2 = o2.badTimes;
				int score1 = -4 * tryTimes1 - (int) (o1.avgTime / 1000);
				int score2 = -4 * tryTimes2 - (int) (o2.avgTime / 1000);

				return score1 - score2;
			}

		});

		final BlockingQueue<Proxy> queues = new LinkedBlockingQueue<>(10_000);
		InitValidateThread[] checkThreads = new InitValidateThread[this.checkThreadsCount];
		final ArrayList<Proxy> backList = new ArrayList<>();
		final ArrayList<Proxy> stillBadList = new ArrayList<>();
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i] = new InitValidateThread(backList, queues, validUrl, maxLatency, validContent, false,
					stillBadList, count, validFileSize, this.doInitCheck);
			checkThreads[i].start();
		}

		for (Proxy proxy : badList) {
			try {
				queues.put(proxy);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.info("wait...");
		while (queues.size() > 0) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {

			}
		}
		logger.info("finishedPut");
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i].stopMe();
		}
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i].waitFinish();
		}

		logger.info("finish moveback");

		badList = stillBadList;
		backList.addAll(proxyList);
		// switch
		this.proxyList = backList;

	}

	public synchronized void checkBadProxys(boolean noProxyCheck) {
		if (noProxyCheck && proxyList.size() > 0)
			return; // 如果是因为没有代理而进行检查，那么如果有了代理（别的线程），直接退出

		logger.info("checkBadProxys: " + badList.size());
		long start = System.currentTimeMillis();
		InitValidateThread[] checkThreads = new InitValidateThread[this.checkThreadsCount];

		final BlockingQueue<Proxy> queues = new LinkedBlockingQueue<>(10_000);
		ArrayList<Proxy> newGoodList = new ArrayList<>();
		ArrayList<Proxy> newBadList = new ArrayList<>();
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i] = new InitValidateThread(newGoodList, queues, validUrl, maxLatency, validContent, false,
					newBadList, Integer.MAX_VALUE, validFileSize, this.doInitCheck);
			checkThreads[i].start();
		}

		for (Proxy host : badList) {
			try {
				queues.put(host);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}

		while (queues.size() > 0) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {

			}
		}
		logger.info("checkBadProxys finishedPut");
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i].stopMe();
		}
		for (int i = 0; i < checkThreads.length; i++) {
			checkThreads[i].waitFinish();
		}
		logger.info("checkBadProxys stopAllInitThread");
		logger.info("checkBadProxys initValidateTime: " + (System.currentTimeMillis() - start) + " ms");
		logger.info("checkBadProxys good proxys: " + newGoodList.size());
		logger.info("checkBadProxys bad proxys: " + newBadList.size());

		newGoodList.addAll(this.proxyList);
		this.badList = newBadList;
		this.proxyList = newGoodList;

	}

	public Proxy getProxy() {
		// 那一个引用，这样validate线程切换时不会出问题
		ArrayList<Proxy> ref = this.proxyList;
		int size = ref.size();
		if (size == 0)
			return null;
		int idx = pt.getAndIncrement() % size;

		return ref.get(idx);

	}
}

class InitValidateThread extends Thread {
	protected static Logger logger = Logger.getLogger(InitValidateThread.class);
	ArrayList<Proxy> proxyList;
	BlockingQueue<Proxy> queues;
	private volatile boolean bStop = false;
	ProxyValidator pv;
	private boolean discardBadHost;
	private ArrayList<Proxy> badList;
	private int maxGood;
	private boolean doCheck;

	public InitValidateThread(ArrayList<Proxy> proxyList, BlockingQueue<Proxy> queues, String validateUrl,
			long maxLatency, String validateContent, boolean discardBadHost, ArrayList<Proxy> badList, int maxGood,
			Integer validFileSize, boolean doCheck) {
		this.proxyList = proxyList;
		this.queues = queues;
		this.discardBadHost = discardBadHost;
		this.badList = badList;
		this.maxGood = maxGood;
		this.doCheck = doCheck;
		pv = new ProxyValidator(validateUrl, maxLatency, validateContent, validFileSize);
	}

	public void stopMe() {
		bStop = true;
	}

	private CountDownLatch latch = new CountDownLatch(1);

	public void waitFinish() {
		try {
			latch.await();
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void run() {
		while (!bStop) {
			try {
				Proxy p = queues.poll(5, TimeUnit.SECONDS);
				if (p == null)
					continue;
				if (proxyList.size() >= maxGood) {
					badList.add(p);
					continue;
				}
				if (this.doCheck) {
					if (pv.isGood(p.host)) {
						synchronized (proxyList) {
							if (proxyList.size() < maxGood) {
								proxyList.add(p);
							} else {
								badList.add(p);
							}
						}
					} else {
						if (!discardBadHost) {
							synchronized (badList) {
								badList.add(p);
							}
						} else {
							logger.warn("discard proxy: " + p);
						}
					}
				} else {
					proxyList.add(p);
				}
			} catch (InterruptedException e) {

			}
		}
		pv.close();
		latch.countDown();
	}
}

class ValidateThread extends Thread {
	private ProxyManager pm;
	private int checkBadTimes;
	private long interval;
	private long checkNewInterval;
	private long lastCheckNew;

	public ValidateThread(ProxyManager pm, long interval, int checkBadTimes, long checkNewInterval) {
		this.pm = pm;
		this.interval = interval;
		this.checkBadTimes = checkBadTimes;
		lastCheckNew = System.currentTimeMillis();
		this.checkNewInterval = checkNewInterval;
	}

	private volatile boolean bStop = false;

	public void stopMe() {
		bStop = true;
	}

	private CountDownLatch latch = new CountDownLatch(1);

	public void waitFinish(long wait) {
		try {
			if (wait <= 0) {
				latch.await();
			} else {
				latch.await(wait, TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void run() {
		int count = 0;
		while (!bStop) {
			count++;
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {

			}
			pm.validateProxys();
			if (count % checkBadTimes == 0) {
				pm.checkBadProxys(false);
			}
			if (System.currentTimeMillis() - this.lastCheckNew > this.checkNewInterval) {
				pm.addProxys();
				this.lastCheckNew = System.currentTimeMillis();
			}
		}
		latch.countDown();
	}
}
