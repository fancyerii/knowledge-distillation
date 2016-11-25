package com.antbrains.fjnet_crawler.extractor;

import com.antbrains.sc.extractor.Extractor;
import com.antbrains.sc.extractor.NullExtractor;
import com.antbrains.sc.extractor.UrlPatternExtractor;
import com.antbrains.sc.extractor.UrlPatternExtractor4Hbase;

public class FjnetExtractor extends UrlPatternExtractor {
	private Extractor[] extractors=new Extractor[]{
		new Level0Extractor(),
		new DetailPageExtractor(),
	};
	
    @Override
    public Extractor getExtractor(String url, String redirectedUrl, int depth) {
		if(depth==0)
			return extractors[0];
		else if(depth==1)
			return extractors[1];
		
		return null;
	}
}
