package com.antbrains.nlp.datrie;

public interface DatrieIterator {
  public String key();

  public int value();

  public int setValue(int v);

  public boolean hasNext();

  public void next();
}
