package com.github.fancyerii.takungpaocrawler.extractor;

import java.util.List;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.extractor.BasicInfoExtractor;

public class Level1Extractor extends BasicInfoExtractor{

	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		
	}

	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) { 
		return null;
	}
	

}
