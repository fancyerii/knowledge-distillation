package com.antbrains.mqtool;

public interface MqReceiver {
	public boolean init();

	public Object receive() throws Exception;

	public Object receive(long timeout) throws Exception;

	public long getQueueSize();

	public void destroy();
}
