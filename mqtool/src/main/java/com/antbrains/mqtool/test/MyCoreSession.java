package com.antbrains.mqtool.test;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientSession;

public class MyCoreSession {

	static ClientSession session = null;

	public static ClientSession getSession() throws HornetQException, Exception {

		if (session == null) {
			System.out.println("creating session at " + new java.util.Date());
			session = MyCoreClientFactory.getConnectionFactory().createSession(false, true, true);
			;

		}

		return session;
	}

	public static void start() throws HornetQException {
		if (session != null)
			session.start();
	}

	public static void close() throws HornetQException {
		if (session != null)
			session.close();
	}

}
