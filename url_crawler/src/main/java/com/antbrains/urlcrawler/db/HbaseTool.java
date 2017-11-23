package com.antbrains.urlcrawler.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.io.compress.Compression.Algorithm;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.apache.hadoop.hbase.util.Bytes;

public class HbaseTool {
	public static final String TB_WEBPAGE=".wp";
	public static final String TB_URLDB_UNCRAWLED=".uc";
	public static final String TB_URLDB_CRAWLING=".cl";
	public static final String TB_URLDB_SUCC=".sc";
	public static final String TB_URLDB_FAIL=".fl";
	
	
	public static final String CF = "cf";
	public static final String CF2 = "cf2";

	public static final byte[] CF_BT = Bytes.toBytes(CF);
	public static final byte[] CF2_BT = Bytes.toBytes(CF2);
	
	public static final String COL_WEBPAGE_URL = "url";
	public static final String COL_WEBPAGE_JSON="json";
	
	public static final String COL_URLDB_URL="url";
	public static final String COL_URLDB_FAIL_COUNT="fc";
	public static final String COL_URLDB_FAIL_REASON="fr";
	
	public static final byte[] COL_WEBPAGE_URL_BT = Bytes.toBytes(COL_WEBPAGE_URL);
	public static final byte[] COL_WEBPAGE_JSON_BT = Bytes.toBytes(COL_WEBPAGE_JSON);
	
	public static final byte[] COL_URLDB_URL_BT = Bytes.toBytes(COL_URLDB_URL);
	public static final byte[] COL_URLDB_FAIL_COUNT_BT = Bytes.toBytes(COL_URLDB_FAIL_COUNT);
	public static final byte[] COL_URLDB_FAIL_REASON_BT = Bytes.toBytes(COL_URLDB_FAIL_REASON);
	
	
	public static List<String> getRows(String dbName, String tableName, Connection conn, int count) throws IOException{
		ArrayList<String> result=new ArrayList<>(count);
		Table table=null;
		ResultScanner rs=null;
		try{
			table=conn.getTable(TableName.valueOf(dbName+tableName));
			Scan scan=new Scan();
			scan.addColumn(CF_BT, COL_URLDB_URL_BT);
			scan.setMaxResultSize(count);
			rs=table.getScanner(scan);
			for(Result  r:rs){
				if(r.isEmpty()) continue;
				byte[] bytes = r.getValue(CF_BT, COL_URLDB_URL_BT);
				if(bytes==null) continue;
				String url=Bytes.toString(bytes);
				result.add(url);
				if(result.size()>=count) break;
			}
			 
		}finally{
			if(rs!=null){
				rs.close();
			}
			if(table!=null){
				table.close();
			}
		}
		return result;
	}
	
	public static void delRows(String dbName, String tableName, Connection conn, Collection<String> urls) throws IOException{
		Table table=null;
		try{
			table=conn.getTable(TableName.valueOf(dbName+tableName));
			ArrayList<Delete> dels=new ArrayList<>(urls.size());
			for(String url:urls){  
				byte[]	key=DigestUtils.md5(url);
				Delete del=new Delete(key); 
				dels.add(del);
			}
			table.delete(dels);
		}finally{
		    if(table!=null){
		        table.close();
		    }
		}
	}
	
	public static List<CrawlTask> getFailedTasks(String dbName, Connection conn, int count, int maxFail) throws IOException{
		ArrayList<CrawlTask> result=new ArrayList<>(count);
		Table table=null;
		ResultScanner rs=null;
		try{
			table=conn.getTable(TableName.valueOf(dbName+TB_URLDB_FAIL));
			Scan scan=new Scan();
			scan.addFamily(CF_BT);
			scan.setMaxResultSize(count);
			FilterList list = new FilterList(FilterList.Operator.MUST_PASS_ALL);
			SingleColumnValueFilter filter1 = new SingleColumnValueFilter(
					CF_BT,
					COL_URLDB_FAIL_COUNT_BT,
					CompareOp.LESS,
					Bytes.toBytes(maxFail));
			list.addFilter(filter1);
			scan.setFilter(list);
			rs=table.getScanner(scan);
			for(Result  r:rs){
				if(r.isEmpty()) continue;
				byte[] bytes = r.getValue(CF_BT, COL_URLDB_URL_BT);
				if(bytes==null) continue;
				String url=Bytes.toString(bytes);
				CrawlTask task=new CrawlTask();
				result.add(task);
				task.crawlUrl=url;
				task.status=CrawlTask.STATUS_FAILED;
				
				bytes=r.getValue(CF_BT, COL_URLDB_FAIL_COUNT_BT);
				int fc=Bytes.toInt(bytes);
				task.failCount=fc;
				
				bytes=r.getValue(CF_BT, COL_URLDB_FAIL_REASON_BT);
				task.failReason=Bytes.toString(bytes);
				if(result.size()>=count) break;
			}
			 
		}finally{
			if(rs!=null){
				rs.close();
			}
			if(table!=null){
				table.close();
			}
		}
		return result;
	}
	
	public static void addFailed(String dbName, Connection conn, Collection<CrawlTask> tasks) throws IOException{
		Table table=null;
		try{
			table=conn.getTable(TableName.valueOf(dbName+TB_URLDB_FAIL));
			ArrayList<Put> puts=new ArrayList<>(tasks.size());
			for(CrawlTask task:tasks){ 
				String url=task.crawlUrl;
				byte[]	key=DigestUtils.md5(url);
				Put put=new Put(key);
				put.addColumn(CF_BT, COL_URLDB_URL_BT, Bytes.toBytes(url));
				put.addColumn(CF_BT, COL_URLDB_FAIL_COUNT_BT, Bytes.toBytes(task.failCount));
				put.addColumn(CF_BT, COL_URLDB_FAIL_REASON_BT, Bytes.toBytes(task.failReason));
				puts.add(put);
			}
			table.put(puts);
		}finally{
		    if(table!=null){
		        table.close();
		    }
		}
	}
	
	public static void addRows(String dbName, String tableName, Connection conn, Collection<String> urls) throws IOException{
		Table table=null;
		try{
			table=conn.getTable(TableName.valueOf(dbName+tableName));
			ArrayList<Put> puts=new ArrayList<>(urls.size());
			for(String url:urls){  
				byte[]	key=DigestUtils.md5(url);
				Put put=new Put(key);
				put.addColumn(CF_BT, COL_URLDB_URL_BT, Bytes.toBytes(url));
				puts.add(put);
			}
			table.put(puts);
		}finally{
		    if(table!=null){
		        table.close();
		    }
		}
	}
	
	public static void updateWebPage(String dbName, Connection conn, Collection<CrawlTask> tasks) throws IOException{
		Table table=null;
		try{
			table=conn.getTable(TableName.valueOf(dbName+TB_WEBPAGE));
			ArrayList<Put> puts=new ArrayList<>(tasks.size());
			for(CrawlTask task:tasks){
				if(task.status!=CrawlTask.STATUS_SUCC) continue;
				if(task.pk==null){
				    task.pk=task.crawlUrl;
				}
				String url=task.crawlUrl;
				String json=task.json;
				byte[]	key=DigestUtils.md5(task.pk);
				Put put=new Put(key);
				put.addColumn(CF_BT, COL_WEBPAGE_URL_BT, Bytes.toBytes(url));
				put.addColumn(CF_BT, COL_WEBPAGE_JSON_BT, Bytes.toBytes(json));
				puts.add(put);
			}
			table.put(puts);
		}finally{
		    if(table!=null){
		        table.close();
		    }
		}
	}
	
	public static void updateWebPage(String dbName, Connection conn, String pk, String url, String json) throws IOException{
		Table table=null;
		try{
			table=conn.getTable(TableName.valueOf(dbName+TB_WEBPAGE));
			if(pk==null) pk=url;
			byte[]	key=DigestUtils.md5(pk);
			Put put=new Put(key);
			put.addColumn(CF_BT, COL_WEBPAGE_URL_BT, Bytes.toBytes(url));
			put.addColumn(CF_BT, COL_WEBPAGE_JSON_BT, Bytes.toBytes(json));
			table.put(put);
		}finally{
		    if(table!=null){
		        table.close();
		    }
		}
	}
	
	public static String getJson(String dbName, Connection conn, String pk) throws IOException{
		Table table=null;
		try{
			table=conn.getTable(TableName.valueOf(dbName+TB_WEBPAGE));
			byte[]	key=DigestUtils.md5(pk);
			Get get=new Get(key);
			Result r=table.get(get);

			if (r.isEmpty())
				return null;

			byte[] bytes = r.getValue(CF_BT, COL_WEBPAGE_JSON_BT);
			if(bytes==null) return null;
			return Bytes.toString(bytes);
		}finally{
		    if(table!=null){
		        table.close();
		    }
		}
	}
	
	private static byte[][] calcSplit(int splitCount) {
		if (splitCount < 2 || splitCount > 255)
			throw new IllegalArgumentException("too much splits: " + splitCount);
		int min = Byte.MIN_VALUE;
		int max = Byte.MAX_VALUE;
		int avg = (max - min) / splitCount;
		if (avg == 0)
			return null;
		byte[][] result = new byte[splitCount - 1][];

		for (int i = 0; i < splitCount - 1; i++) {
			result[i] = new byte[] { (byte) ((i + 1) * avg + min) };
		}
		return result;
	}	
	
	public static boolean deleteTable(Admin admin, String tableName, boolean deleteExist) throws IOException {
		TableName tn=TableName.valueOf(tableName);
		if (admin.tableExists(tn)) {
			if (deleteExist) {
				System.out.println(
						"disable table: " + tableName + ". it may take a little bit long time, pleas wait patiently");
				admin.disableTable(tn);
				System.out.println("delete table: " + tableName);
				admin.deleteTable(tn);
			} else {
				System.out.println("don't delete: " + tableName);
			}
			return true;
		} else {
			System.out.println("table not exist: " + tableName);
			return false;
		}
	}

	public static void createTable(Admin admin, String tableName, BloomType bt, Algorithm compressAlgo,
			boolean blockCacheEnabled, byte[][] presplit, String... columnFamilys) throws IOException {
		HTableDescriptor desc = new HTableDescriptor(TableName.valueOf(tableName));
		for (String cf : columnFamilys) {
			System.out.println("add columnFamily: " + cf);
			HColumnDescriptor col = new HColumnDescriptor(Bytes.toBytes(cf));
			col.setMaxVersions(1);
			col.setBloomFilterType(bt);
			col.setCompressionType(compressAlgo);
			col.setBlockCacheEnabled(blockCacheEnabled);
			desc.addFamily(col);
		}
		System.out.println("creating table: " + tableName);
		admin.createTable(desc, presplit);
	}
	
	public static void main(String[] args) throws Exception{
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("h", "help", false, "print help");
		options.addOption("p", "port", true, "zookeeper client port");

		options.addOption("d", "deleteExist", true, "delete existed tables or not");

		options.addOption("r", "rsCount", true, "number of region server. used to presplit url_db");

		String zkQuorum = null;
		String zkPort = null;
		String helpStr = "HbaseTool hbase.zookeeper.quorum dbName";
		boolean deleteExist = false;
		int rsCount = 1;
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			HelpFormatter formatter = new HelpFormatter();
			if (line.hasOption("help")) {
				formatter.printHelp(helpStr, options);
				System.exit(0);
			}
			if (line.getArgs().length != 2) {
				formatter.printHelp(helpStr, options);
				System.exit(-1);
			}

			if (line.hasOption("port")) {
				zkPort = line.getOptionValue("port");
			}
			if (line.hasOption("d")) {
				if (line.getOptionValue("d").equalsIgnoreCase("true")) {
					deleteExist = true;
				}
			}

			if (line.hasOption("r")) {
				rsCount = Integer.valueOf(line.getOptionValue("r"));
			}

			args = line.getArgs();
			zkQuorum = args[0];

		} catch (ParseException exp) {
			System.out.println("Unexpected exception:" + exp.getMessage());
		}
		String dbName = args[1];
		Admin admin = null;
		Configuration myConf = HBaseConfiguration.create();
		myConf.set("hbase.zookeeper.quorum", zkQuorum);
		if (zkPort != null) {
			myConf.set("hbase.zookeeper.property.clientPort", zkPort);
		}
		System.out.println("deleteExist: " + deleteExist);
		System.out.println("rsCount: " + rsCount);
		byte[][] splits = calcSplit(rsCount);
		Connection conn =ConnectionFactory.createConnection(myConf);
		admin = conn.getAdmin();
		try {
			if (deleteExist) {
				deleteTable(admin, dbName + TB_WEBPAGE, true);
				createTable(admin, dbName + TB_WEBPAGE, BloomType.ROW, Algorithm.GZ, true, splits, CF);
	
				deleteTable(admin, dbName + TB_URLDB_UNCRAWLED, true);
				createTable(admin, dbName + TB_URLDB_UNCRAWLED, BloomType.NONE, Algorithm.NONE, true, splits, CF);

				deleteTable(admin, dbName + TB_URLDB_CRAWLING, true);
				createTable(admin, dbName + TB_URLDB_CRAWLING, BloomType.NONE, Algorithm.NONE, true, splits, CF);
				
				deleteTable(admin, dbName + TB_URLDB_SUCC, true);
				createTable(admin, dbName + TB_URLDB_SUCC, BloomType.NONE, Algorithm.NONE, true, splits, CF);
				
				deleteTable(admin, dbName + TB_URLDB_FAIL, true);
				createTable(admin, dbName + TB_URLDB_FAIL, BloomType.NONE, Algorithm.NONE, true, splits, CF);
			} else {
				boolean exist = deleteTable(admin, dbName + TB_WEBPAGE, false);
				if (!exist) {
					createTable(admin, dbName + TB_WEBPAGE, BloomType.ROW, Algorithm.GZ, true, splits, CF);
				}

				exist = deleteTable(admin, dbName + TB_URLDB_UNCRAWLED, false);
				if (!exist) {
					createTable(admin, dbName + TB_URLDB_UNCRAWLED, BloomType.NONE, Algorithm.NONE, true, splits, CF);
				}
				
				exist = deleteTable(admin, dbName + TB_URLDB_CRAWLING, false);
				if (!exist) {
					createTable(admin, dbName + TB_URLDB_CRAWLING, BloomType.NONE, Algorithm.NONE, true, splits, CF);
				}
				
				exist = deleteTable(admin, dbName + TB_URLDB_SUCC, false);
				if (!exist) {
					createTable(admin, dbName + TB_URLDB_SUCC, BloomType.NONE, Algorithm.NONE, true, splits, CF);
				}
				
				exist = deleteTable(admin, dbName + TB_URLDB_FAIL, false);
				if (!exist) {
					createTable(admin, dbName + TB_URLDB_FAIL, BloomType.NONE, Algorithm.NONE, true, splits, CF);
				}
			}

		} finally {
			if (admin != null) {
				admin.close();
			}
		}
	}

}
