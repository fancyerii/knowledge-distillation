package com.antbrains.nlp.wordseg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.antbrains.nlp.datrie.DoubleArrayTrie;

/**
 * 正向最大匹配分词
 * 
 * @author lili
 *
 */
public class MMSeg {
	private DoubleArrayTrie trie;
	private MaxMatchByHashset mm;
	private static final int MAX_LEN = 10;
	private static final int MAX_TRI_WORDS = 1000000;
	private boolean useHash = false;

	private ReadWriteLock lock = new ReentrantReadWriteLock();

	public MMSeg(List<String> wordList) {
		this(wordList, wordList.size() > MAX_TRI_WORDS);
	}

	public MMSeg(List<String> wordList, boolean useHash) {
		mm = new MaxMatchByHashset(MAX_LEN);
		trie = new DoubleArrayTrie();
		this.useHash = useHash;
		this.addWordList(wordList);
	}

	public int getSize() {
		return mm.getTotalWords() + trie.size();
	}

	private void addWordList(List<String> wordList) {
		this.lock.writeLock().lock();
		try {
			if (!useHash) {
				for (String word : wordList) {
					word = word.trim();
					if (word.length() < 2)
						continue;
					trie.coverInsert(word, 0);
				}
			} else {
				for (String word : wordList) {
					word = word.trim();
					if (word.length() < 2)
						continue;
					if (word.length() > MAX_LEN) {
						trie.coverInsert(word, 0);
					} else {
						mm.add(word);
					}
				}
			}
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	public MMSeg() {
		trie = new DoubleArrayTrie();
		mm = new MaxMatchByHashset(MAX_LEN);
	}

	// not thread safe
	public void addWords(List<String> words) {
		this.addWordList(words);
	}

	public MMSeg(String dictPath) {
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
		this.lock.readLock().lock();
		try {
			int[] arr = trie.find(sen, i);
			if (arr[0] > 0)
				return arr[0];
			return mm.find(sen, i);
		} finally {
			this.lock.readLock().unlock();
		}
	}
	
	private int findInTrie(DoubleArrayTrie dat, String sen, int i){
		if(dat==null) return 0;
		int[] arr=dat.find(sen, i);
		return arr[0];
	}

	public List<Token> seg(String sen, DoubleArrayTrie tmpTrie) {
		List<Token> tokens = new ArrayList<Token>();
		for (int i = 0; i < sen.length(); i++) {
			String ch = sen.substring(i, i + 1);
			int len = this.find(sen, i);
			int tmpLen=this.findInTrie(tmpTrie, sen, i);
			len=Math.max(len, tmpLen);
			if (processNumber && cnNumbers.contains(ch)) { // 处理汉字里的数字，阿拉伯数字在前面lucene的分析器已经处理过了
				int j = i + 1;
				for (; j < sen.length(); j++) {
					ch = sen.substring(j, j + 1);
					if (!cnNumbers.contains(ch))
						break;
				}
				if (j - i > 1 && j - i > len) {
					tokens.add(new Token(sen, i, j));
					i += (j - i - 1);
					continue;
				}

			}
			if (len > 1) {
				tokens.add(new Token(sen, i, i + len));
				i += (len - 1);
			} else {
				tokens.add(new Token(sen, i, i + 1));
			}

		}

		return tokens;
	}

}
