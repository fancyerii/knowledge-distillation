package com.antbrains.sc.archiver;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StatusAndUpdate {
	public String status;
	public Date updateTime;
	
	public StatusAndUpdate(String status, Date updateTime){
		this.status=status;
		this.updateTime=updateTime;
	}
	
	@Override
	public String toString(){
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return this.status+" update in: "+sdf.format(updateTime);
	}
}
