package com.antbrains.nlp.wordseg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.antbrains.crf.BESB1B2MTagConvertor;
import com.antbrains.crf.CrfModel;
import com.antbrains.crf.SgdCrf;
import com.antbrains.crf.TagConvertor;
import com.antbrains.nlp.datrie.DoubleArrayTrie;
import com.antbrains.nlp.wordseg.ChineseSegmenter;
import com.antbrains.nlp.wordseg.Token;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WordSeg {
	protected static Logger logger = Logger.getLogger(WordSeg.class);
	private static WordSeg instance;

	private CrfModel model;
	TagConvertor tc = new BESB1B2MTagConvertor();
	private ChineseSegmenter cs;

	public ChineseSegmenter getCs() {
		return cs;
	}

	private WordSeg() {
		cs = ChineseSegmenter.getInstance();
		try {
			model = cs.getModel();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	static {
		instance = new WordSeg();
	}

	public static WordSeg getInstance() {
		return instance;
	}

	private List<String> tokenToList(List<Token> tokens) {
		List<String> result = new ArrayList<>(tokens.size());
		for (Token token : tokens) {
			result.add(token.getOrigText());
		}
		return result;
	}

	public List<String> mmSeg(String s){
		return mmSeg(s, null);
	}
	public List<String> mmSeg(String s, DoubleArrayTrie tmpTrie) {
		if (s == null || s.equals("")) {
			return new ArrayList<>(0);
		}
		List<Token> tokens = cs.getMmseg().seg(s, tmpTrie);
		return this.tokenToList(tokens);
	}
	public List<String> rmmSeg(String s){
		return rmmSeg(s, null);
	}
	public List<String> rmmSeg(String s, DoubleArrayTrie tmpTrie) {
		if (s == null || s.equals("")) {
			return new ArrayList<>(0);
		}
		List<Token> tokens = cs.getRmmseg().seg(s, tmpTrie);
		return this.tokenToList(tokens);
	}
	
	public List<String> mmRmmSeg(String s){
		return mmRmmSeg(s, null);
	}

	public List<String> mmRmmSeg(String s, DoubleArrayTrie tmpTrie) {
		if (s == null || s.equals("")) {
			return new ArrayList<>(0);
		}
		List<Token> tokens = this.cs.seg(s, tmpTrie);
		return this.tokenToList(tokens);
	}

	public List<String> crfSeg(String s) {
		if (s == null || s.equals("")) {
			return new ArrayList<>(0);
		}
		List<String> tokens = SgdCrf.segment(s, model, tc);
		return tokens;
	}


	public List<String> segForIndex(String s) {
		List<String> tokens = this.mmRmmSeg(s);
		tokens = this.removeNonChars(tokens);
		return fineSeg(tokens);
	}

	private List<String> removeNonChars(List<String> tokens) {
		List<String> result = new ArrayList<>();
		for (String token : tokens) {
			result.addAll(this.splitByNonChars(token));
		}

		return result;
	}

	public List<String> splitByNonChars(String s) {
		List<String> result = new ArrayList<>(2);
		int lastPos = 0;
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (StringTools.isChinese(ch) || StringTools.isDigit(ch) || StringTools.isEnLetter(ch)) {

			} else {// 特殊字符
				if (i > lastPos) {
					result.add(s.substring(lastPos, i));
				}
				result.add(s.substring(i, i + 1));
				lastPos = i + 1;
			}
		}

		if (lastPos < s.length()) {
			result.add(s.substring(lastPos));
		}

		return result;
	}

	private List<String> fineSeg(List<String> tokens) {
		List<String> result = new ArrayList<>();
		// 合并连续的单字
		for (int i = 0; i < tokens.size();) {
			String token = tokens.get(i);
			if (!this.needMerge(token)) {
				if (token.length() > 3) {
					result.add(token);
				} else {
					result.add(token);
				}
				i++;
			} else {
				int j = i + 1;
				StringBuilder sb = new StringBuilder(token);
				for (; j < tokens.size(); j++) {
					String tk = tokens.get(j);
					if (!this.needMerge(tk)) {
						break;
					} else {
						sb.append(tk);
					}
				}

				result.add(sb.toString());
				i = j;
			}
		}
		return result;
	}

	private boolean needMerge(String s) {
		if (s.length() > 1)
			return false;
		char ch = s.charAt(0);
		return StringTools.isChinese(ch) || StringTools.isDigit(ch) || StringTools.isEnLetter(ch);
	}

}
