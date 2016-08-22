package com.antbrains.sc.extractor;

import java.util.Date;
import java.util.List;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;

public class NullExtractor implements Extractor {

	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {

	}

	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		return null;
	}

	@Override
	public void extractBasicInfo(WebPage webPage, String content, Archiver archiver, String taskId) {

	}

	@Override
	public boolean needUpdate(WebPage webPage) {
		return true;
	}

	@Override
	public boolean needAddChildren2FrontierIfNotUpdate(WebPage webPage) {
		return true;
	}

	@Override
	public boolean needSaveHtml(WebPage webPage) {
		return false;
	}

}
