package com.antbrains.mqtool.test;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientRequestor;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.management.ManagementHelper;

public class MyCoreSyncronousTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {

			MyCoreClientFactory.createSettings("localhost", 5445);

			MyCoreMessageProducer m = new MyCoreMessageProducer();

			m.setQueuename("jms.queue.mytestqueue");
			m.send("1;02/20/2012 14:01:59.010;1020,1021,1022");
			System.out.println("queueSize: " + size("jms.queue.mytestqueue"));
			m.send("1;02/20/2012 14:01:59.010;1020,1021,1023");
			System.out.println("queueSize: " + size("jms.queue.mytestqueue"));
			MyCoreMessageProducer c = new MyCoreMessageProducer();

			MyCoreMessageConsumer.getMessages("jms.queue.mytestqueue");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				MyCoreSession.close();
			} catch (HornetQException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			MyCoreClientFactory.close();
		}

		System.exit(0);
	}

	public static int size(String queueName) throws Exception {
		int count = 0;
		try {
			ClientSession coreSession = MyCoreSession.getSession();
			ClientSession.QueueQuery result;
			result = coreSession.queueQuery(new SimpleString(queueName));
			count = (int) result.getMessageCount();

		} catch (HornetQException e) {
			e.printStackTrace();
			return -1;

		}

		return count;

	}
}
