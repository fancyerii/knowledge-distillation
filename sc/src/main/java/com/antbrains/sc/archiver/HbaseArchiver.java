package com.antbrains.sc.archiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.log4j.Logger;

import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.tools.BlockInfo;
import com.antbrains.sc.tools.SCHbaseTool;

public class HbaseArchiver implements Archiver {
	protected static Logger logger = Logger.getLogger(HbaseArchiver.class);
	private String dbName;
	private HConnection conn;
	private UpdateWebPageThread[] wus;
	private SaveFailedThread[] sfs;
	private SaveBlockThread[] sbs;
	private SaveBadThread sbt;

	public HbaseArchiver(String dbName, int updateQueueSize, int failedQueueSize, int blockQueueSize, String zk,
			int updateWebThread, int saveFailThread, int saveBlockThread, int updateCacheSize, long flushInterval)
			throws IOException {
		this.dbName = dbName;
		this.updateQueue = new LinkedBlockingQueue<WebPage>(updateQueueSize);
		this.failedQueue = new LinkedBlockingQueue<WebPage>(failedQueueSize);
		this.blockQueue = new LinkedBlockingQueue<BlockInfo>(blockQueueSize);
		this.badUrlQueue = new LinkedBlockingQueue<>(failedQueueSize);
		Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum", zk);

		conn = HConnectionManager.createConnection(conf);
		wus = new UpdateWebPageThread[updateWebThread];
		for (int i = 0; i < wus.length; i++) {
			wus[i] = new UpdateWebPageThread(conn, updateQueue, updateCacheSize, flushInterval, dbName);
			wus[i].start();
		}
		sfs = new SaveFailedThread[saveFailThread];
		for (int i = 0; i < sfs.length; i++) {
			sfs[i] = new SaveFailedThread(conn, failedQueue, updateCacheSize, flushInterval, dbName);
			sfs[i].start();
		}
		sbs = new SaveBlockThread[saveBlockThread];
		for (int i = 0; i < sbs.length; i++) {
			sbs[i] = new SaveBlockThread(conn, blockQueue, updateCacheSize, flushInterval, dbName);
			sbs[i].start();
		}
		sbt = new SaveBadThread(conn, badUrlQueue, updateCacheSize, flushInterval, dbName);
		sbt.start();
	}

	private BlockingQueue<WebPage> updateQueue;
	private BlockingQueue<WebPage> failedQueue;
	private BlockingQueue<BlockInfo> blockQueue;
	private BlockingQueue<String> badUrlQueue;

	@Override
	public boolean insert2WebPage(WebPage webPage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveUnFinishedWebPage(WebPage webPage, int failInc) {
		webPage.setFailCount(webPage.getFailCount() + failInc);
		try {
			failedQueue.put(webPage);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void updateWebPage(WebPage webPage) {
		try {
			updateQueue.put(webPage);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public Date getLastVisit(String url) {
		try {
			return SCHbaseTool.getLastVisit(conn, dbName, url);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void loadBlocks(WebPage webPage) {
		try {
			SCHbaseTool.loadBlock(conn, dbName, webPage);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void insert2Block(Block block, WebPage webPage, int pos) {
		try {
			blockQueue.put(new BlockInfo(webPage.getUrl(), block, pos));
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void updateBlock(Block block, WebPage webPage, int pos, boolean updateInfo) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void insert2Link(List<Integer> blockIds, List<Integer> parentIds, List<Integer> childIds,
			List<Integer> poses, List<String> linkTexts, List<Map<String, String>> attrsList) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void insert2Attr(WebPage webPage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveLog(String taskId, String type, String msg, Date logTime, int level, String html) {

	}

	@Override
	public void close() {
		for (int i = 0; i < this.wus.length; i++) {
			wus[i].stopMe();
		}
		for (int i = 0; i < this.sbs.length; i++) {
			sbs[i].stopMe();
		}
		for (int i = 0; i < this.sfs.length; i++) {
			sfs[i].stopMe();
		}
		sbt.stopMe();

		for (int i = 0; i < this.wus.length; i++) {
			try {
				wus[i].join();
			} catch (InterruptedException e) {

			}
		}
		for (int i = 0; i < this.sbs.length; i++) {
			try {
				sbs[i].join();
			} catch (InterruptedException e) {

			}
		}
		for (int i = 0; i < this.sfs.length; i++) {
			try {
				sfs[i].join();
			} catch (InterruptedException e) {

			}
		}

		try {
			sbt.join();
		} catch (InterruptedException e) {

		}

		if (conn != null) {
			try {
				conn.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void process404Url(String url) {
		try {
			this.badUrlQueue.put(url);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void updateFinishTime(WebPage webPage, Date date) {
		webPage.setLastFinishTime(date);
		this.updateWebPage(webPage);
	}
}

class SaveBlockThread extends AbstractWorkerThread<BlockInfo> {
	protected static Logger logger = Logger.getLogger(SaveBlockThread.class);
	private String dbName;
	private HConnection conn;

	public SaveBlockThread(HConnection conn, BlockingQueue<BlockInfo> queue, int updateCacheSize, long flushInterval,
			String dbName) {
		super(queue, updateCacheSize, flushInterval);
		this.dbName = dbName;
		this.conn = conn;
	}

	@Override
	protected void doRealWork() {
		try {
			SCHbaseTool.addBlocks(conn, dbName, cache);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	protected void close() {

	}

}

class SaveBadThread extends AbstractWorkerThread<String> {
	protected static Logger logger = Logger.getLogger(SaveBadThread.class);
	private String dbName;
	private HConnection conn;

	public SaveBadThread(HConnection conn, BlockingQueue<String> queue, int updateCacheSize, long flushInterval,
			String dbName) {
		super(queue, updateCacheSize, flushInterval);
		this.dbName = dbName;
		this.conn = conn;
	}

	@Override
	protected void doRealWork() {
		try {
			SCHbaseTool.updateFailUrls(conn, dbName, cache);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	protected void close() {

	}
}

class SaveFailedThread extends AbstractWorkerThread<WebPage> {
	protected static Logger logger = Logger.getLogger(AbstractWorkerThread.class);
	private String dbName;
	private HConnection conn;

	public SaveFailedThread(HConnection conn, BlockingQueue<WebPage> queue, int updateCacheSize, long flushInterval,
			String dbName) {
		super(queue, updateCacheSize, flushInterval);
		this.dbName = dbName;
		this.conn = conn;
	}

	@Override
	protected void doRealWork() {
		try {
			SCHbaseTool.saveFailed(conn, dbName, cache);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	protected void close() {

	}

}

class UpdateWebPageThread extends AbstractWorkerThread<WebPage> {
	protected static Logger logger = Logger.getLogger(UpdateWebPageThread.class);
	private String dbName;
	private HConnection conn;

	public UpdateWebPageThread(HConnection conn, BlockingQueue<WebPage> queue, int updateCacheSize, long flushInterval,
			String dbName) throws IOException {
		super(queue, updateCacheSize, flushInterval);
		this.dbName = dbName;
		this.conn = conn;
	}

	@Override
	protected void doRealWork() {
		try {
			SCHbaseTool.updateWebPages(conn, dbName, cache);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);

		}
	}

	@Override
	protected void close() {
	}

}
