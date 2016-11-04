package com.antbrains.nlp.datrie;

@Deprecated
public class HighFreqRangeCharacterMapping implements CharacterMapping {

  private IntEncoder encoder;
  private RangeMapping mapping;

  public HighFreqRangeCharacterMapping() {
    mapping = new RangeMapping();
    this.encoder = new VIntEncoder(mapping);
  }

  public HighFreqRangeCharacterMapping(RangeMapping mapping, IntEncoder encoder) {
    this.encoder = encoder;
    this.mapping = mapping;
  }

  @Override
  public int getInitSize() {
    return encoder.getCharSize();
  }

  @Override
  public int getCharsetSize() {
    return encoder.getCharSize();
  }

  @Override
  public int zeroId() {
    return encoder.zeroId();
  }

  @Override
  public int[] toIdList(String paramString) {
    return this.encoder.toIdList(paramString);
    // char[] array=new char[paramString.length()*5];
    // int charCount = 1;
    // int pos = 0;
    // for (int i = 0; i < paramString.length(); i += charCount) {
    // int codePoint = paramString.codePointAt(i);
    // charCount = Character.charCount(codePoint);
    // codePoint = this.mappingCodepoint(codePoint);
    //
    // if(codePoint<this.vIntLength[0]){
    // array[pos++]=(char)codePoint;
    // }else if(codePoint<this.vIntLength[1]){
    // codePoint-=vIntLength[0];
    // int v2=(codePoint & 0x7F);
    // int v1=((codePoint >> 7)| 0x80);
    // array[pos++]=(char)v1;
    // array[pos++]=(char)v2;
    // }else if(codePoint<this.vIntLength[2]){
    // codePoint-=vIntLength[1];
    // int v3=(codePoint & 0x7F);
    // int v2=((codePoint >> 7) & 0x7F) | 0x80;
    // int v1=((codePoint >> 14)| 0x80);
    // array[pos++]=(char)v1;
    // array[pos++]=(char)v2;
    // array[pos++]=(char)v3;
    // }else if(codePoint<this.vIntLength[3]){
    // codePoint-=vIntLength[2];
    // int v4=(codePoint & 0x7F);
    // int v3=((codePoint >> 7) & 0x7F) | 0x80;
    // int v2=((codePoint >> 14) & 0x7F) | 0x80;
    // int v1=((codePoint >> 21) | 0x80);
    // array[pos++]=(char)v1;
    // array[pos++]=(char)v2;
    // array[pos++]=(char)v3;
    // array[pos++]=(char)v4;
    // }else{
    // codePoint-=vIntLength[3];
    // int v5=(codePoint & 0x7F);
    // int v4=((codePoint >> 7) & 0x7F) | 0x80;
    // int v3=((codePoint >> 14) & 0x7F) | 0x80;
    // int v2=((codePoint >> 21) & 0x7F) | 0x80;
    // int v1=((codePoint >> 28) & 0x0F) | 0x80;
    // array[pos++]=(char)v1;
    // array[pos++]=(char)v2;
    // array[pos++]=(char)v3;
    // array[pos++]=(char)v4;
    // array[pos++]=(char)v5;
    // }
    //
    // }
    // char[] r = new char[pos];
    // System.arraycopy(array, 0, r, 0, pos);
    // return r;
  }

  @Override
  public int[] toIdList(int codePoint) {
    return this.encoder.toIdList(codePoint);
    // codePoint = this.mappingCodepoint(codePoint);
    // if(codePoint<this.vIntLength[0]){
    // return new char[]{(char)codePoint};
    // }else if(codePoint<this.vIntLength[1]){
    // codePoint-=vIntLength[0];
    // int v2=(codePoint & 0x7F);
    // int v1=((codePoint >> 7)| 0x80);
    // return new char[]{(char)v1,(char)v2};
    // }else if(codePoint<this.vIntLength[2]){
    // codePoint-=vIntLength[1];
    // int v3=(codePoint & 0x7F);
    // int v2=((codePoint >> 7) & 0x7F) | 0x80;
    // int v1=((codePoint >> 14)| 0x80);
    // return new char[]{(char)v1,(char)v2,(char)v3};
    // }else if(codePoint<this.vIntLength[3]){
    // codePoint-=vIntLength[2];
    // int v4=(codePoint & 0x7F);
    // int v3=((codePoint >> 7) & 0x7F) | 0x80;
    // int v2=((codePoint >> 14) & 0x7F) | 0x80;
    // int v1=((codePoint >> 21) | 0x80);
    // return new char[]{(char)v1,(char)v2,(char)v3,(char)v4};
    // }else{
    // codePoint-=vIntLength[3];
    // int v5=(codePoint & 0x7F);
    // int v4=((codePoint >> 7) & 0x7F) | 0x80;
    // int v3=((codePoint >> 14) & 0x7F) | 0x80;
    // int v2=((codePoint >> 21) & 0x7F) | 0x80;
    // int v1=((codePoint >> 28) & 0x0F) | 0x80;
    // return new char[]{(char)v1,(char)v2,(char)v3,(char)v4,(char)v5};
    // }

  }

  public static void main(String[] args) {
    int[] ranges = new int[] { 1, 255, 0x4E00, 0x9FA5, 0xF900, 0xFA2D };
    RangeMapping mapping = new RangeMapping();
    IntEncoder ie = new VIntEncoder(mapping);
    HighFreqRangeCharacterMapping hfrcm = new HighFreqRangeCharacterMapping(mapping, ie);
    int[] testCodePoints = new int[] { 1, 127, 254, 255, 256, 0x4DFF, 0x4E00, 0x4E01, 0x9FA5,
        0x9FA4, 0xF8FF, 0xF900, 0xF901, 0xFA2D, 0xFA2C, 0xFA2E };
    for (int codePoint : testCodePoints) {
      System.out.print(codePoint + "(" + Integer.toHexString(codePoint) + ")" + "->");
      int mapped = mapping.mappingCodepoint(codePoint);
      System.out.println(+mapped);
    }
    String s = "汉字\uD801\uDC00\uD801\uDC00ab\uD801\uDC00\uD801\uDC00cd";
    int[] bytes1 = hfrcm.toIdList(s);
    System.out.println(bytes1.length);
  }

  @Override
  public String toString(int[] ids) {
    throw new UnsupportedOperationException();
  }
}
