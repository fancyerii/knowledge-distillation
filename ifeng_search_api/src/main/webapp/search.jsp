<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@ page import="com.antbrains.ifengsearchapi.jsp.*" %>
    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<style>

table tr td{
        border: 1px solid;
}
</style>

<title>搜索</title>
</head>
<body>
<%
	String q=request.getParameter("q");
	if(q==null) q="";
	int pageNo=1;
	try{
		pageNo=Integer.valueOf(request.getParameter("pageNo"));
	}catch(Exception e){}
	
	
%>
<form>
<div>
        请输入搜索词：&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input id="q" type="text" name="q" value="<%=q%>" maxlength="200" class="inputBox"/>
 	
        <input type="submit" id="submit" value="搜索"/>
</div>
</form>
<%
	if(!q.isEmpty()){
		String s=Search.search(q, pageNo);
		out.write(s);
	}
%>
</body>
</html>