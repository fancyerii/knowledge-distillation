package com.antbrains.sina_es.index;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.antbrains.sina_es.data.Article;
import com.antbrains.sina_es.tools.IntervalParser;
import com.antbrains.sina_es.tools.gson.GsonTool;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ArticleIndexer {
	protected static Logger logger=Logger.getLogger(ArticleIndexer.class);
	
	private Client client ;
	private Gson gson=GsonTool.getGson();
	private long interval;
	private String inDir;
	private boolean deleteAfterProcess;
	private int batchSize=100;
	Set<String> processedFiles=new HashSet<>();
	public ArticleIndexer(String inDir, long interval, boolean deleteAfterProcess, 
			String clusterName, String hostName) throws Exception{
		logger.info("inDir: "+inDir);
		logger.info("interval: "+interval);
		logger.info("deleteAfterProcess: "+deleteAfterProcess);
		logger.info("clusterName: "+clusterName);
		logger.info("hostName: "+hostName);
		
		this.inDir=inDir;
		this.interval=interval;
		this.deleteAfterProcess=deleteAfterProcess;
		Settings settings = Settings.settingsBuilder()
		        .put("cluster.name", clusterName).build();
		client = TransportClient.builder().settings(settings).build()
				   .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostName), 9300));
	}
	
	public void doWork(){
		while(true){
			this.process();
			
			try {
				Thread.sleep(this.interval);
			} catch (InterruptedException e) { 
			}
		}
	}
	
	public void process(){
		Gson gson=GsonTool.getGson();
		for(File file:new File(inDir).listFiles()){
			if(!file.getName().endsWith(".txt")){
				continue;
			}
			if(this.processedFiles.contains(file.getName())){
				continue;
			}
			logger.info("process: "+file.getName());
			
			try(BufferedReader br=new BufferedReader(
					new InputStreamReader(new FileInputStream(file),"UTF8"))){
				String line;
				ArrayList<Article> articles=new ArrayList<>();
				while((line=br.readLine())!=null){
					Article article=gson.fromJson(line, Article.class);
					articles.add(article);
					if(articles.size()>=batchSize){
						BulkResponse rsp=this.index(articles);
						if(rsp.hasFailures()){
							for(BulkItemResponse itemRsp:rsp.getItems()){
								logger.info("indexFail: "+itemRsp.getItemId()+"\t"+this.findUrl(articles, itemRsp.getId())+"\t"+itemRsp.getFailureMessage());
								if(itemRsp.getItemId()<articles.size()){
									logger.info("\tbyId: "+articles.get(itemRsp.getItemId()).getUrl());
								}
							}
						}
						articles.clear();
					}
				}
				if(articles.size()>0){
					this.index(articles);
				}
			}catch(Exception e){
				logger.error(e.getMessage(),e);
				continue;
			}
			logger.info("finish: "+file.getName());
			processedFiles.add(file.getName());
			if(deleteAfterProcess){
				logger.info("delete file: "+file.getName());
				file.delete();
			}
		}
	}
	
	private String findUrl(List<Article> articles, String md5){
		for(Article article:articles){
			if(article.getMd5().equals(md5)){
				return article.getUrl();
			}
		}
		return null;
	}
	
	private BulkResponse index(List<Article> articles){
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for(Article article:articles){
			bulkRequest.add(client.prepareIndex(Constants.indexName, Constants.TYPE_ARTICLE, article.getMd5()).setSource(gson.toJson(article)));
		}
		BulkResponse bulkResponse = bulkRequest.get();
		return bulkResponse;
	}
	public static void main(String[] args) throws Exception{
		if(args.length!=5){
			System.out.println("need 5 args: inDir interval deleteProcessed clusterName hostName");
			System.exit(-1);
		}
		long interval=IntervalParser.parseInterval(args[1]);
		boolean deleteAfterProcess=Boolean.valueOf(args[2]);
		
		ArticleIndexer indexer=new ArticleIndexer(args[0], interval, deleteAfterProcess, args[3], args[4]);
		indexer.doWork();
	}

}
