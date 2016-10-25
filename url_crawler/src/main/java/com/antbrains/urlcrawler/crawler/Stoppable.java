package com.antbrains.urlcrawler.crawler;

public interface Stoppable {
	public void stopMe();

	public void waitFinish(long wait);
}
