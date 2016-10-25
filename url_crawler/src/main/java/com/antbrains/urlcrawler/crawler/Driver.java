package com.antbrains.urlcrawler.crawler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options; 
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import com.antbrains.httpclientfetcher.HttpClientFetcher; 
import com.antbrains.urlcrawler.db.CrawlTask;

public class Driver{
	protected static Logger logger=Logger.getLogger(Driver.class);
	private static final int DEF_TASK_QUEUE_SIZE=1000;
	private static final int DEF_RES_QUEUE_SIZE=1000;
	private static final int DEF_PRODUCER_BATCH_SIZE=100;
	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("h", "help", false, "print help");
		options.addOption("taskQueueSize", true, "taskQueueSize " + DEF_TASK_QUEUE_SIZE);
		options.addOption("resQueueSize", true, "resQueueSize " + DEF_RES_QUEUE_SIZE);
		options.addOption("producerBatchSize", true, "producerBatchSize " + DEF_PRODUCER_BATCH_SIZE);
		options.addOption("zkPort", true, "zkPort null");
		
		CommandLine line = parser.parse(options, args);
		HelpFormatter formatter = new HelpFormatter();
		String helpStr = "Driver fetcherNumber zkQuorum stopPort dbName conAddr jmxUrl";
		args = line.getArgs();
		if (args.length !=6) {
			formatter.printHelp(helpStr, options);
			System.exit(-1);
		}
		
		int taskQueueSize=DEF_TASK_QUEUE_SIZE;
		if(line.hasOption("taskQueueSize")){
			taskQueueSize=Integer.valueOf(line.getOptionValue("taskQueueSize"));
		}
		int resQueueSize=DEF_RES_QUEUE_SIZE;
		if(line.hasOption("resQueueSize")){
			resQueueSize=Integer.valueOf(line.getOptionValue("resQueueSize"));
		}
		
		int producerBatchSize=DEF_PRODUCER_BATCH_SIZE;
		if(line.hasOption("producerBatchSize")){
			producerBatchSize=Integer.valueOf(line.getOptionValue("producerBatchSize"));
		}
		String zkPort=null;
		if(line.hasOption("zkPort")){
			zkPort=Integer.valueOf(line.getOptionValue("zkPort")).toString();
		}
		
		int workerNumber=Integer.valueOf(args[0]);
		String zkQuorum=args[1];
		int stopPort=Integer.valueOf(args[2]);
		String dbName=args[3];
		String conAddr=args[4];
		String jmxUrl=args[5];
		
		//print command args
		logger.info("workerNumber: "+workerNumber);
		logger.info("zkQuorum: "+zkQuorum);
		logger.info("stopPort: " +stopPort);
		logger.info("dbName: "+dbName);
		logger.info("conAddr: "+conAddr);
		logger.info("jmxUrl: "+jmxUrl);
		
		//print options
		logger.info("taskQueueSize: "+taskQueueSize);
		logger.info("resQueueSize: "+resQueueSize);
		logger.info("producerBatchSize: "+producerBatchSize);
		logger.info("zkPort: "+zkPort);
		
		//PoolManager.StartPool("conf", "baiduzhidao");
		
		BlockingQueue<CrawlTask> taskQueue=new ArrayBlockingQueue<>(taskQueueSize);
		BlockingQueue<CrawlTask> resQueue=new ArrayBlockingQueue<>(resQueueSize);
		
		final TaskReceiver receiver = new TaskReceiver(dbName, conAddr, jmxUrl, taskQueue);
		receiver.start();
		
		final Fetcher[] workers=new Fetcher[workerNumber];
		HttpClientFetcher fetcher=new HttpClientFetcher(Driver.class.getSimpleName());
		
		fetcher.init();
		for(int i=0;i<workers.length;i++){
			workers[i]=new Fetcher(fetcher, taskQueue, resQueue);
			workers[i].start();
		}
		
		final Writer writer=new Writer(dbName, resQueue, zkQuorum, zkPort);
		writer.start();
		
		Stoppable stoppable=new Stoppable(){

			@Override
			public void stopMe() {
				bStop=true;
				logger.info("receive stop signal");
				receiver.stopMe();
				try {
					receiver.join();
				} catch (InterruptedException e1) { 
				}
				
				for(int i=0;i<workers.length;i++){
					workers[i].stopMe();
				}
				for(int i=0;i<workers.length;i++){
					try {
						workers[i].join();
					} catch (InterruptedException e) { 
					}
				}
				writer.stopMe();
				try {
					writer.join();
				} catch (InterruptedException e) {
				}
			}

			@Override
			public void waitFinish(long wait) {
				try {
					Thread.sleep(wait);
				} catch (InterruptedException e) { 
				}
			}
			
		};
		
		StopService ss=new StopService(stopPort, stoppable, 5_000);
		ss.start();
		
		while(!bStop){
			Thread.sleep(60_000);
			logger.info("taskQueue: "+taskQueue.size()+"\t"+"resQueue: "+resQueue.size());
		}
		

		
		
		fetcher.close();
	}
	private static volatile boolean bStop=false;

}
