package com.antbrains.sc.extractor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIBuilder;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.archiver.NullArchiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.Link;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.tools.UrlUtils;

public abstract class BasicInfoExtractor implements Extractor {
	public static final String TEST_TASK_ID = "test-task";
	private static Pattern metaPattern = Pattern.compile("<meta\\s+([^>]*http-equiv=(\"|')?content-type(\"|')?[^>]*)>",
			Pattern.CASE_INSENSITIVE);
	private static Pattern charsetPattern = Pattern.compile("charset=\\s*([a-z][_\\-0-9a-z]*)",
			Pattern.CASE_INSENSITIVE);
	private static Pattern titlePtn = Pattern.compile("<title>([^<]*)</title>", Pattern.CASE_INSENSITIVE);

	protected boolean needUpdate(Date lastUpdate, int days, int hours, int minutes, int seconds) {
		if (lastUpdate == null)
			return true;
		long interval = 1000L * (seconds + 60 * minutes + 3600 * hours + 24 * 3600 * days);
		if (interval <= 0) {
			interval = Long.MAX_VALUE;
		}
		return System.currentTimeMillis() - lastUpdate.getTime() > interval;
	}

	public void testUrl(String url) {
		System.out.println("test: " + url);
		WebPage webPage = new WebPage();
		webPage.setUrl(url);
		HttpClientFetcher fetcher = new HttpClientFetcher("");
		fetcher.init();
		String content = null;
		try {
			content = fetcher.httpGet(webPage.getUrl());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (content == null) {
			System.out.println("can't get url: " + url);
			fetcher.close();
			return;
		}
		NekoHtmlParser parser = new NekoHtmlParser();
		try {
			parser.load(content, "UTF8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.extractProps(webPage, parser, fetcher, content, new NullArchiver(), TEST_TASK_ID);
		if (webPage.getAttrs() != null) {
			for (Entry<String, String> entry : webPage.getAttrs().entrySet()) {
				System.out.println(entry.getKey() + "\t" + entry.getValue());
			}
		}
		List<Block> blocks = this.extractBlock(webPage, parser, fetcher, content, new NullArchiver(), TEST_TASK_ID);
		if (blocks != null) {
			for (Block block : blocks) {
				System.out.println(block);
			}
		}
		fetcher.close();
	}

	@Override
	public boolean needSaveHtml(WebPage webPage) {
		return true;
	};

	@Override
	public boolean needAddChildren2FrontierIfNotUpdate(WebPage webPage) {
		return true;
	}

	@Override
	public void extractBasicInfo(WebPage webPage, String content, Archiver archiver, String taskId) {

		if (content == null)
			return;
		if (content.length() > 1024) {// head不应该太靠后
			content = content.substring(0, 1024);
		}
		Matcher m = titlePtn.matcher(content);
		if (m.find()) {
			String title = m.group(1);
			if (title.length() > 255) {
				title = title.substring(0, 255);
			}
			webPage.setTitle(title);
		}
		Matcher metaMatcher = metaPattern.matcher(content);
		String encoding = null;
		if (metaMatcher.find()) {
			Matcher charsetMatcher = charsetPattern.matcher(metaMatcher.group(1));
			if (charsetMatcher.find())
				encoding = new String(charsetMatcher.group(1));
		}
		webPage.setCharSet(encoding);

	}

	protected Block addChild(List<String> urls, List<String> anchors, int depth) {
		return addChild(urls, anchors, depth, null, true, true, true, true);
	}

	protected String normUrl(String url) {
		return url;
	}

	protected Block addChild(List<String> urls, List<String> anchors, int depth,
			List<Map<String, String>> attrsFromParent, boolean removeEmptyAnchor, boolean removeAnchorInUrl,
			boolean dedup, boolean encodeChinese) {
		Block block = new Block();
		List<Link> links = new ArrayList<>(urls.size());
		block.setLinks(links);
		Iterator<String> urlIter = urls.iterator();
		Iterator<String> anchorIter = anchors.iterator();
		Iterator<Map<String, String>> attrsIter = null;
		if (attrsFromParent != null) {
			attrsIter = attrsFromParent.iterator();
		}
		int pos = 0;
		Set<String> existed = new HashSet<>();
		while (urlIter.hasNext()) {
			WebPage child = new WebPage();
			String cUrl = urlIter.next();
			if (removeAnchorInUrl) {
				cUrl = UrlUtils.removeAnchor(cUrl);
			}
			if (encodeChinese) {
				cUrl = UrlUtils.encodeChinese(cUrl);
			}

			cUrl = this.normUrl(cUrl);

			child.setUrl(cUrl);
			child.setDepth(depth);
			if (attrsIter != null) {
				child.setAttrsFromParent(attrsIter.next());
			}

			String anchor = anchorIter.next();
			if (removeEmptyAnchor && anchor.equals(""))
				continue;
			if (dedup) {
				if (!existed.contains(cUrl)) {
					existed.add(cUrl);
				} else {
					continue;
				}
			}
			Link link = new Link();
			link.setLinkText(anchor);
			link.setPos(pos++);
			link.setWebPage(child);
			links.add(link);
		}
		return block;
	}

	public void setNotAddChild(Block b) {
		Map<String, Object> otherInfo = new HashMap<>(1);
		b.setOtherInfo(otherInfo);
		otherInfo.put(Block.OTHER_INFO_ADD_CHILD, false);
	}

	protected Block addChild(String[] urls, String[] anchors, int depth) {
		return addChild(urls, anchors, depth, null);
	}

	protected Block addChild(String[] urls, String[] anchors, int depth, List<Map<String, String>> attrsFromParent) {
		Block block = new Block();
		List<Link> links = new ArrayList<>(urls.length);
		block.setLinks(links);
		Iterator<Map<String, String>> attrsIter = null;
		if (attrsFromParent != null) {
			attrsIter = attrsFromParent.iterator();
		}
		for (int i = 0; i < urls.length; i++) {
			WebPage child = new WebPage();
			child.setUrl(urls[i]);
			child.setDepth(depth);
			if (attrsIter != null) {
				child.setAttrsFromParent(attrsIter.next());
			}

			Link link = new Link();
			link.setLinkText(anchors[i]);
			link.setPos(i);
			link.setWebPage(child);
			links.add(link);
		}
		return block;
	}

	protected static String rewriteUrl(String oldUrl, String key, String value) {
		try {

			URIBuilder ub = new URIBuilder(oldUrl);
			ub.setParameter(key, value);
			return ub.build().toURL().toString();

		} catch (Exception e) {

		}
		return null;
	}

	@Override
	public boolean needUpdate(WebPage webPage){
		return webPage.getLastVisitTime()!=null;
	}
	
	public static void main(String[] args) {
		String oldUrl = "http://shouji.baidu.com/software/list?cid=506";
		String s = rewriteUrl(oldUrl, "page", "3");
		System.out.println(s);
		oldUrl = "http://shouji.baidu.com/software/list?page=1&cid=506";
		s = rewriteUrl(oldUrl, "page", "5");
		System.out.println(s);
	}
}
