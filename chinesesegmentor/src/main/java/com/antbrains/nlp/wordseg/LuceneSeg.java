package com.antbrains.nlp.wordseg;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.antbrains.nlp.wordseg.Token.Type;
import com.antbrains.nlp.wordseg.luceneanalyzer.OffsetAttribute;
import com.antbrains.nlp.wordseg.luceneanalyzer.StandardTokenizer;
import com.antbrains.nlp.wordseg.luceneanalyzer.TypeAttribute;
import com.antbrains.nlp.wordseg.luceneanalyzer.Version;

public class LuceneSeg {

	public static List<List<Token>> processByLuceneAnalyzer(String sen) {
		List<List<Token>> result = new ArrayList<List<Token>>();
		StandardTokenizer tokenizer = new StandardTokenizer(Version.LUCENE_29, new StringReader(sen));

		// CharTermAttribute termAtt = (CharTermAttribute)
		// tokenizer.addAttribute(CharTermAttribute.class);
		OffsetAttribute offsetAtt = (OffsetAttribute) tokenizer.addAttribute(OffsetAttribute.class);
		TypeAttribute typeAtt = (TypeAttribute) tokenizer.addAttribute(TypeAttribute.class);

		List<Token> subSen = new ArrayList<Token>();
		try {
			int lastPos = 0;
			boolean lastIsCn = false;
			while (tokenizer.incrementToken()) {
				int start = offsetAtt.startOffset();
				int end = offsetAtt.endOffset();

				if (lastPos < start) {// 被StandardAnalyzer扔掉的都认为是标点，不用参与分词
					for (int i = lastPos; i < start; i++) {
						if (subSen.size() > 0) {
							result.add(subSen);
						}
						subSen = new ArrayList<Token>();
						subSen.add(new Token(null, sen, i, i + 1, Type.PUNCT));
					}
					lastIsCn = false;
				}
				lastPos = end;

				String wordType = typeAtt.type();

				Token token = new Token(sen, start, end);
				if (wordType.equals("<IDEOGRAPHIC>")) {// 汉字
					token.setType(Type.CWORD);
					if (!lastIsCn) {
						if (subSen.size() > 0) {
							result.add(subSen);
							subSen = new ArrayList<Token>();
						}
						lastIsCn = true;
					}
				} else {
					lastIsCn = false;
					if (subSen.size() > 0) {
						result.add(subSen);
						subSen = new ArrayList<Token>();
					}

					if (wordType.equals("<ALPHANUM>")) {
						token.setType(Type.ALPHA);
					} else if (wordType.equals("<NUM>")) {
						token.setType(Type.NUMBER);
					}
				}
				subSen.add(token);

			}
			if (subSen.size() > 0) {
				result.add(subSen);
				subSen = new ArrayList<Token>();
			}

			for (int i = lastPos; i < sen.length(); i++) {
				subSen = new ArrayList<Token>();
				subSen.add(new Token(null, sen, i, i + 1, Type.PUNCT));
				result.add(subSen);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			tokenizer.close();
		} catch (IOException e) {

		}
		return result;
	}
}
