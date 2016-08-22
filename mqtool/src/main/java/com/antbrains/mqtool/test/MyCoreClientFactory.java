package com.antbrains.mqtool.test;

import java.util.HashMap;
import java.util.Map;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;

public class MyCoreClientFactory {

	private static ClientSessionFactory factory = null;
	static HashMap<String, Object> map = null;
	private static TransportConfiguration configuration;
	private static ServerLocator locator;

	public static ClientSessionFactory getConnectionFactory() throws Exception {
		if (factory == null) {
			configuration = new TransportConfiguration(NettyConnectorFactory.class.getName(), map);
			locator = HornetQClient.createServerLocatorWithoutHA(configuration);
			factory = locator.createSessionFactory();
		}
		return factory;
	}

	public static Map<String, Object> createSettings(String host, int port) {

		if (map == null) {

			map = new HashMap<>();
			map.put("host", host);
			map.put("port", port);

		}
		return map;
	}

	public static void close() {
		if (factory != null) {
			factory.close();
			factory = null;
		}
	}
}
