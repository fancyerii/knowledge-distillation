package com.github.fancyerii.pusa123crawler.extractor;

public class ListPageItem {
	public ListPageItem(String title, String url, String pubTime) {
		this.title = title;
		this.url = url;
		this.pubTime = pubTime;
		if (this.pubTime == null) {
			this.pubTime = "";
		}
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public String getPubTime() {
		return pubTime;
	}

	private String title;
	private String url;
	private String pubTime;

	@Override
	public String toString() {
		return "title: " + title + ", pubTime: " + pubTime + "\tUrl: " + url;
	}
}
