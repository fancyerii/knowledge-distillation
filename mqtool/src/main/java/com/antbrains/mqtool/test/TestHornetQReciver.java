package com.antbrains.mqtool.test;

import javax.jms.TextMessage;

import com.antbrains.mqtool.ActiveMqTools;
import com.antbrains.mqtool.HornetQReceiver;
import com.antbrains.mqtool.HornetQSender;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqReceiver;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;

public class TestHornetQReciver {

	public static void main(String[] args) {
		MqToolsInterface tools = new HornetQTools("jnp://localhost:1099", "localhost:3333");
		tools.init();

		MqReceiver recv = tools.getMqReceiver("test", ActiveMqTools.AUTO_ACKNOWLEDGE);
		recv.init();
		for (int i = 0; i < 10000000; i++) {
			try {
				TextMessage tm = (TextMessage) recv.receive();
				System.out.println(tm.getText());
			} catch (Exception e) {

			}
		}
		recv.destroy();
		tools.destroy();
	}

}
