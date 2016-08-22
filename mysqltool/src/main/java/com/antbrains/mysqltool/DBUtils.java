package com.antbrains.mysqltool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

public class DBUtils {
	protected static Logger logger = Logger.getLogger(DBUtils.class);

	public static void closeAll(Connection conn, Statement stmt, ResultSet rs) {
		if (rs != null)
			try {
				rs.close();
			} catch (Exception ex) {
				logger.error("", ex);
			}
		if (stmt != null)
			try {
				stmt.close();
			} catch (Exception ex) {
				logger.error("", ex);
			}
		if (conn != null)
			try {
				conn.close();
			} catch (Exception ex) {
				logger.error("", ex);
			}
	}

	public static String readHtml(InputStream is) throws IOException {
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

	public static Integer getNullableInt(String colName, ResultSet rs) {
		int colValue;
		try {
			colValue = rs.getInt(colName);
			if (rs.wasNull()) {
				return null;
			}
		} catch (SQLException e) {
			return null;
		}
		return colValue;
	}

	public static Integer getNullableInt(int colIndex, ResultSet rs) {
		int colValue;
		try {
			colValue = rs.getInt(colIndex);
			if (rs.wasNull()) {
				return null;
			}
		} catch (SQLException e) {
			return null;
		}
		return colValue;
	}

	public static Timestamp date2Timestamp(java.util.Date date) {
		if (date == null)
			return null;
		return new Timestamp(date.getTime());
	}

	public static java.util.Date timestamp2Date(java.sql.Timestamp timestamp) {
		if (timestamp == null)
			return null;
		return new java.util.Date(timestamp.getTime());
	}

	private static final String SPLITER = "#@#";

	public static List<String> tagString2List(String tags) {
		if (tags == null)
			return null;

		String[] tokens = tags.split(SPLITER);
		return Arrays.asList(tokens);
	}

	public static String tagList2String(List<String> tagList) {
		if (tagList == null || tagList.size() == 0)
			return null;
		StringBuilder sb = new StringBuilder();
		for (String tag : tagList) {
			sb.append(tag + SPLITER);
		}
		sb.delete(sb.length() - SPLITER.length(), sb.length());
		return sb.toString();
	}

	public static Connection getConnection(String db, String driver, String url, String user, String pass)
			throws Exception {
		Class.forName(driver);
		Connection conn;
		String dbUrl = url;
		if (db != null) {
			dbUrl = url.replace("/${db}", "/" + db);
		}
		logger.debug(url);
		logger.debug(dbUrl);
		conn = DriverManager.getConnection(dbUrl, user, pass);

		return conn;

	}

}
