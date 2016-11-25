package com.github.fancyerii.pusa123crawler.extractor;

import java.util.List;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.extractor.BasicInfoExtractor;

public class Level2Extractor extends BasicInfoExtractor {

	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean needUpdate(WebPage webPage) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
