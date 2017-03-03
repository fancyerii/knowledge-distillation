package com.antbrains.reviewcrawler.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import javax.jms.Session;

import org.apache.log4j.Logger;

import com.antbrains.mqtool.ActiveMqSender;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;
import com.antbrains.mqtool.QueueTools;
import com.antbrains.mysqltool.DBUtils;
import com.antbrains.mysqltool.PoolManager;
import com.antbrains.reviewcrawler.archiver.MysqlArchiver;
import com.antbrains.reviewcrawler.data.CrawlTask;
import com.google.gson.Gson; 

public class ReviewScheduler {
	protected static Logger logger=Logger.getLogger(ReviewScheduler.class);
		

	private long sleepIfNoUrlsInSec=60L*5;
	private int minUpdateIntervalInSec=1*3600;
	private int maxUpdateIntervalInSec=15*24*3600;
	private int updateIntervalDeltaInSec=1*3600;
	private int initUpdateIntervalInSec=24*3600;
	private int scheduleBatch=1_000;
	
	private int maxQueueSize=2_000;
	
	private MqToolsInterface mqtools;
	private MqSender sender;
	
	public ReviewScheduler(String queueName, String conAddr, String jmxUrl){ 
		mqtools = new HornetQTools(conAddr, jmxUrl);
		if (!mqtools.init()) {
			throw new IllegalArgumentException("can't connect to: " + conAddr + "\t" + jmxUrl);
		}
		sender = mqtools.getMqSender(queueName, Session.AUTO_ACKNOWLEDGE);
		if (!sender.init(ActiveMqSender.PERSISTENT)) {
			throw new IllegalArgumentException("can't getMqSender: " + conAddr + "\t" + jmxUrl);
		}
	}
	
	public void doSchedule(){
		while(true){
			long queueSize=sender.getQueueSize();
			if(queueSize==-1){
				logger.error("queueSize can't get");
				return;
			}
			if(queueSize>=this.maxQueueSize){
				try {
					Thread.sleep(sleepIfNoUrlsInSec*1000L);
				} catch (InterruptedException e) { 
				}
				continue;
			}
			int added=this.doASchedule();
			logger.info("added: "+added);
			if(added==-1){
				return;
			}else if(added==0){
				try {
					Thread.sleep(300_000L);
				} catch (InterruptedException e) { 
				}
			}
			
		}
	}
	
	private int doASchedule(){
		logger.info("doASchedule");
		PriorityQueue<CrawlTask> pq=new PriorityQueue<CrawlTask>(scheduleBatch){
			@Override
			protected boolean lessThan(CrawlTask a, CrawlTask b) {
				return a.getPrior()<b.getPrior();
			}
		};
		
		Connection conn=null;
		Statement stmt=null;
		ResultSet rs=null;
		try{
			conn=PoolManager.getConnection();
			stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
			stmt.setFetchSize(Integer.MIN_VALUE);
			
			rs=stmt.executeQuery("select * from review_status");
			long current=System.currentTimeMillis();
			while(rs.next()){
				int id=rs.getInt("page_id");
				String url=rs.getString("page_url");
				Date lastUpdate=DBUtils.timestamp2Date(rs.getTimestamp("lastUpdate"));
				int lastAdded=rs.getInt("lastAdded");
				int update_interval=rs.getInt("update_interval_sec");
				String lastestReviewTime=rs.getString("lastestReviewTime");
				int crawling_status=rs.getInt("crawling_status");
				if(crawling_status==1) continue;
				if(lastUpdate==null){
					pq.insertWithOverflow(new CrawlTask(id+"", url, lastestReviewTime, Integer.MAX_VALUE, this.initUpdateIntervalInSec));
				}else if(current-lastUpdate.getTime()>1000L*update_interval){
					pq.insertWithOverflow(new CrawlTask(id+"", url, lastestReviewTime, lastAdded, update_interval));
				}
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			return -1;
		}finally{
			DBUtils.closeAll(conn, stmt, rs);
		}
		
		ArrayList<CrawlTask> result=new ArrayList<>();
		while(pq.size()>0){
			result.add(pq.pop());
		} 
		Collections.reverse(result);
		Gson gson=new Gson();
		for(CrawlTask task:result){
			String s=gson.toJson(task);
			boolean res=QueueTools.send(sender, s);
			if(!res){
				logger.error("can't send msg: "+s);
				return -1;
			}
		}
		
		//update interval
		try {
			this.updateInterval(result);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return -1;
		}
		
		return result.size();
	}
	
	private void updateInterval(ArrayList<CrawlTask> result) throws Exception{
		Connection conn = null;
		PreparedStatement pstmt = null;
		Boolean autoCommit=null;
		try{
			conn=PoolManager.getConnection();
			autoCommit=conn.getAutoCommit();
			pstmt=conn.prepareStatement("update review_status set lastAdded=0, crawling_status=1, update_interval_sec=? where page_id=?");
			for(CrawlTask task:result){
				pstmt.setInt(2, Integer.valueOf(task.getId()));
				if(task.getPrior()==Integer.MAX_VALUE){
					pstmt.setInt(1, this.initUpdateIntervalInSec);
				}else{
					if(task.getPrior()>0){//有更新
						int updateInterval=task.getUpdateInterval();
						updateInterval-=this.updateIntervalDeltaInSec;
						updateInterval=Math.max(updateInterval, this.minUpdateIntervalInSec);
						pstmt.setInt(1, updateInterval);
					}else{
						int updateInterval=task.getUpdateInterval();
						updateInterval+=this.updateIntervalDeltaInSec;
						updateInterval=Math.min(updateInterval, this.maxUpdateIntervalInSec);
						pstmt.setInt(1, updateInterval);
					}
				}
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		}finally{
			if(autoCommit!=null){
				try{
					conn.setAutoCommit(autoCommit);
				}catch(Exception e){}
			}
			DBUtils.closeAll(conn, pstmt, null);
		}
	}
	
	public static void main(String[] args) {
		if(args.length!=4){
			System.out.println("need 4 args dbName queueName connAddr jmxUrl");
			System.exit(-1);
		}
		PoolManager.StartPool("conf", args[0]);
		ReviewScheduler rs=new ReviewScheduler(args[1], args[2], args[3]);
		
		rs.doSchedule();
	}

}
