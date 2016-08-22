package com.antbrains.httpclientfetcher;

// package com.mobvoi.knowledgegraph.httpclientfetcher;
//
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.List;
//
// import org.apache.http.Header;
// import org.apache.http.HttpHost;
// import org.apache.http.client.config.CookieSpecs;
// import org.apache.http.client.methods.HttpGet;
// import org.apache.http.cookie.CookieSpec;
// import org.apache.http.message.BasicHeader;
// import org.apache.log4j.Logger;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
//
// import com.mobvoi.knowledgegraph.httpclientfetcher.HttpClientFetcher;
//
// import static org.junit.Assert.*;
//
// public class TestHttpClientFetcher {
//
// private HttpClientFetcher fetcher;
//
// @Before
// public void setUp() {
// fetcher = new HttpClientFetcher(this.getClass().getName());
// }
//
// @Test
// public void testDouban() throws Exception{
// fetcher=new HttpClientFetcher("");
// fetcher.setProxy(new HttpHost("127.0.0.1",8888));
// fetcher.setCs(CookieSpecs.BEST_MATCH);
// List<Header> headers=new ArrayList<>();
// Header cookie=new BasicHeader("Cookie","bid=\"n1KaOAIJrSE\"; ll=\"108288\";
// _pk_id.100001.4cf6=888894851e036161.1448871135.3.1449042641.1448942811.;
// __utma=30149280.224012794.1448871135.1448942812.1449042635.3;
// __utmz=30149280.1448871135.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none);
// __utma=223695111.2082579185.1448871136.1448942812.1449042635.3;
// __utmz=223695111.1448871136.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none);
// _pk_ses.100001.4cf6=*; __utmb=30149280.2.10.1449042635; __utmc=30149280;
// __utmt_douban=1; __utmb=223695111.0.10.1449042635; __utmc=223695111");
// headers.add(cookie);
// fetcher.setDefHeaders(headers);
// fetcher.init();
//
// String ss=fetcher.httpGet("http://www.douban.com/");
// System.out.println(ss);
// String s=fetcher.httpGet("http://movie.douban.com/later/beijing/");
// System.out.println(s);
// fetcher.close();
// }
//
// @Test
// public void testAbort() throws Exception {
// fetcher.setKeepAlive(false);
// fetcher.init();
// final HttpGet get = new HttpGet("http://news.qq.com/a/20140114/004144.htm");
// Thread t = (new Thread() {
// @Override
// public void run() {
// try {
// Thread.sleep(500);
// } catch (InterruptedException e) {
//
// }
// System.out.println("abort");
// get.abort();
// }
// });
// t.start();
//
// String s = null;
// try {
// s = fetcher.httpGet(get);
// } catch (Exception e) {
// // e.printStackTrace();
// }
// System.out.println("fetched");
// t.join();
// }
//
// @Test
// public void testNotAbort() throws Exception {
// fetcher.setKeepAlive(false);
// fetcher.init();
// final HttpGet get = new HttpGet("http://news.qq.com/a/20140114/004144.htm");
// Thread t = (new Thread() {
// @Override
// public void run() {
// try {
// Thread.sleep(10000);
// } catch (InterruptedException e) {
//
// }
// System.out.println("abort");
// get.abort();
// }
// });
// t.start();
//
// String s = null;
// try {
// s = fetcher.httpGet(get);
// } catch (Exception e) {
// e.printStackTrace();
// }
// System.out.println("fetched");
// t.join();
// assertNotEquals(null, s);
// }
//
// @Test
// public void testCharDetect() throws Exception {
// fetcher.init();
// String url =
// "http://politics.people.com.cn/BIG5/n/2014/0219/c1001-24409541-2.html";
// String s = fetcher.httpGet(url);
// }
//
// @Test
// public void testRedirect() throws Exception {
// fetcher.init();
// String url = fetcher
// .getRedirectLocation("http://www.baidu.com/link?url=dXF4laNd89AE4dwmlh7WkefS9bUfSpr-mMO5nG5wTagqn-8H-98PljRvropuHK8fzC1-YDwBW5yDaSA8HVHvD_");
// assertEquals(url, "http://club.autohome.com.cn/bbs/forum-c-2945-1.html");
// }
//
// @Test
// public void testConcurrent() {
// fetcher.setMaxTotalConnection(10);
// fetcher.setMaxConnectionPerRoute(2);
// fetcher.setHttpProxy("isasrv", 80);
// fetcher.init();
//
// String[] urls = new String[] {
// "http://news.qq.com/zt2014/2014chunyun/index.htm",
// "http://news.qq.com/a/20140114/004144.htm",
// "http://news.qq.com/a/20140114/002331.htm",
// "http://news.qq.com/a/20140114/002331.htm",
// "http://hc.apache.org/httpcomponents-client-4.3.x/httpclient/examples/org/apache/http/examples/client/ClientWithResponseHandler.java",
// "http://hc.apache.org/httpcomponents-client-4.3.x/httpclient/examples/org/apache/http/examples/client/ClientConnectionRelease.java",
// };
//
// Thread[] threads = new Thread[urls.length];
// for (int i = 0; i < urls.length; i++) {
// threads[i] = new Thread(new GetThread(fetcher, urls[i]));
// }
// for (int i = 0; i < threads.length; i++) {
// threads[i].start();
// }
// for (int i = 0; i < threads.length; i++) {
// try {
// threads[i].join();
// } catch (InterruptedException e) {
// e.printStackTrace();
// }
// }
// }
//
// @After
// public void tearDown() throws IOException {
// fetcher.close();
// }
// }
//
// class GetThread implements Runnable {
// protected static Logger logger = Logger.getLogger(GetThread.class);
// HttpClientFetcher fetcher;
// String url;
//
// public GetThread(HttpClientFetcher fetcher, String url) {
// this.fetcher = fetcher;
// this.url = url;
// }
//
// @Override
// public void run() {
// try {
// logger.info("start: " + url);
// String content = fetcher.httpGet(url);
// if (content == null) {
// logger.info("fail: " + url);
// } else {
// logger.info("success: " + url);
// }
//
// } catch (Exception e) {
// logger.error(e.getMessage(), e);
// }
// }
//
// }
