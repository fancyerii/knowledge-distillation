package com.antbrains.urlcrawler.db;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader; 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.log4j.Logger;
 

public class TestMd5Distribution {
	protected static Logger logger=Logger.getLogger(TestMd5Distribution.class);
	public static void main(String[] args)  throws Exception{
		if(args.length!=2){
			System.err.println("need 2 arg: urlFile count");
			System.exit(-1);
		}
		int max=Integer.valueOf(args[1]);
		BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(args[0]),"UTF8"));
		String line;
		int lineNumber=0;  
		Map<Byte, Integer> counter=new HashMap<>();
		while((line=br.readLine())!=null){
			
			if(line.startsWith("http://wapbaike.baidu.com/")){
				lineNumber++;
				if(lineNumber>max) break;
				if(lineNumber%10000==0){
					logger.info("lineNumber: "+lineNumber);
				}
				String url=line.replace("wapbaike", "baike");
				byte[] md5=DigestUtils.md5(url); 
				Integer count=counter.get(md5[0]);
				if(count==null){
					counter.put(md5[0], 1);
				}else{
					counter.put(md5[0], count+1);
				}
			}
		} 
		br.close(); 
		for(Entry<Byte,Integer> entry:counter.entrySet()){
			logger.info(entry.getKey().toString()+"\t"+entry.getValue().toString());
		}
	}

}
