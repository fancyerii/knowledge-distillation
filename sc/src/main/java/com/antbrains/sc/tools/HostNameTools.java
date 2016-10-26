package com.antbrains.sc.tools;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.log4j.Logger;

public class HostNameTools {
	protected static Logger logger=Logger.getLogger(HostNameTools.class);
	
	private static String hostName=null;
	public static String getHostName(){
		if(hostName!=null) return hostName;
		try {
			hostName= InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostName=UUID.randomUUID().toString();
			logger.warn("unknow hostname use uuid: "+hostName);
		}
		return hostName;
	}
	
	private static String pid=null;
	public static String getProcessId() {
		if(pid!=null) return pid;
		
	    final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
	    final int index = jvmName.indexOf('@');

	    if(index!=-1){
		    try {
		        pid = Long.toString(Long.parseLong(jvmName.substring(0, index)));
		    } catch (NumberFormatException e) {
		        // ignore
		    }
	    }
	    if(pid==null){
	    	pid=UUID.randomUUID().toString();
	    	logger.warn("can't get pid use uuid: "+pid);
	    }
	    return pid;
	}
}
