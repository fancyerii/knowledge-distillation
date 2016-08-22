package com.mobvoi.knowledgegraph.sc;

public class TestRegex {

	public static void main(String[] args) {
		String s = "重启，不行就恢复出厂设置|||需要将路由器";
		String[] arr = s.split("\\|\\|\\|");
		for (String ss : arr) {
			System.out.println(ss);
		}

		String sen = "abc。de？die?d!l！a";
		String[] array = sen.split("[。？！?!]");
		for (String ss : array) {
			System.out.println(ss);
		}
	}

}
