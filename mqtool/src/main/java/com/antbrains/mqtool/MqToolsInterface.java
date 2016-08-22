package com.antbrains.mqtool;

public interface MqToolsInterface {

	// p2p
	public MqSender getMqSender(String queueName);

	public MqSender getMqSender(String queueName, int module);

	public MqSender getMqSender(String queueName, int module, boolean trans);

	public MqReceiver getMqReceiver(String queueName, int module);

	public MqReceiver getMqReceiver(String queueName);

	// topic
	public MqSender getMqTopicSender(String queueName);

	public MqSender getMqTopicSender(String queueName, int module);

	public MqReceiver getMqTopicReceiver(String queueName, int module);

	public MqReceiver getMqTopicReceiver(String queueName);

	public long getQueueSize(String queueName);

	public boolean init();

	public void destroy();
}
