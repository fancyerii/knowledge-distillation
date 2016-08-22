package com.antbrains.sc.extractor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.antbrains.httpclientfetcher.BadResponseException;
import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.Link;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.frontier.Frontier;

public abstract class UrlPatternExtractor {
	protected static Logger logger = Logger.getLogger(UrlPatternExtractor.class);

	protected Date startDate;

	public UrlPatternExtractor() {
		startDate = new java.util.Date();
	}

	/**
	 * 
	 * @param url
	 * @param depth
	 * @return
	 */
	public abstract Extractor getExtractor(String url, String redirectUrl, int depth);

	protected int calcPriority(WebPage child) {
		return child.getDepth();
	}

	public void process(WebPage webPage, HttpClientFetcher fetcher, boolean followRedirect, Frontier frontier,
			Archiver archiver, String taskId, boolean isUpdate) {
		String url = webPage.getUrl();
		if (url.startsWith("http://localhost/"))
			return;

		if (webPage.getRedirectedUrl() != null && followRedirect) {
			url = webPage.getRedirectedUrl();
		}
		Extractor extractor = this.getExtractor(url, webPage.getRedirectedUrl(), webPage.getDepth());

		if (extractor == null) {
			logger.warn("can't get extractor for " + url + " depth:" + webPage.getDepth() + " otherInfo: "
					+ webPage.getOtherInfo());
			return;
		} else if (extractor instanceof NullExtractor) {
			logger.debug("nullext: " + url);
			return;
		} else {
			// logger.info("process: "+url);
		}

		boolean needUpdate = extractor.needUpdate(webPage);
		if (!needUpdate) {
			boolean needAddChildren = extractor.needAddChildren2FrontierIfNotUpdate(webPage);
			if (needAddChildren) {
				// logger.info("addChildren: "+webPage.getUrl());
				archiver.loadBlocks(webPage);
				List<Block> existedBlocks = webPage.getBlocks();
				for (Block block : existedBlocks) {
					addChildren2Frontier(url, frontier, block, webPage.getDepth(), 1);
				}
			} else {
				// logger.info("skipUpdate: "+webPage.getUrl());
			}

			return;
		}
		logger.info("process: " + url);
		String content = null;

		// 如果更新时间比程序开始时间晚，那么也不需要更新
		// 表明有两个或多个父亲节点，是否需要更新？
		// 如果一个节点有两个父亲，其中一个是主抓取流程，另一个只是为了建立关联，那么应该
		// 在BlockConfig里设置 addChild2Frontier
		// 如果两条路径都需要抓取，那么注意minDepth和maxDepth的限制，这个时候判断下面的条件
		// 就能避免一个节点因为两条路径重复抓取
		if (webPage.getLastVisitTime() != null && webPage.getLastVisitTime().getTime() >= this.startDate.getTime()) {
			logger.warn("LastVisitTime >= startTime: " + webPage.getUrl());

			return;
		}

		boolean badUrl = false;
		try {
			// content = fetcher.httpGet(url);
			String[] arr = fetcher.httpGetReturnRedirect(url, 3);
			if (arr == null) {
				content = null;
			} else {
				content = arr[0];
				if (webPage.getUrl().equals(arr[1])) {
					arr[1] = null;
				}
				webPage.setRedirectedUrl(arr[1]);
			}
			String redirectedUrl = webPage.getRedirectedUrl();
			if (redirectedUrl != null && !redirectedUrl.equals(webPage.getUrl())) {
				extractor = this.getExtractor(url, redirectedUrl, webPage.getDepth());
				if (extractor == null) {
					logger.warn("can't get extractor for " + url + " depth:" + webPage.getDepth() + " otherInfo: "
							+ webPage.getOtherInfo());
					return;
				} else if (extractor instanceof NullExtractor) {
					logger.info("nullext: " + url);
					return;
				} else {
					// logger.info("process: "+url);
				}
			}
		} catch (BadResponseException ee) {
			if (ee.getCode() == 404) {
				logger.info("badUrl: " + url);
				badUrl = true; // 404 or 500?
				archiver.process404Url(url);
			} else {

			}
		} catch (Exception e) {

		}
		if (content == null) {
			logger.error("can't get: " + url);
			if (!badUrl) {
				archiver.saveUnFinishedWebPage(webPage, 1);
			}
			return;
		}
		boolean saveHtml = extractor.needSaveHtml(webPage);
		if (saveHtml) {
			webPage.setContent(content);
		}
		NekoHtmlParser parser = new NekoHtmlParser();
		try {
			parser.load(content, "UTF8");
		} catch (Exception e) {
			logger.error("can't parse: " + url);
			return;
		}
		extractor.extractBasicInfo(webPage, content, archiver, taskId);
		extractor.extractProps(webPage, parser, fetcher, content, archiver, taskId);
		webPage.setLastVisitTime(new Date());
		webPage.setCrawlPriority(webPage.getDepth());

		archiver.updateWebPage(webPage);
		List<Block> blocks = extractor.extractBlock(webPage, parser, fetcher, content, archiver, taskId);
		if (blocks != null) {
			this.processBlocks(webPage, blocks, url, frontier, archiver, isUpdate);
		}

	}

	private void processBlocks(WebPage webPage, List<Block> extractedBlocks, String url, Frontier frontier,
			Archiver archiver, boolean isUpdate) {
		archiver.loadBlocks(webPage);
		List<Block> existedBlocks = webPage.getBlocks();

		for (int i = 0; i < Math.max(existedBlocks.size(), extractedBlocks.size()); i++) {

			Block existedBlock = (i < existedBlocks.size()) ? existedBlocks.get(i) : null;
			Block extractedBlock = (i < extractedBlocks.size()) ? extractedBlocks.get(i) : null;
			// block的数据有更新
			boolean needUpdate = false;

			if (existedBlock == null && extractedBlock != null) {// 新抽取到的Block
				// 新插入的Block，它的孩子会马上访问
				needUpdate = true;
				extractedBlock.setLastVisitTime(new java.util.Date());

				archiver.insert2Block(extractedBlock, webPage, i);

			} else if (existedBlock != null && extractedBlock == null) {// 原来的某个block现在没有了

			} else if (existedBlock != null && extractedBlock != null) {
				// 复用原来block的id，只是更新tags
				extractedBlock.setId(existedBlock.getId());
				if (true) {// 如果需要更新，那么修改访问时间
					needUpdate = true;
					extractedBlock.setLastVisitTime(new java.util.Date());
				}
				archiver.updateBlock(extractedBlock, webPage, i, false);
			} else {
				logger.error("unexpected here for block processing");
			}
			if (needUpdate) {// 如果需要更新
				if (extractedBlock == null) {
					// 如果抽取不到新的block，那么可能是这次没有更新，或者更新失败
					// 那么直接把原来的加入frontier
					logger.warn("extractedBlock==null");
					addChildren2Frontier(url, frontier, existedBlock, webPage.getDepth(), 1);
				} else {// extractedBlock!=null
					int linkSize = extractedBlock.getLinks().size();
					List<Integer> bids = new ArrayList<Integer>(linkSize);
					List<Integer> pids = new ArrayList<Integer>(linkSize);
					// List<Map<String,String>> attrsList
					List<Integer> cids = new ArrayList<Integer>(linkSize);
					List<Integer> poses = new ArrayList<Integer>(linkSize);
					List<String> linkTexts = new ArrayList<String>(linkSize);
					List<Map<String, String>> attrsList = new ArrayList<Map<String, String>>();

					for (int j = 0; j < linkSize; j++) {
						this.addBlockLinks(extractedBlock, existedBlock, j, url, bids, pids, cids, poses, linkTexts,
								attrsList, webPage, 0, frontier, 1, archiver, isUpdate);

					} // for
					if (bids.size() > 0) {
						archiver.insert2Link(bids, pids, cids, poses, linkTexts, attrsList);
					}
				}
			}

		} // for

	}

	private void addBlockLinks(Block extractedBlock, Block existedBlock, int j, String url, List<Integer> bids,
			List<Integer> pids, List<Integer> cids, List<Integer> poses, List<String> linkTexts,
			List<Map<String, String>> attrsList, WebPage webPage, int lastLinkPos, Frontier frontier, int childPriority,
			Archiver archiver, boolean isUpdate) {
		Link link = extractedBlock.getLinks().get(j);
		link.getWebPage().setTags(extractedBlock.getTags());
		WebPage existedPage = null;
		if (existedBlock != null) {
			existedPage = existedBlock.getExistWebPage(link.getWebPage(), link.getLinkText());
		}
		if (existedPage == null) {// 页面不存在
			boolean res = archiver.insert2WebPage(link.getWebPage());
			if (!res) {
				logger.error(url + "->" + link.getWebPage().getUrl() + " insert error");
				return;
			}
		} else {
			// 是否parent那里抽取了新的属性
			Map<String, String> attrsFromParent = link.getWebPage().getAttrsFromParent();
			link.getWebPage().setId(existedPage.getId());

			if (attrsFromParent != null && attrsFromParent.size() > 0) {
				existedPage.setAttrsFromParent(attrsFromParent);
				archiver.insert2Attr(existedPage);
			}

		}
		// archiver.insert2Link(extractedBlock.getId(),webPage.getId(),
		// link.getWebPage().getId(), j,
		// link.getLinkText(), link.getLinkAttrs());
		bids.add(extractedBlock.getId());
		pids.add(webPage.getId());
		cids.add(link.getWebPage().getId());
		poses.add(j + lastLinkPos);
		linkTexts.add(link.getLinkText());
		attrsList.add(link.getLinkAttrs());
		boolean addChild2Frontier = true;
		if (extractedBlock != null) {
			Map<String, Object> otherInfo = extractedBlock.getOtherInfo();
			if (otherInfo != null) {
				Boolean b = (Boolean) otherInfo.get(Block.OTHER_INFO_ADD_CHILD);
				if (b != null && b.booleanValue() == false) {
					addChild2Frontier = false;
				}
			}
		}
		if (addChild2Frontier) {
			if (isUpdate) {
				if (existedPage != null) {
					Extractor ext = this.getExtractor(existedPage.getUrl(), existedPage.getRedirectedUrl(),
							webPage.getDepth() + 1);
					if (ext != null && !ext.needUpdate(existedPage)
							&& !ext.needAddChildren2FrontierIfNotUpdate(existedPage)) {
						addChild2Frontier = false;
						logger.info("skipAddChild2Frontier: " + existedPage.getUrl());
					}
				}

			}
		}
		if (addChild2Frontier) {
			if (existedPage != null) {
				existedPage.setCrawlPriority(this.calcPriority(existedPage));
				frontier.addWebPage(existedPage, 0);
			} else {
				WebPage wp = link.getWebPage();
				wp.setCrawlPriority(this.calcPriority(wp));
				frontier.addWebPage(wp, 0);
			}
		} else {
			// logger.info("skip child to frontier: "+url);
		}

	}

	public static final String PARENT_URL = "parent_url";

	private void addChildren2Frontier(String pUrl, Frontier frontier, Block block, int parentLevel, int childPriority) {
		if (block == null)
			return;
		if (block.getLinks() != null) {
			for (Link link : block.getLinks()) {
				WebPage page = link.getWebPage();
				page.getOtherInfo().put(PARENT_URL, pUrl);
				page.setDepth(parentLevel + 1);
				page.setCrawlPriority(this.calcPriority(page));
				frontier.addWebPage(page, 0);
			}
		}
	}

}
