package com.antbrains.mqtool.test;

import javax.jms.TextMessage;

import com.antbrains.mqtool.HornetQSender;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqReceiver;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;

public class TestHornetQTools {

	public static void main(String[] args) {
		MqToolsInterface tools = new HornetQTools("jnp://kg121:1099", "kg121:3333");
		tools.init();
		long size = tools.getQueueSize("baidumusic");
		System.out.println("baidumusic: " + size);
		// size = tools.getQueueSize("extract-queue2");
		// System.out.println("extract-queue2: " + size);
		//
		// MqSender sender = tools.getMqSender("OrderQueue",
		// HornetQTools.AUTO_ACKNOWLEDGE);
		// sender.init(HornetQSender.PERSISTENT);
		// for (int i = 0; i < 100; i++) {
		// sender.send("msg" + i);
		// }
		// sender.destroy();
		// MqReceiver recver = tools.getMqReceiver("OrderQueue",
		// HornetQTools.AUTO_ACKNOWLEDGE);
		// recver.init();
		// size = recver.getQueueSize();
		// System.out.println("queue size: " + size);
		// while (true) {
		// try {
		// TextMessage msg = (TextMessage) recver.receive(5000);
		// if (msg == null)
		// break;
		// System.out.println(msg.getText());
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
		// recver.destroy();
		tools.destroy();
	}

}
