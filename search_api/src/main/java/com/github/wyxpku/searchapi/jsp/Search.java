package com.github.wyxpku.sdearchapi.jsp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import com.github.wyxpku.es_search.search.SearchItem;
import com.github.wyxpku.es_search.search.SearchResult;
import com.github.wyxpku.searchapi.searcher.Searcher;
 
public class Search {
	private static String encode(String s){
		try {
			return java.net.URLEncoder.encode(s, "UTF8");
		} catch (UnsupportedEncodingException e) {
			return s;
		}
	}
	public static String search(String query, int pageNo){
		StringBuilder sb=new StringBuilder("");
		
		SearchResult sr = Searcher.getInstance().search(query, pageNo, "<font color='red'>", "</font>");
 
			
		for(SearchItem item:sr.getItems()){
			sb.append("<table>"); 
			String encodedUrl=encode(item.getUrl()); 
			sb.append("<tr><td><a target='_blank' href='"+encodedUrl+"'>").append(item.getTitleHighlight()).append("</a></td></tr>");
			sb.append("<tr><td>"+item.getContentHightlight()+"</td></tr>");
			sb.append("</table>");
		}
		
		if(pageNo>1){
			String url="./search.jsp?pageNo="+(pageNo-1)+"&q="+encode(query);
			sb.append("<a href='"+url+"'>上一页</a> &nbsp;&nbsp;&nbsp;");
		}
		if(pageNo<100){
			String url="./search.jsp?pageNo="+(pageNo+1)+"&q="+encode(query);
			sb.append("<a href='"+url+"'>下一页</a> &nbsp;&nbsp;&nbsp;");
		}
		
		sb.append("共 "+sr.getTotalResult()+" 条结果");
		
		return sb.toString();
	}
}
