package com.antbrains.reviewcrawler.data;

import java.util.Date;

public class ReviewStatus {
	private String id;
	private String url;
	private Date lastUpdate;
	private String lastestReviewTime;
	private int lastAdded;
	private int totalRv;
	public int getTotalRv() {
		return totalRv;
	}
	public void setTotalRv(int totalRv) {
		this.totalRv = totalRv;
	}
	public int getLastAdded() {
		return lastAdded;
	}
	public void setLastAdded(int lastAdded) {
		this.lastAdded = lastAdded;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public Date getLastUpdate() {
		return lastUpdate;
	}
	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	public String getLastestReviewTime() {
		return lastestReviewTime;
	}
	public void setLastestReviewTime(String lastestReviewTime) {
		this.lastestReviewTime = lastestReviewTime;
	}
}
