package com.antbrains.mqtool.test;

import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class TestKafkaProducer {

	public static void main(String[] args) {
		String topic = "test-topic";
		Properties props = new Properties();
		props.put("metadata.broker.list", "linux157:9092");
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		props.put("request.required.acks", "1");
		ProducerConfig config = new ProducerConfig(props);
		Producer<Integer, String> producer = new Producer<Integer, String>(config);
		for (int i = 0; i < 100; i++) {
			KeyedMessage<Integer, String> data = new KeyedMessage<Integer, String>(topic, "msg" + i);
			producer.send(data);
		}
		producer.close();

	}

}
