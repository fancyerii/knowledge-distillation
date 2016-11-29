package com.github.fancyerii.takungpaocrawler.dumper;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.antbrains.httpclientfetcher.FileTools;
import com.antbrains.mysqltool.PoolManager;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.DumpFilter;
import com.antbrains.sc.archiver.MysqlArchiver;
import com.antbrains.sc.init.MysqlInit;

public class DumpReviewUrl {
	protected static Logger logger = Logger.getLogger(DumpReviewUrl.class);
	MysqlArchiver archiver = new MysqlArchiver();
	private String outDir;
	private long interval;
	boolean dumpAll;
	public static final String LAST_ID = "/.last_id";

	public DumpReviewUrl(String outDir, long interval, boolean dumpAll) {
		this.outDir = outDir;
		this.interval = interval;
		this.dumpAll = dumpAll;
		logger.info("outDir: " + outDir);
		logger.info("interval: " + interval);
		logger.info("dumpAll: " + dumpAll);
	}

	private int readLastId() {
		try {
			String s = FileTools.readFile(outDir + LAST_ID);
			return Integer.valueOf(s);
		} catch (Exception e) {
			return -1;
		}
	}

	public void writeLastId(int id) {
		try {
			FileTools.writeFile(outDir + LAST_ID, "" + id);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private int maxId;

	private String getArticleType(NekoHtmlParser p) {
		Node div = p.selectSingleNode("//DIV[contains(@class,'js_crumb')]");
		if (div == null)
			return null;
		NodeList aList = p.selectNodes("./A", div);
		if (aList == null || aList.getLength() < 2) {
			return null;
		}
		String type = aList.item(1).getTextContent().trim();

		return type;
	}

	public void dump() {
		while (true) {
			final int lastId = this.readLastId();
			logger.info("lastId: " + lastId);
			maxId = lastId;
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
			Date d = new Date();
			final String fileName = sdf.format(d);
			try {
				archiver.dumpUrl(lastId, new DumpFilter() {
					@Override
					public boolean accept(int id, String url, int depth, Timestamp lastVisitTime, String content) {
						maxId = Math.max(maxId, id);
						if (content == null)
							return false;
						NekoHtmlParser parser = new NekoHtmlParser();
						try {
							parser.load(content, "UTF8");
						} catch (Exception e) {
							return false;
						}
						String type = getArticleType(parser);
						if (!"资讯".equals(type))
							return false;
						return depth == 2;
					}
				}, this.outDir + "/" + fileName, 10000);
				File f = new File(this.outDir + "/" + fileName);
				if (f.length() > 0) {
					logger.info("rename to: " + this.outDir + "/" + fileName + ".txt");
					f.renameTo(new File(this.outDir + "/" + fileName + ".txt"));
				} else {
					logger.info("remove empty: " + f.getAbsolutePath());
					f.delete();
				}
				this.writeLastId(maxId);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			dumpAll = false;
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
			}
		}

	}

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.out.println("need 4 arg: dbName outputPath dumpInterval dumpAll[boolean]");
			System.exit(-1);
		}
		PoolManager.StartPool("conf", args[0]);
		String outPath = args[1];
		long dumpInterval = MysqlInit.parseInterval(args[2]);
		boolean dumpAll = Boolean.parseBoolean(args[3]);
		new File(outPath).mkdirs();
		DumpReviewUrl dumpHtml = new DumpReviewUrl(outPath, dumpInterval, dumpAll);
		dumpHtml.dump();

	}

}
