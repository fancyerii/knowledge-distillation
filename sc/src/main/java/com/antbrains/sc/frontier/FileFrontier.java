package com.antbrains.sc.frontier;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.antbrains.sc.data.CrawlTask;
import com.antbrains.sc.data.WebPage;
import com.google.gson.Gson;

public class FileFrontier implements Frontier {
	protected static Logger logger = Logger.getLogger(FileFrontier.class);
	private WriteThread wt;
	private BlockingQueue<WebPage> queue = new LinkedBlockingQueue<WebPage>();

	public FileFrontier(String dir, int recordSize, long maxIdle) {
		wt = new WriteThread(queue, dir, recordSize, maxIdle);
		wt.start();
	}

	@Override
	public void addWebPage(WebPage webPage, int failCountInc) {
		webPage.setFailCount(webPage.getFailCount() + failCountInc);
		queue.add(webPage);
	}

	@Override
	public void close() {
		wt.stopMe();
		try {
			wt.join();
		} catch (InterruptedException e) {

		}
	}

}

class WriteThread extends Thread {
	protected static Logger logger = Logger.getLogger(WriteThread.class);
	BlockingQueue<WebPage> queue;
	long maxIdle;
	private long lastWrite;

	public WriteThread(BlockingQueue<WebPage> queue, String dir, int recordSize, long maxIdle) {
		this.dir = dir;
		this.recordSize = recordSize;
		this.queue = queue;
		this.maxIdle = maxIdle;
		this.lastWrite = System.currentTimeMillis();
		File d = new File(dir);
		if (!d.exists()) {
			d.mkdirs();
		} else {
			if (d.isDirectory()) {

			} else {
				logger.error(dir + " exist but not a directory");
				throw new IllegalArgumentException(dir + " exist but not a directory");
			}
		}

		this.createNewFile();
	}

	private volatile boolean bStop = false;

	public void stopMe() {
		bStop = true;
	}

	private void doWork(WebPage wp) {
		try {
			CrawlTask ct = new CrawlTask();
			ct.url = wp.getUrl();
			ct.failCount = wp.getFailCount();
			ct.priority = wp.getCrawlPriority();
			ct.depth = wp.getDepth();
			if (wp.getId() != null) {
				ct.id = wp.getId();
			}
			ct.redirectedUrl = wp.getRedirectedUrl();
			Date lastVisit = wp.getLastVisitTime();
			if (lastVisit != null) {
				ct.lastVisit = lastVisit.getTime();
			} else {
				ct.lastVisit = -1;
			}
			Date lastFinish = wp.getLastFinishTime();
			if(lastFinish != null){
				ct.lastFinish = lastFinish.getTime();
			}else{
				ct.lastFinish=-1;
			}
			bw.write(gson.toJson(ct) + "\n");
			bw.flush();
			this.lastWrite = System.currentTimeMillis();
			this.currSize++;
			if (currSize >= this.recordSize) {
				this.closeAndRenameOldFile();
				this.createNewFile();
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void run() {
		while (!bStop) {
			if (System.currentTimeMillis() - this.lastWrite > this.maxIdle) {
				// logger.info("exceed maxIdle, so creatNew");
				this.closeAndRenameOldFile();
				this.createNewFile();
			}
			WebPage wp = null;
			try {
				wp = queue.poll(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {

			}
			if (wp == null)
				continue;
			this.doWork(wp);
		}
		logger.info("prepare to stop");
		while (true) {
			WebPage wp = null;
			try {
				wp = queue.poll(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {

			}
			if (wp == null)
				break;
			this.doWork(wp);
		}

		this.closeAndRenameOldFile();

		logger.info("I am stopped");
	}

	private int recordSize;
	private int currSize;

	private String dir;
	private String currFn;
	private BufferedWriter bw;
	private Gson gson = new Gson();

	private void closeAndRenameOldFile() {
		if (bw != null) {
			try {
				bw.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
			bw = null;
		}
		File oldFile = new File(this.dir + "/" + currFn);
		File newFile = new File(this.dir + "/" + currFn.substring(0, currFn.length() - 4));

		oldFile.renameTo(newFile);

	}

	private void createNewFile() {
		currSize = 0;
		this.lastWrite = System.currentTimeMillis();
		currFn = System.currentTimeMillis() + ".txt.bak";
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.dir + "/" + currFn), "UTF8"));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
}
