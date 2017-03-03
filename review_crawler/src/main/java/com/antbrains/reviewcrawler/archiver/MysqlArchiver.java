package com.antbrains.reviewcrawler.archiver;

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
import com.antbrains.reviewcrawler.data.CrawlTask;
import com.antbrains.reviewcrawler.data.ReviewContent;
import com.antbrains.reviewcrawler.data.ReviewStatus; 
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MysqlArchiver{
	protected static Logger logger = Logger.getLogger(MysqlArchiver.class);
	
	private int addComments(Connection conn, List<ReviewContent> reviews) throws SQLException{
		PreparedStatement pstmt=null;
		int affected=0;
		try{
			pstmt=conn.prepareStatement("insert ignore into review_content(rv_id,page_id,date,content) values(?,?,?,?)");
			for(ReviewContent rv:reviews){
				pstmt.setString(1, rv.getId());
				pstmt.setInt(2, rv.getPageId());
				pstmt.setString(3, rv.getDate());
				pstmt.setString(4, rv.getContent());
				pstmt.addBatch();
			}
			int[] affectedRows=pstmt.executeBatch();
			for(int ar:affectedRows){
				affected+=ar;
			}
		}finally{
			DBUtils.closeAll(null, pstmt, null);
		}
		return affected;
	}
	
	public int addComments(List<ReviewContent> reviews) throws SQLException{
		Connection  conn=PoolManager.getConnection();
		boolean autoCommit=conn.getAutoCommit();
		int batchSize=1000;
		int added=0;
		try{
			for(int i=0;i<reviews.size();i+=batchSize){
				List<ReviewContent> subList=reviews.subList(i, Math.min(i+batchSize, reviews.size()));
				added+=this.addComments(conn, subList);
			}
		}finally{
			try{
				conn.setAutoCommit(autoCommit);
			}catch(Exception e){}
			
			DBUtils.closeAll(conn, null, null);
		}
		return added;
	}
	
	public void updateCommentStatus(ReviewStatus rs) throws SQLException{
		Connection conn=PoolManager.getConnection();
		PreparedStatement pstmt=null;
		try{
			pstmt=conn.prepareStatement("update review_status set lastUpdate=now(), crawling_status=2, lastestReviewTime=?, lastAdded=?, total_review=? where page_id=?");
			pstmt.setString(1, rs.getLastestReviewTime());
			pstmt.setInt(2, rs.getLastAdded());
			pstmt.setInt(3, rs.getTotalRv());
			pstmt.setInt(4, Integer.valueOf(rs.getId()));
			pstmt.executeUpdate();
		}finally{
			DBUtils.closeAll(conn, pstmt, null);
		}
	}
	

	public void dumpReview(String outputPath, int printProgressEvery) throws Exception{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		BufferedWriter bw = null;

		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath), "UTF8"));
			conn = PoolManager.getConnection();
			stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
			stmt.setFetchSize(Integer.MIN_VALUE);

			rs = stmt.executeQuery("select rc.page_id, page_url, rv_id, content from review_content as rc join review_status as rs on  rc.page_id=rs.page_id");
			Gson gson = new Gson();
			int i = 0;
			JsonParser parser=new JsonParser();
			int total = 0;
			while (rs.next()) {
				i++;
				if (i % printProgressEvery == 0) {
					logger.info("progress: " + i + "\t write: " + total);
				}
				String rc=rs.getString(4);
				try{
					JsonObject jo=parser.parse(rc).getAsJsonObject();
					int pid=rs.getInt(1);
					String url=rs.getString(2);
					
					total++;
					jo.addProperty("#pid#", pid);
					jo.addProperty("#url#", url);
					bw.write(gson.toJson(jo) + "\n");
				}catch(Exception e){
					logger.warn(e.getMessage());
				}
				
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
	
	public static void main(String[] args) throws SQLException{
		PoolManager.StartPool("conf", "ifeng");
		MysqlArchiver archiver=new MysqlArchiver();
		
		List<ReviewContent> reviews=new ArrayList<>();
		{
			ReviewContent rc=new ReviewContent();
			rc.setId("1");
			rc.setPageId(1);
			rc.setDate("2000/1/1");
			rc.setContent("c1");
			reviews.add(rc);
		}
		{
			ReviewContent rc=new ReviewContent();
			rc.setId("2");
			rc.setPageId(1);
			rc.setDate("2000/1/1");
			rc.setContent("c2");
			reviews.add(rc);
		}
		int added=archiver.addComments(reviews);
		System.out.println("added: "+added);
		
		{
			ReviewContent rc=new ReviewContent();
			rc.setId("3");
			rc.setPageId(1);
			rc.setDate("2000/1/1");
			rc.setContent("c2");
			reviews.add(rc);
		}
		
		added=archiver.addComments(reviews);
		System.out.println("added: "+added);
	}
	
}
