package com.antbrains.sc.data;

import java.util.List;

public class WebSite {
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	private int id;
	private String siteName;
	private List<String> tags;
	private String desc;
	private WebPage indexPage;

	public WebPage getIndexPage() {
		return indexPage;
	}

	public void setIndexPage(WebPage indexPage) {
		this.indexPage = indexPage;
	}
}
