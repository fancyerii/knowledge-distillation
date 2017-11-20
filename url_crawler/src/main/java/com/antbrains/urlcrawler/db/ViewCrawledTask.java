package com.antbrains.urlcrawler.db;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader; 
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
 

public class ViewCrawledTask {
	protected static Logger logger=Logger.getLogger(ViewCrawledTask.class);
	public static void main(String[] args)  throws Exception{
		if(args.length<3){
			System.err.println("need 3 arg:  zk dbName key ...");
			System.exit(-1);
		}
		Configuration myConf = HBaseConfiguration.create();
		myConf.set("hbase.zookeeper.quorum", args[0]);
		String dbName=args[1];
		Connection conn =ConnectionFactory.createConnection(myConf);
		for(int i=2;i<args.length;i++){
		    String json=HbaseTool.getJson(dbName, conn, args[i]);
		    System.out.println(args[i]+"\t"+json);
		}
		conn.close();
	}

}
