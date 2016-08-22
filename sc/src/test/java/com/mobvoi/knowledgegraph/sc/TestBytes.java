package com.mobvoi.knowledgegraph.sc;

import org.apache.hadoop.hbase.util.Bytes;

public class TestBytes {

	public static void main(String[] args) {
		byte[] bytes1 = Bytes.toBytes(0);
		byte[] bytes2 = Bytes.toBytes(-1);
		byte[] bytes3 = Bytes.toBytes(-2);
		for (byte b : bytes1) {
			System.out.print(b + "\t");
		}
		System.out.println();
		for (byte b : bytes2) {
			System.out.print(b + "\t");
		}
		System.out.println();
		for (byte b : bytes3) {
			System.out.print(b + "\t");
		}
		System.out.println();
	}

}
