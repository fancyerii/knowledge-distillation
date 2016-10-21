package com.antbrains.ifengcrawler.dumper;

import java.sql.Timestamp;

import com.antbrains.mysqltool.PoolManager;
import com.antbrains.sc.archiver.DumpFilter;
import com.antbrains.sc.archiver.MysqlArchiver;

public class DumpHtml {

	public static void main(String[] args) throws Exception {
		if(args.length!=2){
			System.out.println("need 2 arg: dbName outputPath");
			System.exit(-1);
		}
		PoolManager.StartPool("conf", args[0]);

		MysqlArchiver archiver=new MysqlArchiver();
		archiver.dumpHtml(new DumpFilter(){
			@Override
			public boolean accept(String url, int depth, Timestamp lastVisitTime) {
				return depth==2;
			}
			
		}, args[1], 1000);
	}
	
}
