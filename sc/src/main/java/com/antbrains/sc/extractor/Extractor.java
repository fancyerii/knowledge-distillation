package com.antbrains.sc.extractor;

import java.util.Date;
import java.util.List;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;

public interface Extractor {
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId);

	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId);

	public void extractBasicInfo(WebPage webPage, String content, Archiver archiver, String taskId);

	public boolean needUpdate(WebPage webPage);

	public boolean needSaveHtml(WebPage webPage);

	public boolean needAddChildren2FrontierIfNotUpdate(WebPage webPage);

}
