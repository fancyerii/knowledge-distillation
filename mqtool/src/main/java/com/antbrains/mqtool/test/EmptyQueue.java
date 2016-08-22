package com.antbrains.mqtool.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.jms.TextMessage;

import com.antbrains.mqtool.ActiveMqTools;
import com.antbrains.mqtool.HornetQReceiver;
import com.antbrains.mqtool.HornetQSender;
import com.antbrains.mqtool.HornetQTools;
import com.antbrains.mqtool.MqReceiver;
import com.antbrains.mqtool.MqSender;
import com.antbrains.mqtool.MqToolsInterface;

public class EmptyQueue {

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.err.println("need 3 args: jnp jmx queueName");
			System.exit(-1);
		}
		System.out.println("jnp: " + args[0]);
		System.out.println("jmx: " + args[1]);
		System.out.println("queue: " + args[2]);
		System.out.println("clear queue: "+args[2]+"? Enter Yes to continue");
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		String cmd=br.readLine();
		if(cmd.trim().equalsIgnoreCase("yes")){
			System.out.println("clear queue");
		}else{
			return;
		}
		MqToolsInterface tools = new HornetQTools(args[0], args[1]);
		tools.init();

		MqReceiver recv = tools.getMqReceiver(args[2], ActiveMqTools.AUTO_ACKNOWLEDGE);
		recv.init();
		int total = 0;
		while (true) {
			try {
				Object o = recv.receive(5000);
				if (o == null)
					break;
				total++;
				if (total % 10000 == 0) {
					System.out.println("recv: " + total);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("total clear: " + total);
		recv.destroy();
		tools.destroy();
		System.out.println("finish clear");
	}

}
