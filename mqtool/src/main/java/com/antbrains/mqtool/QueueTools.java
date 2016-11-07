package com.antbrains.mqtool;

import javax.jms.TextMessage;

import org.apache.log4j.Logger;

public class QueueTools {
	protected static Logger logger=Logger.getLogger(QueueTools.class);
	
	public static boolean send( MqSender sender, String msg){
		return send(sender, msg, 60, 60_000);
	}
	public static boolean send( MqSender sender, String msg, int maxRetry, long interval){
		for(int i=0;i<maxRetry;i++){
			boolean res=sender.send(msg);
			if(res) return true;
			logger.warn("can't send msg, retry: "+(i+1));
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) { 
			}
		}
		logger.error("can't send msg: "+msg+" in "+maxRetry+" with interval "+interval);
		return false;
	}
	public static String receive(MqReceiver receiver, long timeOut) throws Exception{
		return receive(receiver, timeOut, 60, 60_000);
	}
	public static String receive(MqReceiver receiver, long timeOut, int maxRetry, long interval) throws Exception{
		Exception lastException=null;
		for(int i=0;i<maxRetry;i++){
			try {
				TextMessage tm = (TextMessage)receiver.receive(timeOut);
				if(tm==null) return null;
				return tm.getText();
			} catch (Exception e) {
				lastException=e;
			}			
		}
		throw lastException;
	}
}
