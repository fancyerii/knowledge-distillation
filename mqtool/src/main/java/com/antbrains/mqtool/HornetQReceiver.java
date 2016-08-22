package com.antbrains.mqtool;

import java.io.IOException;
import java.util.HashMap;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;
import org.hornetq.api.core.management.ObjectNameBuilder;
import org.hornetq.api.jms.management.JMSQueueControl;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class HornetQReceiver implements MqReceiver {
	private static Logger logger = Logger.getLogger(HornetQReceiver.class);
	private QueueSession session = null;
	private String queueName = null;
	private QueueReceiver receiver = null;
	private Queue receiverQueue = null;
	private long failCount = 0;
	private String jmxUrl;

	public HornetQReceiver() {
	}

	public HornetQReceiver(QueueSession session, String queueName, String jmxUrl) {
		this.session = session;
		this.queueName = queueName;
		this.jmxUrl = jmxUrl;
	}

	@Override
	public boolean init() {
		// TODO Auto-generated method stub
		if (session == null || queueName == null || queueName.isEmpty()) {
			logger.fatal("session or queuename must not be null or empty");
			return false;
		}
		try {
			receiverQueue = this.session.createQueue(queueName);
			receiver = this.session.createReceiver(receiverQueue);
		} catch (JMSException e) {
			logger.fatal(e.getMessage(), e);
		}
		this.failCount = 0;
		return true;
	}

	@Override
	public Message receive() throws JMSException {
		// TODO Auto-generated method stub
		if (this.receiver == null) {
			return null;
		}
		try {
			return this.receiver.receive();
		} catch (JMSException e) {
			logger.error(e.getMessage(), e);
			failCount++;
			if (failCount > 100) {
				logger.fatal("mq get message failed 100 times,thread will stop");
				throw new JMSException("fail 100 times");
			}
			try {
				Thread.sleep(2000);
			} catch (Exception e1) {
				logger.warn(e1.getMessage(), e1);
			}
		}
		return null;
	}

	@Override
	public Message receive(long timeout) throws JMSException {
		// TODO Auto-generated method stub
		if (this.receiver == null) {
			return null;
		}
		try {
			return this.receiver.receive(timeout);
		} catch (JMSException e) {
			logger.error(e.getMessage(), e);
			failCount++;
			if (failCount > 100) {
				logger.fatal("mq get message failed 100 times,thread will stop");
				throw new JMSException("fail 100 times");
			}
			try {
				Thread.sleep(2000);
			} catch (Exception e1) {
				logger.warn(e1.getMessage(), e1);
			}
		}
		return null;
	}

	private JsonParser parser = new JsonParser();

	@Override
	public long getQueueSize() {
		// TODO Auto-generated method stub
		if (session == null) {
			return -1;
		}
		JMXConnector connector = null;
		try {
			String JMX_URL = "service:jmx:rmi:///jndi/rmi://" + jmxUrl + "/jmxrmi";
			ObjectName on = ObjectNameBuilder.DEFAULT.getJMSQueueObjectName(queueName);
			connector = JMXConnectorFactory.connect(new JMXServiceURL(JMX_URL), new HashMap<String, Object>());
			MBeanServerConnection mbsc = connector.getMBeanServerConnection();
			JMSQueueControl queueControl = MBeanServerInvocationHandler.newProxyInstance(mbsc, on,
					JMSQueueControl.class,

					false);

			String counters = queueControl.listMessageCounter();
			return parser.parse(counters).getAsJsonObject().get("messageCount").getAsLong();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return -1;
		} finally {
			if (connector != null) {
				try {
					connector.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		try {
			if (receiver != null) {
				receiver.close();
			}
			if (session != null) {
				session.close();
			}
		} catch (JMSException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
