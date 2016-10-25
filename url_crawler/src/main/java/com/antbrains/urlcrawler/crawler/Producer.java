package com.antbrains.urlcrawler.crawler;

 
import java.io.IOException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import javax.jms.Session;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.log4j.Logger;

import com.antbrains.mqtool.ActiveMqSender;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;
import com.antbrains.urlcrawler.db.CrawlTask;
import com.antbrains.urlcrawler.db.HbaseTool;
import com.google.gson.Gson; 

public class Producer extends Thread{
	protected static Logger logger=Logger.getLogger(Producer.class); 
	Connection conn=null;
	int batchSize;
	String dbName;
	private MqToolsInterface mqtools;
	private MqSender sender;
	int waitQueueSize;
	public Producer(String dbName, String conAddr, String jmxUrl, int batchSize, String zk, int waitQueueSize) throws Exception{
		logger.info("dbName: "+dbName);
		logger.info("conAddr: "+conAddr);
		logger.info("jmxUrl: "+jmxUrl);
		logger.info("zk: "+zk);
		logger.info("waitQueueSize: "+waitQueueSize);
		
		Configuration myConf = HBaseConfiguration.create();
		myConf.set("hbase.zookeeper.quorum", zk);
		this.dbName=dbName;
		conn =ConnectionFactory.createConnection(myConf); 
		this.batchSize=batchSize;
		this.waitQueueSize=waitQueueSize;
		mqtools = new HornetQTools(conAddr, jmxUrl);
		if (!mqtools.init()) {
			throw new IllegalArgumentException("can't connect to: " + conAddr + "\t" + jmxUrl);
		}
		sender = mqtools.getMqSender(dbName, Session.SESSION_TRANSACTED, true);
		if (!sender.init(ActiveMqSender.PERSISTENT)) {
			throw new IllegalArgumentException("can't getMqSender: " + conAddr + "\t" + jmxUrl);
		}
	}
	
	private volatile boolean bStop;
	public void stopMe(){
		bStop=true;
	}
	
	

	int failCount=0;
	private boolean sleepAndCheckExit(int maxFailCount, long sleepMs){
		failCount++;
		try {
			Thread.sleep(sleepMs);
		} catch (InterruptedException e1) {
		}
		if(failCount>maxFailCount){
			logger.warn("Fail_GET");
			return true;
		}
		return false;
	}
	
	private List<String> getUrls(List<CrawlTask> tasks){
		ArrayList<String> urls=new ArrayList<>(tasks.size());
		for(CrawlTask task:tasks){
			urls.add(task.url);
		}
		return urls;
	}
	
	@Override
	public void run(){
		int totalAdded=0;
		boolean first=true;
		Gson gson=new Gson();
		while(!bStop){
			long queueSize=sender.getQueueSize();
			logger.info("queue: "+queueSize);
			if(queueSize<0){
				logger.warn("queueSize: "+queueSize);
				break;
			}
			if(queueSize>=this.waitQueueSize){
				logger.info("sleep for queueSize: "+queueSize);
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) { 
				}
				continue;
			}
			String tbName="";
			try {
				List<CrawlTask> tasks=null;
				if(first){
					List<String> urls=HbaseTool.getRows(dbName, HbaseTool.TB_URLDB_CRAWLING, conn, this.batchSize);
					tbName=HbaseTool.TB_URLDB_CRAWLING;
					logger.info("lastCrawlingTask: "+urls.size());
					if(logger.isDebugEnabled()){
						for(String url:urls){
							logger.info("lastTasks: "+url);
						}
					}
					tasks=new ArrayList<>(urls.size());
					for(String url:urls){
						CrawlTask task=new CrawlTask();
						task.url=url;
						tasks.add(task);
					}
					if(urls.size()<batchSize){
						first=false;
					}else{
						logger.debug("continue first");
					}
				}else{
					List<String> urls=HbaseTool.getRows(dbName, HbaseTool.TB_URLDB_UNCRAWLED, conn, batchSize);
					tbName=HbaseTool.TB_URLDB_UNCRAWLED;
					if(urls.size()==0){
						List<CrawlTask> list=HbaseTool.getFailedTasks(dbName, conn, batchSize, 3);
						logger.debug("failed: "+list.size());
						if(logger.isDebugEnabled()){
							for(CrawlTask task:list){
								logger.debug(task);
							}
						}
						tbName=HbaseTool.TB_URLDB_FAIL;
						tasks=new ArrayList<>(list.size());
						for(CrawlTask task:list){
							if(task.failCount>=3){
								logger.warn("failedCount: "+task);
								continue;
							}
							tasks.add(task);
						}
					}else{
						logger.debug("uncrawled: "+urls.size());
						if(logger.isDebugEnabled()){
							for(String url:urls){
								logger.debug("uncrawled: "+url);
							}
						}
						tasks=new ArrayList<>(urls.size());
						for(String url:urls){
							CrawlTask task=new CrawlTask();
							task.url=url;
							task.status=CrawlTask.STATUS_NOT_CRAWLED;
							tasks.add(task);
						}
					}
				}
				totalAdded+=tasks.size();
				if(totalAdded%10_000==0){
					logger.info("totalAdded: "+totalAdded);
				}
				if(tasks.size()==0){
					boolean skip=this.sleepAndCheckExit(100, 30000);
					if(skip) break;
				}else{
					this.failCount=0;
					//ArrayList<CrawlTask> updatedTasks=new ArrayList<>();
					for(CrawlTask task:tasks){
						sender.send(gson.toJson(task));
						//this.taskQueue.put(task);
						//CrawlTask copyTask=task.copy();
						//copyTask.status=CrawlTask.STATUS_CRAWLING;
						//updatedTasks.add(copyTask);
					}
					sender.commit();
					//PhoenixTool.upsertTasks(conn, updatedTasks); 
					List<String> urls=this.getUrls(tasks);

					HbaseTool.addRows(dbName, HbaseTool.TB_URLDB_CRAWLING, conn, urls);
					HbaseTool.delRows(dbName, tbName, conn, urls);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				boolean skip=this.sleepAndCheckExit(100, 30000);
				if(skip) break;
			}
		}
		
		try {
			conn.close();
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
		}
		if (sender != null) {
			try {
				sender.destroy();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		if (mqtools != null) {
			try {
				mqtools.destroy();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	public static void main(String[] args) throws Exception{
		if(args.length!=6){
			System.out.println("need 6 args: dbName conAddr jmxUrl zk batchSize waitQueueSize");
			System.exit(-1);
		}
		String dbName=args[0];
		String conAddr=args[1];
		String jmxUrl=args[2];
		String zk=args[3];
		int batchSize=Integer.valueOf(args[4]);
		int waitQueueSize=Integer.valueOf(args[5]);
		final Producer producer=new Producer(dbName, conAddr, jmxUrl, batchSize, zk, waitQueueSize);
		producer.start();
		producer.join();
	}
}
