package com.antbrains.sc.data;

import java.util.Map;

public class Link {
	private WebPage webPage;
	private String linkText;
	private int pos;
	private Map<String, String> linkAttrs;

	public Map<String, String> getLinkAttrs() {
		return linkAttrs;
	}

	public void setLinkAttrs(Map<String, String> linkAttrs) {
		this.linkAttrs = linkAttrs;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public WebPage getWebPage() {
		return webPage;
	}

	public void setWebPage(WebPage webPage) {
		this.webPage = webPage;
	}

	public String getLinkText() {
		return linkText;
	}

	public void setLinkText(String linkText) {
		this.linkText = linkText;
	}
}
