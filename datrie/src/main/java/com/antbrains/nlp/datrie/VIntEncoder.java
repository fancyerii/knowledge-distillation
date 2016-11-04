package com.antbrains.nlp.datrie;

/**
 * DO NOT USE IT NOW, THERE ARE BUGS.
 * 
 * @author lili
 *
 */
public class VIntEncoder implements IntEncoder {
  public VIntEncoder(RangeMapping mapping) {
    this.mapping = mapping;
  }

  RangeMapping mapping;
  private int[] vIntLength = new int[] { 1 << 7, (1 << 7) + (1 << 14),
      (1 << 7) + (1 << 14) + (1 << 21), (1 << 7) + (1 << 14) + (1 << 21) + (1 << 28) };;

  @Override
  public int[] toIdList(String paramString) {
    int[] array = new int[paramString.length() * 5];
    int charCount = 1;
    int pos = 0;
    for (int i = 0; i < paramString.length(); i += charCount) {
      int codePoint = paramString.codePointAt(i);
      charCount = Character.charCount(codePoint);
      codePoint = mapping.mappingCodepoint(codePoint);

      if (codePoint < this.vIntLength[0]) {
        array[pos++] = codePoint;
      } else if (codePoint < this.vIntLength[1]) {
        codePoint -= vIntLength[0];
        int v2 = (codePoint & 0x7F);
        int v1 = ((codePoint >> 7) | 0x80);
        array[pos++] = v1;
        array[pos++] = v2;
      } else if (codePoint < this.vIntLength[2]) {
        codePoint -= vIntLength[1];
        int v3 = (codePoint & 0x7F);
        int v2 = ((codePoint >> 7) & 0x7F) | 0x80;
        int v1 = ((codePoint >> 14) | 0x80);
        array[pos++] = v1;
        array[pos++] = v2;
        array[pos++] = v3;
      } else if (codePoint < this.vIntLength[3]) {
        codePoint -= vIntLength[2];
        int v4 = (codePoint & 0x7F);
        int v3 = ((codePoint >> 7) & 0x7F) | 0x80;
        int v2 = ((codePoint >> 14) & 0x7F) | 0x80;
        int v1 = ((codePoint >> 21) | 0x80);
        array[pos++] = v1;
        array[pos++] = v2;
        array[pos++] = v3;
        array[pos++] = v4;
      } else {
        codePoint -= vIntLength[3];
        int v5 = (codePoint & 0x7F);
        int v4 = ((codePoint >> 7) & 0x7F) | 0x80;
        int v3 = ((codePoint >> 14) & 0x7F) | 0x80;
        int v2 = ((codePoint >> 21) & 0x7F) | 0x80;
        int v1 = ((codePoint >> 28) & 0x0F) | 0x80;
        array[pos++] = v1;
        array[pos++] = v2;
        array[pos++] = v3;
        array[pos++] = v4;
        array[pos++] = v5;
      }

    }
    int[] r = new int[pos];
    System.arraycopy(array, 0, r, 0, pos);
    return r;
  }

  @Override
  public int[] toIdList(int codePoint) {
    codePoint = mapping.mappingCodepoint(codePoint);
    if (codePoint < this.vIntLength[0]) {
      return new int[] { codePoint };
    } else if (codePoint < this.vIntLength[1]) {
      codePoint -= vIntLength[0];
      int v2 = (codePoint & 0x7F);
      int v1 = ((codePoint >> 7) | 0x80);
      return new int[] { v1, v2 };
    } else if (codePoint < this.vIntLength[2]) {
      codePoint -= vIntLength[1];
      int v3 = (codePoint & 0x7F);
      int v2 = ((codePoint >> 7) & 0x7F) | 0x80;
      int v1 = ((codePoint >> 14) | 0x80);
      return new int[] { v1, v2, v3 };
    } else if (codePoint < this.vIntLength[3]) {
      codePoint -= vIntLength[2];
      int v4 = (codePoint & 0x7F);
      int v3 = ((codePoint >> 7) & 0x7F) | 0x80;
      int v2 = ((codePoint >> 14) & 0x7F) | 0x80;
      int v1 = ((codePoint >> 21) | 0x80);
      return new int[] { v1, v2, v3, v4 };
    } else {
      codePoint -= vIntLength[3];
      int v5 = (codePoint & 0x7F);
      int v4 = ((codePoint >> 7) & 0x7F) | 0x80;
      int v3 = ((codePoint >> 14) & 0x7F) | 0x80;
      int v2 = ((codePoint >> 21) & 0x7F) | 0x80;
      int v1 = ((codePoint >> 28) & 0x0F) | 0x80;
      return new int[] { v1, v2, v3, v4, v5 };
    }

  }

  @Override
  public int zeroId() {
    return 0;
  }

  @Override
  public int getCharSize() {
    return 256;
  }
}
