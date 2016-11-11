package com.antbrains.ifengcrawler.dumper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.antbrains.mysqltool.PoolManager;
import com.antbrains.sc.archiver.MysqlArchiver;
import com.antbrains.sc.init.MysqlInit;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ImportReviewUrl {
	protected static Logger logger=Logger.getLogger(ImportReviewUrl.class);
	private String inDir; 
	private boolean deleteAfterProcess;
	private long interval;
	private MysqlArchiver archiver;
	public ImportReviewUrl(String inDir, long interval, boolean deleteAfterProcess){
		this.inDir=inDir; 
		this.interval=interval;
		this.deleteAfterProcess=deleteAfterProcess;
		logger.info("inDir: "+inDir); 
		logger.info("interval: "+interval);
		logger.info("deleteAfterProcess: "+deleteAfterProcess); 
		archiver=new MysqlArchiver();
	}
	Set<String> processedFiles=new HashSet<>();
	public void doWork(){
		while(true){
			this.process();
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) { 
			}
		}
	}
	
	public void process(){
		for(File file:new File(inDir).listFiles()){
			if(!file.getName().endsWith(".txt")){
				continue;
			}
			if(this.processedFiles.contains(file.getName())){
				continue;
			}
			logger.info("process: "+file.getName());
			
			try(	BufferedReader br=new BufferedReader(
					new InputStreamReader(new FileInputStream(file),"UTF8")); ){
				String line;
				JsonParser parser=new JsonParser(); 
				List<Integer> ids=new ArrayList<>(1000);
				List<String> urls=new ArrayList<>(1000);
				while((line=br.readLine())!=null){
					JsonObject jo=parser.parse(line).getAsJsonObject();
					String url=jo.get("#url#").getAsString();
					int id=jo.get("#id#").getAsInt();
					ids.add(id);
					urls.add(url);
					if(ids.size()>=1000){
						archiver.addReviewUrls(ids, urls);
						ids.clear();
						urls.clear();
					}
				}
				if(ids.size()>0){
					archiver.addReviewUrls(ids, urls);
					ids.clear();
					urls.clear();
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
 
	 
	public static void main(String[] args) {
		if(args.length!=4){
			System.out.println("need 4 args: inDir interval deleteProcessed dbName");
			System.exit(-1);
		}
		PoolManager.StartPool("conf", args[3]);
		long interval=MysqlInit.parseInterval(args[1]);
		boolean deleteProcessed=Boolean.valueOf(args[2]);
		new File(args[1]).mkdirs(); 
		ImportReviewUrl pa=new ImportReviewUrl(args[0] , interval, deleteProcessed);
		pa.doWork();
	}

}
