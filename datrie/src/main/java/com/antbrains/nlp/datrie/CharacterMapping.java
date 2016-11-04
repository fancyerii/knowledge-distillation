package com.antbrains.nlp.datrie;

public interface CharacterMapping {
  public int getInitSize();

  public int getCharsetSize();

  public int zeroId();

  public int[] toIdList(String paramString);

  public int[] toIdList(int codePoint);

  public String toString(int[] ids);
}
