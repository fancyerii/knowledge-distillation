package com.antbrains.mqtool;

import javax.jms.JMSException;

public interface MqSender {
	public boolean send(String msg);

	public boolean send(String msg, int prior);

	public boolean send(Object obj);

	public long getQueueSize();

	public void destroy();

	public boolean init(int persitentType);

	public void commit() throws JMSException;
}
