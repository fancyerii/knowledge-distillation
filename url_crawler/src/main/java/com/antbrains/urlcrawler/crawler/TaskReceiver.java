package com.antbrains.urlcrawler.crawler;

import java.util.concurrent.BlockingQueue;

import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;

import com.antbrains.mqtool.ActiveMqSender;
import com.antbrains.mqtool.ActiveMqTools;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqReceiver;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;
import com.antbrains.mqtool.QueueTools;
import com.antbrains.urlcrawler.db.CrawlTask;
import com.google.gson.Gson;

public class TaskReceiver extends Thread{
	protected static Logger logger=Logger.getLogger(TaskReceiver.class);
	
	private MqToolsInterface mqtools;
	private MqReceiver receiver;
	private BlockingQueue<CrawlTask> taskQueue;
	public TaskReceiver(String queueName, String conAddr, String jmxUrl, BlockingQueue<CrawlTask> taskQueue) throws Exception{
		mqtools = new HornetQTools(conAddr, jmxUrl);
		if (!mqtools.init()) {
			throw new IllegalArgumentException("can't connect to: " + conAddr + "\t" + jmxUrl);
		}
		receiver = mqtools.getMqReceiver(queueName, ActiveMqTools.AUTO_ACKNOWLEDGE);
		if (!receiver.init()) {
			throw new IllegalArgumentException("can't init receiver in FetchAndExtractWorker");
		}
		this.taskQueue=taskQueue;
	}
	
	private volatile boolean bStop;
	public void stopMe(){
		
	}
	
	@Override
	public void run(){
		Gson gson=new Gson();
		while(!bStop){
			try{
				String s = QueueTools.receive(receiver, 5000);
				if(s==null) continue;
				
				CrawlTask ct = gson.fromJson(s, CrawlTask.class);
				this.taskQueue.put(ct);
			}catch(Exception e){
				//logger.error(e.getMessage(),e);
				return;
			}
		}
		logger.info("stop");
	}
}
