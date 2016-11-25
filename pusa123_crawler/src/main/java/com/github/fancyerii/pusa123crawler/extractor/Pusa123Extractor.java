package com.github.fancyerii.pusa123crawler.extractor;

import com.antbrains.sc.extractor.Extractor;
import com.antbrains.sc.extractor.UrlPatternExtractor;

public class Pusa123Extractor extends UrlPatternExtractor {

	private Extractor[] extractors = new Extractor[] { new Level0Extractor(), new Level1Extractor(),
			new Level2Extractor(), };

	@Override
	public Extractor getExtractor(String url, String redirectedUrl, int depth) {
		// TODO Auto-generated method stub
		if (depth == 0) {
			return extractors[0];
		} else if (depth == 1) {
			return extractors[1];
		} else if (depth == 2) {
			return extractors[2];
		}
		return null;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String[] urls = new String[] { "http://www.pusa123.com/pusa/news/", };
		String[] redirectedUrls = new String[urls.length];
		int[] depths = new int[] { 0, };
		Pusa123Extractor bme = new Pusa123Extractor();
		for (int i = 0; i < urls.length; i++) {
			System.out.println(urls[i] + "\t" + redirectedUrls[i] + "\t" + depths[i]);
			Extractor ext = bme.getExtractor(urls[i], redirectedUrls[i], depths[i]);
			if (ext == null) {
				System.out.println("\tnull");
			} else {
				System.out.println("\t" + ext.getClass().getSimpleName());
			}
		}
	}

}
