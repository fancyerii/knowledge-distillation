package com.antbrains.sc.scheduler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Session;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.antbrains.mqtool.ActiveMqSender;
import com.antbrains.mqtool.ActiveMqTools;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;
import com.antbrains.mqtool.QueueTools;
import com.antbrains.mysqltool.PoolManager;
import com.antbrains.sc.archiver.Constants;
import com.antbrains.sc.archiver.MysqlArchiver;
import com.antbrains.sc.data.CrawlTask;
import com.antbrains.sc.tools.ByteArrayWrapper;
import com.antbrains.sc.tools.GlobalConstants;
import com.antbrains.sc.tools.StopService;
import com.antbrains.sc.tools.StopableWorker;
import com.google.gson.Gson;

public class FileUnfinishedScheduler extends StopableWorker {
	protected static Logger logger = Logger.getLogger(FileUnfinishedScheduler.class);
	public static final int DEFAULT_MAX_QUEUE_SIZE = 50_000;
	public static final int DEFAULT_BATCH_SIZE = 10_000;
	public static final int DEFAULT_MAX_EMPTY_COUNT = 1_000_000;
	public static final int DEFAULT_MAX_ENTRIES_IN_CACHE = 1_000_000;

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("h", "help", false, "print help");
		options.addOption("maxQueueSize", true, "maxQueueSize, default " + DEFAULT_MAX_QUEUE_SIZE);
		options.addOption("batchSize", true, "batchSize, default " + DEFAULT_BATCH_SIZE);
		options.addOption("maxEmptyCount", true, "maxEmptyCount, default " + DEFAULT_MAX_EMPTY_COUNT);

		options.addOption("maxEntriesLocalHeap", true, "maxEntriesLocalHeap, default " + DEFAULT_MAX_ENTRIES_IN_CACHE);

		CommandLine line = parser.parse(options, args);
		HelpFormatter formatter = new HelpFormatter();
		String helpStr = "UnfinishedScheduler conAddr jmxUrl queueName fileDir stopPort";
		args = line.getArgs();
		if (args.length != 5) {
			formatter.printHelp(helpStr, options);
			System.exit(-1);
		}
		if (line.hasOption("help")) {
			formatter.printHelp(helpStr, options);
			System.exit(0);
		}

		int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
		int batchSize = DEFAULT_BATCH_SIZE;
		int maxEmptyCount = DEFAULT_MAX_EMPTY_COUNT;
		int maxEntriesLocalHeap = DEFAULT_MAX_ENTRIES_IN_CACHE;
		if (line.hasOption("maxQueueSize")) {
			maxQueueSize = Integer.valueOf(line.getOptionValue("maxQueueSize"));
		}
		if (line.hasOption("batchSize")) {
			batchSize = Integer.valueOf(line.getOptionValue("batchSize"));
		}
		if (line.hasOption("maxEmptyCount")) {
			maxEmptyCount = Integer.valueOf(line.getOptionValue("maxEmptyCount"));
		}
		if (line.hasOption("maxEntriesLocalHeap")) {
			maxEntriesLocalHeap = Integer.valueOf(line.getOptionValue("maxEntriesLocalHeap"));
		}
		FileUnfinishedScheduler scheduler = new FileUnfinishedScheduler(args[0], args[1], maxQueueSize, batchSize,
				maxEmptyCount, args[2], args[3], maxEntriesLocalHeap);
		StopService ss = new StopService(Integer.valueOf(args[4]), scheduler, 0);
		ss.start();
		scheduler.doSchedule();
	}

	private MqToolsInterface mqtools;
	private MqSender sender;
	private MysqlArchiver mysqlArchiver;
	private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
	private int batchSize = DEFAULT_BATCH_SIZE;
	private int maxEmptyCount = DEFAULT_MAX_EMPTY_COUNT;

	private String dbName;
	private File dir;
	CacheManager manager = CacheManager.create();
	Cache cache;

	public FileUnfinishedScheduler(String conAddr, String jmxUrl, int maxQueueSize, int batchSize, int maxEmptyCount,
			String dbName, String fileDir, int maxEntriesLocalHeap) {
		logger.info("conAddr: " + conAddr);
		logger.info("jmxUrl: " + jmxUrl);

		logger.info("maxQueueSize: " + maxQueueSize);
		logger.info("batchSize: " + batchSize);
		logger.info("maxEmptyCount: " + maxEmptyCount);
		logger.info("dbName: " + dbName);
		logger.info("fileDir: " + fileDir);
		logger.info("maxEntriesLocalHeap: " + maxEntriesLocalHeap);

		// Create a singleton CacheManager using defaults

		// Create a Cache specifying its configuration.
		cache = new Cache(new CacheConfiguration("keywordsCache", maxEntriesLocalHeap)
				.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU).eternal(true).timeToLiveSeconds(0)
				.timeToIdleSeconds(0).diskExpiryThreadIntervalSeconds(0)
				.persistence(new PersistenceConfiguration().strategy(Strategy.NONE)));
		manager.addCache(cache);

		this.maxQueueSize = maxQueueSize;
		this.batchSize = batchSize;
		this.maxEmptyCount = maxEmptyCount;
		this.dbName = dbName;
		this.dir = new File(fileDir);
		if (!dir.exists()) {
			throw new IllegalArgumentException(fileDir + " does not exist");
		}
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException(fileDir + " is not a dir");
		}

		mqtools = new HornetQTools(conAddr, jmxUrl);
		if (!mqtools.init()) {
			throw new IllegalArgumentException("can't connect to: " + conAddr + "\t" + jmxUrl);
		}
		sender = mqtools.getMqSender(dbName, Session.AUTO_ACKNOWLEDGE);
		if (!sender.init(ActiveMqSender.PERSISTENT)) {
			throw new IllegalArgumentException("can't getMqSender: " + conAddr + "\t" + jmxUrl);
		}
		PoolManager.StartPool("conf", dbName);
		mysqlArchiver=new MysqlArchiver();
	}

	public void close() {
		if (sender != null) {
			try {
				sender.destroy();
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
		this.manager.shutdown();
	}
	
	private boolean hasTask(){
		for (File f : this.dir.listFiles()) {
			if (f.getName().endsWith(".txt")||f.getName().endsWith(".txt.bak")) {
				if(f.length()>0) return true;
			}
		}
		return false;
	}

	private String getAFile() {
		List<String> paths = new ArrayList<>();
		for (File f : this.dir.listFiles()) {
			if (f.getName().endsWith(".txt")) {
				paths.add(f.getAbsolutePath());
			}
		}
		Collections.sort(paths);
		if (paths.size() == 0)
			return null;
		return paths.iterator().next();
	}

	private Gson gson = new Gson();
	private String currFile;
	private boolean succ;

	private boolean urlExist(String url) {
		byte[] md5 = DigestUtils.md5(url);
		ByteArrayWrapper key = new ByteArrayWrapper(md5);
		if (cache.get(key) == null) {
			cache.put(new Element(key, null));
			return false;
		} else {
			return true;
		}
	}

	private List<CrawlTask> getTasks(int batchSize) {
		List<CrawlTask> tasks = new ArrayList<>();
		currFile = this.getAFile();
		if (currFile == null)
			return tasks;
		logger.info("read: " + currFile);
		BufferedReader br = null;
		succ = false;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(currFile), "UTF8"));
			String line;
			while ((line = br.readLine()) != null) {
				CrawlTask ct = gson.fromJson(line, CrawlTask.class);
				if(ct.url==null){
					logger.info("clear cache for null url: "+line);
					logger.info("before clear: "+cache.getSize());
					cache.removeAll();
					logger.info("after clear: "+cache.getSize());
					continue;
				}
				String url = ct.url;
				if (url.equals("")) {
					logger.warn("invalid: " + url);
					continue;
				}
				if (this.urlExist(url)) {
					logger.info("duplicate: " + url);
				} else {
					tasks.add(ct);
				}

			}

			succ = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}

		Collections.sort(tasks, new Comparator<CrawlTask>() {

			@Override
			public int compare(CrawlTask o1, CrawlTask o2) {
				int cmp = o1.failCount - o2.failCount;
				if (cmp != 0)
					return cmp;
				cmp = o2.priority - o1.priority;
				return cmp;
			}

		});

		return tasks;
	}

	private void deleteTasks() {
		if (succ && this.currFile != null) {
			logger.info("delete: " + this.currFile);
			new File(this.currFile).delete();
			this.currFile = null;
		}
	}
	
	private long lastUpdate=-1;
	private void updateStatus(){
		if(System.currentTimeMillis()-lastUpdate>Constants.UPDATE_COMPONENT_STATUS_INTERVAL){
			if(this.hasTask()){
				this.mysqlArchiver.updateComponentStatus(Constants.COMPONENT_SCHEDULER, Constants.STATUS_HASTASK);
			}else{
				this.mysqlArchiver.updateComponentStatus(Constants.COMPONENT_SCHEDULER, Constants.STATUS_NOTASK);
			}
			lastUpdate=System.currentTimeMillis();
		}
	}

	public void doSchedule() {
		int emptyCount = 0;
		Gson gson = new Gson();
		while (!bStop) {
			this.updateStatus();
			// get queue size
			long queueSize = sender.getQueueSize();
			if (queueSize == -1) {
				logger.warn("can't get queuesize");
				break;
			}
			logger.info("queueSize: " + queueSize);
			if (queueSize >= maxQueueSize) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {

				}
				continue;
			}

			List<CrawlTask> tasks = getTasks(batchSize);
			if (tasks.size() == 0) {
				emptyCount++;
				if (emptyCount >= maxEmptyCount) {
					logger.info("no tasks");
					bStop = true;
					break;
				}
				try {
					Thread.sleep(30_000);
				} catch (InterruptedException e) {

				}
			} else {
				emptyCount = 0;
			}
			int count = 0;
			for (CrawlTask task : tasks) {
				String s = gson.toJson(task); 
				boolean res=QueueTools.send(sender, s);
				if(!res){
					logger.error("can't send msg: "+s);
					return;
				}
				count++;
//				if (count % 10_000 == 0)
//					try {
//						sender.commit();
//					} catch (JMSException e) {
//						logger.error(e.getMessage(), e);
//					}
				logger.debug("send: "+task.url+"\t"+task.depth);
			}
//			try {
//				sender.commit();
//			} catch (JMSException e) {
//				logger.error(e.getMessage(), e);
//			}
			deleteTasks();
			logger.info("sent: " + count);
		}
		logger.info("stop");
		super.finished();
		this.close();
	}
}
