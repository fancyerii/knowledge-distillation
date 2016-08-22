package com.antbrains.sc.data;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.antbrains.mysqltool.DBUtils;

import java.util.Set;

public class Block {
	public static final String OTHER_INFO_ADD_CHILD = "add_child";

	public Integer getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public Date getLastVisitTime() {
		return lastVisitTime;
	}

	public void setLastVisitTime(Date lastVisitTime) {
		this.lastVisitTime = lastVisitTime;
	}

	public Date getLastFinishTime() {
		return lastFinishTime;
	}

	public void setLastFinishTime(Date lastFinishTime) {
		this.lastFinishTime = lastFinishTime;
	}

	public List<Link> getLinks() {
		return links;
	}

	public void setLinks(List<Link> links) {
		this.links = links;
	}

	private Integer id;
	private List<String> tags;
	private Date lastVisitTime;
	private Date lastFinishTime;
	private List<Link> links;

	/**
	 * 用来保存block的更新信息的，比如最后更新的时间
	 */
	private String updateInfo;

	private Map<String, Object> otherInfo;

	public Map<String, Object> getOtherInfo() {
		return otherInfo;
	}

	public void setOtherInfo(Map<String, Object> otherInfo) {
		this.otherInfo = otherInfo;
	}

	public String getUpdateInfo() {
		return updateInfo;
	}

	public void setUpdateInfo(String updateInfo) {
		this.updateInfo = updateInfo;
	}

	public String toString() {
		return toString(0);
	}

	private void addTab(int c, StringBuilder sb) {
		for (int i = 0; i < c; i++) {
			sb.append("\t");
		}
	}

	public String toString(int tab) {
		StringBuilder sb = new StringBuilder();
		addTab(tab, sb);
		sb.append("tags: " + DBUtils.tagList2String(tags) + "\n");
		sb.append("updateInfo: " + this.updateInfo + "\n");
		if (links == null) {
			addTab(tab, sb);
			sb.append("links is null\n");
		} else {
			sb.append("total links: " + links.size() + "\n");
			for (Link link : links) {
				addTab(tab, sb);
				sb.append(link.getWebPage().getUrl() + "-->" + link.getLinkText() + "\n");
				Map<String, String> attrs = link.getLinkAttrs();
				if (attrs != null) {
					for (Entry<String, String> entry : attrs.entrySet()) {
						addTab(tab, sb);
						sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\t");
					}

				}
				WebPage child = link.getWebPage();
				if (child != null) {
					Map<String, String> attrsFromParent = child.getAttrsFromParent();
					if (attrsFromParent != null) {
						for (Entry<String, String> entry : attrsFromParent.entrySet()) {
							addTab(tab, sb);
							sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\t");
						}

					}
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	/**
	 * 这个Block是否包含某个页面 根据它来插入或更新child页面 这个实现简单的根据url来判断 如果有的网站重构，可能导致url的变化，
	 * 那么也许可以根据linkText以及webPage的attrFromParent和attr来判断
	 * 
	 * @param webPage
	 * @return
	 */
	public WebPage getExistWebPage(WebPage webPage, String linkText) {
		if (links == null)
			return null;
		// 没有同步，可能导致多次初始化
		if (childUrls == null) {
			Map<String, WebPage> urls = new HashMap<String, WebPage>();
			for (Link link : links) {
				urls.put(link.getWebPage().getUrl(), link.getWebPage());
			}
			// 切换
			// 即使多次初始化，总有一个是好的
			this.childUrls = urls;
		}

		return childUrls.get(webPage.getUrl());
	}

	private Map<String, WebPage> childUrls;
}
