package com.antbrains.ifengcrawler.scheduler;

import org.apache.log4j.Logger;
 
import com.antbrains.sc.init.MysqlInit;

public class IfengInit {
	protected static Logger logger = Logger.getLogger(IfengInit.class);
	public static void main(String[] args) throws Exception {
		// put first page to queue
		MysqlInit.main(args);
	}

}
