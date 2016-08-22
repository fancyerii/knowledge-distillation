package com.antbrains.sc.data;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.antbrains.mysqltool.DBUtils;

public class WebPage implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1772759107828003823L;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCharSet() {
		return charSet;
	}

	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getLastVisitTime() {
		return lastVisitTime;
	}

	public void setLastVisitTime(Date lastVisitTime) {
		this.lastVisitTime = lastVisitTime;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	private int failCount;

	public int getFailCount() {
		return failCount;
	}

	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}

	public Map<String, String> getAttrsFromParent() {
		return attrsFromParent;
	}

	public void setAttrsFromParent(Map<String, String> attrsFromParent) {
		this.attrsFromParent = attrsFromParent;
	}

	public Map<String, String> getAttrs() {
		return attrs;
	}

	public void setAttrs(Map<String, String> attrs) {
		this.attrs = attrs;
	}

	public List<Block> getBlocks() {
		return blocks;
	}

	public void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}

	private Integer id;
	private String url;
	private String redirectedUrl;

	public String getRedirectedUrl() {
		return redirectedUrl;
	}

	public void setRedirectedUrl(String redirectedUrl) {
		this.redirectedUrl = redirectedUrl;
	}

	private String title;
	private String charSet;
	private List<String> tags;
	private int depth;
	private String content;
	private Date lastVisitTime;
	private Date lastFinishTime;
	public Date getLastFinishTime() {
		return lastFinishTime;
	}

	public void setLastFinishTime(Date lastFinishTime) {
		this.lastFinishTime = lastFinishTime;
	}

	private int type;
	private Map<String, String> attrs;
	private Map<String, String> attrsFromParent;

	private int crawlPriority;

	public int getCrawlPriority() {
		return crawlPriority;
	}

	public void setCrawlPriority(int crawlPriority) {
		this.crawlPriority = crawlPriority;
	}

	private Map<String, Object> otherInfo = new HashMap<String, Object>(2);

	public Map<String, Object> getOtherInfo() {
		return otherInfo;
	}

	public void setOtherInfo(Map<String, Object> otherInfo) {
		this.otherInfo = otherInfo;
	}

	transient private List<Block> blocks;

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id: " + (id == null ? null : id.intValue()) + "\n");
		sb.append("url: " + url + "\n");
		sb.append("redirectedUrl: " + redirectedUrl + "\n");
		sb.append("title: " + title + "\n");
		sb.append("charSet: " + charSet + "\n");
		sb.append("tags: " + DBUtils.tagList2String(tags) + "\n");
		sb.append("depth: " + depth + "\n");
		sb.append("content: " + content + "\n");
		sb.append("lastVisitTime: " + lastVisitTime + "\n");

		sb.append("type: " + type + "\n");
		if (attrs != null) {
			for (Entry<String, String> entry : attrs.entrySet()) {
				sb.append("[attr]" + entry.getKey() + ": " + entry.getValue() + "\n");
			}
		}
		if (attrsFromParent != null) {
			for (Entry<String, String> entry : attrsFromParent.entrySet()) {
				sb.append("[attrFromParent]" + entry.getKey() + ": " + entry.getValue() + "\n");
			}
		}
		if (blocks != null) {
			for (Block block : blocks) {
				sb.append(block.toString());
			}
		}
		sb.append("otherInfo: " + this.otherInfo + "\n");
		return sb.toString();
	}
}
