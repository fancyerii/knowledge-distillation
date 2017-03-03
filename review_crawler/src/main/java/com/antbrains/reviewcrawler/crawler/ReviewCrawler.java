package com.antbrains.reviewcrawler.crawler;
 
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.antbrains.reviewcrawler.archiver.MysqlArchiver;
import com.antbrains.reviewcrawler.data.CrawlTask;
import com.antbrains.reviewcrawler.data.ReviewContent;
import com.antbrains.reviewcrawler.data.ReviewStatus;
import com.antbrains.reviewcrawler.fetcher.Fetcher;
import com.google.gson.Gson; 
import com.google.gson.JsonParser;

public abstract class ReviewCrawler extends Thread{
	protected static Logger logger=Logger.getLogger(ReviewCrawler.class);
	private BlockingQueue<CrawlTask> taskQueue;
	protected Fetcher fetcher;
	protected JsonParser parser=new JsonParser();
	protected Gson gson=new Gson();
	private MysqlArchiver archiver;
	public ReviewCrawler(){
		archiver=new MysqlArchiver();
	}
	
	public boolean init(BlockingQueue<CrawlTask> taskQueue, Fetcher fetcher){
		this.taskQueue=taskQueue;
		this.fetcher=fetcher;
		return true;
	}
	
	private volatile boolean bStop;
	public void stopMe(){
		bStop=true;
	}
	
	@Override
	public void run(){
		if(taskQueue==null){
			logger.warn("init is not called");
			return;
		}
		while(!bStop){
			try {
				CrawlTask task=taskQueue.poll(5, TimeUnit.SECONDS);
				if(task==null) continue;
				try {
					this.doWork(task);
				} catch (Exception e) {
					logger.error(e.getMessage());
					logger.error("can't process: "+task.getId()+", "+task.getUrl());
				}
			} catch (InterruptedException e) { 
			}
		}
		
		logger.info("prepare to stop");
		while(true){
			try {
				CrawlTask task=taskQueue.poll(1, TimeUnit.SECONDS);
				if(task==null) break;
				try {
					this.doWork(task);
				} catch (Exception e) {
					logger.error(e.getMessage());
					logger.error("can't process: "+task.getId()+", "+task.getUrl());
				}
			} catch (InterruptedException e) { 
			}
		}
		logger.info("stop");
	}
	
	protected abstract List<ReviewContent> crawlAReview(CrawlTask task) throws Exception;
	
	private void doWork(CrawlTask task) throws Exception{
		
		List<ReviewContent> reviews=this.crawlAReview(task);
		if(reviews==null){
			logger.warn("reviews is null: "+task.getUrl()+"\t"+task.getId());
			return;
		}
		int added=this.archiver.addComments(reviews);
		ReviewStatus rs=new ReviewStatus();
		rs.setId(task.getId());
		rs.setLastestReviewTime(this.getLatestTime(reviews));
		rs.setLastAdded(added);
		
		this.archiver.updateCommentStatus(rs);
	}
	
	private String getLatestTime(List<ReviewContent> reviews){
		String max="";
		for(ReviewContent rv:reviews){
			if(max.compareTo(rv.getDate())<0){
				max=rv.getDate();
			}
		}
		return max;
		
	}
	

}
