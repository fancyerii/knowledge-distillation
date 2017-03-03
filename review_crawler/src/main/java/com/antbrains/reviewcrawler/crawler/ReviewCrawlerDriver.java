package com.antbrains.reviewcrawler.crawler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.mysqltool.PoolManager;
import com.antbrains.reviewcrawler.data.CrawlTask;
import com.antbrains.reviewcrawler.fetcher.HcFetcher;

public class ReviewCrawlerDriver {
	protected static Logger logger=Logger.getLogger(ReviewCrawlerDriver.class);
	private static final int DEF_TASK_QUEUE_SIZE=100;
	public static void main(String[] args) throws Exception{
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("h", "help", false, "print help");
		options.addOption("taskQueueSize", true, "taskQueueSize " + DEF_TASK_QUEUE_SIZE);
		
		
		CommandLine line = parser.parse(options, args);
		int taskQueueSize=DEF_TASK_QUEUE_SIZE;
		if(line.hasOption("taskQueueSize")){
			taskQueueSize=Integer.valueOf(line.getOptionValue("taskQueueSize"));
		}
		
		HelpFormatter formatter = new HelpFormatter();
		String helpStr = " fetcherNumber dbName queueName conAddr jmxUrl reviewCrawlerClass";
		args = line.getArgs();
		if (args.length !=6) {
			formatter.printHelp(helpStr, options);
			System.exit(-1);
		}
		String fetcherNumber=args[0];
		String dbName=args[1];
		String qName=args[2];
		String conAddr=args[3];
		String jmxUrl=args[4];
		String reviewCrawlerClass=args[5];
		logger.info("fetcherNumber: "+fetcherNumber);
		logger.info("dbName: "+dbName);
		logger.info("qName: "+qName);
		logger.info("conAddr: "+conAddr);
		logger.info("jmxUrl: "+jmxUrl);
		logger.info("reviewCrawlerClass: "+reviewCrawlerClass);
		
		int workerNumber=Integer.valueOf(fetcherNumber);
		PoolManager.StartPool("conf", dbName);
		
		ReviewCrawler[] crawlers=new ReviewCrawler[workerNumber];
		BlockingQueue<CrawlTask> taskQueue=new ArrayBlockingQueue<CrawlTask>(taskQueueSize);
		
		Class cls=Class.forName(reviewCrawlerClass);
		HttpClientFetcher fetcher=new HttpClientFetcher("");
		fetcher.init();
		for(int i=0;i<crawlers.length;i++){
			crawlers[i]=(ReviewCrawler) cls.newInstance();
			crawlers[i].init(taskQueue, new HcFetcher(fetcher));
			crawlers[i].start();
		}
		CrawlTaskMsgReceiver rcv=new CrawlTaskMsgReceiver(conAddr, jmxUrl, qName, taskQueue);
		Thread t=new Thread(rcv);
		t.start();
		t.join();
		for(int i=0;i<crawlers.length;i++){
			crawlers[i].stopMe();
		}
		for(int i=0;i<crawlers.length;i++){
			crawlers[i].join();
		}
		
	}

}
