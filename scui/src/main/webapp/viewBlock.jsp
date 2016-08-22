<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="com.mobvoi.knowledgegraph.scui.jsp.*" %>    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<style>
 
table tr td{
  	border: 1px solid;
}  
</style>
<title>查看块</title>
</head>
<body>
<%
	String idS=request.getParameter("bId");
	int bid=-1;
	if(idS!=null){
		try{
			bid=Integer.valueOf(idS);
		}catch(Exception e){}
	}else{
		idS="";
	}
	String dbName=request.getParameter("dbName");
	if(dbName==null) dbName="";
 
	int pageNo=1;
	String s=request.getParameter("pageNo");
	if(s!=null){
		try{
			pageNo=Integer.valueOf(s);
		}catch(Exception e){}
	}
	
 	String url=request.getParameter("url");
	
%>
<form>
 	
</form>
<%
	String ss=ViewWebPageMysql.getInstance().viewBlock(url, dbName, bid, pageNo);
	out.write(ss);
%>
</body>
</html>