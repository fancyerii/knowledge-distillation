package com.antbrains.sc.worker;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.antbrains.httpclientfetcher.HttpClientFetcher;
import com.antbrains.sc.archiver.Archiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.CrawlTask;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.extractor.Extractor;
import com.antbrains.sc.extractor.UrlPatternExtractor;
import com.antbrains.sc.frontier.Frontier;
import com.antbrains.sc.tools.StopableWorker;

public class FetcherAndExtractor extends StopableWorker implements Runnable {
	protected static Logger logger = Logger.getLogger(FetcherAndExtractor.class);
	private BlockingQueue<CrawlTask> tasks;
	private UrlPatternExtractor extractor;
	private HttpClientFetcher fetcher;
	private Frontier frontier;
	private Archiver archiver;
	private String taskId;
	private boolean isUpdate;

	public FetcherAndExtractor(BlockingQueue<CrawlTask> tasks, UrlPatternExtractor extractor, HttpClientFetcher fetcher,
			Frontier frontier, Archiver archiver, String taskId, boolean isUpdate) {
		this.tasks = tasks;
		this.extractor = extractor;
		this.fetcher = fetcher;
		this.frontier = frontier;
		this.archiver = archiver;
		this.taskId = taskId;
		this.isUpdate = isUpdate;
	}

	@Override
	public void run() {
		while (!bStop) {
			try {
				CrawlTask task = tasks.poll(5, TimeUnit.SECONDS);
				if (task == null)
					continue;
				WebPage webPage = new WebPage();
				webPage.setUrl(task.url);
				webPage.setDepth(task.depth);
				if (task.lastVisit > 0) {
					webPage.setLastVisitTime(new Date(task.lastVisit));
				}
				if(task.lastFinish>0){
					webPage.setLastFinishTime(new Date(task.lastFinish));
				}
				webPage.setId(task.id);
				webPage.setRedirectedUrl(task.redirectedUrl);
				webPage.setFailCount(task.failCount);
				extractor.process(webPage, fetcher, true, frontier, archiver, taskId, isUpdate);

			} catch (InterruptedException e) {

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

		}

		super.finished();
		logger.info("I am stopped");
	}

}
