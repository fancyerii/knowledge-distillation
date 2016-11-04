package com.antbrains.ifeng_es.tools.gson;

import java.text.DateFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonTool {
	public static Gson getGson(){
        GsonBuilder gb = new GsonBuilder();
        gb.setDateFormat("yyyy-MM-dd HH:mm:ss");
        Gson gson = gb.create();
        return gson;	
	}
}
