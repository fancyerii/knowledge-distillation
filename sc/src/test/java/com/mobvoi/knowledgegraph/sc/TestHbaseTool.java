package com.mobvoi.knowledgegraph.sc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;

import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.Link;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.tools.BlockInfo;
import com.antbrains.sc.tools.SCHbaseTool;

public class TestHbaseTool {

	public static void main(String[] args) throws Exception {
		Configuration conf = HBaseConfiguration.create();
		String zk = "kg121";
		conf.set("hbase.zookeeper.quorum", zk);
		HConnection conn = HConnectionManager.createConnection(conf);

		String dbName = "test_db";
		SCHbaseTool.main(new String[] { zk, dbName });
		// testWebPage(conn, dbName);
		// testGetWebPage(conn, dbName);
		//
		// testAddFail(conn, dbName);
		// testGetFail(conn, dbName);
		//
		// testAddBlock(conn, dbName);
		// testLoadBlock(conn, dbName);

		testGetLastUpdate(conn, dbName);
		conn.close();
	}

	public static void testGetLastUpdate(HConnection conn, String dbName) throws Exception {
		WebPage wp = new WebPage();
		wp.setUrl("test_data_url");
		Date d = new Date();
		wp.setLastVisitTime(d);
		ArrayList<WebPage> wps = new ArrayList<>(1);
		wps.add(wp);
		SCHbaseTool.updateWebPages(conn, dbName, wps);
		Date d2 = SCHbaseTool.getLastVisit(conn, dbName, wp.getUrl());
		if (!d.equals(d2)) {
			throw new RuntimeException("bug!");
		}
	}

	public static void testLoadBlock(HConnection conn, String dbName) throws Exception {
		WebPage wp1 = SCHbaseTool.getWebPage(conn, dbName, "b_url1", false);
		SCHbaseTool.loadBlock(conn, dbName, wp1);
		System.out.println(wp1);
		Set<String> parents = SCHbaseTool.getParent(conn, dbName, "b_url3");
		for (String p : parents) {
			System.out.println(p);
		}
	}

	public static void testAddBlock(HConnection conn, String dbName) throws Exception {
		// 1 -b1-> 2
		// 1 -b2-> 3
		// 4 -b1-> 3
		// 4 -b1-> 5
		WebPage wp1 = new WebPage();
		wp1.setUrl("b_url1");
		WebPage wp2 = new WebPage();
		wp2.setUrl("b_url2");
		WebPage wp3 = new WebPage();
		wp3.setUrl("b_url3");
		WebPage wp4 = new WebPage();
		wp4.setUrl("b_url4");
		WebPage wp5 = new WebPage();
		wp5.setUrl("b_url5");

		ArrayList<Block> blocks1 = new ArrayList<>();
		wp1.setBlocks(blocks1);
		Block b11 = new Block();
		blocks1.add(b11);
		ArrayList<Link> links11 = new ArrayList<>();
		b11.setLinks(links11);
		Link link11 = new Link();
		links11.add(link11);
		link11.setWebPage(wp2);

		Block b12 = new Block();
		blocks1.add(b12);
		ArrayList<Link> links12 = new ArrayList<>();
		b12.setLinks(links12);
		Link link12 = new Link();
		links12.add(link12);
		link12.setWebPage(wp3);

		ArrayList<Block> blocks4 = new ArrayList<>();
		wp4.setBlocks(blocks4);
		Block b4 = new Block();
		blocks4.add(b4);
		ArrayList<Link> links4 = new ArrayList<>();
		b4.setLinks(links4);
		Link link41 = new Link();
		links4.add(link41);
		link41.setWebPage(wp3);
		Link link42 = new Link();
		links4.add(link42);
		link42.setWebPage(wp5);

		List<WebPage> webPages = new ArrayList<>(5);
		webPages.add(wp1);
		webPages.add(wp2);
		webPages.add(wp3);
		webPages.add(wp4);
		webPages.add(wp5);

		SCHbaseTool.updateWebPages(conn, dbName, webPages);

		List<BlockInfo> bis = new ArrayList<>();
		bis.add(new BlockInfo("b_url1", blocks1.get(0), 0));
		bis.add(new BlockInfo("b_url1", blocks1.get(1), 1));
		bis.add(new BlockInfo("b_url4", blocks4.get(0), 0));

		SCHbaseTool.addBlocks(conn, dbName, bis);

	}

	public static void testGetFail(HConnection conn, String dbName) throws Exception {
		List<WebPage> webPages = SCHbaseTool.getFailed(conn, dbName, 50, false);
		System.out.println(webPages.size());
		for (WebPage wp : webPages) {
			System.out.println(wp.getFailCount() + "\t" + wp.getCrawlPriority() + "\t" + wp.getUrl());
		}
		webPages = SCHbaseTool.getFailed(conn, dbName, 100, true);
		System.out.println(webPages.size());
		webPages = SCHbaseTool.getFailed(conn, dbName, 50, false);
		System.out.println(webPages.size());
	}

	public static void testAddFail(HConnection conn, String dbName) throws Exception {
		List<WebPage> webPages = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			WebPage wp = new WebPage();
			webPages.add(wp);
			wp.setUrl("url" + i);
			if (i % 5 != 0) {
				wp.setRedirectedUrl("ru" + i);
			}
			wp.setDepth(i % 10);
			wp.setFailCount(i % 3);
			wp.setCrawlPriority((i + 1) % 4);
			wp.setLastVisitTime(new java.util.Date());

		}

		SCHbaseTool.saveFailed(conn, dbName, webPages);
	}

	public static void testWebPage(HConnection conn, String dbName) throws Exception {
		List<WebPage> webPages = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			WebPage wp = new WebPage();
			webPages.add(wp);
			wp.setUrl("url" + i);
			if (i % 5 != 0) {
				wp.setRedirectedUrl("ru" + i);
			}
			wp.setDepth(i % 10);
			if (i % 4 == 0) {
				wp.setContent("content" + i);
			}
			wp.setLastVisitTime(new java.util.Date());
			wp.setTitle("title" + i);
			if (i % 2 == 0) {
				Map<String, String> attrs = new HashMap<>();
				attrs.put("attr" + i, "v" + i);
				attrs.put("attr" + (i + 100000), "v" + (i + 100000));
				wp.setAttrs(attrs);
			}
		}

		SCHbaseTool.updateWebPages(conn, dbName, webPages);
	}

	public static void testGetWebPage(HConnection conn, String dbName) throws Exception {
		String url = "url4";
		WebPage wp = SCHbaseTool.getWebPage(conn, dbName, url, true);
		System.out.println(wp);
	}
}
