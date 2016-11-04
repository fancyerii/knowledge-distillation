package com.antbrains.sc.scheduler;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.jms.Session;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import com.antbrains.mqtool.ActiveMqSender;
import com.antbrains.mqtool.ActiveMqTools;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;
import com.antbrains.mysqltool.DBUtils;
import com.antbrains.mysqltool.PoolManager;
import com.antbrains.sc.archiver.Constants;
import com.antbrains.sc.archiver.MysqlArchiver;
import com.antbrains.sc.data.CrawlTask;
import com.antbrains.sc.tools.GlobalConstants;
import com.antbrains.sc.tools.StopService;
import com.antbrains.sc.tools.StopableWorker;
import com.google.gson.Gson;

public class UnfinishedScheduler extends StopableWorker {
	protected static Logger logger = Logger.getLogger(UnfinishedScheduler.class);
	public static final int DEFAULT_MAX_QUEUE_SIZE = 50_000;
	public static final int DEFAULT_BATCH_SIZE = 10_000;
	public static final int DEFAULT_MAX_EMPTY_COUNT = 1_000_000;
	public static final int DEFAULT_MAX_FAIL_COUNT = 3;
	private static final int STOP_PORT_SCHEDULER = 23432;

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("h", "help", false, "print help");
		options.addOption("maxQueueSize", true, "maxQueueSize, default " + DEFAULT_MAX_QUEUE_SIZE);
		options.addOption("batchSize", true, "batchSize, default " + DEFAULT_BATCH_SIZE);
		options.addOption("maxEmptyCount", true, "maxEmptyCount, default " + DEFAULT_MAX_EMPTY_COUNT);
		options.addOption("maxFailCount", true, "maxFailCount, defualt " + DEFAULT_MAX_FAIL_COUNT);
		options.addOption("stopPort", true, "stop listen port, default " + STOP_PORT_SCHEDULER);
		CommandLine line = parser.parse(options, args);
		HelpFormatter formatter = new HelpFormatter();
		String helpStr = "UnfinishedScheduler amqConnStr queueName";
		args = line.getArgs();
		if (args.length != 2) {
			formatter.printHelp(helpStr, options);
			System.exit(-1);
		}
		if (line.hasOption("help")) {
			formatter.printHelp(helpStr, options);
			System.exit(0);
		}
		int stopPort = STOP_PORT_SCHEDULER;
		int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
		int batchSize = DEFAULT_BATCH_SIZE;
		int maxEmptyCount = DEFAULT_MAX_EMPTY_COUNT;
		int maxFailCount = DEFAULT_MAX_FAIL_COUNT;
		if (line.hasOption("maxQueueSize")) {
			maxQueueSize = Integer.valueOf(line.getOptionValue("maxQueueSize"));
		}
		if (line.hasOption("batchSize")) {
			batchSize = Integer.valueOf(line.getOptionValue("batchSize"));
		}
		if (line.hasOption("maxEmptyCount")) {
			maxEmptyCount = Integer.valueOf(line.getOptionValue("maxEmptyCount"));
		}
		if (line.hasOption("maxFailCount")) {
			maxFailCount = Integer.valueOf(line.getOptionValue("maxFailCount"));
		}
		if (line.hasOption("stopPort")) {
			stopPort = Integer.valueOf(line.getOptionValue("stopPort"));
		}
		UnfinishedScheduler scheduler = new UnfinishedScheduler(args[0], maxQueueSize, batchSize, maxEmptyCount,
				args[1], maxFailCount);
		StopService ss = new StopService(stopPort, scheduler, 0);
		ss.start();
		scheduler.doSchedule();
	}

	private MqToolsInterface mqtools;
	private MqSender sender;
	private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
	private int batchSize = DEFAULT_BATCH_SIZE;
	private int maxEmptyCount = DEFAULT_MAX_EMPTY_COUNT;
	private int maxFailCount = DEFAULT_MAX_FAIL_COUNT;
	
	private String dbName;

	private MysqlArchiver mysqlArchiver=new MysqlArchiver();
	public UnfinishedScheduler(String amqConnStr, int maxQueueSize, int batchSize, int maxEmptyCount, String dbName,
			int maxFailCount) {
		logger.info("amqConnStr: " + amqConnStr);
		logger.info("maxQueueSize: " + maxQueueSize);
		logger.info("batchSize: " + batchSize);
		logger.info("maxEmptyCount: " + maxEmptyCount);
		logger.info("maxFailCount: " + maxFailCount);
		logger.info("dbName: " + dbName);

		this.maxQueueSize = maxQueueSize;
		this.batchSize = batchSize;
		this.maxEmptyCount = maxEmptyCount;
		this.dbName = dbName;
		this.maxFailCount = maxFailCount;

		mqtools = new ActiveMqTools(amqConnStr);
		if (!mqtools.init()) {
			throw new IllegalArgumentException("can't connect to: " + amqConnStr);
		}
		sender = mqtools.getMqSender(dbName, Session.AUTO_ACKNOWLEDGE);
		if (!sender.init(ActiveMqSender.PERSISTENT)) {
			throw new IllegalArgumentException("can't getMqSender: " + amqConnStr);
		}
		
		PoolManager.StartPool("conf", dbName);
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
	}

	private List<CrawlTask> getTasks(int batchSize) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet res = null;
		List<CrawlTask> tasks = new ArrayList<>();
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement(
					"select url,failcount,priority,depth,id,redirectedUrl from unfinished where failcount<= "
							+ this.maxFailCount + " order by failcount,priority limit " + batchSize);
			res = pstmt.executeQuery();
			while (res.next()) {
				CrawlTask ct = new CrawlTask();
				ct.url = res.getString(1);
				ct.failCount = res.getInt(2);
				ct.priority = res.getInt(3);
				ct.depth = res.getInt(4);
				ct.id = res.getInt(5);
				ct.redirectedUrl = res.getString(6);
				tasks.add(ct);
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
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, res);
		}

		return tasks;
	}

	private void deleteTasks(List<CrawlTask> tasks) {
		if (tasks.size() == 0)
			return;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement("delete from unfinished where id=?");
			for (CrawlTask task : tasks) {
				pstmt.clearParameters();
				pstmt.setLong(1, task.id);
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, null);
		}
	}

	private long lastUpdate=-1;
	private void updateStatus(){
		if(System.currentTimeMillis()-lastUpdate>Constants.UPDATE_COMPONENT_STATUS_INTERVAL){
			if(this.hasTask){
				this.mysqlArchiver.updateComponentStatus(Constants.COMPONENT_SCHEDULER, Constants.STATUS_HASTASK);
			}else{
				this.mysqlArchiver.updateComponentStatus(Constants.COMPONENT_SCHEDULER, Constants.STATUS_NOTASK);
			}
			lastUpdate=System.currentTimeMillis();
		}
	}
	private boolean hasTask=true;
	public void doSchedule() {
		int emptyCount = 0;
		Gson gson = new Gson();
		PoolManager.StartPool("conf", dbName);
		while (!bStop) {
			this.updateStatus();
			// get queue size
			long queueSize = sender.getQueueSize();
			if (queueSize >= maxQueueSize) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					
				}
				hasTask=true;
				continue;
			}

			List<CrawlTask> tasks = getTasks(batchSize);
			hasTask=!tasks.isEmpty();
			if (tasks.size() == 0) {
				emptyCount++;
				if (emptyCount >= maxEmptyCount) {
					logger.info("no tasks");
					//bStop = true;
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {

				}
			} else {
				emptyCount = 0;
			}
			for (CrawlTask task : tasks) {
				String s = gson.toJson(task);
				this.sender.send(s);
				logger.info("send: " + task.url + "\t" + task.depth);
			}

			// TODO uncomment after DEBUG
			deleteTasks(tasks);
		}
		logger.info("stop");
		super.finished();
		this.close();
	}
}
