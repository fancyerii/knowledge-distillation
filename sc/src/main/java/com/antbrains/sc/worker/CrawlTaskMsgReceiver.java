package com.antbrains.sc.worker;

import java.util.concurrent.BlockingQueue;

import javax.jms.TextMessage;

import org.apache.log4j.Logger;

import com.antbrains.mqtool.ActiveMqTools;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqReceiver;
import com.antbrains.mqtool.MqToolsInterface;
import com.antbrains.mqtool.QueueTools;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.archiver.Constants;
import com.antbrains.sc.archiver.MysqlArchiver;
import com.antbrains.sc.data.CrawlTask;
import com.antbrains.sc.tools.StopableWorker;
import com.google.gson.Gson;

public class CrawlTaskMsgReceiver extends StopableWorker implements Runnable {
	protected static Logger logger = Logger.getLogger(CrawlTaskMsgReceiver.class);
	private MqToolsInterface mqtools;
	private MqReceiver receiver;
	private BlockingQueue<CrawlTask> queue;
	MysqlArchiver archiver;
	public CrawlTaskMsgReceiver(MysqlArchiver archiver, String conAddr, String jmxUrl, String queueName, BlockingQueue<CrawlTask> queue) {
		mqtools = new HornetQTools(conAddr, jmxUrl);
		if (!mqtools.init()) {
			throw new IllegalArgumentException("can't init mqtools: " + conAddr + "\t" + jmxUrl);
		}

		receiver = mqtools.getMqReceiver(queueName, ActiveMqTools.AUTO_ACKNOWLEDGE);
		if (!receiver.init()) {
			throw new IllegalArgumentException("can't init receiver in FetchAndExtractWorker");
		}

		this.queue = queue;
		this.archiver=archiver;
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
	
	private long lastUpdate=-1;
	private void updateStatus(){
		if(System.currentTimeMillis()-lastUpdate>Constants.UPDATE_COMPONENT_STATUS_INTERVAL){
			if(this.queue.size()>0){
				this.archiver.updateComponentStatus(Constants.COMPONENT_MSGRECV, Constants.STATUS_HASTASK);
			}else{
				this.archiver.updateComponentStatus(Constants.COMPONENT_MSGRECV, Constants.STATUS_NOTASK);
			}
			lastUpdate=System.currentTimeMillis();
		}
	}

	@Override
	public void run() {
		Gson gson = new Gson(); 
		while (!bStop) {
			try {
				this.updateStatus();
				String s=QueueTools.receive(receiver, 5000);
				if(s==null) continue;
				CrawlTask ct = gson.fromJson(s, CrawlTask.class);
				queue.put(ct);
			} catch (Exception e) {
				logger.error(e.getMessage());
				return;
			}
		}

		this.close();
		super.finished();
		logger.info("I am stopped");
	}
}
