package com.antbrains.reviewcrawler.data;

public class CrawlTask {
	public CrawlTask(String id, String url, String lastestReviewTime, int prior, int updateInterval){
		this.id=id;
		this.url=url;
		this.lastestReviewTime=lastestReviewTime;
		this.prior=prior;
		this.updateInterval=updateInterval;
	}
	
	public int getUpdateInterval() {
		return updateInterval;
	}

	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
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
	public String getLastestReviewTime() {
		return lastestReviewTime;
	}
	public void setLastestReviewTime(String lastestReviewTime) {
		this.lastestReviewTime = lastestReviewTime;
	}
	private String id;
	private String url;
	private String lastestReviewTime;
	private int prior;
	private int updateInterval;
	private int crawlingStatus;
	public int getCrawlingStatus() {
		return crawlingStatus;
	}

	public void setCrawlingStatus(int crawlingStatus) {
		this.crawlingStatus = crawlingStatus;
	}

	public int getPrior() {
		return prior;
	}

	public void setPrior(int prior) {
		this.prior = prior;
	}
}
