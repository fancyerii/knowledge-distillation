package com.antbrains.mqtool;

import java.io.IOException;
import java.util.HashMap;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;
import org.hornetq.api.core.management.ObjectNameBuilder;
import org.hornetq.api.jms.management.JMSQueueControl;

import com.google.gson.JsonParser;

public class HornetQSender implements MqSender {
	public static int PERSISTENT = DeliveryMode.PERSISTENT;
	public static int NON_PERSISTENT = DeliveryMode.NON_PERSISTENT;

	private static Logger logger = Logger.getLogger(HornetQSender.class);
	private QueueSession session = null;
	private String queueName = null;
	private QueueSender sender = null;
	private Queue senderQueue = null;
	private String jmxUrl;

	public HornetQSender() {
	}

	public HornetQSender(QueueSession session, String queueName, String jmxUrl) {
		this.session = session;
		this.queueName = queueName;
		this.jmxUrl = jmxUrl;
	}

	@Override
	public boolean init(int persitentType) {
		if (this.session == null || this.queueName == null || this.queueName.isEmpty()) {
			logger.fatal("session or queueName must not be null or empty");
			return false;
		}
		try {
			// create queue
			senderQueue = this.session.createQueue(this.queueName);
			sender = this.session.createSender(senderQueue);
			sender.setDeliveryMode(persitentType);
		} catch (Exception e) {
			logger.fatal("init error");
			logger.fatal(e.getMessage(), e);
			return false;
		}
		return true;
	}

	@Override
	public boolean send(String msg) {
		if (msg == null || sender == null) {
			return false;
		}
		try {
			TextMessage tMsg = this.session.createTextMessage(msg);
			this.sender.send(tMsg);
		} catch (JMSException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	public boolean send(Message msg) {
		if (msg == null || sender == null) {
			logger.warn("msg is null");
			return false;
		}
		try {
			this.sender.send(msg);
			return true;
		} catch (Exception e) {
			logger.error("send msg error");
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean send(Object obj) {
		if (obj instanceof Message) {
			return send((Message) obj);
		}
		return false;
	}

	private JsonParser parser = new JsonParser();

	@Override
	public long getQueueSize() {
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
		try {
			if (sender != null) {
				sender.close();
			}
			if (session != null) {
				session.close();
			}

		} catch (JMSException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/* getter and setter */
	public QueueSession getSession() {
		return session;
	}

	public void setSession(QueueSession session) {
		this.session = session;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public QueueSender getSender() {
		return sender;
	}

	public void setSender(QueueSender sender) {
		this.sender = sender;
	}

	public Queue getSenderQueue() {
		return senderQueue;
	}

	public void setSenderQueue(Queue senderQueue) {
		this.senderQueue = senderQueue;
	}

	@Override
	public boolean send(String msg, int prior) {
		if (msg == null || sender == null) {
			return false;
		}
		try {
			TextMessage tMsg = this.session.createTextMessage(msg);
			tMsg.setJMSPriority(prior);
			this.sender.send(tMsg);
		} catch (JMSException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	@Override
	public void commit() throws JMSException {
		this.session.commit();
	}

}
