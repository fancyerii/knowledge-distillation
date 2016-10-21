package com.antbrains.sc.archiver;

import java.sql.Timestamp;

public interface DumpFilter {
	public boolean accept(String url, int depth, Timestamp lastVisitTime);
}
