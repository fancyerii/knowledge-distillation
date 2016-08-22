package com.antbrains.ifengcrawler.scheduler;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.antbrains.mqtool.ActiveMqSender;
import com.antbrains.mqtool.ActiveMqTools;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;
import com.antbrains.mysqltool.PoolManager;
import com.antbrains.sc.archiver.MysqlArchiver;
import com.antbrains.sc.data.CrawlTask;
import com.antbrains.sc.data.WebPage;

public class IfengScheduler {
	protected static Logger logger = Logger.getLogger(IfengScheduler.class);

	public static void main(String[] args) {
		// put first page to queue
		if (args.length != 3) {
			logger.error("need 3 args conAddress jmxUrl dbName");
			System.exit(-1);
		}
		PoolManager.StartPool("conf", args[2]);
		MysqlArchiver archiver = new MysqlArchiver();
		WebPage webPage = archiver.getWebPage(1);

		CrawlTask ct = new CrawlTask();
		ct.url = webPage.getUrl();
		ct.depth = webPage.getDepth();
		ct.failCount = 0;
		ct.priority = 0;
		ct.id = webPage.getId();
		ct.redirectedUrl = webPage.getRedirectedUrl();
		ct.lastVisit = 0;
		if (webPage.getLastVisitTime() != null) {
			ct.lastVisit = webPage.getLastVisitTime().getTime();
		}
		Gson gson = new Gson();
		String s = gson.toJson(ct);
		MqToolsInterface mqtools = new HornetQTools(args[0], args[1]);
		if (!mqtools.init()) {
			throw new IllegalArgumentException("can't connect to: " + args[0]);
		}
		MqSender sender = mqtools.getMqSender(args[2], ActiveMqSender.PERSISTENT);
		if (!sender.init(ActiveMqSender.PERSISTENT)) {
			throw new IllegalArgumentException("can't getMqSender: " + args[1]);
		}

		sender.send(s);

		sender.destroy();

		mqtools.destroy();
	}

}
