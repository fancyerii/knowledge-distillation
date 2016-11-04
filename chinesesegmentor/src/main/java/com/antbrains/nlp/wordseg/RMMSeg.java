package com.antbrains.nlp.wordseg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.antbrains.nlp.datrie.DoubleArrayTrie;

import java.util.Map.Entry;

/**
 * 逆向最大匹配分词
 * 
 * @author lili
 *
 */
public class RMMSeg {
	private DoubleArrayTrie trie;
	private MaxMatchByHashset mm;
	private static final int MAX_LEN = 10;
	private static final int MAX_TRI_WORDS = 1000000;
	private boolean useHash = false;

	private ReadWriteLock lock = new ReentrantReadWriteLock();

	private String reverseString(String s) {
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = s.length() - 1; i >= 0; i--) {
			sb.append(s.charAt(i));
		}
		return sb.toString();
	}

	public RMMSeg() {
		trie = new DoubleArrayTrie();
		mm = new MaxMatchByHashset(MAX_LEN);
	}

	public int getSize() {
		return mm.getTotalWords() + trie.size();
	}

	private void addWordList(List<String> wordList) {
		lock.writeLock().lock();
		try{
			if (!useHash) {
				for (String word : wordList) {
					word = word.trim();
					if (word.length() < 2)
						continue;
					trie.coverInsert(reverseString(word), 0);
				}
			} else {
				for (String word : wordList) {
					word = word.trim();
					if (word.length() < 2)
						continue;
					if (word.length() > MAX_LEN) {
						trie.coverInsert(reverseString(word), 0);
					} else {
						mm.add(reverseString(word));
					}
				}
			}
		}finally{
			lock.writeLock().unlock();
		}

	}

	public RMMSeg(List<String> wordList) {
		this(wordList, wordList.size() > MAX_TRI_WORDS);
	}

	public RMMSeg(List<String> wordList, boolean useHash) {
		mm = new MaxMatchByHashset(MAX_LEN);
		trie = new DoubleArrayTrie();
		this.useHash = useHash;
		this.addWordList(wordList);
	}

	// thread safe
	public void addWords(List<String> words) {
		this.addWordList(words);
	}

	public RMMSeg(String dictPath) {
		try {
			List<String> wordList = FileTools.readFile2List(dictPath, "UTF-8");
			this.addWordList(wordList);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String cnNumbers = "零一二三四五六七八九十百千万亿";
	public boolean processNumber = false;

	private int find(String sen, int i) {
		lock.readLock().lock();
		try{
			int[] arr = trie.find(sen, i);
			if (arr[0] > 0)
				return arr[0];
			return mm.find(sen, i);
		}finally{
			lock.readLock().unlock();
		}
	}
	
	private int findInTrie(DoubleArrayTrie dat, String sen, int i){
		if(dat==null) return 0;
		int[] arr=dat.find(sen, i);
		return arr[0];
	}

	public List<Token> seg(String sentence, DoubleArrayTrie tmpTrie) {
		String s = this.reverseString(sentence);
		List<Token> tokens = new ArrayList<Token>();
		Stack<Token> stack = new Stack<Token>();
		int length = sentence.length();

		for (int i = 0; i < s.length(); i++) {
			String ch = s.substring(i, i + 1);
			int len = this.find(s, i);
			int tmpLen=this.findInTrie(tmpTrie, s, i);
			len=Math.max(len, tmpLen);
			if (processNumber && cnNumbers.contains(ch)) { // 处理汉字里的数字，阿拉伯数字在前面lucene的分析器已经处理过了
				int j = i + 1;
				for (; j < s.length(); j++) {
					ch = s.substring(j, j + 1);
					if (!cnNumbers.contains(ch))
						break;
				}
				if (j - i > 1 && j - i > len) {
					stack.add(new Token(sentence, length - j, length - i));
					i += (j - i - 1);
					continue;
				}
			}

			if (len > 1) {
				stack.push(new Token(sentence, length - i - len, length - i));
				i += (len - 1);
			} else {
				stack.push(new Token(sentence, length - i - 1, length - i));
			}
		}

		while (!stack.empty()) {
			tokens.add(stack.pop());
		}
		return tokens;
	}

}
