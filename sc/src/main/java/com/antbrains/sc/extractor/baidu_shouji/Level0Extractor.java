package com.antbrains.sc.extractor.baidu_shouji;

import java.util.ArrayList;
import java.util.List;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.Link;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.extractor.BasicInfoExtractor;

public class Level0Extractor extends BasicInfoExtractor {

	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {

	}

	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		List<Block> blocks = new ArrayList<Block>(1);
		String[] urls = { "http://shouji.baidu.com/software", "http://shouji.baidu.com/game" };
		String[] anchors = { "软件", "游戏" };
		blocks.add(addChild(urls, anchors, 1));
		return blocks;
	}

	@Override
	public boolean needUpdate(WebPage webPage) {
		return true;
	}

}
