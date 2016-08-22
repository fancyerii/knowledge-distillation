package com.antbrains.mqtool;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.naming.Context;

import org.apache.log4j.Logger;
import org.hornetq.jms.client.HornetQConnection;
import org.hornetq.jms.client.HornetQConnectionFactory;

public class HornetQTools implements MqToolsInterface {
	public static int AUTO_ACKNOWLEDGE = Session.AUTO_ACKNOWLEDGE;
	public static int CLIENT_ACKNOWLEDGE = Session.CLIENT_ACKNOWLEDGE;
	public static int DUPS_OK_ACKNOWLEDGE = Session.DUPS_OK_ACKNOWLEDGE;

	private static Logger logger = Logger.getLogger(HornetQTools.class);
	private String conAddress = null;
	private QueueConnection qConn = null;
	private TopicConnection tConn = null;
	private boolean topicConInited = false;
	private String jmxUrl;
	private Context ic = null;

	public HornetQTools() {

	}

	public HornetQTools(String conAddress, String jmxUrl) {
		this.conAddress = conAddress;
		this.jmxUrl = jmxUrl;
	}

	@Override
	public boolean init() {
		if (this.conAddress == null) {
			logger.fatal("The connection address is not inited.");
			return false;
		}
		try {
			java.util.Properties p = new java.util.Properties();

			p.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
			p.put(javax.naming.Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
			p.put(javax.naming.Context.PROVIDER_URL, this.conAddress);

			ic = new javax.naming.InitialContext(p);
			HornetQConnectionFactory connFactory = (HornetQConnectionFactory) ic.lookup("/ConnectionFactory");
			qConn = connFactory.createQueueConnection();
			qConn.start();
		} catch (Exception e) {
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
			java.util.Properties p = new java.util.Properties();

			p.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
			p.put(javax.naming.Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
			p.put(javax.naming.Context.PROVIDER_URL, this.conAddress);

			ic = new javax.naming.InitialContext(p);
			HornetQConnectionFactory connFactory = (HornetQConnectionFactory) ic.lookup("/ConnectionFactory");
			tConn = connFactory.createTopicConnection();
			tConn.start();
		} catch (Exception e) {
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
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		try {
			QueueSession session = this.qConn.createQueueSession(false, module);
			return new HornetQSender(session, queueName, jmxUrl);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public MqReceiver getMqReceiver(String queueName, int module) {
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		try {
			QueueSession session = this.qConn.createQueueSession(false, module);
			return new HornetQReceiver(session, queueName, jmxUrl);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public MqReceiver getMqReceiver(String queueName) {
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		try {
			QueueSession session = this.qConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			return new HornetQReceiver(session, queueName, jmxUrl);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public MqSender getMqSender(String queueName) {
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		try {
			QueueSession session = this.qConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			return new HornetQSender(session, queueName, jmxUrl);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

	/********************************* topic ****************************************************/
	@Override
	public MqSender getMqTopicSender(String queueName) {
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
		if (queueName == null || queueName.isEmpty()) {
			logger.error("queueName cannot be null or empty");
			return null;
		}
		try {
			QueueSession session = this.qConn.createQueueSession(trans, module);
			return new HornetQSender(session, queueName, jmxUrl);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		return null;
	}

}
