package com.antbrains.ifengsearchapi.searcher;

import org.apache.log4j.Logger;

import com.antbrains.ifeng_es.search.ArticleSearcher;
import com.antbrains.ifeng_es.search.SearchResult;
import com.antbrains.ifengsearchapi.jsp.ConfigReader;

public class Searcher {
	protected static Logger logger=Logger.getLogger(Searcher.class);
	
	private float titleBoost=5.0f;
	private Searcher(){
		String clusterName=ConfigReader.getProps().getProperty("es.clusterName");
		String hostName=ConfigReader.getProps().getProperty("es.hostName");
		String titleBoostStr=ConfigReader.getProps().getProperty("es.titleBoost");
		if(titleBoostStr!=null&&!titleBoostStr.isEmpty()){
			titleBoost=Float.valueOf(titleBoostStr);
		}
		try {
			searcher=new ArticleSearcher(clusterName, hostName);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
	
	private ArticleSearcher searcher;
	
	public SearchResult search(String query, int page, String startTag, String endTag){
		return searcher.search(query, titleBoost, page, startTag, endTag);
	}
	
	private static Searcher instance=new Searcher();
	public static Searcher getInstance(){
		return instance;
	}
}

