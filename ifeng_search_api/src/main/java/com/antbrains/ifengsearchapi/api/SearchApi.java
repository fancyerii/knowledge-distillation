package com.antbrains.ifengsearchapi.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.antbrains.ifeng_es.search.SearchResult;
import com.antbrains.ifengsearchapi.searcher.Searcher;
import com.google.gson.Gson;
 
 
public class SearchApi extends HttpServlet {
	protected static Logger logger = Logger.getLogger(SearchApi.class);
	private static final long serialVersionUID = 1L;
	private Gson gson=new Gson();
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		
		response.getWriter().write(gson.toJson(this.doSearch(request)));
	}
	
	private String getParam(HttpServletRequest req,String name, String def){
		String v=req.getParameter(name);
		if(v==null||v.trim().equals("")) return def;
		return v;
	}
	
	private SearchResult doSearch(HttpServletRequest request){
		String query=request.getParameter("q");
		if(query==null||query.trim().equals("")){
			return SearchResult.EMPTY;
		}
		
		int pageNo=1;
		try{
			pageNo=Integer.valueOf(request.getParameter("page"));
		}catch(Exception e){
			
		}
		
		String startTag=this.getParam(request, "startTag", "<span class='hl'>");
		String endTag=this.getParam(request, "endTag", "</span>");
		
		SearchResult sr=Searcher.getInstance().search(query, pageNo, startTag, endTag);
		return sr;
	}
}
