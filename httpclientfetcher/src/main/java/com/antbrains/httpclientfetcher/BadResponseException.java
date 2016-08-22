package com.antbrains.httpclientfetcher;

import java.io.IOException;

public class BadResponseException extends IOException {
	private static final long serialVersionUID = 1849425623118234332L;

	public String getMsg() {
		return msg;
	}

	public int getCode() {
		return code;
	}

	private String msg;
	private int code;

	public BadResponseException(String msg, int code) {
		this.msg = msg;
		this.code = code;
	}

	@Override
	public String toString() {
		return code + "\t" + msg;
	}
}
