package com.antbrains.nlp.wordseg.luceneanalyzer;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
public interface CharTermAttribute extends Attribute, CharSequence, Appendable {
 
	public void copyBuffer(char[] buffer, int offset, int length);
 
	public char[] buffer();
 
	public char[] resizeBuffer(int newSize);
 
	public CharTermAttribute setLength(int length);
 
	public CharTermAttribute setEmpty();

	// the following methods are redefined to get rid of IOException
	// declaration:
	public CharTermAttribute append(CharSequence csq);

	public CharTermAttribute append(CharSequence csq, int start, int end);

	public CharTermAttribute append(char c);
 
	public CharTermAttribute append(String s);
 
	public CharTermAttribute append(StringBuilder sb);
 
	public CharTermAttribute append(CharTermAttribute termAtt);
}
