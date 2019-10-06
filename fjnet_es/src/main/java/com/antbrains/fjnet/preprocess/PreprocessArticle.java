package com.antbrains.fjnet.preprocess;

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

import com.antbrains.fjnet.data.Article;
import com.antbrains.fjnet.tools.IntervalParser;
import com.antbrains.fjnet.tools.UrlTools;
import com.antbrains.fjnet.tools.gson.GsonTool;
import com.antbrains.nekohtmlparser.NekoHtmlParser;
import com.antbrains.nlp.wordseg.SentenceSplit;
import com.antbrains.nlp.wordseg.WordSeg;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PreprocessArticle {
	protected static Logger logger=Logger.getLogger(PreprocessArticle.class);
	private String inDir;
	private String outDir;
	private WordSeg ws;
	private boolean deleteAfterProcess;
	private long interval;
	public PreprocessArticle(String inDir, String outDir, long interval, boolean deleteAfterProcess){
		this.inDir=inDir;
		this.outDir=outDir;
		this.interval=interval;
		this.deleteAfterProcess=deleteAfterProcess;
		logger=Logger.getLogger(PreprocessArticle.class);
		logger.info("inDir: "+inDir);
		logger.info("outDir: "+outDir);
		logger.info("interval: "+interval);
		logger.info("deleteAfterProcess: "+deleteAfterProcess);
		ws=WordSeg.getInstance();
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
					new InputStreamReader(new FileInputStream(file),"UTF8"));
					BufferedWriter bw = new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream(outDir+"/"+file.getName()), "UTF8"));){
				String line;
				JsonParser parser=new JsonParser();
				Gson gson=GsonTool.getGson();
				while((line=br.readLine())!=null){
					JsonObject jo=parser.parse(line).getAsJsonObject();
					String url=jo.get("#url#").getAsString();
					String html=jo.get("#html#").getAsString();

					NekoHtmlParser p=new NekoHtmlParser();
					p.load(html, "UTF8");
					/*
					String type=this.getArticleType(p);
					
					if(type==null){
						logger.warn("can't get type: "+url);
						continue;
					}*/
					Article article=null;
					article=this.parseNews(p, url);
					
					if(article==null){
						logger.warn("article null:  "+url);
					}else{
						article.setTypes(new String[]{"正文"});
						article.setSegContent(this.seg(article.getContent()));
						article.setSegTitle(seg(article.getTitle()));
						bw.write(gson.toJson(article)+"\n");
					}
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
	
	private String seg(String s){
		if(s==null||s.isEmpty()) return "";
		String[] sens=SentenceSplit.splitSentences(s);
		StringBuilder sb=new StringBuilder("");
		for(String sen:sens){
			if(sen.length()>0 && ( sen.charAt(sen.length()-1)=='\n' ||  sen.charAt(sen.length()-1)=='\r')){
				sen=sen.substring(0, sen.length()-1);
			}
			List<String> words=ws.mmRmmSeg(sen);
			for(String word:words){
				sb.append(word).append("\t");
			}
		}
		if(sb.length()==0) return "";
		return sb.substring(0, sb.length()-1);
	}
	
	private Pattern ptn1=Pattern.compile("(\\d+)年(\\d+)月(\\d+)日 (\\d+):(\\d+):(\\d+)");
	private Pattern ptn2=Pattern.compile("(\\d+)年(\\d+)月(\\d+)日");
									   
	private Date parsePubTime(String s){
		try{
			Matcher m=ptn1.matcher(s);
			if(m.matches()){
				int year=Integer.valueOf(m.group(1));
				int month=Integer.valueOf(m.group(2));
				int day=Integer.valueOf(m.group(3));
				int hour=Integer.valueOf(m.group(4));
				int minute=Integer.valueOf(m.group(5));
				int second=Integer.valueOf(m.group(6));
				Calendar cal=Calendar.getInstance();
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, month-1);
				cal.set(Calendar.DAY_OF_MONTH, day);
				cal.set(Calendar.HOUR, hour);
				cal.set(Calendar.MINUTE, minute);
				cal.set(Calendar.SECOND, second);
				return cal.getTime();
			}
			m=ptn2.matcher(s);
			if(m.matches()){
				int year=Integer.valueOf(m.group(1));
				int month=Integer.valueOf(m.group(2));
				int day=Integer.valueOf(m.group(3));
				Calendar cal=Calendar.getInstance();
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, month-1);
				cal.set(Calendar.DAY_OF_MONTH, day);
				return cal.getTime();
			}
		}catch(Exception e){
			return null;
		}
		return null;
	}
	
	private int parseInt(String s, int defaultV){
		try{
			return Integer.valueOf(s);
		}catch(Exception e){
			return defaultV;
		}
	}
	
	private String empty2Null(String s){
		if(s==null||s.trim().isEmpty()){
			return null;
		}
		return s;
	}
	
	Article parseNews(NekoHtmlParser p, String url){
		Article article=new Article();
		article.setUrl(url);
		String md5=DigestUtils.md5Hex(url);
		article.setMd5(md5);
		String host=null;
		try{
			host=UrlTools.getDomainName(url);
		}catch(Exception e){}
		article.setHost(host);
		String title=p.getNodeText("//H1");
		if(title.isEmpty()) {
			logger.warn("title is null");
			return null;
			}
		article.setTitle(title);
		String str=p.getNodeText("//EM").trim();
		int beg_index=str.indexOf("  ");
		int end_index=str.lastIndexOf("  ");
		String pubStr=null;
		if(beg_index!=-1)
			pubStr=str.substring(0, beg_index).trim();
		Date pubTime=this.parsePubTime(pubStr);
		if(pubTime==null) {
			logger.warn("pubTime is null \n pubStr:"+pubStr);
			return null;
			}
		article.setPubTime(pubTime);
		String source=null;
		if(beg_index!=-1&&end_index!=-1)
			source=str.substring(beg_index+1,end_index);
		article.setSource(empty2Null(source));
		int comments=0;
		
		article.setComments(comments);
		Node contentNode=p.selectSingleNode("//DIV[@class='s_content']");
		if(contentNode==null) {
			logger.warn("contentNode is null");
			return null;
			}
		NodeList nl=p.selectNodes(".//P[not(@align)]", contentNode);
		StringBuilder sb=new StringBuilder("");
		for(int i=0;i<nl.getLength();i++){
			if(i>0){
				sb.append("\n");
			}
			sb.append(nl.item(i).getTextContent().trim());
		}
		
		String content=sb.toString();
		article.setContent(content);
		String author=p.getNodeText("//DIV[@class='s_content_author']").trim();
		if(!author.isEmpty()){
			author=author.substring(author.indexOf("：")+1);
			article.setAuthors(new String[]{author});
		}
		NodeList imgs=p.selectNodes("//DIV[@class='s_content']//IMG");
		if(imgs.getLength()>0){
			ArrayList<String> imgUrls=new ArrayList<>(imgs.getLength());
			for(int i=0;i<imgs.getLength();i++){
				String imgUrl=p.getNodeText("./@src", imgs.item(i));
				imgUrl=UrlTools.getAbsoluteUrl(url, imgUrl);
				imgUrls.add(imgUrl);
			}
			article.setMainImage(imgUrls.get(0));
			article.setImgs(imgUrls.toArray(new String[0]));
		}
		return article;
	}
	/*
	private String getArticleType(NekoHtmlParser p){
		Node div=p.selectSingleNode("//DIV[contains(@class,'js_crumb')]");
		if(div==null) return null;
		NodeList aList=p.selectNodes("./A", div);
		if(aList==null||aList.getLength()<2){
			return null;
		}
		String type=aList.item(1).getTextContent().trim();
		
		return type;
	}*/
	public static void main(String[] args) {
		if(args.length!=4){
			System.out.println("need 4 args: inDir outDir interval deleteProcessed");
			System.exit(-1);
		}		
		
		long interval=IntervalParser.parseInterval(args[2]);
		boolean deleteProcessed=Boolean.valueOf(args[3]);
		new File(args[1]).mkdirs(); 
		PreprocessArticle pa=new PreprocessArticle(args[0], args[1], interval, deleteProcessed);
		pa.doWork();
	}

}
