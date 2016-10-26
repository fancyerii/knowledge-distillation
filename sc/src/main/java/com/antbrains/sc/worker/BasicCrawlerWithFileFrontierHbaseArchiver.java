package com.antbrains.sc.worker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.jgroups.util.UUID;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.mysqltool.PoolManager;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.archiver.HbaseArchiver;
import com.antbrains.sc.archiver.MysqlArchiver;
import com.antbrains.sc.data.CrawlTask;
import com.antbrains.sc.extractor.UrlPatternExtractor;
import com.antbrains.sc.extractor.UrlPatternExtractor4Hbase;
import com.antbrains.sc.frontier.FileFrontier;
import com.antbrains.sc.frontier.Frontier;
import com.antbrains.sc.tools.GlobalConstants;
import com.antbrains.sc.tools.StopService;
import com.antbrains.sc.tools.StopableWorker;

public class BasicCrawlerWithFileFrontierHbaseArchiver extends StopableWorker {
	protected static Logger logger = Logger.getLogger(BasicCrawlerWithFileFrontierHbaseArchiver.class);

	public static final int DEF_QUEUE_SIZE = 1000;
	public static final int DEF_WORKER_NUM = 10;

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("h", "help", false, "print help");
		options.addOption("taskQueueSize", true, "taskQueueSize, default " + DEF_QUEUE_SIZE);
		options.addOption("workerNum", true, "workerNum, default " + DEF_WORKER_NUM);
		options.addOption("isUpdate", true, "isUpdate, default " + false);

		options.addOption("updateQueueSize", true, "updateQueueSize, default " + 100_000);
		options.addOption("failedQueueSize", true, "failedQueueSize, default " + 100_000);
		options.addOption("blockQueueSize", true, "blockQueueSize, default " + 10_000);
		options.addOption("updateWebThread", true, "updateWebThread, default 1");
		options.addOption("saveBlockThread", true, "saveBlockThread, default 1");
		options.addOption("saveFailThread", true, "saveFailThread, default 1");

		options.addOption("updateCacheSize", true, "updateCacheSize, default " + 1_000);
		options.addOption("flushInterval", true, "flushInterval, default " + 60_000);

		int workerNum = DEF_WORKER_NUM;
		int taskQueueSize = DEF_QUEUE_SIZE;

		CommandLine line = parser.parse(options, args);
		HelpFormatter formatter = new HelpFormatter();
		String helpStr = "BasicCrawler conAddr jmxUrl queueName extractorClass frontierDataDir zk stopPort";
		args = line.getArgs();
		if (args.length != 7) {
			formatter.printHelp(helpStr, options);
			System.exit(-1);
		}
		if (line.hasOption("help")) {
			formatter.printHelp(helpStr, options);
			System.exit(0);
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

		int updateQueueSize = 100_000;
		int failedQueueSize = 100_000;
		int blockQueueSize = 10_000;
		String zk = args[5];
		int updateWebThread = 1;
		int saveFailThread = 1;
		int saveBlockThread = 1;
		int updateCacheSize = 1_000;
		long flushInterval = 60_000;

		if (line.hasOption("updateQueueSize")) {
			updateQueueSize = Integer.valueOf(line.getOptionValue("updateQueueSize"));
		}
		if (line.hasOption("failedQueueSize")) {
			failedQueueSize = Integer.valueOf(line.getOptionValue("failedQueueSize"));
		}
		if (line.hasOption("blockQueueSize")) {
			blockQueueSize = Integer.valueOf(line.getOptionValue("blockQueueSize"));
		}

		if (line.hasOption("updateWebThread")) {
			updateWebThread = Integer.valueOf(line.getOptionValue("updateWebThread"));
		}
		if (line.hasOption("saveFailThread")) {
			saveFailThread = Integer.valueOf(line.getOptionValue("saveFailThread"));
		}
		if (line.hasOption("saveBlockThread")) {
			saveBlockThread = Integer.valueOf(line.getOptionValue("saveBlockThread"));
		}

		if (line.hasOption("updateCacheSize")) {
			updateCacheSize = Integer.valueOf(line.getOptionValue("updateCacheSize"));
		}
		if (line.hasOption("flushInterval")) {
			flushInterval = Long.valueOf(line.getOptionValue("flushInterval"));
		}

		BasicCrawlerWithFileFrontierHbaseArchiver bc = new BasicCrawlerWithFileFrontierHbaseArchiver(workerNum,
				taskQueueSize, args[3], args[0], args[1], args[2], isUpdate, args[4], updateQueueSize, failedQueueSize,
				blockQueueSize, zk, updateWebThread, saveFailThread, saveBlockThread, updateCacheSize, flushInterval);
		StopService ss = new StopService(Integer.valueOf(args[6]), bc, 0);
		ss.start();
	}

	private CrawlTaskMsgReceiver msgReceiver;
	private Frontier frontier;
	private HbaseArchiver archiver;
	private FetcherAndExtractor4Hbase[] workers;
	private HttpClientFetcher fetcher;

	public BasicCrawlerWithFileFrontierHbaseArchiver(int workerNum, int taskQueueSize, String extractorClass,
			String conAddr, String jmxUrl, String queueName, boolean isUpdate, String frontierDir, int updateQueueSize,
			int failedQueueSize, int blockQueueSize, String zk, int updateWebThread, int saveFailThread,
			int saveBlockThread, int updateCacheSize, long flushInterval) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String taskId = sdf.format(new Date()) + "-" + UUID.randomUUID().toString().hashCode();
		logger.info("taskId: " + taskId);
		logger.info("isUpdate: " + isUpdate);
		logger.info("frontierDir: " + frontierDir);
		logger.info("updateQueueSize: " + updateQueueSize);

		logger.info("updateQueueSize: " + updateQueueSize);
		logger.info("failedQueueSize: " + failedQueueSize);
		logger.info("blockQueueSize: " + blockQueueSize);

		logger.info("updateWebThread: " + updateWebThread);
		logger.info("saveFailThread: " + saveFailThread);
		logger.info("saveBlockThread: " + saveBlockThread);

		logger.info("updateCacheSize: " + updateCacheSize);
		logger.info("flushInterval: " + flushInterval);

		this.workers = new FetcherAndExtractor4Hbase[workerNum];
		BlockingQueue<CrawlTask> tasks = new LinkedBlockingQueue<CrawlTask>(taskQueueSize);
		UrlPatternExtractor4Hbase extractor = (UrlPatternExtractor4Hbase) Class.forName(extractorClass).newInstance();

		fetcher = new HttpClientFetcher(this.getClass().getName());
		fetcher.setMaxConnectionPerRoute(workerNum * 2);
		fetcher.setMaxTotalConnection(workerNum * 2);
		fetcher.init();
		archiver = new HbaseArchiver(queueName, updateQueueSize, failedQueueSize, blockQueueSize, zk, updateWebThread,
				saveFailThread, saveBlockThread, updateCacheSize, flushInterval);
		PoolManager.StartPool("conf", queueName);
		msgReceiver = new CrawlTaskMsgReceiver(new MysqlArchiver(), conAddr, jmxUrl, queueName, tasks);
		new Thread(msgReceiver).start();
		frontier = new FileFrontier(frontierDir, 10_000, 60_000);

		for (int i = 0; i < workers.length; i++) {
			workers[i] = new FetcherAndExtractor4Hbase(tasks, extractor, fetcher, frontier, archiver, taskId);
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
		this.frontier.close();
		archiver.close();
		logger.info("I am stopped");

		super.finished();
	}

}
