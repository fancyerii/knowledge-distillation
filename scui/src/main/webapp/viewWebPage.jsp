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
table tr th{
  	border: 1px solid;
} 
.inputBox{
	width: 500px;
}

</style>
<title>查看网页</title>
</head>
<body>
<%
	String url=request.getParameter("url");
	if(url==null) url="";
	String dbName=request.getParameter("dbName");
	if(dbName==null) dbName=""; 
 	boolean viewParent=false;
 	String s=request.getParameter("viewParent");
 	if(s!=null&&s.equalsIgnoreCase("true")){
 		viewParent=true;
 	}
	
%>
<form>
<div>
	url：&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	<input id="url" type="text" name="url" value="<%=url%>" maxlength="200" class="inputBox"/>
	<br/>
	数据库名称：&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	<input id="dbName" type="text" name="dbName" value="<%=dbName%>" maxlength="100" class="inputBox"/>
	<br/>	
	<br/>		
	<input type="submit" id="submit" value="搜索"/>
</div>
</form>
<%
	String ss=ViewWebPageMysql.getInstance().search(url, dbName, viewParent);
	out.write(ss);
%>
</body>
</html>