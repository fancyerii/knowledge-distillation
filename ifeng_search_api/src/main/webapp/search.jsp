<%@ page language="java" contentType="application/x-json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.antbrains.ifengsearchapi.jsp.*" %>

<%
	String q=request.getParameter("q");
	if(q==null) q="";
	int pageNo=1;
	try{
		pageNo=Integer.valueOf(request.getParameter("pageNo"));
	}catch(Exception e){}
	
	if(!q.isEmpty()){
		String s=Search.search(q, pageNo);
		out.write(s);
	}
%>