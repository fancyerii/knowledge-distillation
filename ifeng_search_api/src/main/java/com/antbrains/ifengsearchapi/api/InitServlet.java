package com.antbrains.ifengsearchapi.api;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.antbrains.ifengsearchapi.searcher.Searcher;
 

public class InitServlet extends HttpServlet{ 
	private static final long serialVersionUID = 1L;

	@Override
	  public void init() throws ServletException{
		Searcher.getInstance().search("北京", 0, "", "");
	  }
 
	  
	  @Override
	  public void destroy(){ 
	  }
}
