package com.antbrains.nlp.wordseg;

import gnu.trove.set.hash.THashSet;

import java.util.ArrayList;

public class MaxMatchByHashset {
	private int maxLenth;
	private int totalWords;

	public int getTotalWords() {
		return totalWords;
	}

	public MaxMatchByHashset(int maxLen) {
		this.maxLenth = maxLen;
		dicts = new ArrayList<>(maxLen - 1);
		for (int i = maxLen; i >= 2; i--) {
			THashSet<String> set = new THashSet<>();
			dicts.add(set);
		}
	}

	public void add(String s) {
		int len = s.length();
		if (len > maxLenth || len < 2)
			return;
		THashSet<String> set = dicts.get(maxLenth - len);
		int prevSize = set.size();
		set.add(s);
		totalWords += (set.size() - prevSize);
	}

	public int find(String s, int pos) {
		if (totalWords == 0)
			return -1;
		int maxLen = Math.min(maxLenth, s.length() - pos);
		for (int len = maxLen; len >= 2; len--) {
			THashSet<String> set = dicts.get(maxLenth - len);
			if (set.contains(s.substring(pos, pos + len))) {
				return len;
			}
		}
		return -1;
	}

	private ArrayList<THashSet<String>> dicts;

	// private List <HashSet<String>> dicts;
	public static void main(String[] args) {
		MaxMatchByHashset mm = new MaxMatchByHashset(10);
		mm.add("刘德华");
		mm.add("李娜");
		mm.add("刘德");
		mm.add("aaaaaaaaaaaaafdsgsdfgsdfgaa");
		mm.add("张学友");

		String entity = "";
		String question = "阿斯顿发送到发送到发的说法刘德";
		// ///// 实体识别
		for (int pos = 0; pos < question.length(); pos++) {
			int len = mm.find(question, pos);
			if (len != -1) {
				entity = question.substring(pos, pos + len);
				break;
			}
		}
		// int len=mm.find("请问刘德华有多高", 2);
		System.out.println(entity);
		// len=mm.find("请问刘德华有多高", 2);
		// System.out.println(len);
	}

}