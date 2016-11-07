package com.antbrains.ifeng_es.search;

import java.util.ArrayList;

public class SearchResult {
	public static final SearchResult EMPTY=new SearchResult();
	private ArrayList<SearchItem> items=new ArrayList<>(10);
	private long totalResult;
	public long getTotalResult() {
		return totalResult;
	}

	public void setTotalResult(long totalResult) {
		this.totalResult = totalResult;
	}

	public ArrayList<SearchItem> getItems() {
		return items;
	}

	public void setItems(ArrayList<SearchItem> items) {
		this.items = items;
	}
}
