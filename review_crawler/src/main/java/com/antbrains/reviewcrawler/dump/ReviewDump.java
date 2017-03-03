package com.antbrains.reviewcrawler.dump;

import com.antbrains.mysqltool.PoolManager;
import com.antbrains.reviewcrawler.archiver.MysqlArchiver;

public class ReviewDump {
	public static void main(String[] args) throws Exception {
		if(args.length!=1){
			System.out.println("need 1 arg");
			System.exit(-1);
		}
		PoolManager.StartPool("conf", "ifeng");
		MysqlArchiver archiver=new MysqlArchiver();
		archiver.dumpReview(args[0], 1000);
	}

}
