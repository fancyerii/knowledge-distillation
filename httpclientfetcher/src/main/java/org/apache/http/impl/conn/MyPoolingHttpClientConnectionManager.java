package org.apache.http.impl.conn;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.conn.CPoolEntry;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

public class MyPoolingHttpClientConnectionManager extends PoolingHttpClientConnectionManager {
	protected static Logger logger = Logger.getLogger(MyPoolingHttpClientConnectionManager.class);

	protected HttpClientConnection leaseConnection(final Future<CPoolEntry> future, final long timeout,
			final TimeUnit tunit) throws InterruptedException, ExecutionException, ConnectionPoolTimeoutException {
		long start = System.currentTimeMillis();
		HttpClientConnection conn = super.leaseConnection(future, timeout, tunit);
		logger.info("leaseConnection: " + (System.currentTimeMillis() - start) + " ms");
		return conn;
	}
}
