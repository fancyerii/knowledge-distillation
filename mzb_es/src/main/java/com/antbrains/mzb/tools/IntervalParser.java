package com.antbrains.mzb.tools;

public class IntervalParser {

	public static long parseInterval(String s){
		try{
			return Long.valueOf(s);
		}catch(Exception e){}
		try{
			String v=s.substring(0, s.length()-1);
			long res=Long.valueOf(v);
			String lastChar=s.substring(s.length()-1);
			if(lastChar.equalsIgnoreCase("s")){
				return res*1000;
			}else if(lastChar.equalsIgnoreCase("m")){
				return res*1000*60;
			}else if(lastChar.equalsIgnoreCase("h")){
				return res*1000*3600;
			}else if(lastChar.equalsIgnoreCase("d")){
				return res*1000*24*3600;
			}
		}catch(Exception e){
			
		}
		throw new IllegalArgumentException(s);
	}
}
