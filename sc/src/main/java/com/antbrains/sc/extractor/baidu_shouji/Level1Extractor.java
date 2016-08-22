package com.antbrains.sc.extractor.baidu_shouji;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.archiver.NullArchiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.extractor.BasicInfoExtractor;
import com.antbrains.sc.tools.UrlUtils;

public class Level1Extractor extends BasicInfoExtractor {

	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {

	}

	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {

		List<Block> blocks = new ArrayList<Block>(1);
		NodeList nl = parser.selectNodes("//DIV[@id='doc']/UL/LI/DIV/A");
		List<String> urls = new ArrayList<>();
		List<String> texts = new ArrayList<>();
		for (int i = 0; i < nl.getLength(); i++) {
			Node a = nl.item(i);
			String text = a.getTextContent();
			String href = a.getAttributes().getNamedItem("href").getNodeValue();
			if (href == null || href.trim().equals("")) {
				continue;
			}
			String abUrl = UrlUtils.getAbsoluteUrl(webPage.getUrl(), href);
			urls.add(abUrl);
			texts.add(text);
		}
		blocks.add(addChild(urls, texts, 2));
		return blocks;
	}

	public static void main(String[] args) {
		HttpClientFetcher fetcher = new HttpClientFetcher("test");
		fetcher.init();
		String url = "http://shouji.baidu.com/software";
		WebPage webPage = new WebPage();
		webPage.setUrl(url);
		webPage.setDepth(1);
		String s = null;
		try {
			s = fetcher.httpGet(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		NekoHtmlParser parser = new NekoHtmlParser();

		try {
			parser.load(s, "UTF8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		Level1Extractor ext = new Level1Extractor();
		List<Block> blocks = ext.extractBlock(webPage, parser, fetcher, s, new NullArchiver(), "test_task");
		for (Block block : blocks) {
			System.out.println(block);
		}
	}

	@Override
	public boolean needUpdate(WebPage webPage) {
		return true;
	}
}
