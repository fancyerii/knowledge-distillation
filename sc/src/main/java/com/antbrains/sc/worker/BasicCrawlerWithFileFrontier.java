package com.antbrains.sc.worker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.jgroups.util.UUID;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.httpclientfetcher.MyHttpRoutePlanner;
import com.antbrains.httpclientfetcher.ProxyDiscover;
import com.antbrains.httpclientfetcher.ProxyManager;
import com.antbrains.httpclientfetcher.ResultValidator;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.archiver.MysqlArchiver;
import com.antbrains.sc.data.CrawlTask;
import com.antbrains.sc.extractor.UrlPatternExtractor;
import com.antbrains.sc.frontier.FileFrontier;
import com.antbrains.sc.frontier.Frontier;
import com.antbrains.sc.tools.GlobalConstants;
import com.antbrains.sc.tools.StopService;
import com.antbrains.sc.tools.StopableWorker;

public class BasicCrawlerWithFileFrontier extends StopableWorker {
	protected static Logger logger = Logger.getLogger(BasicCrawlerWithFileFrontier.class);
	private static final int STOP_PORT_CRAWLER = 22346;
	public static final int DEF_QUEUE_SIZE = 1000;
	public static final int DEF_WORKER_NUM = 10;
	public static final long DEF_MAX_LATENCY = 5000;
	public static final int DEF_MAX_FAIL_COUNT = 3;
	public static final long DEF_CHECK_INTERVAL = 60000;
	public static final int DEF_CHECK_BAD_TIMES = 10;
	public static final int DEF_INIT_CHECK_THREADS = 30;
	public static final int DEF_MIN_THRESHOLD = 30;
	public static final long DEF_CHECK_NEW_INTERVAL = 6 * 3600 * 1000L;
	public static final int DEF_EXPECTED_PROXY = 60;
	public static final int DEF_MAX_TOTAL_FAIL_COUNT = 20;
	public static final double DEF_MAX_FAIL_RATIO = .5;

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("h", "help", false, "print help");
		options.addOption("taskQueueSize", true, "taskQueueSize, default " + DEF_QUEUE_SIZE);
		options.addOption("workerNum", true, "workerNum, default " + DEF_WORKER_NUM);
		options.addOption("rv", true, "resultValidatorClass, default null");
		options.addOption("isUpdate", true, "isUpdate, default " + false);
		options.addOption("useProxy", false, "whether use proxy");
		options.addOption("validUrl", true, "validUrl");
		options.addOption("validContent", true, "validContent");
		options.addOption("maxLatency", true, "maxLatency, default " + DEF_MAX_LATENCY);
		options.addOption("maxFailCount", true, "maxFailCount, default " + DEF_MAX_FAIL_COUNT);
		options.addOption("checkInterval", true, "checkInterval, default " + DEF_CHECK_INTERVAL);
		options.addOption("checkBadTimes", true, "checkBadTimes, default " + DEF_CHECK_BAD_TIMES);
		options.addOption("notDiscardBadHost", false, "notDiscardBadHost");
		options.addOption("initCheckThreads", true, "initCheckThreads default " + DEF_INIT_CHECK_THREADS);
		options.addOption("minThreshold", true, "minThreshold " + DEF_MIN_THRESHOLD);
		options.addOption("checkNewInterval", true, "checkNewInterval " + DEF_CHECK_NEW_INTERVAL);
		options.addOption("expectedProxy", true, "expectedProxy " + DEF_EXPECTED_PROXY);
		options.addOption("excludeSelf", false, "excludeSelf");
		options.addOption("notDoInitCheck", false, "notDoInitCheck");
		options.addOption("maxTotalFailCount", true, "maxTotalFailCount " + DEF_MAX_TOTAL_FAIL_COUNT);
		options.addOption("maxFailRatio", true, "maxFailRatio " + DEF_MAX_FAIL_RATIO);
		options.addOption("stopPort", true, "stop listen port, default " + STOP_PORT_CRAWLER);
		int workerNum = DEF_WORKER_NUM;
		int taskQueueSize = DEF_QUEUE_SIZE;
		int stopPort = STOP_PORT_CRAWLER;
		CommandLine line = parser.parse(options, args);
		HelpFormatter formatter = new HelpFormatter();
		String helpStr = "BasicCrawler conAddr jmxUrl queueName extractorClass frontierDataDir";
		args = line.getArgs();
		if (args.length != 5) {
			formatter.printHelp(helpStr, options);
			System.exit(-1);
		}

		ProxyParams pp = null;
		if (line.hasOption("useProxy")) {
			pp = new ProxyParams();
			if (line.hasOption("validUrl")) {
				pp.validUrl = line.getOptionValue("validUrl");
			}
			if (line.hasOption("validContent")) {
				pp.validContent = line.getOptionValue("validContent");
			}
			if (line.hasOption("maxLatency")) {
				pp.maxLatency = Long.valueOf(line.getOptionValue("maxLatency"));
			} else {
				pp.maxLatency = DEF_MAX_LATENCY;
			}
			if (line.hasOption("maxFailCount")) {
				pp.maxFailCount = Integer.valueOf(line.getOptionValue("maxFailCount"));
			} else {
				pp.maxFailCount = DEF_MAX_FAIL_COUNT;
			}
			if (line.hasOption("checkInterval")) {
				pp.checkInterval = Long.valueOf(line.getOptionValue("checkInterval"));
			} else {
				pp.checkInterval = DEF_CHECK_INTERVAL;
			}
			if (line.hasOption("checkBadTimes")) {
				pp.checkBadTimes = Integer.valueOf(line.getOptionValue("checkBadTimes"));
			} else {
				pp.checkBadTimes = DEF_CHECK_BAD_TIMES;
			}
			if (line.hasOption("notDiscardBadHost")) {
				pp.discardBadHost = false;
			} else {
				pp.discardBadHost = true;
			}
			if (line.hasOption("initCheckThreads")) {
				pp.initCheckThreads = Integer.valueOf(line.getOptionValue("initCheckThreads"));
			} else {
				pp.initCheckThreads = DEF_INIT_CHECK_THREADS;
			}
			if (line.hasOption("minThreshold")) {
				pp.minThreshold = Integer.valueOf(line.getOptionValue("minThreshold"));
			} else {
				pp.minThreshold = DEF_MIN_THRESHOLD;
			}
			if (line.hasOption("checkNewInterval")) {
				pp.checkNewInterval = Long.valueOf(line.getOptionValue("checkNewInterval"));
			} else {
				pp.checkNewInterval = DEF_CHECK_NEW_INTERVAL;
			}
			if (line.hasOption("expectedProxy")) {
				pp.expectedProxy = Integer.valueOf(line.getOptionValue("expectedProxy"));
			} else {
				pp.expectedProxy = DEF_EXPECTED_PROXY;
			}
			if (line.hasOption("excludeSelf")) {
				pp.excludeSelf = true;
			} else {
				pp.excludeSelf = false;
			}
			if (line.hasOption("notDoInitCheck")) {
				pp.doInitCheck = false;
			} else {
				pp.doInitCheck = true;
			}
			if (line.hasOption("maxTotalFailCount")) {
				pp.maxTotalFailCount = Integer.valueOf(line.getOptionValue("maxTotalFailCount"));
			} else {
				pp.maxTotalFailCount = DEF_MAX_FAIL_COUNT;
			}
			if (line.hasOption("maxFailRatio")) {
				pp.maxFailRatio = Double.valueOf(line.getOptionValue("maxFailRatio"));
			} else {
				pp.maxFailRatio = DEF_MAX_FAIL_RATIO;
			}
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
		if (line.hasOption("stopPort")) {
			stopPort = Integer.valueOf(line.getOptionValue("stopPort"));
		}
		boolean isUpdate = false;
		if (line.hasOption("isUpdate")) {
			isUpdate = Boolean.valueOf(line.getOptionValue("isUpdate"));
		}
		String rv = null;
		if (line.hasOption("rv")) {
			rv = line.getOptionValue("rv");
		}
		BasicCrawlerWithFileFrontier bc = new BasicCrawlerWithFileFrontier(workerNum, taskQueueSize, args[3], args[0],
				args[1], args[2], isUpdate, args[4], rv, pp);
		StopService ss = new StopService(stopPort, bc, 0);
		ss.start();
	}

	private CrawlTaskMsgReceiver msgReceiver;
	private Frontier frontier;
	private FetcherAndExtractor[] workers;
	private HttpClientFetcher fetcher;

	public BasicCrawlerWithFileFrontier(int workerNum, int taskQueueSize, String extractorClass, String conAddr,
			String jmxUrl, String queueName, boolean isUpdate, String frontierDir, String resultValidatorClass,
			ProxyParams pp) throws Exception {
		com.antbrains.mysqltool.PoolManager.StartPool("conf", queueName);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String taskId = sdf.format(new Date()) + "-" + UUID.randomUUID().toString().hashCode();
		logger.info("taskId: " + taskId);
		logger.info("isUpdate: " + isUpdate);
		logger.info("frontierDir: " + frontierDir);

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
		if (pp != null) {
			logger.info("pp: " + pp);
			/**
			 * public ProxyManager(List<HttpHost> proxys,String validUrl,String
			 * validContent,long maxLatency,int maxFailCount,long checkInterval,
			 * int checkBadTimes,boolean discardBadHost, int initCheckThreads,
			 * int minThreshold, long checkNewInterval, int expectedProxy,
			 * boolean addItself, Integer validFileSize, boolean doInitCheck,
			 * int maxTotalFailCount, double maxFailRatio){
			 */
			List<HttpHost> proxys = new ArrayList<>(ProxyDiscover.findProxys());
			ProxyManager pm = new ProxyManager(proxys, pp.validUrl, pp.validContent, pp.maxLatency, pp.maxFailCount,
					pp.checkInterval, pp.checkBadTimes, pp.discardBadHost, pp.initCheckThreads, pp.minThreshold,
					pp.checkNewInterval, pp.expectedProxy, !pp.excludeSelf, null, pp.doInitCheck, pp.maxTotalFailCount,
					pp.maxFailRatio);
			MyHttpRoutePlanner routePlanner = new MyHttpRoutePlanner(pm);
			fetcher.setRoutePlanner(routePlanner);
		}
		fetcher.init();

		msgReceiver = new CrawlTaskMsgReceiver(conAddr, jmxUrl, queueName, tasks);
		new Thread(msgReceiver).start();
		frontier = new FileFrontier(frontierDir, 10_000, 60_000);
		Archiver archiver = new MysqlArchiver();
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
		this.frontier.close();
		logger.info("I am stopped");

		super.finished();
	}

}

class ProxyParams {
	public String validUrl;
	public String validContent;
	public long maxLatency;
	public int maxFailCount;
	public long checkInterval;
	public int checkBadTimes;
	public boolean discardBadHost;
	public int initCheckThreads;
	public int minThreshold;
	public long checkNewInterval;
	public int expectedProxy;
	public boolean excludeSelf;
	public boolean doInitCheck;
	public int maxTotalFailCount;
	public double maxFailRatio;

	@Override
	public String toString() {
		return "validUrl: " + validUrl + "\n" + "validContent: " + validContent + "\n" + "maxLatency: " + maxLatency
				+ "\n" + "maxFailCount: " + maxFailCount + "\n" + "checkInterval: " + checkInterval + "\n"
				+ "checkBadTimes: " + checkBadTimes + "\n" + "discardBadHost: " + discardBadHost + "\n"
				+ "initCheckThreads: " + initCheckThreads + "\n" + "minThreshold: " + minThreshold + "\n"
				+ "checkNewInterval: " + checkNewInterval + "\n" + "expectedProxy: " + expectedProxy + "\n"
				+ "excludeSelf: " + excludeSelf + "\n" + "doInitCheck: " + doInitCheck + "\n" + "maxTotalFailCount: "
				+ maxTotalFailCount + "\n" + "maxFailRatio: " + maxFailRatio;
	}
}
