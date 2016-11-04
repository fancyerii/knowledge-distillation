package com.antbrains.ifeng_es.search;

public class SearchItem {
	private String title;
	private String titleHighlight;
	private String content;
	private String contentHightlight;
	private String pubTime;
	private String url;
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getTitleHighlight() {
		return titleHighlight;
	}
	public void setTitleHighlight(String titleHighlight) {
		this.titleHighlight = titleHighlight;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getContentHightlight() {
		return contentHightlight;
	}
	public void setContentHightlight(String contentHightlight) {
		this.contentHightlight = contentHightlight;
	}
	public String getPubTime() {
		return pubTime;
	}
	public void setPubTime(String pubTime) {
		this.pubTime = pubTime;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
}
