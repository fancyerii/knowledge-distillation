package com.antbrains.sc.archiver;

public interface DumpFilter {
	public boolean accept(String url, int depth);
}
