package com.antbrains.rufodao.scheduler;

import org.apache.log4j.Logger;
 
import com.antbrains.sc.init.MysqlInit;

public class Init {
	protected static Logger logger = Logger.getLogger(Init.class);
	public static void main(String[] args) throws Exception {
		// put first page to queue
		MysqlInit.main(args);
	}

}
