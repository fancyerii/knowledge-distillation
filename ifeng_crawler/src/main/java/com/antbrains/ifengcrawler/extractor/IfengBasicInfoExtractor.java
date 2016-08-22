package com.antbrains.ifengcrawler.extractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.antbrains.sc.extractor.BasicInfoExtractor;

public abstract class IfengBasicInfoExtractor extends BasicInfoExtractor {

	@Override
	public String normUrl(String url) {
		return url;
	}
}
