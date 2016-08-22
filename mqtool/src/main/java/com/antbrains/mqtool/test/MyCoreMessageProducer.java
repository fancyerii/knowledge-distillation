package com.antbrains.mqtool.test;

import org.hornetq.api.core.HornetQException;

public class MyCoreMessageProducer {

	private org.hornetq.api.core.client.ClientProducer producer;
	private String queuename;
	private org.hornetq.api.core.client.ClientMessage message;

	public void send(String _message) throws HornetQException, Exception {
		message = MyCoreSession.getSession().createMessage(false);
		message.putStringProperty("ecg", _message);
		producer.send(message);
		System.out.println("message sent successfully");

	}

	public void setQueuename(String queuename) throws HornetQException, Exception {
		// TODO Auto-generated method stub
		if (producer == null) {
			producer = MyCoreSession.getSession().createProducer(queuename);
		}

	}

}
