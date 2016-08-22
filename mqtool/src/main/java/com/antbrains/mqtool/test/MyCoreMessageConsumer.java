package com.antbrains.mqtool.test;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;

public class MyCoreMessageConsumer {

	private static ClientConsumer consumer;
	private static ClientMessage message;

	public static void getMessages(String queuename) throws HornetQException, Exception {
		MyCoreSession.start();
		consumer = MyCoreSession.getSession().createConsumer(queuename);
		while (true) {
			message = consumer.receive(1000);
			if (message == null)
				break;
			System.out.println("Received TextMessage:" + message.getStringProperty("ecg"));
			message.acknowledge();
		}

	}

	public void describeMessage() throws IllegalArgumentException, IllegalAccessException {

	}

}
