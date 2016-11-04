package com.antbrains.sc.frontier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;

import org.apache.log4j.Logger;

import com.antbrains.mysqltool.DBUtils;
import com.antbrains.mysqltool.PoolManager;
import com.antbrains.sc.data.WebPage;

public class MysqlFrontier implements Frontier {
	protected static Logger logger = Logger.getLogger(MysqlFrontier.class);

	@Override
	public void addWebPage(WebPage webPage, int failCountInc) {
		if(webPage.getUrl()==null) {
			logger.info("skip null");
			return;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = PoolManager.getConnection();
			webPage.setFailCount(webPage.getFailCount() + failCountInc);
			pstmt = conn.prepareStatement(
					"insert IGNORE into unfinished(id,url,depth,failcount,priority,lastVisit,redirectedUrl) values(?,?,?,?,?,?,?)");
			pstmt.setInt(1, webPage.getId());
			pstmt.setString(2, webPage.getUrl());
			pstmt.setInt(3, webPage.getDepth());
			pstmt.setInt(4, webPage.getFailCount());
			pstmt.setInt(5, webPage.getCrawlPriority());
			Date lastVisit = webPage.getLastVisitTime();
			if (lastVisit == null) {
				pstmt.setLong(6, 0);
			} else {
				pstmt.setLong(6, lastVisit.getTime());
			}
			pstmt.setString(7, webPage.getRedirectedUrl());

			pstmt.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DBUtils.closeAll(conn, pstmt, null);
		}
	}

	@Override
	public void close() {

	}

}
