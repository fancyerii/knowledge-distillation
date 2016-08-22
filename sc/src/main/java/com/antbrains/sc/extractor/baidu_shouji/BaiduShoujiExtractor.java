package com.antbrains.sc.extractor.baidu_shouji;

import com.antbrains.sc.extractor.Extractor;
import com.antbrains.sc.extractor.UrlPatternExtractor;

public class BaiduShoujiExtractor extends UrlPatternExtractor {
	private Extractor level0Ext = new Level0Extractor();

	@Override
	public Extractor getExtractor(String url, String redirectedUrl, int depth) {
		if (url.endsWith("http://shouji.baidu.com")) {
			return level0Ext;
		} else if (depth == 1) {

		}

		return null;
	}

}
