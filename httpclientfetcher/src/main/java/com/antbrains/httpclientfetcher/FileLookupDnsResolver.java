package com.antbrains.httpclientfetcher;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.conn.DnsResolver;

public class FileLookupDnsResolver implements DnsResolver{ 
	private ConcurrentHashMap<String, InetAddress[]> map=new ConcurrentHashMap<>();
	
	private File file;
	public FileLookupDnsResolver(File file){
		this(file, 5*60_000L);
	}
	public FileLookupDnsResolver(File file, long updateInterval){
		this.file=file;
		this.updateInterval=updateInterval;
		this.refresh();
	}
	
	private synchronized void  refresh(){
		if(System.currentTimeMillis()-this.lastUpdate<this.updateInterval) return;
		try {
			List<String> lines=FileTools.readFile2List(file.getAbsolutePath(), "UTF8");
			for(String line:lines){
				line=line.trim();
				if(line.startsWith("#")) continue;
				String[] arr=line.split("\\s+");
				if(arr.length<2){
					continue;
				}
				String host=arr[0];
				List<InetAddress> ipAddrs=new ArrayList<>(arr.length-1);
				for(int i=1;i<arr.length;i++){
					String ip=arr[i];
					try{
						InetAddress ipAddr=InetAddress.getByName(ip);
						ipAddrs.add(ipAddr);
					}catch(Exception e){
						
					}
				}
				this.map.put(host, ipAddrs.toArray(new InetAddress[0]));
			}
		} catch (IOException e) { 
			e.printStackTrace();
		}
		lastUpdate=System.currentTimeMillis();
	}
	
	private long lastUpdate=0;
	private long updateInterval=5*60_000L;
	
	private AtomicInteger counter=new AtomicInteger();
	@Override
	public InetAddress[] resolve(String host) throws UnknownHostException {
		if(System.currentTimeMillis()-lastUpdate>this.updateInterval){
			new Thread(new Runnable(){
				@Override
				public void run() {
					FileLookupDnsResolver.this.refresh();
				}
				
			}).start();
		}
		InetAddress[] addr=this.map.get(host);
		if(addr!=null && addr.length>0){
			int idx=counter.getAndIncrement()%addr.length;
			return new InetAddress[]{addr[idx]};
		}
		return InetAddress.getAllByName(host);
	}

}
