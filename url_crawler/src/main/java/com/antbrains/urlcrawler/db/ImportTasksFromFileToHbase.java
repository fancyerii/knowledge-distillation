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
 

public class ImportTasksFromFileToHbase {
	protected static Logger logger=Logger.getLogger(ImportTasksFromFileToHbase.class);
	public static void main(String[] args)  throws Exception{
		if(args.length!=3){
			System.err.println("need 3 arg: urlFile zk dbName");
			System.exit(-1);
		}
		Configuration myConf = HBaseConfiguration.create();
		myConf.set("hbase.zookeeper.quorum", args[1]);
 
		Connection conn =ConnectionFactory.createConnection(myConf);
		BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(args[0]),"UTF8"));
		String line;
		int lineNumber=0; 
		ArrayList<String> tasks=new ArrayList<>();
		String dbName=args[2];
		while((line=br.readLine())!=null){
            lineNumber++;
            if(lineNumber%10000==0){
                logger.info("lineNumber: "+lineNumber);
            }
            line=line.trim();
            if(line.isEmpty()) continue;
            tasks.add(line);
            if(tasks.size()>1000){
                HbaseTool.addRows(dbName, HbaseTool.TB_URLDB_UNCRAWLED, conn, tasks);
                tasks.clear();
            }
		}
		if(tasks.size()>0){
			HbaseTool.addRows(dbName, HbaseTool.TB_URLDB_UNCRAWLED, conn, tasks);
		}
		br.close();
		conn.close();
	}

}
