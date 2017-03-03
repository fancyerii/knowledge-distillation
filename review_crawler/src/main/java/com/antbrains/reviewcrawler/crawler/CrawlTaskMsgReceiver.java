package com.antbrains.reviewcrawler.crawler;

import java.util.concurrent.BlockingQueue;


import org.apache.log4j.Logger;

import com.antbrains.mqtool.ActiveMqTools;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqReceiver;
import com.antbrains.mqtool.MqToolsInterface;
import com.antbrains.mqtool.QueueTools;
import com.antbrains.reviewcrawler.data.CrawlTask;
import com.google.gson.Gson;

public class CrawlTaskMsgReceiver implements Runnable {
	protected static Logger logger = Logger.getLogger(CrawlTaskMsgReceiver.class);
	private MqToolsInterface mqtools;
	private MqReceiver receiver;
	private BlockingQueue<CrawlTask> queue; 
	public CrawlTaskMsgReceiver(String conAddr, String jmxUrl, String queueName, BlockingQueue<CrawlTask> queue) {
		mqtools = new HornetQTools(conAddr, jmxUrl);
		if (!mqtools.init()) {
			throw new IllegalArgumentException("can't init mqtools: " + conAddr + "\t" + jmxUrl);
		}

		receiver = mqtools.getMqReceiver(queueName, ActiveMqTools.AUTO_ACKNOWLEDGE);
		if (!receiver.init()) {
			throw new IllegalArgumentException("can't init receiver in FetchAndExtractWorker");
		}

		this.queue = queue; 
	}

	public void close() {
		if (receiver != null) {
			try {
				receiver.destroy();
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
 

	@Override
	public void run() {
		Gson gson = new Gson(); 
		while (true) {
			try { 
				String s=QueueTools.receive(receiver, 5000);
				if(s==null) continue;
				CrawlTask ct = gson.fromJson(s, CrawlTask.class);
				queue.put(ct);
			} catch (Exception e) {
				logger.error(e.getMessage());
				break;
			}
		}

		this.close();
		logger.info("I am stopped");
	}
}
