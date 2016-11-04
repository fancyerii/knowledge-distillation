package com.antbrains.nlp.datrie;

public class VIntAndUtf8 implements IntEncoder {
  RangeMapping mapping;

  public VIntAndUtf8(RangeMapping mapping) {
    this.mapping = mapping;
  }

  private static final int MAX = 1 << 15;
  private static final int[] EMPTYLIST = new int[0];

  @Override
  public int[] toIdList(int codePoint) {
    codePoint = mapping.mappingCodepoint(codePoint);
    if (codePoint == 0) {
      return new int[] { 256 };
    } else if (codePoint < MAX) {
      return new int[] { (codePoint >>> 8), (codePoint & 0xFF) };
    } else {// not frequent here!
      int count;
      if (codePoint < 0x80)
        count = 2;
      else if (codePoint < 0x800)
        count = 3;
      else if (codePoint < 0x10000)
        count = 4;
      else if (codePoint < 0x200000)
        count = 5;
      else if (codePoint < 0x4000000)
        count = 6;
      else if (codePoint <= 0x7fffffff)
        count = 7;
      else
        return EMPTYLIST;
      int[] r = new int[count];
      switch (count) { /* note: code falls through cases! */
      case 7:
        r[6] = (0x80 | (codePoint & 0x3f));
        codePoint = codePoint >> 6;
        codePoint |= 0x4000000;
      case 6:
        r[5] = (0x80 | (codePoint & 0x3f));
        codePoint = codePoint >> 6;
        codePoint |= 0x200000;
      case 5:
        r[4] = (0x80 | (codePoint & 0x3f));
        codePoint = codePoint >> 6;
        codePoint |= 0x10000;
      case 4:
        r[3] = (0x80 | (codePoint & 0x3f));
        codePoint = codePoint >> 6;
        codePoint |= 0x800;
      case 3:
        r[2] = (0x80 | (codePoint & 0x3f));
        codePoint = codePoint >> 6;
        codePoint |= 0xc0;
      case 2:
        r[1] = codePoint;
      }
      r[0] = 128;
      return r;
    }

  }

  @Override
  public int[] toIdList(String paramString) {
    int[] array = new int[paramString.length() * 6];
    int charCount = 1;
    int pos = 0;
    for (int i = 0; i < paramString.length(); i += charCount) {
      int codePoint = paramString.codePointAt(i);
      charCount = Character.charCount(codePoint);
      codePoint = mapping.mappingCodepoint(codePoint);
      if (codePoint == 0) {
        array[pos++] = 256;
      } else if (codePoint < MAX) {
        array[pos++] = (codePoint >>> 8);
        if (array[pos] >= 128) {
          System.out.println();
        }
        array[pos++] = (codePoint & 0xFF);

      } else {// not frequent here!
        int count;
        if (codePoint < 0x80)
          count = 2;
        else if (codePoint < 0x800)
          count = 3;
        else if (codePoint < 0x10000)
          count = 4;
        else if (codePoint < 0x200000)
          count = 5;
        else if (codePoint < 0x4000000)
          count = 6;
        else if (codePoint <= 0x7fffffff)
          count = 7;
        else
          return EMPTYLIST;

        switch (count) { /* note: code falls through cases! */
        case 7:
          array[pos + 6] = (0x80 | (codePoint & 0x3f));
          codePoint = codePoint >> 6;
          codePoint |= 0x4000000;
        case 6:
          array[pos + 5] = (0x80 | (codePoint & 0x3f));
          codePoint = codePoint >> 6;
          codePoint |= 0x200000;
        case 5:
          array[pos + 4] = (0x80 | (codePoint & 0x3f));
          codePoint = codePoint >> 6;
          codePoint |= 0x10000;
        case 4:
          array[pos + 3] = (0x80 | (codePoint & 0x3f));
          codePoint = codePoint >> 6;
          codePoint |= 0x800;
        case 3:
          array[pos + 2] = (0x80 | (codePoint & 0x3f));
          codePoint = codePoint >> 6;
          codePoint |= 0xc0;
        case 2:
          array[pos + 1] = codePoint;
        }
        array[pos] = 128;
        pos += count;
      }
    }
    int[] r = new int[pos];
    for (int i : array) {
      if (i > 256) {
        System.out.println(paramString);
      }
    }
    System.arraycopy(array, 0, r, 0, pos);
    return r;
  }

  @Override
  public int zeroId() {
    return 256;
  }

  @Override
  public int getCharSize() {
    return 257;
  }

}
