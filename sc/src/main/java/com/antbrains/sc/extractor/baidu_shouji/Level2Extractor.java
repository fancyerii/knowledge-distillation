package com.antbrains.sc.extractor.baidu_shouji;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.w3c.dom.NodeList;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.extractor.BasicInfoExtractor;
import com.antbrains.sc.tools.GlobalConstants;

public class Level2Extractor extends BasicInfoExtractor {

	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {

	}

	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		List<Block> blocks = new ArrayList<>(1);
		// get total page
		int total = getTotalPage(parser);
		if (total < 1) {
			archiver.saveLog(taskId, "getTotalPageError", "getTotalPageError", new Date(),
					GlobalConstants.LOG_LEVEL_WARN, "");
			return null;
		}

		for (int i = 1; i <= total; i++) {
			if (i > 1) {
				String newUrl = super.rewriteUrl(webPage.getUrl(), "page_num", "" + i);
				content = null;
				try {
					content = fetcher.httpGet(newUrl, 3);
				} catch (Exception e) {
				}
				if (content == null) {
					archiver.saveLog(taskId, "getContent", "url:" + newUrl, new Date(), GlobalConstants.LOG_LEVEL_WARN,
							"");
					continue;
				}
				try {
					parser.load(content, "UTF8");
				} catch (Exception e) {
					archiver.saveLog(taskId, "loadContent", "url:" + newUrl, new Date(), GlobalConstants.LOG_LEVEL_WARN,
							"");
					continue;
				}
			}

			// doparser
		}
		return blocks;
	}

	private int getTotalPage(NekoHtmlParser parser) {
		NodeList nl = parser.selectNodes("//DIV[@class='pager']//A[@href]");
		int max = 0;
		for (int i = 0; i < nl.getLength(); i++) {
			String s = nl.item(i).getTextContent().trim();
			try {
				int page = Integer.valueOf(s);
				if (page > max) {
					max = page;
				}
			} catch (Exception e) {

			}
		}

		return max;
	}

	@Override
	public boolean needUpdate(WebPage webPage) {
		return true;
	}

}
