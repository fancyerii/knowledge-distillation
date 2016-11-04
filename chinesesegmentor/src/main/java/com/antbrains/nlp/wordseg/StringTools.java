package com.antbrains.nlp.wordseg;

public class StringTools {
	public static boolean isChinese(char ch) {
		return (ch >= '\u4E00' && ch <= '\u9FA5') || (ch >= '\uF900' && ch <= '\uFA2D');
	}

	public static boolean isEnLetter(char ch) {
		return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
	}

	public static boolean isDigit(char ch) {
		return ch >= '0' && ch <= '9';
	}

	public static boolean isChinese(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!isChinese(s.charAt(i)))
				return false;
		}
		return true;
	}

	public static boolean hasChinese(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (isChinese(s.charAt(i)))
				return true;
		}
		return false;
	}

	public static boolean isCnEnDigit(char ch) {
		return isChinese(ch) || isEnLetter(ch) || isDigit(ch);
	}

	public static boolean isCnEnDigit(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!isCnEnDigit(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}
}
