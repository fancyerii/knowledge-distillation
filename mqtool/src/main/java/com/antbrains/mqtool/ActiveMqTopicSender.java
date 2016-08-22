package com.antbrains.mqtool;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import org.apache.log4j.Logger;

public class ActiveMqTopicSender implements MqSender {
	private static Logger logger = Logger.getLogger(ActiveMqTopicSender.class);
	public static int PERSISTENT = DeliveryMode.PERSISTENT;
	public static int NON_PERSISTENT = DeliveryMode.NON_PERSISTENT;

	private TopicSession session = null;
	private String topicName = null;
	private Topic topic = null;
	private TopicPublisher sender = null;

	public ActiveMqTopicSender() {
	}

	public ActiveMqTopicSender(TopicSession session, String topicName) {
		this.session = session;
		this.topicName = topicName;
	}

	@Override
	public boolean init(int persitentType) {
		if (this.session == null || this.topicName == null || this.topicName.isEmpty()) {
			logger.fatal("session or topicname must not be null");
			return false;
		}
		try {
			topic = this.session.createTopic(this.topicName);
			this.sender = this.session.createPublisher(topic);
			this.sender.setDeliveryMode(persitentType);
			return true;
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean send(String msg) {
		if (this.sender == null || msg == null) {
			return false;
		}
		try {
			TextMessage tMsg = this.session.createTextMessage(msg);
			this.sender.publish(tMsg);
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean send(Object obj) {
		if (obj == null) {
			return false;
		}
		try {
			if (obj instanceof Message) {
				this.sender.publish((Message) obj);
			}
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public long getQueueSize() {
		return 0;
	}

	@Override
	public void destroy() {
		try {
			if (this.session != null) {
				this.session.close();
			}
			if (this.sender != null) {
				this.sender.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/* getter and setter */
	public TopicSession getSession() {
		return session;
	}

	public void setSession(TopicSession session) {
		this.session = session;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	@Override
	public boolean send(String msg, int prior) {
		if (this.sender == null || msg == null) {
			return false;
		}
		try {
			TextMessage tMsg = this.session.createTextMessage(msg);
			tMsg.setJMSPriority(prior);
			this.sender.publish(tMsg);
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public void commit() throws JMSException {
		this.session.commit();
	}

}
