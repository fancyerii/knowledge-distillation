package com.antbrains.sc.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.io.compress.Compression.Algorithm;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.apache.hadoop.hbase.util.Bytes;

import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.Link;
import com.antbrains.sc.data.WebPage;

public class SCHbaseTool {
	public static final String TB_WEBPAGE = ".wp";
	public static final String TB_FAILED = ".fl";
	public static final String TB_LINK = ".lk";
	public static final String TB_RLINK = ".rk";
	public static final String TB_BADURL = ".bu";

	public static final String USER_COL_RPEFIX = "u.";

	public static final String COL_WEBPAGE_URL = "url";
	public static final String COL_WEBPAGE_REDIRECTED_URL = "ru";
	public static final String COL_WEBPAGE_TITLE = "tt";
	public static final String COL_WEBPAGE_DEPTH = "dp";
	public static final String COL_WEBPAGE_LAST_VISIT = "lv";
	public static final String COL_WEBPAGE_LAST_FINISH = "lf";
	public static final String COL_WEBPAGE_HTML = "hl";

	public static final String COL_BADURL_URL = "url";

	public static final String COL_FAILED_URL = "url";
	public static final String COL_FAILED_REDIRECTED_URL = "ru";
	public static final String COL_FAILED_DEPTH = "dp";
	public static final String COL_FAILED_LAST_VISIT = "lv";

	public static final String COL_LINK_POS = "ps";
	public static final String COL_LINK_LINKTEXT = "lt";
	public static final String COL_LINK_CURL = "url";

	public static final String COL_RLINK_PURL = "url";

	public static final String CF = "cf";
	public static final String CF2 = "cf2";

	public static final byte[] CF_BT = Bytes.toBytes(CF);
	public static final byte[] CF2_BT = Bytes.toBytes(CF2);

	public static final byte[] COL_WEBPAGE_URL_BT = Bytes.toBytes(COL_WEBPAGE_URL);
	public static final byte[] COL_WEBPAGE_REDIRECTED_URL_BT = Bytes.toBytes(COL_WEBPAGE_REDIRECTED_URL);
	public static final byte[] COL_WEBPAGE_TITLE_BT = Bytes.toBytes(COL_WEBPAGE_TITLE);
	public static final byte[] COL_WEBPAGE_DEPTH_BT = Bytes.toBytes(COL_WEBPAGE_DEPTH);
	public static final byte[] COL_WEBPAGE_LAST_VISIT_BT = Bytes.toBytes(COL_WEBPAGE_LAST_VISIT);
	public static final byte[] COL_WEBPAGE_LAST_FINISH_BT = Bytes.toBytes(COL_WEBPAGE_LAST_FINISH);
	public static final byte[] COL_WEBPAGE_HTML_BT = Bytes.toBytes(COL_WEBPAGE_HTML);

	public static final byte[] COL_BADURL_URL_BT = Bytes.toBytes(COL_BADURL_URL);

	public static final byte[] COL_FAILED_URL_BT = Bytes.toBytes(COL_FAILED_URL);
	public static final byte[] COL_FAILED_REDIRECTED_URL_BT = Bytes.toBytes(COL_FAILED_REDIRECTED_URL);
	public static final byte[] COL_FAILED_DEPTH_BT = Bytes.toBytes(COL_FAILED_DEPTH);
	public static final byte[] COL_FAILED_LAST_VISIT_BT = Bytes.toBytes(COL_FAILED_LAST_VISIT);

	public static final byte[] COL_LINK_POS_BT = Bytes.toBytes(COL_LINK_POS);
	public static final byte[] COL_LINK_LINKTEXT_BT = Bytes.toBytes(COL_LINK_LINKTEXT);
	public static final byte[] COL_LINK_CURL_BT = Bytes.toBytes(COL_LINK_CURL);

	public static final byte[] COL_RLINK_PURL_BT = Bytes.toBytes(COL_RLINK_PURL);

	public static Date getLastVisit(HConnection conn, String dbName, String url) throws Exception {
		if (url == null || url.length() == 0)
			return null;
		HTableInterface table = null;
		try {
			table = conn.getTable(dbName + TB_WEBPAGE);
			byte[] md5 = DigestUtils.md5(url);
			Get get = new Get(md5);
			get.addColumn(CF_BT, COL_WEBPAGE_LAST_VISIT_BT);
			Result r = table.get(get);
			if (r.isEmpty())
				return null;

			byte[] bytes = r.getValue(CF_BT, COL_WEBPAGE_LAST_VISIT_BT);
			if (bytes != null) {
				return new java.util.Date(Bytes.toLong(bytes));
			} else {
				return null;
			}
		} finally {
			if (table != null) {
				table.close();
			}
		}
	}

	public static WebPage getFromResult(Result r, boolean needHtml) {
		WebPage wp = new WebPage();
		wp.setUrl(Bytes.toString(r.getValue(CF_BT, COL_WEBPAGE_URL_BT)));
		wp.setRedirectedUrl(Bytes.toString(r.getValue(CF_BT, COL_WEBPAGE_REDIRECTED_URL_BT)));
		wp.setTitle(Bytes.toString(r.getValue(CF_BT, COL_WEBPAGE_TITLE_BT)));
		wp.setDepth(Bytes.toInt(r.getValue(CF_BT, COL_WEBPAGE_DEPTH_BT)));
		byte[] bytes = r.getValue(CF_BT, COL_WEBPAGE_LAST_VISIT_BT);
		if (bytes != null) {
			wp.setLastVisitTime(new java.util.Date(Bytes.toLong(bytes)));
		}
		bytes = r.getValue(CF_BT, COL_WEBPAGE_LAST_FINISH_BT);
		if (bytes != null) {
			wp.setLastFinishTime(new java.util.Date(Bytes.toLong(bytes)));
		}
		if (needHtml) {
			wp.setContent(Bytes.toString(r.getValue(CF2_BT, COL_WEBPAGE_HTML_BT)));
		}

		// get attributes
		NavigableMap<byte[], byte[]> familyMap = r.getFamilyMap(CF_BT);
		Map<String, String> attrs = new HashMap<>();
		wp.setAttrs(attrs);
		for (Entry<byte[], byte[]> entry : familyMap.entrySet()) {
			String key = Bytes.toString(entry.getKey());
			if (key.startsWith(USER_COL_RPEFIX)) {
				String value = Bytes.toString(entry.getValue());
				key = key.substring(2);
				attrs.put(key, value);
			}
		}
		return wp;
	}

	public static WebPage getWebPage(HConnection conn, String dbName, String url, boolean needHtml) throws Exception {
		if (url == null || url.length() == 0)
			return null;
		HTableInterface table = null;
		try {
			table = conn.getTable(dbName + TB_WEBPAGE);
			byte[] md5 = DigestUtils.md5(url);
			Get get = new Get(md5);
			if (!needHtml) {
				get.addFamily(CF_BT);
			}
			Result r = table.get(get);
			if (r.isEmpty())
				return null;
			return getFromResult(r, needHtml);
		} finally {
			if (table != null) {
				table.close();
			}
		}
	}

	public static List<WebPage> getFailed(HConnection conn, String dbName, int count, boolean deleteAfterGet)
			throws Exception {
		HTableInterface table = null;
		ResultScanner rs = null;
		List<WebPage> result = new ArrayList<>(count);
		try {
			table = conn.getTable(dbName + TB_FAILED);
			Scan scan = new Scan();
			scan.setMaxResultSize(count);

			rs = table.getScanner(scan);
			List<Delete> deletes = null;
			if (deleteAfterGet) {
				deletes = new ArrayList<>(count);
			}
			new ArrayList<>();
			for (Result r : rs) {
				WebPage wp = new WebPage();
				byte[] keys = r.getRow();
				int failCount = Bytes.toInt(keys, 0);
				int priority = Integer.MAX_VALUE - Bytes.toInt(keys, Bytes.SIZEOF_INT);
				wp.setFailCount(failCount);
				wp.setCrawlPriority(priority);
				wp.setUrl(Bytes.toString(r.getValue(CF_BT, COL_FAILED_URL_BT)));
				wp.setDepth(Bytes.toInt(r.getValue(CF_BT, COL_FAILED_DEPTH_BT)));
				wp.setRedirectedUrl(Bytes.toString(r.getValue(CF_BT, COL_FAILED_REDIRECTED_URL_BT)));
				byte[] bytes = r.getValue(CF_BT, COL_FAILED_LAST_VISIT_BT);
				if (bytes != null) {
					wp.setLastVisitTime(new java.util.Date(Bytes.toLong(bytes)));
				}
				result.add(wp);
				if (deleteAfterGet) {
					deletes.add(new Delete(keys));
				}
				if (result.size() == count)
					break;
			}
			if (deleteAfterGet) {
				table.delete(deletes);
			}
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {

				}
			}
			if (table != null) {
				table.close();
			}
		}

		return result;
	}

	private static final int FAILED_KEY_SIZE = Bytes.SIZEOF_INT * 2 + Bytes.SIZEOF_LONG + 16;

	public static void saveFailed(HConnection conn, String dbName, List<WebPage> webPages) throws Exception {
		HTableInterface table = null;
		try {
			List<Put> puts = new ArrayList<Put>(webPages.size());
			for (WebPage webPage : webPages) {
				String url = webPage.getUrl();
				if (url == null || url.trim().equals(""))
					continue;
				byte[] md5 = DigestUtils.md5(url);

				String redirectedUrl = webPage.getRedirectedUrl();
				byte[] key = new byte[FAILED_KEY_SIZE];
				// key由 failCount + priority + currentTime组成，其中priority降序，所以取反
				int offset = Bytes.putInt(key, 0, webPage.getFailCount());
				offset = Bytes.putInt(key, offset, Integer.MAX_VALUE - webPage.getCrawlPriority());
				offset = Bytes.putLong(key, offset, System.currentTimeMillis());
				Bytes.putBytes(key, offset, md5, 0, 16);
				Put p = new Put(key);
				p.add(CF_BT, COL_FAILED_URL_BT, Bytes.toBytes(url));
				if (redirectedUrl != null) {
					p.add(CF_BT, COL_FAILED_REDIRECTED_URL_BT, Bytes.toBytes(redirectedUrl));
				}
				p.add(CF_BT, COL_FAILED_DEPTH_BT, Bytes.toBytes(webPage.getDepth()));
				if (webPage.getLastVisitTime() != null) {
					p.add(CF_BT, COL_FAILED_LAST_VISIT_BT, Bytes.toBytes(webPage.getLastVisitTime().getTime()));
				}
				puts.add(p);
			}
			table = conn.getTable(dbName + TB_FAILED);
			table.put(puts);
		} finally {
			if (table != null) {
				table.close();
			}
		}
	}

	public static void updateFailUrls(HConnection conn, String dbName, List<String> urls) throws IOException {
		HTableInterface table = null;
		try {
			List<Put> puts = new ArrayList<Put>(urls.size());
			for (String url : urls) {
				byte[] md5 = DigestUtils.md5(url);
				Put p = new Put(md5);
				p.add(CF_BT, COL_BADURL_URL_BT, Bytes.toBytes(url));
				puts.add(p);
			}
			table = conn.getTable(dbName + TB_FAILED);
			table.put(puts);
		} finally {
			if (table != null) {
				table.close();
			}
		}
	}

	public static void updateWebPages(HConnection conn, String dbName, List<WebPage> webPages) throws Exception {
		HTableInterface table = null;
		try {
			List<Put> puts = new ArrayList<Put>(webPages.size());
			for (WebPage webPage : webPages) {
				String url = webPage.getUrl();
				if (url == null || url.trim().equals(""))
					continue;
				byte[] md5 = DigestUtils.md5(url);
				Put p = new Put(md5);
				String redirectedUrl = webPage.getRedirectedUrl();
				String title = webPage.getTitle();
				// url
				p.add(CF_BT, COL_WEBPAGE_URL_BT, Bytes.toBytes(url));
				// redirected url
				if (redirectedUrl != null) {
					p.add(CF_BT, COL_WEBPAGE_REDIRECTED_URL_BT, Bytes.toBytes(redirectedUrl));
				}
				if (title != null && title.length() > 0) {
					p.add(CF_BT, COL_WEBPAGE_TITLE_BT, Bytes.toBytes(title));
				}
				p.add(CF_BT, COL_WEBPAGE_DEPTH_BT, Bytes.toBytes(webPage.getDepth()));
				if (webPage.getLastVisitTime() != null) {
					p.add(CF_BT, COL_WEBPAGE_LAST_VISIT_BT, Bytes.toBytes(webPage.getLastVisitTime().getTime()));
				}
				if (webPage.getLastFinishTime() != null) {
					p.add(CF_BT, COL_WEBPAGE_LAST_FINISH_BT, Bytes.toBytes(webPage.getLastFinishTime().getTime()));
				}
				if (webPage.getContent() != null) {
					p.add(CF2_BT, COL_WEBPAGE_HTML_BT, Bytes.toBytes(webPage.getContent()));
				}
				// 其它属性
				Map<String, String> attrs = webPage.getAttrs();
				if (attrs != null) {
					for (Entry<String, String> entry : attrs.entrySet()) {
						if (entry.getKey() == null || entry.getValue() == null)
							continue;
						p.add(CF_BT, Bytes.toBytes(USER_COL_RPEFIX + entry.getKey()), Bytes.toBytes(entry.getValue()));
					}
				}
				Map<String, String> attrsFromParent = webPage.getAttrsFromParent();
				if (attrsFromParent != null) {
					for (Entry<String, String> entry : attrsFromParent.entrySet()) {
						if (entry.getKey() == null || entry.getValue() == null)
							continue;
						p.add(CF_BT, Bytes.toBytes(USER_COL_RPEFIX + entry.getKey()), Bytes.toBytes(entry.getValue()));
					}
				}

				puts.add(p);
			}
			table = conn.getTable(dbName + TB_WEBPAGE);
			table.put(puts);
		} finally {
			if (table != null) {
				table.close();
			}
		}
	}

	private static final int BLOCK_KEY_LEN = 32 + Bytes.SIZEOF_INT;

	public static void addBlocks(HConnection conn, String dbName, List<BlockInfo> blocks) throws Exception {
		HTableInterface table1 = null;
		HTableInterface table2 = null;
		try {
			List<Put> puts1 = new ArrayList<Put>(blocks.size());
			List<Put> puts2 = new ArrayList<Put>(blocks.size());
			for (BlockInfo bi : blocks) {
				Block b = bi.getBlock();
				if (b == null)
					continue;
				String pUrl = bi.getParentUrl();
				if (pUrl == null || pUrl.length() == 0)
					continue;
				byte[] pMd5 = DigestUtils.md5(pUrl);
				int bId = bi.getPos();
				List<Link> links = b.getLinks();
				if (links == null)
					continue;
				int pos = 0;
				for (Link link : links) {
					String cUrl = link.getWebPage().getUrl();
					if (cUrl == null || cUrl.length() == 0)
						continue;
					byte[] cMd5 = DigestUtils.md5(cUrl);
					byte[] key1 = new byte[BLOCK_KEY_LEN];
					Bytes.putBytes(key1, 0, pMd5, 0, 16);
					int offset = Bytes.putInt(key1, 16, bId);
					Bytes.putBytes(key1, offset, cMd5, 0, 16);
					Put p1 = new Put(key1);
					p1.add(CF_BT, COL_LINK_POS_BT, Bytes.toBytes(pos));
					p1.add(CF_BT, COL_LINK_CURL_BT, Bytes.toBytes(cUrl));
					String linkText = link.getLinkText();
					if (linkText != null) {
						p1.add(CF_BT, COL_LINK_LINKTEXT_BT, Bytes.toBytes(linkText));
					}
					Map<String, String> attrs = link.getLinkAttrs();
					if (attrs != null) {
						for (Entry<String, String> entry : attrs.entrySet()) {
							if (entry.getKey() == null || entry.getValue() == null)
								continue;
							p1.add(CF_BT, Bytes.toBytes(USER_COL_RPEFIX + entry.getKey()),
									Bytes.toBytes(entry.getValue()));
						}
					}
					puts1.add(p1);

					byte[] key2 = new byte[BLOCK_KEY_LEN];
					Bytes.putBytes(key2, 0, cMd5, 0, 16);
					offset = Bytes.putInt(key2, 16, bId);
					Bytes.putBytes(key2, offset, pMd5, 0, 16);
					Put p2 = new Put(key2);
					p2.add(CF_BT, COL_RLINK_PURL_BT, Bytes.toBytes(pUrl));
					puts2.add(p2);

					pos++;
				}
			}
			table1 = conn.getTable(dbName + TB_LINK);
			table1.put(puts1);
			table2 = conn.getTable(dbName + TB_RLINK);
			table2.put(puts2);
		} finally {
			if (table1 != null) {
				try {
					table1.close();
				} catch (Exception e) {

				}
			}
			if (table2 != null) {
				try {
					table2.close();
				} catch (Exception e) {

				}
			}
		}
	}

	public static void loadBlock(HConnection conn, String dbName, WebPage wp) throws Exception {
		TreeMap<Integer, List<Link>> children = getChildrenUrlsGroupbyBlock(conn, dbName, wp.getUrl());
		List<Block> blocks = new ArrayList<>(children.size());
		wp.setBlocks(blocks);
		if (children.size() == 0)
			return;
		Set<String> cUrls = new TreeSet<>();
		for (Entry<Integer, List<Link>> entry : children.entrySet()) {
			for (Link link : entry.getValue()) {
				cUrls.add(link.getWebPage().getUrl());
			}
		}
		// TODO稍微优点浪费，Link里的WebPage只是为了存一个url
		Map<String, WebPage> map = getWebPages(conn, dbName, cUrls);
		for (Entry<Integer, List<Link>> entry : children.entrySet()) {
			Block block = new Block();
			List<Link> links = new ArrayList<>(entry.getValue().size());
			block.setLinks(links);
			blocks.add(block);
			for (Link link : entry.getValue()) {
				WebPage child = map.get(link.getWebPage().getUrl());
				if (child != null) {
					link.setWebPage(child);
				} else {
					link.getWebPage().setDepth(wp.getDepth() + 1);
				}
				links.add(link);
			}
		}
	}

	private static Map<String, String> readUserAttrs(NavigableMap<byte[], byte[]> familyMap) {
		Map<String, String> attrs = new HashMap<>();
		for (Entry<byte[], byte[]> entry : familyMap.entrySet()) {
			String key = Bytes.toString(entry.getKey());
			if (key.startsWith(USER_COL_RPEFIX)) {
				String value = Bytes.toString(entry.getValue());
				key = key.substring(2);
				attrs.put(key, value);
			}
		}
		return attrs;
	}

	private static Map<String, WebPage> getWebPages(HConnection conn, String dbName, Collection<String> urls)
			throws Exception {
		HTableInterface table = null;
		Map<String, WebPage> result = new HashMap<>();
		try {
			ArrayList<Get> gets = new ArrayList<Get>(urls.size());
			for (String url : urls) {
				Get get = new Get(DigestUtils.md5(url));
				get.addFamily(CF_BT);
				gets.add(get);
			}

			table = conn.getTable(dbName + TB_WEBPAGE);
			Result[] rs = table.get(gets);
			for (Result r : rs) {
				if (r.isEmpty())
					continue;
				WebPage wp = new WebPage();

				wp.setUrl(Bytes.toString(r.getValue(CF_BT, COL_WEBPAGE_URL_BT)));
				wp.setRedirectedUrl(Bytes.toString(r.getValue(CF_BT, COL_WEBPAGE_REDIRECTED_URL_BT)));
				wp.setTitle(Bytes.toString(r.getValue(CF_BT, COL_WEBPAGE_TITLE_BT)));
				wp.setDepth(Bytes.toInt(r.getValue(CF_BT, COL_WEBPAGE_DEPTH_BT)));
				byte[] bytes = r.getValue(CF_BT, COL_WEBPAGE_LAST_VISIT_BT);
				if (bytes != null) {
					wp.setLastVisitTime(new java.util.Date(Bytes.toLong(bytes)));
				}
				bytes = r.getValue(CF_BT, COL_WEBPAGE_LAST_FINISH_BT);
				if (bytes != null) {
					wp.setLastFinishTime(new java.util.Date(Bytes.toLong(bytes)));
				}
				// get attributes
				NavigableMap<byte[], byte[]> familyMap = r.getFamilyMap(CF_BT);
				if (familyMap.size() > 0) {
					Map<String, String> attrs = readUserAttrs(familyMap);
					wp.setAttrs(attrs);
				}

				result.put(wp.getUrl(), wp);
			}
		} finally {
			if (table != null) {
				table.close();
			}
		}
		return result;
	}

	private static TreeMap<Integer, List<Link>> getChildrenUrlsGroupbyBlock(HConnection conn, String dbName, String url)
			throws Exception {
		TreeMap<Integer, List<Link>> result = new TreeMap<>();
		if (url == null || url.length() == 0)
			return result;
		HTableInterface table = null;
		ResultScanner rs = null;

		try {
			table = conn.getTable(dbName + TB_LINK);
			Scan scan = new Scan();
			byte[] md5 = DigestUtils.md5(url);
			scan.setStartRow(md5);
			scan.setFilter(new PrefixFilter(md5));

			rs = table.getScanner(scan);

			for (Result r : rs) {
				byte[] keys = r.getRow();
				Integer bId = Bytes.toInt(keys, 16);
				String cUrl = Bytes.toString(r.getValue(CF_BT, COL_LINK_CURL_BT));
				int pos = Bytes.toInt(r.getValue(CF_BT, COL_LINK_POS_BT));
				String linkText = Bytes.toString(r.getValue(CF_BT, COL_LINK_LINKTEXT_BT));
				List<Link> links = result.get(bId);
				if (links == null) {
					links = new ArrayList<>();
					result.put(bId, links);
				}
				Link link = new Link();
				link.setPos(pos);
				link.setLinkText(linkText);
				// read attrs
				NavigableMap<byte[], byte[]> familyMap = r.getFamilyMap(CF_BT);
				if (familyMap.size() > 0) {
					Map<String, String> linkAttrs = readUserAttrs(familyMap);
					link.setLinkAttrs(linkAttrs);
				}

				WebPage child = new WebPage();
				link.setWebPage(child);
				child.setUrl(cUrl);
				links.add(link);
			}
			for (List<Link> list : result.values()) {
				Collections.sort(list, new Comparator<Link>() {
					@Override
					public int compare(Link o1, Link o2) {
						return o1.getPos() - o2.getPos();
					}
				});
			}
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {

				}
			}
			if (table != null) {
				table.close();
			}
		}

		return result;
	}

	public static Set<String> getParent(HConnection conn, String dbName, String url) throws Exception {
		Set<String> parents = new HashSet<>();
		HTableInterface table = null;
		ResultScanner rs = null;
		try {
			table = conn.getTable(dbName + TB_RLINK);
			Scan scan = new Scan();
			byte[] md5 = DigestUtils.md5(url);
			scan.setStartRow(md5);
			scan.setFilter(new PrefixFilter(md5));
			rs = table.getScanner(scan);
			for (Result r : rs) {
				String pUrl = Bytes.toString(r.getValue(CF_BT, COL_RLINK_PURL_BT));
				parents.add(pUrl);
			}
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}
			if (table != null) {
				table.close();
			}
		}

		return parents;
	}

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("h", "help", false, "print help");
		options.addOption("p", "port", true, "zookeeper client port");

		options.addOption("d", "deleteExist", true, "delete existed tables or not");

		options.addOption("r", "rsCount", true, "number of region server. used to presplit url_db");

		String zkQuorum = null;
		String zkPort = null;
		String helpStr = "HbaseTool hbase.zookeeper.quorum dbName";
		boolean deleteExist = false;
		int rsCount = 10;
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			HelpFormatter formatter = new HelpFormatter();
			if (line.hasOption("help")) {
				formatter.printHelp(helpStr, options);
				System.exit(0);
			}
			if (line.getArgs().length != 2) {
				formatter.printHelp(helpStr, options);
				System.exit(-1);
			}

			if (line.hasOption("port")) {
				zkPort = line.getOptionValue("port");
			}
			if (line.hasOption("d")) {
				if (line.getOptionValue("d").equalsIgnoreCase("false")) {
					deleteExist = false;
				}
			}

			if (line.hasOption("r")) {
				rsCount = Integer.valueOf(line.getOptionValue("r"));
			}

			args = line.getArgs();
			zkQuorum = args[0];

		} catch (ParseException exp) {
			System.out.println("Unexpected exception:" + exp.getMessage());
		}
		String dbName = args[1];
		HBaseAdmin admin = null;
		Configuration myConf = HBaseConfiguration.create();
		myConf.set("hbase.zookeeper.quorum", zkQuorum);
		if (zkPort != null) {
			myConf.set("hbase.zookeeper.property.clientPort", zkPort);
		}
		System.out.println("deleteExist: " + deleteExist);
		System.out.println("rsCount: " + rsCount);
		byte[][] splits = calcSplit(rsCount);
		admin = new HBaseAdmin(myConf);
		try {
			if (deleteExist) {
				deleteTable(admin, dbName + TB_WEBPAGE, true);
				createTable(admin, dbName + TB_WEBPAGE, BloomType.ROW, Algorithm.GZ, true, splits, CF, CF2);

				deleteTable(admin, dbName + TB_FAILED, true);
				createTable(admin, dbName + TB_FAILED, BloomType.NONE, Algorithm.NONE, true, null, CF);

				deleteTable(admin, dbName + TB_LINK, true);
				createTable(admin, dbName + TB_LINK, BloomType.ROW, Algorithm.GZ, true, splits, CF);

				deleteTable(admin, dbName + TB_RLINK, true);
				createTable(admin, dbName + TB_RLINK, BloomType.ROW, Algorithm.GZ, true, splits, CF);

				deleteTable(admin, dbName + TB_BADURL, true);
				createTable(admin, dbName + TB_BADURL, BloomType.ROW, Algorithm.GZ, true, splits, CF);
			} else {
				boolean exist = deleteTable(admin, dbName + TB_WEBPAGE, false);
				if (!exist) {
					createTable(admin, dbName + TB_WEBPAGE, BloomType.ROW, Algorithm.GZ, true, splits, CF, CF2);
				}

				exist = deleteTable(admin, dbName + TB_FAILED, false);
				if (!exist) {
					createTable(admin, dbName + TB_FAILED, BloomType.NONE, Algorithm.NONE, true, null, CF);
				}

				exist = deleteTable(admin, dbName + TB_LINK, false);
				if (!exist) {
					createTable(admin, dbName + TB_LINK, BloomType.ROW, Algorithm.GZ, true, splits, CF);
				}

				exist = deleteTable(admin, dbName + TB_RLINK, false);
				if (!exist) {
					createTable(admin, dbName + TB_RLINK, BloomType.ROW, Algorithm.GZ, true, splits, CF);
				}

				exist = deleteTable(admin, dbName + TB_BADURL, false);
				if (!exist) {
					createTable(admin, dbName + TB_BADURL, BloomType.ROW, Algorithm.GZ, true, splits, CF);
				}
			}

		} finally {
			if (admin != null) {
				admin.close();
			}
		}
	}

	public static boolean deleteTable(HBaseAdmin admin, String tableName, boolean deleteExist) throws IOException {
		if (admin.tableExists(tableName)) {
			if (deleteExist) {
				System.out.println(
						"disable table: " + tableName + ". it may take a little bit long time, pleas wait patiently");
				admin.disableTable(tableName);
				System.out.println("delete table: " + tableName);
				admin.deleteTable(tableName);
			} else {
				System.out.println("don't delete: " + tableName);
			}
			return true;
		} else {
			System.out.println("table not exist: " + tableName);
			return false;
		}
	}

	public static void createTable(HBaseAdmin admin, String tableName, BloomType bt, Algorithm compressAlgo,
			boolean blockCacheEnabled, byte[][] presplit, String... columnFamilys) throws IOException {
		HTableDescriptor desc = new HTableDescriptor(TableName.valueOf(tableName));
		for (String cf : columnFamilys) {
			System.out.println("add columnFamily: " + cf);
			HColumnDescriptor col = new HColumnDescriptor(Bytes.toBytes(cf));
			col.setMaxVersions(1);
			col.setBloomFilterType(bt);
			col.setCompressionType(compressAlgo);
			col.setBlockCacheEnabled(blockCacheEnabled);
			desc.addFamily(col);
		}
		System.out.println("creating table: " + tableName);
		admin.createTable(desc, presplit);
	}

	private static byte[][] calcSplit(int splitCount) {
		if (splitCount < 2 || splitCount > 255)
			throw new IllegalArgumentException("too much splits: " + splitCount);
		int min = Byte.MIN_VALUE;
		int max = Byte.MAX_VALUE;
		int avg = (max - min) / splitCount;
		if (avg == 0)
			return null;
		byte[][] result = new byte[splitCount - 1][];

		for (int i = 0; i < splitCount - 1; i++) {
			result[i] = new byte[] { (byte) ((i + 1) * avg + min) };
		}
		return result;
	}
}
