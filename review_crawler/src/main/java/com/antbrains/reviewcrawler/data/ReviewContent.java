package com.antbrains.reviewcrawler.data;

public class ReviewContent {
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getPageId() {
		return pageId;
	}
	public void setPageId(int pageId) {
		this.pageId = pageId;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	private String id;
	private int pageId;
	private String date;
	private String content;
	
}
