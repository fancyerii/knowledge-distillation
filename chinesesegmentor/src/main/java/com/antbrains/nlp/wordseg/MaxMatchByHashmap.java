package com.antbrains.nlp.wordseg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MaxMatchByHashmap {
	private int maxLenth;
	private int totalWords;

	public MaxMatchByHashmap(int maxLen) {
		this.maxLenth = maxLen;
		dicts = new ArrayList<>(maxLen - 1);
		for (int i = maxLen; i >= 2; i--) {
			Map<String, String> map = new HashMap<>();
			dicts.add(map);
		}
	}

	public void add(String s, String v) {
		add(s, v, true);
	}

	public void add(String s, String v, boolean internValue) {
		int len = s.length();
		if (len > maxLenth || len < 2)
			return;
		Map<String, String> map = dicts.get(maxLenth - len);
		int prevSize = map.size();
		if (internValue) {
			map.put(s, v.intern());
		} else {
			map.put(s, v);
		}
		totalWords += (map.size() - prevSize);
	}

	public StringInt find(String s, int pos) {
		if (totalWords == 0)
			return null;
		int maxLen = Math.min(maxLenth, s.length() - pos);
		for (int len = maxLen; len >= 2; len--) {
			Map<String, String> map = dicts.get(maxLenth - len);
			String v = map.get(s.substring(pos, pos + len));
			if (v != null) {
				return new StringInt(v, len);
			}
		}
		return null;
	}

	private ArrayList<Map<String, String>> dicts;

	// private List <HashSet<String>> dicts;
	public static void main(String[] args) {
		MaxMatchByHashmap mm = new MaxMatchByHashmap(10);
		mm.add("刘德华", "person");
		mm.add("刘德", "person");
		mm.add("张学友", "person");
		mm.add("北京市", "district");
		String s = "我要去北京市看刘德的演唱会";
		for (int i = 0; i < s.length(); i++) {
			StringInt si = mm.find(s, i);
			if (si == null)
				continue;
			String ss = s.substring(i, i + si.intValue);
			String v = si.strValue;
			System.out.println(ss + "\t" + v);
			i += (si.intValue - 1);
		}
	}

}