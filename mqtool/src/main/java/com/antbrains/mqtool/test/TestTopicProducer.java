package com.antbrains.mqtool.test;

import com.antbrains.mqtool.ActiveMqTopicSender;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;

public class TestTopicProducer {

	public static void main(String[] args) throws Exception {
		MqToolsInterface tools = new HornetQTools("jnp://172.19.34.157:1099", "172.19.34.157:4000");
		tools.init();
		MqSender sender = tools.getMqTopicSender("extract-topic2");
		sender.init(ActiveMqTopicSender.NON_PERSISTENT);
		for (int i = 0; i < 1000; i++) {
			Thread.sleep(3000);
			sender.send("topic" + i);
		}
		sender.destroy();
	}

}
