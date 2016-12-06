package com.antbrains.urlcrawler.crawler;
 	
import java.io.IOException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.log4j.Logger;

import com.antbrains.urlcrawler.db.CrawlTask;
import com.antbrains.urlcrawler.db.HbaseTool; 

public class Writer extends Thread {
	protected static Logger logger = Logger.getLogger(Writer.class);
	BlockingQueue<CrawlTask> resQueue;
	Connection hbaseConn;
	String dbName;
	public Writer(String dbName, BlockingQueue<CrawlTask> resQueue, String zkQuorum, String zkPort) throws Exception {
		this.resQueue = resQueue;
		this.dbName=dbName;
		Configuration myConf = HBaseConfiguration.create();
		myConf.set("hbase.zookeeper.quorum", zkQuorum);
		if (zkPort != null) {
			myConf.set("hbase.zookeeper.property.clientPort", zkPort);
		}
		hbaseConn =ConnectionFactory.createConnection(myConf);
	}

	private volatile boolean bStop;

	public void stopMe() {
		logger.info("receive stop signal");
		bStop = true;
	}

	int batchSize = 100;
	ArrayList<CrawlTask> cache = new ArrayList<>(batchSize);
	long updateInterval = 60_000L;
	long lastUpdate;

	@Override
	public void run() {
		lastUpdate = System.currentTimeMillis();
		while (!bStop) {
			try {
				CrawlTask task = resQueue.poll(3, TimeUnit.SECONDS);
				if (task == null) {
					if (System.currentTimeMillis() - lastUpdate > this.updateInterval) {
						this.flushCache();
					}
				} else {
					this.cache.add(task);
					if (cache.size() >= this.batchSize) {
						this.flushCache();
					}
				}
			} catch (InterruptedException e) {
			}
		}
		this.flushCache();
		//DBUtils.closeAll(phoenixConn, null, null);
		try {
			hbaseConn.close();
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
		}
		logger.info("stopped");
	}
	
	
	private void flushCache() {
		// save to hbase
		try {
			HbaseTool.updateWebPage(dbName,hbaseConn, cache);
			ArrayList<String> succ=new ArrayList<>();
			ArrayList<CrawlTask> fail=new ArrayList<>();
			ArrayList<String> all=new ArrayList<>();
			for(CrawlTask task:cache){
				all.add(task.url);
				if(task.status==CrawlTask.STATUS_FAILED){
					fail.add(task);
				}else if(task.status==CrawlTask.STATUS_SUCC){
					succ.add(task.url);
				}else{
					logger.warn("algo bug: "+task);
				}
			}
			HbaseTool.addRows(dbName, HbaseTool.TB_URLDB_SUCC, hbaseConn, succ);
			HbaseTool.addFailed(dbName, hbaseConn, fail);
			HbaseTool.delRows(dbName, HbaseTool.TB_URLDB_CRAWLING, hbaseConn, all);
			//PhoenixTool.upsertTasks(phoenixConn, cache);
		} catch (Exception e) {
			//logger.error(e.getMessage(), e);
			logger.error(e.getMessage());
		}
		// update mysql status

		cache.clear();
		lastUpdate = System.currentTimeMillis();
	}
}
