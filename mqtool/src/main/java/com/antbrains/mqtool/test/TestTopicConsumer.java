package com.antbrains.mqtool.test;

import javax.jms.TextMessage;

import com.antbrains.mqtool.ActiveMqTopicSender;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqReceiver;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;

public class TestTopicConsumer {

	public static void main(String[] args) throws Exception {
		MqToolsInterface tools = new HornetQTools("jnp://172.19.34.157:1099", "");
		tools.init();
		MqReceiver recver = tools.getMqTopicReceiver("extract-topic2");
		recver.init();

		while (true) {
			TextMessage msg = (TextMessage) recver.receive();
			System.out.println(msg.getText());
		}
	}

}
