package com.antbrains.sc.archiver;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.WebPage;

/**
 * Need thread safe
 * 
 * @author lili
 *
 */
public interface Archiver {
	public boolean insert2WebPage(WebPage webPage);

	public void saveUnFinishedWebPage(WebPage webPage, int failInc);

	public void updateWebPage(WebPage webPage);
	
	public void updateFinishTime(WebPage webPage, Date date);

	public void loadBlocks(WebPage webPage);

	public void insert2Block(Block block, WebPage webPage, int pos);

	public void updateBlock(Block block, WebPage webPage, int pos, boolean updateInfo);

	public void insert2Link(List<Integer> blockIds, List<Integer> parentIds, List<Integer> childIds,
			List<Integer> poses, List<String> linkTexts, List<Map<String, String>> attrsList);

	public void insert2Attr(WebPage webPage);

	public void saveLog(String taskId, String type, String msg, Date logTime, int level, String html);

	public void close();

	public void process404Url(String url);
}
