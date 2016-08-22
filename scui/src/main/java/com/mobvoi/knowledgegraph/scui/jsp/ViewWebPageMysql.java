package com.mobvoi.knowledgegraph.scui.jsp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
 
import org.apache.log4j.Logger;

import com.antbrains.mysqltool.PoolManager;
import com.antbrains.sc.archiver.MysqlArchiver;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.Link;
import com.antbrains.sc.data.WebPage; 

public class ViewWebPageMysql {
	protected static Logger logger = Logger.getLogger(ViewWebPageMysql.class);
	private  MysqlArchiver archiver;
	private ViewWebPageMysql() {
		Properties props=ConfigReader.getProps();
		String driver = props.getProperty("MYSQL_DRIVER");
		String url = props.getProperty("MYSQL_URL"); 
		String Name = props.getProperty("MYSQL_USER");
		String Password = props.getProperty("MYSQL_PASS");
		PoolManager.StartPool(url, Name, Password, driver);
		archiver = new MysqlArchiver();
	}

	private WebPage getWebPage(String url, String dbName) {
		// TODO read cache
		WebPage webPage = null;
		try {
			webPage = archiver.getWebPage(url);
			if (webPage == null)
				return null; 
			archiver.loadBlocks(webPage);
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return webPage;
	}

	public String viewBlock(String url, String dbName, int bId, int page) {
		if (dbName == null || dbName.equals(""))
			return "";
		if (!dbName.matches("\\w+"))
			return "invalid dbName: " + dbName;
		if (url == null || url.equals(""))
			return "";
		StringBuilder sb = new StringBuilder("");
		try {
			WebPage webPage = this.getWebPage(url, dbName);
			if (webPage == null)
				return "";
			List<Block> blocks = webPage.getBlocks();
			if (blocks == null || blocks.size() <= bId)
				return "";
			Block block = blocks.get(bId);
			this.printBlock(sb, block, page, dbName, bId, url);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return sb.toString();
	}

	private void printBlock(StringBuilder sb, Block b, int pageNo, String dbName, int bId, String pUrl) {
		if (b == null)
			return;
		List<Link> links = b.getLinks();
		if (links == null || links.size() == 0)
			return;
		if (pageNo < 1)
			pageNo = 1;
		int start = (pageNo - 1) * 10;
		int end = Math.min(start + 10, links.size());
		sb.append("<table>");
		for (int i = start; i < end; i++) {
			Link link = links.get(i);
			sb.append("<tr>");
			Map<String, String> attrs = link.getLinkAttrs();
			int pos = link.getPos();
			sb.append("<td>").append(pos).append("</td>");
			String cUrl = link.getWebPage().getUrl();
			cUrl = this.encodeParam(cUrl);
			String url = "./viewWebPage.jsp?url=" + cUrl + "&dbName=" + dbName;
			String linkText = null2Empty(link.getLinkText());
			sb.append("<td><a target='_blank' href='" + url + "'>").append(linkText).append("</a></td>");
			if (attrs != null && attrs.size() > 0) {
				sb.append("<td>").append(this.printAttrs(attrs, 3)).append("</td>");
			}
			sb.append("</tr>");
		}
		sb.append("<tr>");
		if (pageNo > 1) {
			String url = "./viewBlock.jsp?url=" + this.encodeParam(pUrl) + "&dbName=" + dbName + "&bId=" + bId
					+ "&pageNo=" + (pageNo - 1);
			sb.append("<td><a href='" + url + "'>上一页</a></td>");
		} else {
			sb.append("<td>上一页</td>");
		}
		if (end < links.size()) {
			String url = "./viewBlock.jsp?url=" + this.encodeParam(pUrl) + "&dbName=" + dbName + "&bId=" + bId
					+ "&pageNo=" + (pageNo + 1);
			sb.append("<td><a href='" + url + "'>下一页</a></td>");
		} else {
			sb.append("<td>下一页</td>");
		}
		sb.append("</tr>");

		sb.append("</table>");
	}

	public String search(String url, String dbName, boolean viewParent) {
		if (dbName == null || dbName.equals(""))
			return "";
		if (!dbName.matches("\\w+"))
			return "invalid dbName: " + dbName;
		if (url == null || url.equals(""))
			return "";
		StringBuilder sb = new StringBuilder("");
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			WebPage webPage = this.getWebPage(url, dbName);
			if (webPage == null) {
				sb.append("<h3>还没抓取</h3>");
				return sb.toString();
			}

			this.printBasicInfo(sb, webPage, sdf);
			Map<String, String> attrs = webPage.getAttrs();
			if (attrs == null) {
				attrs = new HashMap<>(0);
			}
			this.printAttrs(attrs, sb, "<h3>属性</h3>", null);

			List<Block> blocks = webPage.getBlocks();
			if (blocks != null && blocks.size() > 0) {
				for (int i = 0; i < blocks.size(); i++) {
					sb.append("<h3>Block " + (i + 1) + "</h3>");
					this.printBlock(blocks.get(i), sb, i, url, dbName);
				}
			}
			if (viewParent) {
				@SuppressWarnings("unchecked")
				Set<String> parents = (Set<String>) webPage.getOtherInfo().get("#%parents%#");
				this.printParents(sb, parents);
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return sb.toString();
	}

	private String null2Empty(String s) {
		return s == null ? "" : s;
	}

	private void printBlock(Block b, StringBuilder sb, int bId, String pUrl, String dbName) {
		if (b == null)
			return;
		List<Link> links = b.getLinks();
		if (links == null || links.size() == 0)
			return;
		sb.append("<table>");
		for (int i = 0; i < Math.min(10, links.size()); i++) {
			Link link = links.get(i);
			sb.append("<tr>");
			Map<String, String> attrs = link.getLinkAttrs();
			int pos = link.getPos();
			sb.append("<td>").append(pos).append("</td>");
			String cUrl = link.getWebPage().getUrl();
			cUrl = this.encodeParam(cUrl);
			String url = "./viewWebPage.jsp?url=" + cUrl + "&dbName=" + dbName;
			String linkText = null2Empty(link.getLinkText());
			sb.append("<td><a target='_blank' href='" + url + "'>").append(linkText).append("</a></td>");
			if (attrs != null && attrs.size() > 0) {
				sb.append("<td>").append(this.printAttrs(attrs, 3)).append("</td>");
			}
			sb.append("</tr>");
		}
		if (links.size() > 10) {
			String bUrl = "./viewBlock.jsp?url=" + this.encodeParam(pUrl) + "&bId=" + bId + "&dbName=" + dbName;
			sb.append("<tr><td><a href='" + bUrl + "'>查看更多...</a></td></tr>");
		}
		sb.append("</table>");
	}

	private void printParents(StringBuilder sb, Collection<String> pUrls) {
		if (pUrls.size() == 0)
			return;
		sb.append("<h3>父节点</h3>");
		sb.append("<table>");
		for (String pUrl : pUrls) {
			sb.append("<tr>").append("<td>").append("ID").append("</td>")
					.append("<td><a href='target='_blank' " + pUrl + "'>").append(pUrl).append("</a></td>")
					.append("</tr>");
		}
		;

		sb.append("</table>");
	}

	private void printBasicInfo(StringBuilder sb, WebPage wp, SimpleDateFormat sdf) {
		sb.append("<h3>网页基本信息</h3>");
		sb.append("<table>");

		sb.append("<tr>").append("<td>").append("url").append("</td>")
				.append("<td><a target='_blank' href='" + (wp.getUrl()) + "'>").append(wp.getUrl()).append("</a></td>")
				.append("</tr>");

		sb.append("<tr>").append("<td>").append("title").append("</td>").append("<td>").append(wp.getTitle())
				.append("</td>").append("</tr>");

		sb.append("<tr>").append("<td>").append("depth").append("</td>").append("<td>").append(wp.getDepth())
				.append("</td>").append("</tr>");

		Date d = wp.getLastVisitTime();
		String ds = "";
		if (d != null) {
			ds = sdf.format(d);
		}
		sb.append("<tr>").append("<td>").append("lastVisitTime").append("</td>").append("<td>").append(ds)
				.append("</td>").append("</tr>");

		sb.append("<tr>").append("<td>").append("redirectUrl").append("</td>").append("<td>")
				.append(wp.getRedirectedUrl()).append("</td>").append("</tr>");

		String html = wp.getContent();

		if (html == null) {
			html = "";
		} else {
			html = "<textarea cols='80' rows='10'>" + StringEscapeUtils.escapeHtml4(html) + "</textarea>";
		}

		sb.append("<tr>").append("<td>").append("html").append("</td>").append("<td>").append(html).append("</td>")
				.append("</tr>");

		sb.append("<table>");
	}

	private String printAttrs(Map<String, String> attrs, int cols) {
		if (attrs == null || attrs.size() == 0)
			return "";
		StringBuilder sb = new StringBuilder("<table>");
		int idx = 0;
		int mod = 0;
		for (Entry<String, String> entry : attrs.entrySet()) {
			mod = idx % cols;
			if (mod == 0) {
				sb.append("<tr>");
			}
			sb.append("<td>").append(StringEscapeUtils.escapeHtml4(entry.getKey())).append("</td>").append("<td>")
					.append(StringEscapeUtils.escapeHtml4(entry.getValue())).append("</td>");
			if (mod == cols - 1) {
				sb.append("</tr>");
			}
			idx++;
		}
		if (mod != cols - 1) {
			sb.append("</tr>");
		}
		sb.append("</table>");
		return sb.toString();
	}

	private void printAttrs(Map<String, String> attrs, StringBuilder sb, String title, String linkText) {
		if (attrs.size() == 0 && (linkText == null || linkText.trim().equals("")))
			return;
		sb.append(title);
		sb.append("<table>");
		sb.append("<tr><th>属性名</th><th>属性值</th></tr>");
		if (linkText != null && !linkText.trim().equals("")) {
			sb.append("<tr>").append("<td>").append("链接文本").append("</td>").append("<td>")
					.append(StringEscapeUtils.escapeHtml4(linkText)).append("</td>").append("</tr>");
		}
		for (Entry<String, String> entry : attrs.entrySet()) {
			sb.append("<tr>").append("<td>").append(StringEscapeUtils.escapeHtml4(entry.getKey())).append("</td>")
					.append("<td>").append(StringEscapeUtils.escapeHtml4(entry.getValue())).append("</td>")
					.append("</tr>");
		}
		sb.append("</table>");
	}

	private String encodeParam(String param) {
		if (param == null)
			return "";
		try {
			return java.net.URLEncoder.encode(param, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
			return "";
		}
	}

	private static ViewWebPageMysql instance;
	static {
		instance = new ViewWebPageMysql();
	}

	public static ViewWebPageMysql getInstance() {
		return instance;
	}
}
