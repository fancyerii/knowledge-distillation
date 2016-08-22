package com.antbrains.mqtool.test;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientRequestor;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.management.ManagementHelper;
import org.hornetq.jms.client.HornetQConnection;
import org.hornetq.jms.client.HornetQConnectionFactory;

public class TestHornetQ {
	javax.naming.Context ic = null;
	HornetQConnectionFactory cf = null;
	HornetQConnection connection = null;
	javax.jms.Queue queue = null;
	javax.jms.Session session = null;

	String destinationName = "queue/DLQ";

	void connectAndCreateSession() throws NamingException, JMSException {
		cf = (HornetQConnectionFactory) ic.lookup("/ConnectionFactory");
		queue = (javax.jms.Queue) ic.lookup(destinationName);
		connection = (HornetQConnection) cf.createConnection();
		session = connection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
		connection.start();
	}

	void produceMessage() throws JMSException {
		String theECG = "1;02/20/2012 14:01:59.010;1020,1021,1022";
		javax.jms.MessageProducer publisher = session.createProducer(queue);
		javax.jms.TextMessage message = session.createTextMessage(theECG);
		publisher.send(message);
		System.out.println("Message sent!");
		publisher.close();
	}

	void consumeMessage() throws JMSException {
		javax.jms.MessageConsumer messageConsumer = session.createConsumer(queue);
		javax.jms.TextMessage messageReceived = (javax.jms.TextMessage) messageConsumer.receive(5000);

		System.out.println("Received message: " + messageReceived.getText());
		messageConsumer.close();
	}

	public void closeConnection() {
		if (session != null) {
			try {
				session.close();
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}

		if (connection != null) {
			try {
				connection.close();
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}

	}

	void getInitialContext() throws NamingException {

		java.util.Properties p = new java.util.Properties();

		p.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
		p.put(javax.naming.Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
		p.put(javax.naming.Context.PROVIDER_URL, "jnp://172.19.34.157:1099");

		ic = new javax.naming.InitialContext(p);

	}

	public static void main(String[] args) throws Exception {
		TestHornetQ t = new TestHornetQ();
		t.getInitialContext();
		t.connectAndCreateSession();
		t.produceMessage();
		t.consumeMessage();
		t.closeConnection();
	}

}
