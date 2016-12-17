package com.antbrains.sc.archiver;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.antbrains.mysqltool.DBUtils;
import com.antbrains.mysqltool.PoolManager;
import com.antbrains.sc.data.Block;
import com.antbrains.sc.data.Link;
import com.antbrains.sc.data.WebPage;
import com.antbrains.sc.tools.HostNameTools;
import com.google.gson.Gson;

public class MysqlArchiver implements Archiver {
	protected static Logger logger = Logger.getLogger(MysqlArchiver.class);
	public static final String URLKEY = "#url#";
	public static final String LINKTEXT = "#linktext#";
	public static final int ATTR_SRC_PARENT = 1;

	@Override
	public boolean insert2WebPage(WebPage webPage) {
		Connection conn = null;

		try {
			conn = PoolManager.getConnection();
			Integer id = this.hasExisted(webPage, conn);
			Exception e = null;
			if (id == null) {// 不存在
				int retry = 0;
				do {
					retry++;
					try {
						id = this.insertAndReturnId(conn, webPage);
					} catch (Exception ex) {
						e = ex;
					}
					if (id != null)
						break;
					// 可能多个线程同时插入,再试一次
					Random rnd = new Random();
					long secs = rnd.nextInt(2000);
					Thread.sleep(secs);
					id = this.hasExisted(webPage, conn);
				} while (id == null && retry < 2);
			} else {
				logger.debug(webPage.getUrl() + " already existed.");
			}
			if (id == null) {
				logger.error("Can't insert page: " + webPage);
				if (e != null) {
					logger.error(e.getMessage(), e);
				}
				return false;
			}
			webPage.setId(id);

			this.insert2Attr(webPage, conn);
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, null, null);
		}
		// return -1;
		return false;
	}

	public List<Integer> getChildrenIds(int id) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		List<Integer> children = new ArrayList<>();
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement(
					"select lk.pageId from link as lk, page_block as pb where pb.pageId=? and pb.blockId=lk.blockId");
			pstmt.setInt(1, id);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				children.add(rs.getInt(1));
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, rs);
		}

		return children;
	}

	private byte[] gzipHtml(String html) throws Exception {
		if (html == null || html.length() == 0)
			return null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gos = new GZIPOutputStream(baos);
		gos.write(html.getBytes("UTF8"));
		gos.close();
		return baos.toByteArray();

	}

	private Integer insertAndReturnId(Connection conn, WebPage webPage) throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(
					"insert into webpage(url,title,charSet,tags,depth,content,lastVisitTime,lastFinishTime,type,websiteId,redirectedUrl,md5) values(?,?,?,?,?,?,?,?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setString(1, webPage.getUrl());
			pstmt.setString(2, webPage.getTitle());
			pstmt.setString(3, webPage.getCharSet());
			pstmt.setString(4, DBUtils.tagList2String(webPage.getTags()));
			pstmt.setInt(5, webPage.getDepth());
			byte[] bytes = this.gzipHtml(webPage.getContent());
			if (bytes == null) {
				pstmt.setNull(6, java.sql.Types.BLOB);
			} else {
				pstmt.setBlob(6, new ByteArrayInputStream(bytes));
			}

			// pstmt.setString(6, webPage.getContent());
			pstmt.setTimestamp(7, DBUtils.date2Timestamp(webPage.getLastVisitTime()));
			pstmt.setTimestamp(8, null);
			pstmt.setInt(9, webPage.getType());
			pstmt.setInt(10, 0);
			pstmt.setString(11, webPage.getRedirectedUrl());
			pstmt.setString(12, DigestUtils.md5Hex(webPage.getUrl()));
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			DBUtils.closeAll(null, pstmt, rs);
		}
		return null;
	}

	public Integer hasExisted(WebPage webPage) {
		Connection conn = null;

		try {
			conn = PoolManager.getConnection();
			return this.hasExisted(webPage, conn);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		} finally {
			DBUtils.closeAll(conn, null, null);
		}
	}

	private Integer hasExisted(WebPage webPage, Connection conn) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			String md5 = DigestUtils.md5Hex(webPage.getUrl());
			pstmt = conn.prepareStatement("select id from webpage where md5=?");
			pstmt.setString(1, md5);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(null, pstmt, rs);
		}
		return null;
	}

	@Override
	public void saveUnFinishedWebPage(WebPage webPage, int failCountInc) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = PoolManager.getConnection();
			webPage.setFailCount(webPage.getFailCount() + failCountInc);
			pstmt = conn.prepareStatement(
					"insert ignore into unfinished(id,url,depth,failcount,priority,lastVisit) values(?,?,?,?,?,?)");
			pstmt.setInt(1, webPage.getId());
			pstmt.setString(2, webPage.getUrl());
			pstmt.setInt(3, webPage.getDepth());
			pstmt.setInt(4, webPage.getFailCount());
			pstmt.setInt(5, webPage.getCrawlPriority());
			pstmt.setLong(6, System.currentTimeMillis());
			pstmt.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, null);
		}
	}

	@Override
	public void updateWebPage(WebPage webPage) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		// ResultSet rs=null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement(
					"update webpage set url=?,title=?,charSet=?,tags=?,depth=?,content=?,lastVisitTime=?,type=?,websiteId=?,redirectedUrl=?,md5=? where id=?");

			pstmt.setString(1, webPage.getUrl());
			String title = webPage.getTitle();
			if (title != null && title.length() > 255) {
				title = title.substring(0, 255);
			}
			pstmt.setString(2, title);
			pstmt.setString(3, webPage.getCharSet());
			pstmt.setString(4, DBUtils.tagList2String(webPage.getTags()));
			pstmt.setInt(5, webPage.getDepth());
			// pstmt.setString(6, webPage.getContent());
			byte[] bytes = this.gzipHtml(webPage.getContent());

			if (bytes == null) {
				pstmt.setNull(6, java.sql.Types.BLOB);
			} else {
				pstmt.setBlob(6, new ByteArrayInputStream(bytes));
			}
			pstmt.setTimestamp(7, DBUtils.date2Timestamp(webPage.getLastVisitTime()));
			// pstmt.setTimestamp(8, null);
			pstmt.setInt(8, webPage.getType());
			pstmt.setInt(9, 0);
			pstmt.setString(10, webPage.getRedirectedUrl());
			pstmt.setString(11, DigestUtils.md5Hex(webPage.getUrl()));
			pstmt.setInt(12, webPage.getId());

			pstmt.executeUpdate();

			this.insert2Attr(webPage, conn);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, null);
		}
	}

	@Override
	public void loadBlocks(WebPage webPage) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = PoolManager.getConnection();
			// page_block <--> block
			pstmt = conn.prepareStatement(
					"select b.id,b.tags,b.lastVisitTime,b.lastFinishTime,pb.pos,b.updateInfo from `block` b, page_block pb where pb.pageId=? and b.id=pb.blockId order by pb.pos");
			pstmt.setInt(1, webPage.getId());
			rs = pstmt.executeQuery();
			List<Block> blocks = new ArrayList<Block>();
			webPage.setBlocks(blocks);

			while (rs.next()) {
				Block block = new Block();
				blocks.add(block);
				block.setId(rs.getInt(1));
				block.setTags(DBUtils.tagString2List(rs.getString(2)));
				block.setLastVisitTime(DBUtils.timestamp2Date(rs.getTimestamp(3)));
				block.setLastFinishTime(DBUtils.timestamp2Date(rs.getTimestamp(4)));

				block.setUpdateInfo(rs.getString(6));
				loadPagesFromBlock(block, conn);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, rs);
		}
	}

	public void loadAttr(WebPage webPage) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement("select name,value,src from attr where pageId=?");
			Map<String, String> attrs = new HashMap<>();
			pstmt.setInt(1, webPage.getId());

			rs = pstmt.executeQuery();
			while (rs.next()) {
				String name = rs.getString(1);
				String value = rs.getString(2);
				int src = rs.getInt(3);
				if (!rs.wasNull() && src == 1) {
					name += "(P)";
				}
				attrs.put(name, value);
			}
			webPage.setAttrs(attrs);
		} finally {
			DBUtils.closeAll(conn, pstmt, rs);
		}

	}

	private void loadPagesFromBlock(Block block, Connection conn) throws Exception {
		if (block.getId() == null) {
			logger.error("block id is null " + DBUtils.tagList2String(block.getTags()));
			return;
		}
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(
					"select pageId,pos,linkText,url,title,charSet,tags,depth,lastVisitTime,lastFinishTime,type,redirectedUrl from link l,webpage p where l.blockId=? and l.pageId=p.id");
			pstmt.setInt(1, block.getId());
			rs = pstmt.executeQuery();
			List<Link> links = new ArrayList<Link>();
			block.setLinks(links);
			while (rs.next()) {
				Link link = new Link();
				links.add(link);
				link.setLinkText(rs.getString(3));
				link.setPos(rs.getInt(2));
				WebPage childPage = new WebPage();
				link.setWebPage(childPage);
				childPage.setId(rs.getInt(1));

				childPage.setUrl(rs.getString(4));
				childPage.setTitle(rs.getString(5));
				childPage.setCharSet(rs.getString(6));
				childPage.setTags(DBUtils.tagString2List(rs.getString(7)));
				childPage.setDepth(DBUtils.getNullableInt(8, rs));
				childPage.setLastVisitTime(DBUtils.timestamp2Date(rs.getTimestamp(9)));
				childPage.setLastFinishTime(DBUtils.timestamp2Date(rs.getTimestamp(10)));
				childPage.setType(DBUtils.getNullableInt(11, rs));
				childPage.setRedirectedUrl(rs.getString(12));

			}
		} catch (Exception e) {
			throw e;
		} finally {
			DBUtils.closeAll(null, pstmt, rs);
		}
	}

	@Override
	public void insert2Block(Block block, WebPage webPage, int pos) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement("insert into block(tags,lastVisitTime,updateInfo) values(?,now(),?)",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setString(1, DBUtils.tagList2String(block.getTags()));
			pstmt.setString(2, block.getUpdateInfo());
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				block.setId(rs.getInt(1));
				insert2Page_Block(webPage.getId(), block.getId(), pos, conn);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, rs);
		}
	}

	private void insert2Page_Block(int pageId, int blockId, int pos, Connection conn) {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("insert into page_block(pageId,blockId,pos) values(?,?,?)");
			pstmt.setInt(1, pageId);
			pstmt.setInt(2, blockId);
			pstmt.setInt(3, pos);
			pstmt.executeUpdate();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(null, pstmt, null);
		}
	}

	@Override
	public void updateBlock(Block block, WebPage webPage, int pos, boolean updateInfo) {
		block.setLastVisitTime(new java.util.Date());
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = PoolManager.getConnection();
			if (updateInfo) {
				pstmt = conn.prepareStatement("update block set tags=?,lastVisitTime=?,updateInfo=? where id=?");
				pstmt.setString(1, DBUtils.tagList2String(block.getTags()));
				pstmt.setTimestamp(2, DBUtils.date2Timestamp(block.getLastVisitTime()));
				pstmt.setString(3, block.getUpdateInfo());
				pstmt.setInt(4, block.getId());
			} else {
				pstmt = conn.prepareStatement("update block set tags=?,lastVisitTime=? where id=?");
				pstmt.setString(1, DBUtils.tagList2String(block.getTags()));
				pstmt.setTimestamp(2, DBUtils.date2Timestamp(block.getLastVisitTime()));
				pstmt.setInt(3, block.getId());
			}
			pstmt.executeUpdate();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, null);
		}
	}

	private static final int batchInsertSize = 1000;

	@Override
	public void insert2Link(List<Integer> blockIds, List<Integer> parentIds, List<Integer> childIds,
			List<Integer> poses, List<String> linkTexts, List<Map<String, String>> attrsList) {
		Connection conn = null;
		PreparedStatement pstmt1 = null;
		PreparedStatement pstmt2 = null;
		try {
			conn = PoolManager.getConnection();
			pstmt1 = conn.prepareStatement(
					"insert into link(blockId,pageId,pos,linkText) values(?,?,?,?) on duplicate key update blockId=values(blockId),pageId=values(pageId),pos=?,linkText=?");
			pstmt2 = conn.prepareStatement(
					"insert into linkattr(blockId,pageId,name,value) values(?,?,?,?) on duplicate key update blockId=values(blockId),pageId=values(pageId),name=values(name),value=?");
			int linkCount = 0;
			for (int i = 0; i < blockIds.size(); i++) {
				int blockId = blockIds.get(i);
				// int parentId=parentIds.get(i);
				int childPageId = childIds.get(i);
				int pos = poses.get(i);
				String linkText = linkTexts.get(i);
				if (linkText != null && linkText.length() > 255) {
					linkText = linkText.substring(0, 255);
				}
				Map<String, String> attrs = attrsList.get(i);

				pstmt1.setInt(1, blockId);
				pstmt1.setInt(2, childPageId);
				pstmt1.setInt(3, pos);
				pstmt1.setString(4, linkText);
				pstmt1.setInt(5, pos);
				pstmt1.setString(6, linkText);
				pstmt1.addBatch();

				linkCount++;

				if (attrs != null && attrs.size() > 0) {
					pstmt2.setInt(1, blockId);
					pstmt2.setInt(2, childPageId);
					for (Entry<String, String> entry : attrs.entrySet()) {
						pstmt2.setString(3, entry.getKey());
						pstmt2.setString(4, entry.getValue());
						pstmt2.setString(5, entry.getValue());
						pstmt2.addBatch();
					}
				}

				if (linkCount == batchInsertSize) {
					pstmt1.executeBatch();
					pstmt2.executeBatch();
					DBUtils.closeAll(null, pstmt1, null);
					DBUtils.closeAll(null, pstmt2, null);
					linkCount = 0;
					pstmt1 = conn.prepareStatement(
							"insert into link(blockId,pageId,pos,linkText) values(?,?,?,?) on duplicate key update blockId=values(blockId),pageId=values(pageId),pos=?,linkText=?");
					pstmt2 = conn.prepareStatement(
							"insert into linkattr(blockId,pageId,name,value) values(?,?,?,?) on duplicate key update blockId=values(blockId),pageId=values(pageId),name=values(name),value=?");

				}
			}
			if (linkCount > 0) {
				pstmt1.executeBatch();
				pstmt2.executeBatch();
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(null, pstmt2, null);
			DBUtils.closeAll(conn, pstmt1, null);
		}
	}

	@Override
	public void insert2Attr(WebPage webPage) {
		Connection conn = null;

		try {
			conn = PoolManager.getConnection();
			this.insert2Attr(webPage, conn);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, null, null);
		}
	}

	/**
	 * 把webPage的属性插入attr表里
	 * 需要更新/插入的数据放在webPage.getAttrs()和webPage.getAttrsFromParent()里
	 * 如果attrs和attrsFromParent都不空，那么删除原来的所有属性 如果attrs不空，删除抽取的属性
	 * 如果attrsFromParent不空，那么删除来自parent的属性
	 * 
	 * @param webPage
	 * @param conn
	 */
	private void insert2Attr2(WebPage webPage, Connection conn) {

		boolean deleteAttrFromExt = false;
		boolean deleteAttrFromParent = false;
		if (webPage.getAttrsFromParent() != null && webPage.getAttrsFromParent().size() > 0) {
			deleteAttrFromParent = true;
		}
		if (webPage.getAttrs() != null && webPage.getAttrs().size() > 0) {
			deleteAttrFromExt = true;
		}
		// if(webPage.getAttrs()==null || webPage.getAttrs().size()==0){
		// webPage.setAttrs(webPage.getAttrsFromParent());
		// }
		// if(webPage.getAttrs()==null||webPage.getAttrs().size()==0) return;
		if (!deleteAttrFromExt && !deleteAttrFromParent)
			return;
		PreparedStatement pstmt = null;
		try {
			// 先删除原来的属性，保留现在的属性
			// 可以考虑 ON DUPLICATE KEY UPDATE 但是这样原来有而现在没有的属性会保留下来
			// 以后可以考虑保存多个版本，以防某次出错
			if (deleteAttrFromParent && deleteAttrFromExt) {
				deleteAllAttr(webPage.getId(), conn);
			} else if (deleteAttrFromParent) {// 删除来自父亲的属性
				deleteParentAttr(webPage.getId(), conn);
			} else if (deleteAttrFromExt) {// 删除抽取的属性
				deleteExtAttr(webPage.getId(), conn);
			}

			pstmt = conn.prepareStatement("insert ignore into attr(pageId,name,value) values(?,?,?)");
			pstmt.setInt(1, webPage.getId());
			if (deleteAttrFromParent) {
				for (Entry<String, String> entry : webPage.getAttrsFromParent().entrySet()) {
					if (entry.getKey().equals(URLKEY) || entry.getKey().equals(LINKTEXT)) {
						continue;
					}
					pstmt.setString(2, entry.getKey());
					pstmt.setString(3, entry.getValue());
					pstmt.addBatch();
				}

			}
			if (deleteAttrFromExt) {
				for (Entry<String, String> entry : webPage.getAttrs().entrySet()) {
					if (entry.getKey().equals(URLKEY) || entry.getKey().equals(LINKTEXT)) {
						continue;
					}
					pstmt.setString(2, entry.getKey());
					pstmt.setString(3, entry.getValue());
					pstmt.addBatch();
				}
			}

			pstmt.executeBatch();
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(null, pstmt, null);
		}

	}

	private void insert2Attr(WebPage webPage, Connection conn) {
		Map<String, String> attrsFromParent = webPage.getAttrsFromParent();
		Map<String, String> attrs = webPage.getAttrs();
		if (attrsFromParent == null && attrs == null)
			return;
		PreparedStatement pstmt = null;
		try {

			pstmt = conn.prepareStatement("insert ignore into attr(pageId,name,value) values(?,?,?)");
			pstmt.setInt(1, webPage.getId());
			if (attrsFromParent != null) {
				for (Entry<String, String> entry : attrsFromParent.entrySet()) {
					if (entry.getKey().equals(URLKEY) || entry.getKey().equals(LINKTEXT)) {
						continue;
					}
					pstmt.setString(2, entry.getKey());
					pstmt.setString(3, entry.getValue());
					pstmt.addBatch();
				}

			}
			if (attrs != null) {
				for (Entry<String, String> entry : attrs.entrySet()) {
					if (entry.getKey().equals(URLKEY) || entry.getKey().equals(LINKTEXT)) {
						continue;
					}
					pstmt.setString(2, entry.getKey());
					pstmt.setString(3, entry.getValue());
					pstmt.addBatch();
				}
			}

			pstmt.executeBatch();
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(null, pstmt, null);
		}

	}

	private void deleteParentAttr(int pageId, Connection conn) {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("delete from attr where pageId=? and src=?");
			pstmt.setInt(1, pageId);
			pstmt.setInt(2, ATTR_SRC_PARENT);
			pstmt.executeUpdate();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(null, pstmt, null);
		}
	}

	private void deleteAllAttr(int pageId, Connection conn) {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("delete from attr where pageId=?");
			pstmt.setInt(1, pageId);
			pstmt.executeUpdate();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(null, pstmt, null);
		}
	}

	private void deleteExtAttr(int pageId, Connection conn) {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("delete from attr where pageId=? and src is null");
			pstmt.setInt(1, pageId);
			pstmt.executeUpdate();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(null, pstmt, null);
		}
	}

	@Override
	public void saveLog(String taskId, String type, String msg, Date logTime, int level, String html) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		// ResultSet rs=null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement(
					"insert into crawl_log(taskId,type,msg,logTime,level,content) values(?,?,?,?,?,?)");
			pstmt.setString(1, taskId);
			pstmt.setString(2, type);
			pstmt.setString(3, msg);
			pstmt.setTimestamp(4, DBUtils.date2Timestamp(logTime));
			pstmt.setInt(5, level);
			pstmt.setString(6, html);
			pstmt.executeUpdate();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, null);
		}
	}

	private String readHtml(InputStream is) throws IOException {
		if (is == null)
			return null;
		GZIPInputStream gis = new GZIPInputStream(is);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];
		while ((nRead = gis.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();
		return new String(buffer.toByteArray(), "UTF8");
	}

	public WebPage getWebPage(String url) {
		WebPage page = new WebPage();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement(
					"select url,title,charSet,tags,depth,lastVisitTime,lastFinishTime,redirectedUrl,content,id from webpage where url=?");
			pstmt.setString(1, url);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				page.setUrl(rs.getString(1));
				page.setTitle(rs.getString(2));
				page.setCharSet(rs.getString(3));
				page.setTags(DBUtils.tagString2List(rs.getString(4)));
				page.setDepth(DBUtils.getNullableInt(5, rs));
				page.setLastVisitTime(DBUtils.timestamp2Date(rs.getTimestamp(6)));
				page.setLastFinishTime(DBUtils.timestamp2Date(rs.getTimestamp(7)));
				page.setRedirectedUrl(rs.getString(8));

				// page.setContent(rs.getString(9));
				page.setContent(this.readHtml(rs.getBinaryStream(9)));
				page.setId(rs.getInt(10));
				return page;
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, rs);
		}
		return null;
	}

	public WebPage getWebPage(int pageId) {
		WebPage page = new WebPage();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement(
					"select url,title,charSet,tags,depth,lastVisitTime,lastFinishTime,redirectedUrl,content from webpage where id=?");
			pstmt.setInt(1, pageId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				page.setUrl(rs.getString(1));
				page.setTitle(rs.getString(2));
				page.setCharSet(rs.getString(3));
				page.setTags(DBUtils.tagString2List(rs.getString(4)));
				page.setDepth(DBUtils.getNullableInt(5, rs));
				page.setLastVisitTime(DBUtils.timestamp2Date(rs.getTimestamp(6)));
				page.setLastFinishTime(DBUtils.timestamp2Date(rs.getTimestamp(7)));
				page.setRedirectedUrl(rs.getString(8));
				page.setId(pageId);
				// page.setContent(rs.getString(9));
				page.setContent(this.readHtml(rs.getBinaryStream(9)));
				return page;
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, rs);
		}
		return null;
	}

	@Override
	public void close() {

	}

	@Override
	public void process404Url(String url) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		// ResultSet rs=null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement(
					"insert into bad_url(url,md5,lastUpdate) values(?,?,now()) on duplicate key update lastUpdate=now()");
			pstmt.setString(1, url);
			pstmt.setString(2, DigestUtils.md5Hex(url));
			pstmt.executeUpdate();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, null);
		}
	}

	@Override
	public void updateFinishTime(WebPage webPage, Date date) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement("update webpage set  lastFinishTime=? where id=?");
			pstmt.setTimestamp(1, DBUtils.date2Timestamp(date));
			pstmt.setInt(2, webPage.getId());

			pstmt.executeUpdate();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, null);
		}
	}
	
	public void dumpUrl(int startId, DumpFilter filter, String outputPath, int printProgressEvery) throws Exception{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		BufferedWriter bw = null;

		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath), "UTF8"));
			conn = PoolManager.getConnection();
			stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
			stmt.setFetchSize(Integer.MIN_VALUE);

			rs = stmt.executeQuery("select id, url, depth, lastVisitTime, content from webpage where id>"+startId);
			Gson gson = new Gson();
			int i = 0;
			int total = 0;
			while (rs.next()) {
				i++;
				if (i % printProgressEvery == 0) {
					logger.info("progress: " + i + "\t write: " + total);
				}
				int id=rs.getInt(1);
				String url = rs.getString(2);
				int depth=rs.getInt(3);
				Timestamp ts=rs.getTimestamp(4);
				String content = this.readHtml(rs.getBinaryStream(5));
				if (!filter.accept(id, url, depth, ts, content))
					continue;
				total++; 
				Map<String, String> jsonObj = new HashMap<>(2);
				jsonObj.put("#url#", url);
				jsonObj.put("#id#", id+"");
				logger.info("Debug "+id+" "+url);
				bw.write(gson.toJson(jsonObj) + "\n");
			}
			logger.info("progress: " + i + "\t write: " + total);
		} catch (Exception e) {
			throw e;
		} finally {
			if (bw != null) {
				bw.close();
			}
			DBUtils.closeAll(conn, stmt, rs);
		}
	}

	public void dumpHtml(DumpFilter filter, String outputPath, int printProgressEvery) throws Exception {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		BufferedWriter bw = null;

		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath), "UTF8"));
			conn = PoolManager.getConnection();
			stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
			stmt.setFetchSize(Integer.MIN_VALUE);

			rs = stmt.executeQuery("select url,depth,content,lastVisitTime,id from webpage");
			Gson gson = new Gson();
			int i = 0;
			int total = 0;
			while (rs.next()) {
				i++;
				if (i % printProgressEvery == 0) {
					logger.info("progress: " + i + "\t write: " + total);
				}
				String url = rs.getString(1);
				int depth=rs.getInt(2);
				Timestamp ts=rs.getTimestamp(4);
				int id=rs.getInt(5);
				if (!filter.accept(-1, url, depth, ts, null))
					continue;
				total++;
				String content = this.readHtml(rs.getBinaryStream(3));
				Map<String, String> jsonObj = new HashMap<>(2);
				jsonObj.put("#url#", url);
				jsonObj.put("#html#", content);
				jsonObj.put("#id#", id+"");
				
				bw.write(gson.toJson(jsonObj) + "\n");
			}
			logger.info("progress: " + i + "\t write: " + total);
		} catch (Exception e) {
			throw e;
		} finally {
			if (bw != null) {
				bw.close();
			}
			DBUtils.closeAll(conn, stmt, rs);
		}
	}
	
	public void clearComponentStatus(){
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement("delete from component_status");
			pstmt.executeUpdate();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, null);
		}
	}
	
	public Map<String,StatusAndUpdate> getComponentStatus(){
		Map<String,StatusAndUpdate> result=new HashMap<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs=null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement("select * from component_status");
			rs=pstmt.executeQuery();
			while(rs.next()){
				String key=rs.getString("host_name_port");
				String status=rs.getString("status");
				Date d=new Date(rs.getTimestamp("last_update").getTime());
				result.put(key, new StatusAndUpdate(status, d));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, rs);
		}
		
		return result;
	}
	
	public void updateComponentStatus(String key, String status) {
		String realKey=HostNameTools.getHostName()+"\t"+HostNameTools.getProcessId()+"\t"+key;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = PoolManager.getConnection();
			pstmt = conn.prepareStatement(
					"insert into component_status(host_name_port, status, last_update) values(?,?,now())"
					+"on duplicate key update status=?, last_update=now()");
			pstmt.setString(1, realKey);
			pstmt.setString(2, status);
			pstmt.setString(3, status);
			pstmt.executeUpdate();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, null);
		}
	}
	
	public void addReviewUrls(List<Integer> ids, List<String> urls) throws Exception{
		Connection conn = PoolManager.getConnection();
		int batchSize=1000;
		boolean autoCommit=conn.getAutoCommit();
		try{
			conn.setAutoCommit(false);
			for(int i=0;i<ids.size();i+=batchSize){
				List<Integer> subIds=ids.subList(i, Math.min(i+batchSize, ids.size()));
				List<String> subUrls=urls.subList(i, Math.min(i+batchSize, ids.size()));
				this.doAddReviewUrls(conn, subIds, subUrls);
			}
		}finally{
			conn.setAutoCommit(autoCommit);
			DBUtils.closeAll(conn, null, null);
		}
	}
	
	private void doAddReviewUrls(Connection conn, List<Integer> ids, List<String> urls) throws SQLException{
		
		PreparedStatement pstmt = null;
		try {
			
			pstmt = conn.prepareStatement(
					"insert ignore into review_status(page_id, page_url, add_time) values(?,?,now())");
			Iterator<Integer> idIter=ids.iterator();
			Iterator<String> urlIter=urls.iterator();
			while(idIter.hasNext()){
				pstmt.setInt(1, idIter.next());
				pstmt.setString(2, urlIter.next()); 
				pstmt.addBatch();
			} 

			pstmt.executeBatch();

		}finally {
			DBUtils.closeAll(null, pstmt, null);
		}
	}
}
