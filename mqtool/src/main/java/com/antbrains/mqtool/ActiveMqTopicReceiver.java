package com.antbrains.mqtool;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.apache.log4j.Logger;

public class ActiveMqTopicReceiver implements MqReceiver {

	private static Logger logger = Logger.getLogger(ActiveMqTopicReceiver.class);
	private TopicSession session = null;
	private String topicName = null;
	private Topic topic = null;
	private TopicSubscriber receiver = null;
	private int failCount = 0;

	public ActiveMqTopicReceiver() {
	}

	public ActiveMqTopicReceiver(TopicSession session, String topicName) {
		this.session = session;
		this.topicName = topicName;
	}

	@Override
	public boolean init() {
		// TODO Auto-generated method stub
		if (session == null || topicName == null || topicName.isEmpty()) {
			logger.fatal("session or topicName must not be null or empty");
			return false;
		}
		try {
			topic = this.session.createTopic(this.topicName);
			this.receiver = this.session.createSubscriber(topic);
			return true;
		} catch (JMSException e) {
			logger.fatal(e.getMessage(), e);
		}
		this.failCount = 0;
		return false;
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
			failCount++;
			if (failCount > 100) {
				logger.fatal("mq get topic message failed 100 times");
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
			failCount++;
			if (failCount > 100) {
				logger.fatal("mq get topic message failed 100 times");
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
	public long getQueueSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		try {
			if (this.receiver != null) {
				this.receiver.close();
			}
			if (this.session != null) {
				this.session.close();
			}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
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

	public int getFailCount() {
		return failCount;
	}

	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}
}
