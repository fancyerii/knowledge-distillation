package com.antbrains.mqtool;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;

import org.apache.log4j.Logger;

public class ActiveMqReceiver implements MqReceiver {
	private static Logger logger = Logger.getLogger(ActiveMqReceiver.class);
	private QueueSession session = null;
	private String queueName = null;
	private QueueReceiver receiver = null;
	private Queue receiverQueue = null;
	private long failCount = 0;

	public ActiveMqReceiver() {
	}

	public ActiveMqReceiver(QueueSession session, String queueName) {
		this.session = session;
		this.queueName = queueName;
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

	@Override
	public long getQueueSize() {
		// TODO Auto-generated method stub
		if (session == null) {
			return -1;
		}
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		try {
			Queue replyTo = session.createTemporaryQueue();
			consumer = session.createConsumer(replyTo);

			String queueName = "ActiveMQ.Statistics.Destination." + this.queueName;
			Queue testQueue = session.createQueue(queueName);
			producer = session.createProducer(testQueue);
			Message msg = session.createMessage();
			msg.setJMSReplyTo(replyTo);
			producer.send(msg);

			MapMessage reply = (MapMessage) consumer.receive(1000);
			if (reply != null) {
				return (Long) reply.getObject("size");
			} else {
				return -1;
			}
		} catch (JMSException e) {
			logger.error(e.getMessage(), e);
		} finally {
			if (producer != null) {
				try {
					producer.close();
				} catch (JMSException e) {
					logger.error(e.getMessage(), e);
				}
			}
			if (consumer != null) {
				try {
					consumer.close();
				} catch (JMSException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		return 0;
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
