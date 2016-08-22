package com.antbrains.sc.tools;
 
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeTools {
	public static String formatDate(Date date){
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}
	public static Date parseDate(String s){
		if(s==null) return null;
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try{
			return sdf.parse(s);
		}catch(Exception e){
		}
		
		sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm");
		try{
			return sdf.parse(s);
		}catch(Exception e){
		}
		
		sdf=new SimpleDateFormat("yyyy-MM-dd");
		try{
			return sdf.parse(s);
		}catch(Exception e){
		}
		
		
		return null;
	}
	public static void main(String[] args) {
		Date d=DateTimeTools.parseDate("2016-07-31 10:50");
		System.out.println(d.toString());
	}

}
