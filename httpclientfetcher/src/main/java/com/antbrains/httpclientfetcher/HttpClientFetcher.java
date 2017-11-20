package com.antbrains.httpclientfetcher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.SSLException;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.CharArrayBuffer;
import org.apache.log4j.Logger;

public class HttpClientFetcher implements Closeable {
	protected static Logger logger = Logger.getLogger(HttpClientFetcher.class);
	private CloseableHttpClient client;
	private volatile boolean isValid = false;

	private ConcurrentLinkedQueue<HttpTime> requestQueue;
	private ResultValidator rv;

	public ResultValidator getRv() {
		return rv;
	}

	public void setRv(ResultValidator rv) {
		this.rv = rv;
	}

	private String creator;

	public HttpClientFetcher(String creator) {
		this(creator, new NullResultValidator());
	}

	public HttpClientFetcher(String creator, ResultValidator rv) {
		if (rv == null)
			throw new IllegalArgumentException("rv is null");
		this.creator = creator;
		this.rv = rv;
	}

	private FetchMonitorThread monitorThread;

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getConnectionRequestTimeout() {
		return connectionRequestTimeout;
	}

	public void setConnectionRequestTimeout(int connectionRequestTimeout) {
		this.connectionRequestTimeout = connectionRequestTimeout;
	}

	private int readTimeout = 30000;
	private int sweepInterval = 60_000;
	private int monitorTimeout = 60000;

	public int getMonitorTimeout() {
		return monitorTimeout;
	}

	public void setMonitorTimeout(int monitorTimeout) {
		this.monitorTimeout = monitorTimeout;
	}

	public int getSweepInterval() {
		return sweepInterval;
	}

	public void setSweepInterval(int sweepInterval) {
		this.sweepInterval = sweepInterval;
	}

	private int connectTimeout = 10000;
	private int connectionRequestTimeout = 0;
	private int retryCount = 0;
	private long maxContentLength = 10L * 1024 * 1024; // 10Mb
	private String referer;
	private String cs = CookieSpecs.IGNORE_COOKIES;

	public String getCs() {
		return cs;
	}

	public void setCs(String cs) {
		this.cs = cs;
	}

	public String getReferer() {
		return referer;
	}

	public void setReferer(String referer) {
		this.referer = referer;
	}

	public long getMaxContentLength() {
		return maxContentLength;
	}

	public void setMaxContentLength(long maxContentLength) {
		this.maxContentLength = maxContentLength;
	}

	private boolean keepAlive = true;
	private long defaultKeepAlive = 3000;
	private int dnsCacheSize = 100000;
	
	private File dnsFile;

	public void setDnsFile(File dnsFile) {
		this.dnsFile = dnsFile;
	}

	public int getDnsCacheSize() {
		return dnsCacheSize;
	}

	public void setDnsCacheSize(int dnsCacheSize) {
		this.dnsCacheSize = dnsCacheSize;
	}

	public long getDefaultKeepAlive() {
		return defaultKeepAlive;
	}

	public void setDefaultKeepAlive(long defaultKeepAlive) {
		this.defaultKeepAlive = defaultKeepAlive;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public int getMaxTotalConnection() {
		return maxTotalConnection;
	}

	public void setMaxTotalConnection(int maxTotalConnection) {
		this.maxTotalConnection = maxTotalConnection;
	}

	public int getMaxConnectionPerRoute() {
		return maxConnectionPerRoute;
	}

	public void setMaxConnectionPerRoute(int maxConnectionPerRoute) {
		this.maxConnectionPerRoute = maxConnectionPerRoute;
	}

	private int maxTotalConnection = 100;
	private int maxConnectionPerRoute = 10;

	private MyHttpRoutePlanner routePlanner;
	private boolean doSweep = true;

	public boolean isDoSweep() {
		return doSweep;
	}

	public void setDoSweep(boolean doSweep) {
		this.doSweep = doSweep;
	}

	public void setRoutePlanner(MyHttpRoutePlanner routePlanner) {
		this.routePlanner = routePlanner;
	}

	private String userAgent = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0)";
	//private String userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:10.0) Gecko/20100101 Firefox/10.0";
	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	private HttpHost proxy;

	public HttpHost getProxy() {
		return proxy;
	}

	public void setProxy(HttpHost proxy) {
		this.proxy = proxy;
	}

	public void setHttpProxy(String host, int port) {
		this.setProxy(new HttpHost(host, port));
	}

	private RequestConfig defaultRequestConfig;

	private List<Header> defHeaders;

	public List<Header> getDefHeaders() {
		return defHeaders;
	}

	public void setDefHeaders(List<Header> defHeaders) {
		this.defHeaders = defHeaders;
	}

	/**
	 * after set all configs, we should always call init before any connection
	 */
	public void init() {
		defaultRequestConfig = RequestConfig.custom().setCookieSpec(this.getCs()).setExpectContinueEnabled(true)
				// .setStaleConnectionCheckEnabled(true)
				.setConnectTimeout(this.getConnectTimeout())
				.setConnectionRequestTimeout(this.getConnectionRequestTimeout()).setSocketTimeout(this.getReadTimeout())
				.build();

		HttpClientBuilder builder = HttpClients.custom();

		ConnectionKeepAliveStrategy myStrategy = null;
		HttpClientConnectionManager connManager = null;
		DnsResolver dnsResolover=null;
		if(this.dnsFile!=null && dnsFile.exists() && dnsFile.isFile()){
			dnsResolover=new FileLookupDnsResolver(dnsFile);
		}
		if (this.keepAlive) {
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
			connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, null, dnsResolover);

			((PoolingHttpClientConnectionManager) connManager).setMaxTotal(this.getMaxTotalConnection());
			((PoolingHttpClientConnectionManager) connManager).setDefaultMaxPerRoute(this.getMaxConnectionPerRoute());
			myStrategy = new ConnectionKeepAliveStrategy() {
				public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
					// Honor 'keep-alive' header
					HeaderElementIterator it = new BasicHeaderElementIterator(
							response.headerIterator(HTTP.CONN_KEEP_ALIVE));
					while (it.hasNext()) {
						HeaderElement he = it.nextElement();
						String param = he.getName();
						String value = he.getValue();
						if (value != null && param.equalsIgnoreCase("timeout")) {
							try {
								return Long.parseLong(value) * 1000;
							} catch (NumberFormatException ignore) {
							}
						}
					}
					return HttpClientFetcher.this.defaultKeepAlive;
				}

			};
			builder.setKeepAliveStrategy(myStrategy);
		} else {
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
			connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, null, dnsResolover);

			((PoolingHttpClientConnectionManager) connManager).setMaxTotal(this.getMaxTotalConnection());
			((PoolingHttpClientConnectionManager) connManager).setDefaultMaxPerRoute(this.getMaxConnectionPerRoute());

			builder.setConnectionReuseStrategy(new ConnectionReuseStrategy() {
				@Override
				public boolean keepAlive(HttpResponse response, HttpContext context) {
					return false;
				}
			});
		}
		
		if (this.routePlanner != null) {
			if (this.proxy != null) {
				logger.warn("proxy is set so routePlanner is useless");
			}
			builder.setRoutePlanner(routePlanner);
		}
		List<Header> headers = new ArrayList<>();
		if (defHeaders != null) {
			headers.addAll(defHeaders);
		}
		if (this.getReferer() != null) {
			headers.add(new BasicHeader(HttpHeaders.REFERER, this.getReferer()));
		}

		client = builder.setConnectionManager(connManager).setProxy(getProxy())
				// .setKeepAliveStrategy(myStrategy)
				.setRetryHandler(new MyHttpRequestRetryHander(retryCount)).setDefaultHeaders(headers)
				.setDefaultRequestConfig(defaultRequestConfig).setUserAgent(this.getUserAgent()).build();

		if (this.doSweep) {
			logger.info("start Monitor");
			this.requestQueue = new ConcurrentLinkedQueue<HttpTime>();
			this.monitorThread = new FetchMonitorThread(this.getMonitorTimeout(), this.getSweepInterval(), requestQueue,
					this.creator);
			this.monitorThread.start();
		}
		isValid = true;
	}

	public String getRedirectLocation(String url) throws Exception {
		if (!isValid)
			throw new RuntimeException("not valid now, you should init first");
		HttpGet httpget = new HttpGet(url);
		RequestConfig cfg = RequestConfig.copy(defaultRequestConfig).setRedirectsEnabled(false).build();
		// RequestConfig cfg=RequestConfig.custom()
		// .setRedirectsEnabled(false)
		// .build();

		httpget.setConfig(cfg);
		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 300 && status < 400) {
					return response.getFirstHeader("Location").getValue();
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};

		String responseBody = client.execute(httpget, responseHandler);
		return responseBody;

	}

	public Object[] httpGetStream(HttpGet httpget) throws Exception {
		HttpContext ctx = new BasicHttpContext();
		HttpTime ht = new HttpTime(httpget, System.currentTimeMillis());
		if (this.doSweep) {
			this.requestQueue.offer(ht);
		}
		HttpResponse hr = client.execute(httpget, ctx);
		return new Object[] { hr, ht, ctx };

	}

	public String httpGet(HttpGet httpget, final String encoding) throws Exception {
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					return entity != null ? entityToString(entity, Charset.forName(encoding)) : null;
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};

		return this.doExecute(httpget, responseHandler);
	}

	public void updateStatus(HttpContext ctx, HttpTime ht, boolean succ) {
		if (ctx != null) {
			if (this.routePlanner != null) {
				Proxy p = (Proxy) ctx.getAttribute("proxy");
				if (p != null) {
					long startTime = (Long) ctx.getAttribute("startTime");
					long timeUsed = System.currentTimeMillis() - startTime;
					routePlanner.getPm().updateProxyStatus(p, timeUsed, succ);
				}
			}
		}
		if (ht != null) {
			ht.finished = true;
			ht.get = null;
		}
	}

	private String doExecute(HttpPut httppost, ResponseHandler<String> responseHandler) throws Exception {
		HttpContext ctx = new BasicHttpContext();
		boolean succ = false;
		try {
			String responseBody = client.execute(httppost, responseHandler, ctx);
			if (rv.isValid(responseBody)) {
				succ = true;
				return responseBody;
			} else {
				succ = false;
				return null;
			}
		} finally {
			if (this.routePlanner != null) {
				Proxy p = (Proxy) ctx.getAttribute("proxy");
				if (p != null) {
					long startTime = (Long) ctx.getAttribute("startTime");
					long timeUsed = System.currentTimeMillis() - startTime;
					routePlanner.getPm().updateProxyStatus(p, timeUsed, succ);
				}
			}
		}
	}

	private String doExecute(HttpPost httppost, ResponseHandler<String> responseHandler) throws Exception {
		HttpContext ctx = new BasicHttpContext();
		boolean succ = false;
		try {
			String responseBody = client.execute(httppost, responseHandler, ctx);
			if (rv.isValid(responseBody)) {
				succ = true;
				return responseBody;
			} else {
				succ = false;
				return null;
			}
		} finally {
			if (this.routePlanner != null) {
				Proxy p = (Proxy) ctx.getAttribute("proxy");
				if (p != null) {
					long startTime = (Long) ctx.getAttribute("startTime");
					long timeUsed = System.currentTimeMillis() - startTime;
					routePlanner.getPm().updateProxyStatus(p, timeUsed, succ);
				}
			}
		}
	}

	private String doExecute(HttpGet httpget, ResponseHandler<String> responseHandler) throws Exception {
		HttpContext ctx = new BasicHttpContext();
		HttpTime ht = new HttpTime(httpget, System.currentTimeMillis());
		if (this.doSweep) {
			this.requestQueue.offer(ht);
		}
		boolean succ = false;
		try {
			String responseBody = client.execute(httpget, responseHandler, ctx);
			if (rv.isValid(responseBody)) {
				succ = true;
				return responseBody;
			} else {
				succ = false;
				return null;
			}
		} finally {
			if (this.routePlanner != null) {
				Proxy p = (Proxy) ctx.getAttribute("proxy");
				if (p != null) {
					long startTime = (Long) ctx.getAttribute("startTime");
					long timeUsed = System.currentTimeMillis() - startTime;
					routePlanner.getPm().updateProxyStatus(p, timeUsed, succ);
				}
			}
			ht.finished = true;
			ht.get = null;
		}
	}

	private Object[] doExecuteAndReturnRedirectCode(HttpGet httpget, ResponseHandler<Object[]> responseHandler)
			throws Exception {
		HttpClientContext ctx = HttpClientContext.create();
		HttpTime ht = new HttpTime(httpget, System.currentTimeMillis());
		if (this.doSweep) {
			this.requestQueue.offer(ht);
		}
		boolean succ = false;
		try {
			Object[] responseBody = client.execute(httpget, responseHandler, ctx);
			if (responseBody != null && rv.isValid((String) responseBody[1])) {
				succ = true;
				HttpHost target = ctx.getTargetHost();
				List<URI> redirectLocations = ctx.getRedirectLocations();
				URI location = URIUtils.resolve(httpget.getURI(), target, redirectLocations);
				responseBody[2] = location.toString();
				return responseBody;
			} else {
				return responseBody;
			}

		} finally {
			if (this.routePlanner != null) {
				Proxy p = (Proxy) ctx.getAttribute("proxy");
				if (p != null) {
					long startTime = (Long) ctx.getAttribute("startTime");
					long timeUsed = System.currentTimeMillis() - startTime;
					routePlanner.getPm().updateProxyStatus(p, timeUsed, succ);
				}
			}
			ht.finished = true;
			ht.get = null;
		}
	}

	private String[] doExecuteAndReturnRedirect(HttpGet httpget, ResponseHandler<String> responseHandler)
			throws Exception {
		HttpClientContext ctx = HttpClientContext.create();
		HttpTime ht = new HttpTime(httpget, System.currentTimeMillis());
		if (this.doSweep) {
			this.requestQueue.offer(ht);
		}
		boolean succ = false;
		try {
			String responseBody = client.execute(httpget, responseHandler, ctx);
			if (rv.isValid(responseBody)) {
				succ = true;
				HttpHost target = ctx.getTargetHost();
				List<URI> redirectLocations = ctx.getRedirectLocations();
				URI location = URIUtils.resolve(httpget.getURI(), target, redirectLocations);
				return new String[] { responseBody, location.toString() };
			} else {
				succ = false;
				return null;
			}

		} finally {
			if (this.routePlanner != null) {
				Proxy p = (Proxy) ctx.getAttribute("proxy");
				if (p != null) {
					long startTime = (Long) ctx.getAttribute("startTime");
					long timeUsed = System.currentTimeMillis() - startTime;
					routePlanner.getPm().updateProxyStatus(p, timeUsed, succ);
				}
			}
			ht.finished = true;
			ht.get = null;
		}
	}
	
	public String[] httpGetReturnRedirect(final String url, int retry) throws Exception{
	    return httpGetReturnRedirect(url, retry, null);
	}

	public String[] httpGetReturnRedirect(final String url, int retry, String encoding) throws Exception {
		for (int i = 0; i < retry; i++) {
			String[] arr = this.httpGetReturnRedirect(url, encoding);
			if (arr != null)
				return arr;
		}

		return null;
	}
	
	
	
	public Object[] httpGetReturnRedirectAndCode(final String url) throws Exception {
	    return httpGetReturnRedirectAndCode(url, null);
	}
	
	private ResponseHandler<Object[]> buildResponseHandler(final String url, final String encoding){
	       return new ResponseHandler<Object[]>() {

	            public Object[] handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
	                int status = response.getStatusLine().getStatusCode();
	                if (status >= 200 && status < 300) {
	                    HttpEntity entity = response.getEntity();
	                    if (entity == null)
	                        return null;
	                    if (entity.getContentLength() > HttpClientFetcher.this.maxContentLength) {
	                        throw new RuntimeException("too large content: " + url);
	                    }
	                    if(encoding==null){
	                        byte[] bytes = toByteArray(entity);
	                        ContentType contentType = ContentType.get(entity);
	                        if (contentType != null) {
	                            Charset charset = contentType.getCharset();
	                            if (charset != null) {
	                                return new Object[] { status, new String(bytes, charset), null };
	                            }
	                        }
	                        String charSet = CharsetDetector.getCharset(bytes);
	                        return new Object[] { status, new String(bytes, charSet), null };
	                    }else{
	                        byte[] bytes = toByteArray(entity);
	                        return new Object[] { status, new String(bytes, encoding), null };
	                    }

	                    
	                } else {
	                    return new Object[] { status, null, null };
	                }
	            }

	        };

	}

	public Object[] httpGetReturnRedirectAndCode(final String url, final String encoding) throws Exception {
		if (!isValid)
			throw new RuntimeException("not valid now, you should init first");
		HttpGet httpget = new HttpGet(url);

		// Create a custom response handler
		ResponseHandler<Object[]> responseHandler = this.buildResponseHandler(url, encoding);

		return this.doExecuteAndReturnRedirectCode(httpget, responseHandler);
	}
	
	public String[] httpGetReturnRedirect(final String url) throws Exception{
	    return httpGetReturnRedirect(url, null);
	}
	
	public String[] httpGetReturnRedirect(final String url, final String encoding) throws Exception {
		if (!isValid)
			throw new RuntimeException("not valid now, you should init first");
		HttpGet httpget = new HttpGet(url);

		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					if (entity == null)
						return null;
					if (entity.getContentLength() > HttpClientFetcher.this.maxContentLength) {
						throw new RuntimeException("too large content: " + url);
					}
					byte[] bytes = toByteArray(entity);
					if(encoding==null){
    					ContentType contentType = ContentType.get(entity);
    					if (contentType != null) {
    						Charset charset = contentType.getCharset();
    						if (charset != null) {
    							return new String(bytes, charset);
    						}
    					}
    					String charSet = CharsetDetector.getCharset(bytes);
    
    					return new String(bytes, charSet);
					}else{
					    return new String(bytes, encoding);
					}
				} else {
					throw new BadResponseException("", status);
				}
			}

		};

		return this.doExecuteAndReturnRedirect(httpget, responseHandler);

	}

	public String httpGet(String url, final String encoding) throws Exception {
		if (!isValid)
			throw new RuntimeException("not valid now, you should init first");
		HttpGet httpget = new HttpGet(url);

		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					return entity != null ? entityToString(entity, Charset.forName(encoding)) : null;
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};

		return this.doExecute(httpget, responseHandler);

	}

	/**
	 * Get the entity content as a String, using the provided default character
	 * set if none is found in the entity. If defaultCharset is null, the
	 * default "ISO-8859-1" is used.
	 * 
	 * @param entity
	 *            must not be null
	 * @param defaultCharset
	 *            character set to be applied if none found in the entity
	 * @return the entity content as a String. May be null if
	 *         {@link HttpEntity#getContent()} is null.
	 * @throws ParseException
	 *             if header elements cannot be parsed
	 * @throws IllegalArgumentException
	 *             if entity is null or if content length > Integer.MAX_VALUE
	 * @throws IOException
	 *             if an error occurs reading the input stream
	 * @throws UnsupportedCharsetException
	 *             Thrown when the named charset is not available in this
	 *             instance of the Java virtual machine
	 */
	private String entityToString(final HttpEntity entity, final Charset defaultCharset)
			throws IOException, ParseException {
		Args.notNull(entity, "Entity");
		final InputStream instream = entity.getContent();
		if (instream == null) {
			return null;
		}
		try {
			Args.check(entity.getContentLength() <= Integer.MAX_VALUE,
					"HTTP entity too large to be buffered in memory");
			int i = (int) entity.getContentLength();
			if (i < 0) {
				i = 4096;
			}
			Charset charset = null;
			try {
				final ContentType contentType = ContentType.get(entity);
				if (contentType != null) {
					charset = contentType.getCharset();
				}
			} catch (final UnsupportedCharsetException ex) {
				throw new UnsupportedEncodingException(ex.getMessage());
			}
			if (charset == null) {
				charset = defaultCharset;
			}
			if (charset == null) {
				charset = HTTP.DEF_CONTENT_CHARSET;
			}
			final Reader reader = new InputStreamReader(instream, charset);
			final CharArrayBuffer buffer = new CharArrayBuffer(i);
			final char[] tmp = new char[1024];
			int l;
			while ((l = reader.read(tmp)) != -1) {
				if (buffer.length() > this.maxContentLength) {
					throw new IOException("entity too long");
				}
				buffer.append(tmp, 0, l);
			}
			return buffer.toString();
		} finally {
			instream.close();
		}
	}

	/**
	 * Read the contents of an entity and return it as a byte array.
	 * 
	 * @param entity
	 *            the entity to read from=
	 * @return byte array containing the entity content. May be null if
	 *         {@link HttpEntity#getContent()} is null.
	 * @throws IOException
	 *             if an error occurs reading the input stream
	 * @throws IllegalArgumentException
	 *             if entity is null or if content length > Integer.MAX_VALUE
	 */
	private byte[] toByteArray(final HttpEntity entity) throws IOException {
		Args.notNull(entity, "Entity");
		final InputStream instream = entity.getContent();
		if (instream == null) {
			return null;
		}
		try {
			Args.check(entity.getContentLength() <= this.maxContentLength,
					"HTTP entity too large to be buffered in memory");
			int i = (int) entity.getContentLength();
			if (i < 0) {
				i = 4096;
			}
			final ByteArrayBuffer buffer = new ByteArrayBuffer(i);
			final byte[] tmp = new byte[4096];
			int l;
			while ((l = instream.read(tmp)) != -1) {
				if (buffer.length() >= this.maxContentLength)
					throw new IOException("entity too long");
				buffer.append(tmp, 0, l);
			}
			return buffer.toByteArray();
		} finally {
			instream.close();
		}
	}

	public String httpGet(final HttpGet httpget) throws Exception {
		if (!isValid)
			throw new RuntimeException("not valid now, you should init first");
		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					if (entity == null)
						return null;
					if (entity.getContentLength() > HttpClientFetcher.this.maxContentLength) {
						throw new RuntimeException("too large content: " + httpget.getURI().toString());
					}
					byte[] bytes = toByteArray(entity);
					ContentType contentType = ContentType.get(entity);
					if (contentType != null) {
						Charset charset = contentType.getCharset();
						if (charset != null) {
							return new String(bytes, charset);
						}
					}
					String charSet = CharsetDetector.getCharset(bytes);

					return new String(bytes, charSet);
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};
		return this.doExecute(httpget, responseHandler);
	}

	public String httpGet(final String url, int retry) throws Exception {
		Exception lastException = null;
		for (int i = 0; i < retry; i++) {
			try {
				String s = this.httpGet(url);
				return s;
			} catch (Exception e) {
				lastException = e;
			}
		}
		throw lastException;
	}

	public HttpResponse httpGetStreamWithoutProxy(final String url) throws Exception {
		if (!isValid)
			throw new RuntimeException("not valid now, you should init first");
		HttpGet httpget = new HttpGet(url);
		HttpTime ht = new HttpTime(httpget, System.currentTimeMillis());
		if (this.doSweep) {
			this.requestQueue.offer(ht);
		}
		return client.execute(httpget);
	}

	public Object[] httpGetStream(final String url) throws Exception {
		if (!isValid)
			throw new RuntimeException("not valid now, you should init first");
		HttpGet httpget = new HttpGet(url);
		return this.httpGetStream(httpget);
	}

	public String httpPut(final String url, String postData, String contentType) throws Exception {
		if (!isValid)
			throw new RuntimeException("not valid now, you should init first");
		HttpPut httppost = new HttpPut(url);
		if (contentType != null) {
			httppost.setHeader("Content-Type", contentType);
		}
		HttpEntity body = new ByteArrayEntity(postData.getBytes("UTF8"));
		httppost.setEntity(body);
		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					if (entity == null)
						return null;
					if (entity.getContentLength() > HttpClientFetcher.this.maxContentLength) {
						throw new RuntimeException("too large content: " + url);
					}
					byte[] bytes = toByteArray(entity);
					ContentType contentType = ContentType.get(entity);
					if (contentType != null) {
						Charset charset = contentType.getCharset();
						if (charset != null) {
							return new String(bytes, charset);
						}
					}
					String charSet = CharsetDetector.getCharset(bytes);
					if ("gb2312".equalsIgnoreCase(charSet)) {// 修正一些网站的问题，很多网站使用了gbk的编码，但是head里写的是gb2312
						charSet = "GBK";
					}
					return new String(bytes, charSet);
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};

		return this.doExecute(httppost, responseHandler);
	}

	public String httpPost(final String url, String postData, String contentType) throws Exception {
		if (!isValid)
			throw new RuntimeException("not valid now, you should init first");
		HttpPost httppost = new HttpPost(url);
		if (contentType != null) {
			httppost.setHeader("Content-Type", contentType);
		}
		HttpEntity body = new ByteArrayEntity(postData.getBytes("UTF8"));
		httppost.setEntity(body);
		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					if (entity == null)
						return null;
					if (entity.getContentLength() > HttpClientFetcher.this.maxContentLength) {
						throw new RuntimeException("too large content: " + url);
					}
					byte[] bytes = toByteArray(entity);
					ContentType contentType = ContentType.get(entity);
					if (contentType != null) {
						Charset charset = contentType.getCharset();
						if (charset != null) {
							return new String(bytes, charset);
						}
					}
					String charSet = CharsetDetector.getCharset(bytes);
					if ("gb2312".equalsIgnoreCase(charSet)) {// 修正一些网站的问题，很多网站使用了gbk的编码，但是head里写的是gb2312
						charSet = "GBK";
					}
					return new String(bytes, charSet);
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};

		return this.doExecute(httppost, responseHandler);
	}

	public String httpGet(final String url) throws Exception {
		if (!isValid)
			throw new RuntimeException("not valid now, you should init first");
		HttpGet httpget = new HttpGet(url);

		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					if (entity == null)
						return null;
					if (entity.getContentLength() > HttpClientFetcher.this.maxContentLength) {
						throw new RuntimeException("too large content: " + url);
					}
					byte[] bytes = toByteArray(entity);
					ContentType contentType = ContentType.get(entity);
					if (contentType != null) {
						Charset charset = contentType.getCharset();
						if (charset != null) {
							return new String(bytes, charset);
						}
					}
					String charSet = CharsetDetector.getCharset(bytes);
					if ("gb2312".equalsIgnoreCase(charSet)) {// 修正一些网站的问题，很多网站使用了gbk的编码，但是head里写的是gb2312
						charSet = "GBK";
					}
					return new String(bytes, charSet);
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};

		return this.doExecute(httpget, responseHandler);

	}
	
	public void close() {
		isValid = false;
		if (this.client != null) {
			try {
				client.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
		if (this.doSweep) {
			this.monitorThread.stopMe();
			this.monitorThread.interrupt();
		}
	}

	public static void main(String[] args) throws Exception {

		{
			HttpClientFetcher fetcher = new HttpClientFetcher("main");
			List<HttpHost> proxys = new ArrayList<>(ProxyDiscover.findProxys());

			ProxyManager pm = new ProxyManager(proxys, "http://baike.baidu.com/view/1.htm", "<title>百度百科_百度百科</title>",
					1000, 3, 60_000, 30, true, 50, 100, 4 * 3600_000, 300, 1000);
			fetcher.setRoutePlanner(new MyHttpRoutePlanner(pm));
			fetcher.setMaxConnectionPerRoute(100);
			fetcher.setMaxTotalConnection(200);
			fetcher.init();
			int N = 20;
			TestThread[] threads = new TestThread[N];
			for (int i = 0; i < threads.length; i++) {
				threads[i] = new TestThread(i * 100, (i + 1) * 100, fetcher);
				threads[i].start();
			}

			for (int i = 0; i < threads.length; i++) {
				threads[i].join();
			}
			System.out.println("finished");
			fetcher.close();
			pm.stopValidateThread();
			pm.waitValidateThread(0);
		}
	}

}

class TestThread extends Thread {
	private int startId;
	private int endId;
	private HttpClientFetcher fetcher;
	protected static Logger logger = Logger.getLogger(TestThread.class);

	public TestThread(int startId, int endId, HttpClientFetcher fetcher) {
		this.startId = startId;
		this.endId = endId;
		this.fetcher = fetcher;
	}

	@Override
	public void run() {
		long start = System.currentTimeMillis();
		int succ = 0;
		int fail = 0;
		for (int i = startId; i < endId; i++) {
			String url = "http://baike.baidu.com/view/" + i + ".htm";
			try {
				String[] arr = fetcher.httpGetReturnRedirect(url);
				if (!arr[1].equals(url)) {
					System.out.println("redirect: " + url + "->" + arr[1]);
				}
				succ++;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				fail++;
			}
			int total = succ + fail;
			if (total % 100 == 0) {
				logger.info("total: " + total);
				logger.info("avg: " + (System.currentTimeMillis() - start) / total + " ms");
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("total: " + (end - start) + " ms");
		System.out.println("avg: " + (1.0 * (end - start) / (succ + fail)) + " ms");
	}
}

class MyHttpRequestRetryHander implements HttpRequestRetryHandler {

	private int retryCount = 3;

	public MyHttpRequestRetryHander(int retryCount) {
		this.retryCount = retryCount;
	}

	@Override
	public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
		if (executionCount > retryCount) {
			// Do not retry if over max retry count
			return false;
		}
		if (exception instanceof InterruptedIOException) {
			// Timeout
			return true;
		}
		if (exception instanceof UnknownHostException) {
			// Unknown host
			return false;
		}
		if (exception instanceof ConnectTimeoutException) {
			// Connection refused
			return true;
		}
		if (exception instanceof SSLException) {
			// SSL handshake exception
			return false;
		}
		// HttpClientContext clientContext = HttpClientContext.adapt(context);
		// HttpRequest request = clientContext.getRequest();
		// boolean idempotent = !(request instanceof
		// HttpEntityEnclosingRequest);
		// if (idempotent) {
		// // Retry if the request is considered idempotent
		// return true;
		// }
		return false;
	}

};
