package com.antbrains.nlp.wordseg;

public class SentenceSplit {
	
	public static String[] splitSentences(String s){
		String[] arr = s.split("[。！？?!\n\r]");
		String[] result=new String[arr.length];
		int idx=0;
		for(int i=0;i<arr.length;i++){
			idx+=arr[i].length();
			if(idx<s.length()){
				result[i]=arr[i]+s.charAt(idx);
			}else{
				result[i]=arr[i];
			}
			idx++;
		}
		
		return result;
	}
}
