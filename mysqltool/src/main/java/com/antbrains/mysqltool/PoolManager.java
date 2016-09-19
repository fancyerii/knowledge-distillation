package com.antbrains.mysqltool;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

public class PoolManager {
	protected static Logger logger = Logger.getLogger(PoolManager.class);

	private static String driver = "", // 驱动
			url = "", // URL
			Name = "", // 用户名
			Password = "";// 密码
	private static int maxConn = 20;
	private static int maxIdle = 10;
	private static int minIdle = 5;

	private static Class driverClass = null;
	private static GenericObjectPool connectionPool = null;

	public PoolManager() {
	}

	/**
	 * 装配配置文件 initProperties
	 */
	private static void loadProperties(String confDir, String dbName) {
		try {

			InputStream is = new FileInputStream(confDir + "/dbConf.properties");

			java.util.Properties props = new java.util.Properties();
			props.load(is);

			driver = props.getProperty("MYSQL_DRIVER");
			url = props.getProperty("MYSQL_URL");
			url = url.replace("/${db}", "/" + dbName);
			Name = props.getProperty("MYSQL_USER");
			Password = props.getProperty("MYSQL_PASS");

			try {
				maxConn = Integer.valueOf(props.getProperty("maxConn", "20"));
			} catch (Exception e) {
				logger.warn("maxConn: " + props.getProperty("maxConn"));
			}

			try {
				maxIdle = Integer.valueOf(props.getProperty("maxIdle", "10"));
			} catch (Exception e) {
				logger.warn("maxIdle: " + props.getProperty("maxIdle"));
			}

			try {
				minIdle = Integer.valueOf(props.getProperty("minIdle", "5"));
			} catch (Exception e) {
				logger.warn("minIdle: " + props.getProperty("minIdle"));
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public static synchronized void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			@Override
			public void run() {
				if (connectionPool != null) {
					PoolManager.ShutdownPool();
				}
			}

		}));
	}

	/**
	 * 初始化数据源
	 */
	private static synchronized void initDataSource() {
		if (driverClass == null) {
			try {
				driverClass = Class.forName(driver);
			} catch (ClassNotFoundException e) {
				logger.error(e);
			}
		}
	}

	public static synchronized void StartPool(String url, String Name, String Password, String driverClazz, int maxConn,
			int minIdle, int maxIdle) {
		addShutdownHook();
		try {
			driverClass = Class.forName(driverClazz);
		} catch (ClassNotFoundException e1) {
			logger.error(e1);
		}

		if (connectionPool != null) {
			ShutdownPool();
		}
		try {
			connectionPool = new GenericObjectPool(null);
			connectionPool.setMaxActive(maxConn);
			connectionPool.setMinIdle(minIdle);
			connectionPool.setMaxIdle(maxIdle);

			ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, Name, Password);
			PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,
					connectionPool, null, null, false, true);
			connectionPool.setFactory(poolableConnectionFactory);
			Class.forName("org.apache.commons.dbcp.PoolingDriver");
			PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
			driver.registerPool("dbpool", connectionPool);
			logger.info("数据库连接池加载成功");
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public static synchronized void StartPool(String url, String Name, String Password, String driverClazz) {
		StartPool(url, Name, Password, driverClazz, maxConn, minIdle, maxIdle);
	}

	/**
	 * 连接池启动
	 * 
	 */
	public static synchronized void StartPool(String confDir, String dbName) {
		addShutdownHook();
		loadProperties(confDir, dbName);
		initDataSource();
		if (connectionPool != null) {
			ShutdownPool();
		}
		try {
			connectionPool = new GenericObjectPool(null);
			connectionPool.setMaxActive(maxConn);
			connectionPool.setMinIdle(minIdle);
			connectionPool.setMaxIdle(maxIdle);
			ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, Name, Password);
			PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,
					connectionPool, null, null, false, true);
			connectionPool.setFactory(poolableConnectionFactory);
			Class.forName("org.apache.commons.dbcp.PoolingDriver");
			PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
			driver.registerPool("dbpool", connectionPool);
			logger.info("数据库连接池加载成功");
		} catch (Exception e) {
			logger.error(e);
		}
	}

	/**
	 * 释放连接池
	 */
	public static void ShutdownPool() {
		logger.info("shut down pool");
		try {
			PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
			driver.closePool("dbpool");
		} catch (SQLException e) {
			logger.error(e);
		}
	}

	/**
	 * 取得连接池中的连接
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static Connection getConnection(String confDir, String dbName) throws SQLException {
		Connection conn = null;
		if (connectionPool == null) {
			StartPool(confDir, dbName);
		}

		conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:dbpool");

		return conn;
	}

	public static Connection getConnection() throws SQLException {
		if (connectionPool == null) {
			throw new RuntimeException("You must call StartPool or getConnection(String) before call getConnection()");
		}
		return DriverManager.getConnection("jdbc:apache:commons:dbcp:dbpool");

	}

	public static Connection getConnection(String dbName) throws SQLException {
		if (connectionPool == null) {
			throw new RuntimeException("You must call StartPool or getConnection(String) before call getConnection()");
		}
		Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:dbpool");

		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("use " + dbName);
			pstmt.execute();
		} finally {
			if (pstmt != null) {
				pstmt.close();
			}
		}
		return conn;

	}

	/**
	 * 释放连接 freeConnection
	 * 
	 * @param conn
	 */
	public static void freeConnection(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.error(e);
			}
		}
	}

	/**
	 * 释放连接 freeConnection
	 * 
	 * @param name
	 * @param con
	 */
	public static void freeConnection(String name, Connection con) {
		freeConnection(con);
	}
	

}