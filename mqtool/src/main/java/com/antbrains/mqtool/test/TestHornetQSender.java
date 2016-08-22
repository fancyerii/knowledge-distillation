package com.antbrains.mqtool.test;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.antbrains.mqtool.HornetQSender;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqReceiver;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;

public class TestHornetQSender {

	public static void main(String[] args) throws JMSException {
		MqToolsInterface tools = new HornetQTools("jnp://localhost:1099", "localhost:3333");
		tools.init();

		MqSender sender = tools.getMqSender("test", Session.SESSION_TRANSACTED, true);
		sender.init(HornetQSender.PERSISTENT);
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			if (i > 0 && i % 10_000 == 0) {
				sender.commit();
			}
			sender.send("msg" + i);

		}
		sender.commit();
		System.out.println("time: " + (System.currentTimeMillis() - start));
		sender.destroy();
		tools.destroy();
	}

}
