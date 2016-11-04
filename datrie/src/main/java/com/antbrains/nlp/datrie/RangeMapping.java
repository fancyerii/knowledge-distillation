package com.antbrains.nlp.datrie;

import java.util.ArrayList;

public class RangeMapping {
  private int[] ranges;
  private int[] mappings;
  private int max = 0;

  public RangeMapping() {
    this(new int[] { 1, 255, 0x4E00, 0x9FA5, 0xF900, 0xFA2D });
  }

  public RangeMapping(int[] ranges) {

    if (ranges.length == 0 || ranges.length % 2 != 0) {
      throw new IllegalArgumentException("ranges.length must be even and larger than 0");
    }
    int rangeCount = ranges.length / 2;

    this.ranges = ranges;
    int prev = ranges[0];
    for (int i = 1; i < ranges.length; i++) {
      if (ranges[i] < 0)
        throw new IllegalArgumentException("ranges[" + i + "]<0");
      if (ranges[i] < prev)
        throw new IllegalArgumentException("ranges shoud be ordered");
      prev = ranges[i];
    }
    max = prev;
    int len = 0;
    for (int i = 0; i < ranges.length; i += 2) {
      len += (ranges[i + 1] - ranges[i] + 1);
    }
    ArrayList<int[]> mappingList = new ArrayList<int[]>(rangeCount * 2);
    int lastChar = 1;
    int offset1 = 1;
    int offset2 = len + 1;
    for (int i = 0; i < ranges.length; i += 2) {
      int startChar = ranges[i];
      int endChar = ranges[i + 1];
      if (startChar > lastChar) {
        int charNum2 = startChar - lastChar;
        mappingList.add(new int[] { startChar - 1, offset2 - lastChar });
        offset2 += charNum2;
      }
      int charNum1 = endChar - startChar + 1;
      mappingList.add(new int[] { endChar, offset1 - startChar });
      offset1 += charNum1;
      lastChar = endChar + 1;
    }
    mappings = new int[mappingList.size() * 2];
    for (int i = 0; i < mappingList.size(); i++) {
      int[] arr = mappingList.get(i);
      mappings[2 * i] = arr[0];
      mappings[2 * i + 1] = arr[1];
    }

  }

  public int mappingCodepoint(int codePoint) {

    if (codePoint > this.max)
      return codePoint;
    int p = 0;
    int end;
    while (true) {
      end = this.mappings[p++];
      if (codePoint <= end) {
        return codePoint + this.mappings[p];
      } else {
        p++;
      }
    }
  }

}
