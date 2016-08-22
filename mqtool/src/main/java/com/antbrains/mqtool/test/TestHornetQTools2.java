package com.antbrains.mqtool.test;

import javax.jms.TextMessage;

import com.antbrains.mqtool.HornetQSender;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqReceiver;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;

public class TestHornetQTools2 {

	public static void main(String[] args) {
		MqToolsInterface tools = new HornetQTools("jnp://172.19.34.157:1099", "172.19.34.157:4000");
		tools.init();
		MqSender sender = tools.getMqTopicSender("OrderTopic");
		sender.init(HornetQSender.PERSISTENT);
		for (int i = 0; i < 100; i++) {
			sender.send("topic" + i);
		}
		sender.destroy();
		MqReceiver recver = tools.getMqTopicReceiver("OrderTopic");
		recver.init();
		long size = recver.getQueueSize();
		System.out.println("queue size: " + size);
		while (true) {
			try {
				TextMessage msg = (TextMessage) recver.receive(5000);
				if (msg == null)
					break;
				System.out.println(msg.getText());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		recver.destroy();
		tools.destroy();
	}

}
