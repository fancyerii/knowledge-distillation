package com.antbrains.fjnet_crawler.dumper;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import com.antbrains.httpclientfetcher.FileTools;
import com.antbrains.mysqltool.PoolManager;
import com.antbrains.sc.archiver.DumpFilter;
import com.antbrains.sc.archiver.MysqlArchiver;
import com.antbrains.sc.init.MysqlInit;

public class DumpHtml {
	protected static Logger logger=Logger.getLogger(DumpHtml.class);
	MysqlArchiver archiver=new MysqlArchiver();
	private String outDir;
	private long interval;
	boolean dumpAll;
	static File dumpDir;
	public DumpHtml(String outDir, long interval, boolean dumpAll){
		this.outDir=outDir;
		this.interval=interval;
		this.dumpAll=dumpAll;
		logger.info("outDir: "+outDir);
		logger.info("interval: "+interval);
		logger.info("dumpAll: "+dumpAll);
	}
	
	private Timestamp readLastDumpTime(){
		try{
			String s = FileTools.readFile(outDir+"/.last_dump_time");
			SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return new Timestamp(sdf.parse(s).getTime());
		}catch(Exception e){
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, 2000);
			cal.set(Calendar.MONTH, Calendar.JANUARY);
			cal.set(Calendar.DAY_OF_MONTH, 1);
		    return  new Timestamp(cal.getTimeInMillis());
		}
	}
	
	public void writeLastDumpTime(Date d	){
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			FileTools.writeFile(outDir+"/.last_dump_time", sdf.format(d));
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
	
	
	public void dump(){
		while(true){
			final Timestamp lastDump=this.readLastDumpTime();
			final SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
			logger.info("lastDump: "+sdf.format(lastDump));
			Date d=new Date();
			final String fileName=sdf.format(d);
			//File createFile=new File(dumpDir,fileName);
		
			try{
				archiver.dumpHtml(new DumpFilter(){
					@Override
					public boolean accept(int id, String url, int depth, Timestamp lastVisitTime, String content) {
						if(lastVisitTime==null) return false;
						if(!dumpAll && lastDump.after(lastVisitTime)){
							logger.debug("skip: "+url+": "+sdf.format(lastVisitTime));
							return false;
						}
						return depth==1;
					}
					
				}, this.outDir+"/"+fileName, 1000);
				File f=new File(this.outDir+"/"+fileName);
				if(f.length()>0){
					logger.info("rename to: "+this.outDir+"/"+fileName+".txt");
					f.renameTo(new File(this.outDir+"/"+fileName+".txt"));
				}else{
					logger.info("remove empty: "+f.getAbsolutePath());
					f.delete();
				}
				this.writeLastDumpTime(new Date(d.getTime()-1000));
			}catch(Exception e){
				logger.error(e.getMessage(),e);
			}
			dumpAll=false;
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) { 
			}
		}

	}
	public static void main(String[] args) throws Exception {
		if(args.length!=4){
			System.out.println("need 4 arg: dbName outputPath dumpInterval dumpAll[boolean]");
			System.exit(-1);
		}
		PoolManager.StartPool("conf", args[0]);
		String outPath=args[1];
		long dumpInterval=MysqlInit.parseInterval(args[2]);
		boolean dumpAll=Boolean.parseBoolean(args[3]);
		dumpDir = new File(outPath);
		boolean b=dumpDir.mkdirs();
		System.out.print(b);
		DumpHtml dumpHtml=new DumpHtml(outPath, dumpInterval, dumpAll);
		dumpHtml.dump();

	}
	
}
