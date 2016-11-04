package com.antbrains.nlp.datrie;

public interface IntEncoder {
  public int[] toIdList(int codePoint);

  public int[] toIdList(String paramString);

  public int zeroId();

  public int getCharSize();
}
