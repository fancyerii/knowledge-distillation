package com.antbrains.httpclientfetcher;

public class NullResultValidator implements ResultValidator {

	@Override
	public boolean isValid(String html) {
		return html != null;
	}

}
