package com.antbrains.sc.worker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.jgroups.util.UUID;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.httpclientfetcher.ResultValidator;
import com.antbrains.mysqltool.PoolManager;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.archiver.MysqlArchiver;
import com.antbrains.sc.data.CrawlTask;
import com.antbrains.sc.extractor.UrlPatternExtractor;
import com.antbrains.sc.frontier.Frontier;
import com.antbrains.sc.frontier.MysqlFrontier;
import com.antbrains.sc.tools.GlobalConstants;
import com.antbrains.sc.tools.StopService;
import com.antbrains.sc.tools.StopableWorker;

public class BasicCrawler extends StopableWorker {
	protected static Logger logger = Logger.getLogger(BasicCrawler.class);

	public static final int DEF_QUEUE_SIZE = 1000;
	public static final int DEF_WORKER_NUM = 10;
	private static final int STOP_PORT_CRAWLER = 22346;

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("h", "help", false, "print help");
		options.addOption("taskQueueSize", true, "taskQueueSize, default " + DEF_QUEUE_SIZE);
		options.addOption("workerNum", true, "workerNum, default " + DEF_WORKER_NUM);
		options.addOption("stopPort", true, "stop listen port, default " + STOP_PORT_CRAWLER);
		options.addOption("isUpdate", true, "isUpdate, default " + false);
		options.addOption("rv", true, "resultValidatorClass, default null");
		int workerNum = DEF_WORKER_NUM;
		int taskQueueSize = DEF_QUEUE_SIZE;
		int stopPort = STOP_PORT_CRAWLER;
		CommandLine line = parser.parse(options, args);
		HelpFormatter formatter = new HelpFormatter();
		String helpStr = "BasicCrawler conAddr jmxUrl queueName extractorClass";
		args = line.getArgs();
		if (args.length != 4) {
			formatter.printHelp(helpStr, options);
			System.exit(-1);
		}
		if (line.hasOption("help")) {
			formatter.printHelp(helpStr, options);
			System.exit(0);
		}

		if (line.hasOption("stopPort")) {
			stopPort = Integer.valueOf(line.getOptionValue("stopPort"));
		}

		if (line.hasOption("taskQueueSize")) {
			taskQueueSize = Integer.valueOf(line.getOptionValue("taskQueueSize"));
		}
		if (line.hasOption("workerNum")) {
			workerNum = Integer.valueOf(line.getOptionValue("workerNum"));
		}
		boolean isUpdate = false;
		if (line.hasOption("isUpdate")) {
			isUpdate = Boolean.valueOf(line.getOptionValue("isUpdate"));
		}
		String rv = null;
		if (line.hasOption("rv")) {
			rv = line.getOptionValue("rv");
		}
		BasicCrawler bc = new BasicCrawler(workerNum, taskQueueSize, args[3], args[0], args[1], args[2], isUpdate, rv);
		StopService ss = new StopService(stopPort, bc, 0);
		ss.start();
	}

	private CrawlTaskMsgReceiver msgReceiver;
	private FetcherAndExtractor[] workers;
	private HttpClientFetcher fetcher;

	public BasicCrawler(int workerNum, int taskQueueSize, String extractorClass, String conAddr, String jmxUrl,
			String queueName, boolean isUpdate, String resultValidatorClass) throws Exception {
		PoolManager.StartPool("conf", queueName);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String taskId = sdf.format(new Date()) + "-" + UUID.randomUUID().toString().hashCode();
		logger.info("taskId: " + taskId);
		logger.info("isUpdate: " + isUpdate);

		this.workers = new FetcherAndExtractor[workerNum];
		BlockingQueue<CrawlTask> tasks = new LinkedBlockingQueue<CrawlTask>(taskQueueSize);
		UrlPatternExtractor extractor = (UrlPatternExtractor) Class.forName(extractorClass).newInstance();

		ResultValidator rv = null;
		if (resultValidatorClass != null && !resultValidatorClass.isEmpty()) {
			try {
				rv = (ResultValidator) Class.forName(resultValidatorClass).newInstance();
				logger.info("rvClass: " + resultValidatorClass + ", instance: " + rv);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		if (rv != null) {
			fetcher = new HttpClientFetcher(this.getClass().getName(), rv);
		} else {
			fetcher = new HttpClientFetcher(this.getClass().getName());
		}
		fetcher.setMaxConnectionPerRoute(workerNum * 2);
		fetcher.setMaxTotalConnection(workerNum * 2);
		fetcher.init();
		MysqlArchiver archiver = new MysqlArchiver();
		msgReceiver = new CrawlTaskMsgReceiver(archiver, conAddr, jmxUrl, queueName, tasks);
		new Thread(msgReceiver).start();
		Frontier frontier = new MysqlFrontier();
		
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new FetcherAndExtractor(tasks, extractor, fetcher, frontier, archiver, taskId, isUpdate);
		}

		for (int i = 0; i < workers.length; i++) {
			new Thread(workers[i]).start();
		}

	}

	@Override
	public void stopMe() {
		msgReceiver.stopMe();
		msgReceiver.waitFinish(0);
		for (int i = 0; i < workers.length; i++) {
			workers[i].stopMe();
		}
		for (int i = 0; i < workers.length; i++) {
			workers[i].waitFinish(0);
		}
		fetcher.close();
		logger.info("I am stopped");

		super.finished();
	}

}
