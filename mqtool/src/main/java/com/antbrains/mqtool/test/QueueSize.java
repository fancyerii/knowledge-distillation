package com.antbrains.mqtool.test;

import javax.jms.TextMessage;

import com.antbrains.mqtool.ActiveMqTools;
import com.antbrains.mqtool.HornetQReceiver;
import com.antbrains.mqtool.HornetQSender;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqReceiver;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;

public class QueueSize {

	public static void main(String[] args) {
		if (args.length != 3) {
			System.err.println("need 3 args: jnp jmx queueName");
			System.exit(-1);
		}
		System.out.println("jnp: " + args[0]);
		System.out.println("jmx: " + args[1]);
		System.out.println("queue: " + args[2]);

		MqToolsInterface tools = new HornetQTools(args[0], args[1]);
		tools.init();
		long queueSize = tools.getQueueSize(args[2]);
		System.out.println("size: " + queueSize);
		tools.destroy();
	}

}
