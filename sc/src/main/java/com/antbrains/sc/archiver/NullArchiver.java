package com.antbrains.sc.archiver;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;

/**
 * for debug useage
 * 
 * @author lili
 *
 */
public class NullArchiver implements Archiver {

	@Override
	public boolean insert2WebPage(WebPage webPage) {
		return true;
	}

	@Override
	public void saveUnFinishedWebPage(WebPage webPage, int failInc) {

	}

	@Override
	public void updateWebPage(WebPage webPage) {

	}

	@Override
	public void loadBlocks(WebPage webPage) {

	}

	@Override
	public void insert2Block(Block block, WebPage webPage, int pos) {

	}

	@Override
	public void updateBlock(Block block, WebPage webPage, int pos, boolean updateInfo) {

	}

	@Override
	public void insert2Link(List<Integer> blockIds, List<Integer> parentIds, List<Integer> childIds,
			List<Integer> poses, List<String> linkTexts, List<Map<String, String>> attrsList) {

	}

	@Override
	public void insert2Attr(WebPage webPage) {

	}

	@Override
	public void saveLog(String taskId, String type, String msg, Date logTime, int level, String html) {
		System.out.println(
				"savelog: " + taskId + "\t" + type + "\t" + msg + "\t" + logTime + "\t" + level + "\t" + "html");
	}

	@Override
	public void close() {

	}

	@Override
	public void process404Url(String url) {

	}

	@Override
	public void updateFinishTime(WebPage webPage, Date date) {

	}

}
