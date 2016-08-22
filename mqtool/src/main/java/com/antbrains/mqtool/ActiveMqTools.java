package com.antbrains.mqtool;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

public class ActiveMqTools implements MqToolsInterface {
	public static int AUTO_ACKNOWLEDGE = Session.AUTO_ACKNOWLEDGE;
	public static int CLIENT_ACKNOWLEDGE = Session.CLIENT_ACKNOWLEDGE;
	public static int DUPS_OK_ACKNOWLEDGE = Session.DUPS_OK_ACKNOWLEDGE;

	private static Logger logger = Logger.getLogger(ActiveMqTools.class);
	private String conAddress = null;
	private QueueConnection qConn = null;
	private TopicConnection tConn = null;
	private boolean topicConInited = false;

	public ActiveMqTools() {

	}

	public ActiveMqTools(String conAddress) {
		this.conAddress = conAddress;
	}

	@Override
	public boolean init() {
		// TODO Auto-generated method stub
		if (this.conAddress == null) {
			logger.fatal("The connection address is not inited.");
			return false;
		}
		try {
			ActiveMQConnectionFactory connFactory = new ActiveMQConnectionFactory(conAddress);
			qConn = connFactory.createQueueConnection();
			qConn.start();
		} catch (JMSException e) {
			logger.fatal("init error");
			logger.fatal(e.getMessage(), e);
			return false;
		}
		logger.info("queue connection init success, [" + this.conAddress + "]");
		return true;
	}

	// lazy init
	private boolean initTopic() {
		if (this.conAddress == null) {
			logger.fatal("The connection address is not inited.");
			return false;
		}
		try {
			ActiveMQConnectionFactory connFactory = new ActiveMQConnectionFactory(conAddress);
			tConn = connFactory.createTopicConnection();
			tConn.start();
		} catch (JMSException e) {
			logger.fatal("init topic error");
			logger.fatal(e.getMessage(), e);
			return false;
		}
		this.topicConInited = true;
		logger.info("init topic connecton success, [" + this.conAddress + "]");
		return true;
	}

	/****************************** p2p *******************************************************/
	@Override
	public MqSender getMqSender(String queueName, int module) {
		// TODO Auto-generated method stub
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		try {
			QueueSession session = this.qConn.createQueueSession(false, module);
			return new ActiveMqSender(session, queueName);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public MqReceiver getMqReceiver(String queueName, int module) {
		// TODO Auto-generated method stub
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		try {
			QueueSession session = this.qConn.createQueueSession(false, module);
			return new ActiveMqReceiver(session, queueName);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public MqReceiver getMqReceiver(String queueName) {
		// TODO Auto-generated method stub
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		try {
			QueueSession session = this.qConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			return new ActiveMqReceiver(session, queueName);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public MqSender getMqSender(String queueName) {
		// TODO Auto-generated method stub
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		try {
			QueueSession session = this.qConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			return new ActiveMqSender(session, queueName);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	/********************************* topic ****************************************************/
	@Override
	public MqSender getMqTopicSender(String queueName) {
		// TODO Auto-generated method stub
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		if (this.topicConInited == false) {
			if (!initTopic()) {
				return null;
			}
		}
		try {
			TopicSession session = this.tConn.createTopicSession(false, AUTO_ACKNOWLEDGE);
			return new ActiveMqTopicSender(session, queueName);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public MqSender getMqTopicSender(String queueName, int module) {
		// TODO Auto-generated method stub
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		if (this.topicConInited == false) {
			if (!initTopic()) {
				return null;
			}
		}
		try {
			TopicSession session = this.tConn.createTopicSession(false, module);
			return new ActiveMqTopicSender(session, queueName);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public MqReceiver getMqTopicReceiver(String queueName, int module) {
		// TODO Auto-generated method stub
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		if (this.topicConInited == false) {
			if (!initTopic()) {
				return null;
			}
		}
		try {
			TopicSession session = this.tConn.createTopicSession(false, module);
			return new ActiveMqTopicReceiver(session, queueName);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public MqReceiver getMqTopicReceiver(String queueName) {
		// TODO Auto-generated method stub
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		if (this.topicConInited == false) {
			if (!initTopic()) {
				return null;
			}
		}
		try {
			TopicSession session = this.tConn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			return new ActiveMqTopicReceiver(session, queueName);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	/*************************************************************************************/
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		try {
			if (this.qConn != null) {
				qConn.close();
				qConn = null;
			}

		} catch (JMSException e) {
			logger.error(e.getMessage(), e);
		}
		try {
			if (this.tConn != null) {
				tConn.close();
				tConn = null;
			}
		} catch (JMSException e) {
			logger.error(e.getMessage(), e);
		}
		logger.info("destroy done");
	}

	@Override
	public long getQueueSize(String queueName) {
		// TODO Auto-generated method stub
		MqSender tmp = getMqSender(queueName);
		if (tmp != null) {
			long size = tmp.getQueueSize();
			tmp.destroy();
			return size;
		}
		return -1;
	}

	/* getter and setter */
	public String getConAddress() {
		return conAddress;
	}

	public void setConAddress(String conAddress) {
		this.conAddress = conAddress;
	}

	@Override
	public MqSender getMqSender(String queueName, int module, boolean trans) {
		// TODO Auto-generated method stub
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		try {
			QueueSession session = this.qConn.createQueueSession(trans, module);
			return new ActiveMqSender(session, queueName);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

}
