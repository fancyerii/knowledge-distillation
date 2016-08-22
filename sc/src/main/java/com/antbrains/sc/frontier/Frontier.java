package com.antbrains.sc.frontier;

import com.antbrains.sc.data.WebPage;

/**
 * need Thread safe
 * 
 * @author lili
 *
 */
public interface Frontier {
	public void addWebPage(WebPage webPage, int failCount);

	public void close();
}
