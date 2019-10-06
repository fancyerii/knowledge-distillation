package com.antbrains.saraxwzx.data;

import java.util.Date;

public class Article {
	private String md5;
	private String url;
	private String host;
	private String title;
	private String segTitle;
	private String content;
	private String segContent;
	public String getSegTitle() {
		return segTitle;
	}
	public void setSegTitle(String segTitle) {
		this.segTitle = segTitle;
	}
	public String getSegContent() {
		return segContent;
	}
	public void setSegContent(String segContent) {
		this.segContent = segContent;
	}
	private Date pubTime;
	//标签：少林寺 法海
	private String[] tags;
	private String source;
	//类型：新闻，视频，等等
	private String[] types;
	public String getMd5() {
		return md5;
	}
	public void setMd5(String md5) {
		this.md5 = md5;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public Date getPubTime() {
		return pubTime;
	}
	public void setPubTime(Date pubTime) {
		this.pubTime = pubTime;
	}
	public String[] getTags() {
		return tags;
	}
	public void setTags(String[] tags) {
		this.tags = tags;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String[] getTypes() {
		return types;
	}
	public void setTypes(String[] types) {
		this.types = types;
	}
	public String[] getAuthors() {
		return authors;
	}
	public void setAuthors(String[] authors) {
		this.authors = authors;
	}
	public int getComments() {
		return comments;
	}
	public void setComments(int comments) {
		this.comments = comments;
	}
	public String getMainImage() {
		return mainImage;
	}
	public void setMainImage(String mainImage) {
		this.mainImage = mainImage;
	}
	public String[] getImgs() {
		return imgs;
	}
	public void setImgs(String[] imgs) {
		this.imgs = imgs;
	}
	private String[] authors;
	private int comments;
	private String mainImage;
	private String[] imgs;
}
