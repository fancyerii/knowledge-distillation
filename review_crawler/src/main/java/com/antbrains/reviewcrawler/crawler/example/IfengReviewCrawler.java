package com.antbrains.reviewcrawler.crawler.example;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.antbrains.reviewcrawler.crawler.ReviewCrawler;
import com.antbrains.reviewcrawler.data.CrawlTask;
import com.antbrains.reviewcrawler.data.ReviewContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class IfengReviewCrawler extends ReviewCrawler{


	private String getReviewUrl(String url, int page) throws UnsupportedEncodingException{
		String encodedUrl=java.net.URLEncoder.encode(url, "UTF8");
		
		return "http://comment.ifeng.com/get.php?callback=newCommentListCallBack&orderby=&docUrl="+
			encodedUrl+"&format=js&job=1&p="+page+"&pageSize=20";
	 
	}
	
	private JsonObject parseJson(String html){
		int startIdx=html.indexOf("={");
		if(startIdx==-1) return null;
		int endIdx=html.lastIndexOf("newCommentListCallBack");
		if(endIdx==-1) return null;
		endIdx=html.lastIndexOf("};", endIdx);
		if(endIdx==-1) return null;
		String json=html.substring(startIdx+1, endIdx+1);
		return parser.parse(json).getAsJsonObject();
	}
	
	private boolean isDateValid(String date){
		return date.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}");
	}
	
	private void processAPage(JsonObject jo, String lastDate, int taskId, List<ReviewContent> reviews){
		JsonArray comments=jo.get("comments").getAsJsonArray();
		for(int i=0;i<comments.size();i++){
			JsonObject comment=comments.get(i).getAsJsonObject();
			String comment_id=comment.get("comment_id").getAsString();
			String uname=comment.get("uname").getAsString();
			String user_id=comment.get("user_id").getAsString();
			String comment_contents=comment.get("comment_contents").getAsString();
			String comment_date=comment.get("comment_date").getAsString();
			if(!this.isDateValid(comment_date)){
				logger.warn("invalid date: "+comment_date);
				continue;
			}
			if(comment_date.compareTo(lastDate)>=0){
				ReviewContent review=new ReviewContent();
				review.setId(comment_id);
				review.setPageId(taskId);
				review.setDate(comment_date);
				HashMap<String,String> map=new HashMap<>();
				map.put("user_id", user_id);
				map.put("uname", uname);
				map.put("comment_contents", comment_contents);
				review.setContent(gson.toJson(map));
				reviews.add(review);
			}
		}
	}
	
	@Override
	protected List<ReviewContent> crawlAReview(CrawlTask task) throws Exception {
		String url=this.getReviewUrl(task.getUrl(), 1); 
		String html=fetcher.httpGet(url, 3);
		if(html==null){
			logger.warn("can't get: "+url);
			return null;
		}
		JsonObject jo=this.parseJson(html);
		int total=jo.get("count").getAsInt();
		
		String lastDate=task.getLastestReviewTime();
		if(lastDate==null){
			lastDate="";
		}
		List<ReviewContent> reviews=new ArrayList<>();
		int id= Integer.valueOf(task.getId());
		this.processAPage(jo, lastDate,id, reviews);
		
		int totalPage=1+(total-1)/20;
		for(int p=2;p<=totalPage;p++){
			String u=this.getReviewUrl(task.getUrl(), p);
			html=fetcher.httpGet(u, 3);
			if(html==null){
				logger.warn("can't get: "+url);
				return null;
			}
			jo=this.parseJson(html);
			this.processAPage(jo, lastDate, id, reviews);
		}
		return reviews;
	}
}
