package com.antbrains.urlcrawler.db;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader; 
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
 

public class ViewFailedTask {
	protected static Logger logger=Logger.getLogger(ViewFailedTask.class);
	public static void main(String[] args)  throws Exception{
		if(args.length<2){
			System.err.println("need at least 2 arg:  zk dbName [total] [maxFail]");
			System.exit(-1);
		}
		Configuration myConf = HBaseConfiguration.create();
		myConf.set("hbase.zookeeper.quorum", args[0]);
		String dbName=args[1];
		int total=1000;
		int maxFail=Integer.MAX_VALUE;
		if(args.length>2){
		    total=Integer.parseInt(args[2]);
		}
		if(args.length>3){
		    maxFail=Integer.parseInt(args[3]);
		}
		Gson gson=new Gson();
		Connection conn =ConnectionFactory.createConnection(myConf);
        List<CrawlTask>tasks=HbaseTool.getFailedTasks(dbName, conn, total, maxFail);
        for(CrawlTask task:tasks){
            System.out.println(gson.toJson(task));
        }
		conn.close();
	}

}
