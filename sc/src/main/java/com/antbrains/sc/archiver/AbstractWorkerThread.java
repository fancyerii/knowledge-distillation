package com.antbrains.sc.archiver;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public abstract class AbstractWorkerThread<T> extends Thread {
	protected static Logger logger = Logger.getLogger(UpdateWebPageThread.class);
	protected BlockingQueue<T> queue;
	protected ArrayList<T> cache;
	protected int updateCacheSize;
	protected long lastFlush = 0;
	protected long flushInterval;

	public AbstractWorkerThread(BlockingQueue<T> queue, int updateCacheSize, long flushInterval) {
		this.queue = queue;
		this.cache = new ArrayList<>(updateCacheSize);
		this.updateCacheSize = updateCacheSize;
		this.flushInterval = flushInterval;
	}

	private volatile boolean bStop = false;

	public void stopMe() {
		bStop = true;
	}

	@Override
	public void run() {
		lastFlush = System.currentTimeMillis();
		while (!bStop) {
			if (System.currentTimeMillis() - lastFlush > this.flushInterval) {
				this.flushCache();
			}
			try {
				T task = queue.poll(5, TimeUnit.SECONDS);
				if (task == null)
					continue;
				doWork(task);
			} catch (InterruptedException e) {

			}
		}
		logger.info("prepare to stop");
		while (true) {
			try {
				T task = queue.poll(5, TimeUnit.SECONDS);
				if (task == null)
					break;
				doWork(task);
			} catch (InterruptedException e) {

			}
		}
		if (cache.size() > 0) {
			this.doRealWork();
		}
		this.close();
		logger.info("I am stopped");
	}

	private void flushCache() {
		if (cache.size() > 0) {
			doRealWork();
			this.cache.clear();
		}
		this.lastFlush = System.currentTimeMillis();
	}

	protected abstract void doRealWork();

	protected abstract void close();

	private void doWork(T task) {
		this.cache.add(task);
		if (cache.size() >= updateCacheSize) {
			flushCache();
		}
	}
}
