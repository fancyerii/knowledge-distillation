package com.antbrains.nlp.datrie;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

public class DoubleArrayTrie implements Serializable {
  private static final long serialVersionUID = 5586394930559218801L;
  private static final int leafBit = 1073741824;
  private static final int ROOT_INDEX = 1;
  private static final int ROOT_BASE = 1;
  private static final int[] EMPTY_WALK_STATE = { -1, -1 };
  CharacterMapping charMap;
  private char unuseChar = '\000';
  private int unuseCharValue = 0;
  IntArrayList check;
  IntArrayList base;
  private int number;

  public void setMultiplyExpanding(boolean multiplyExpanding) {
    check.setMultiplyExpanding(multiplyExpanding);
    base.setMultiplyExpanding(multiplyExpanding);
  }

  public void setMultiply(double multiply) {
    check.setMultiply(multiply);
    base.setMultiply(multiply);
  }

  public void setExpandingFactor(int ef) {
    check.setExpandFactor(ef);
    base.setExpandFactor(ef);
  }

  public DoubleArrayTrie() {
    this(new Utf8CharacterMapping());
  }

  public DoubleArrayTrie(CharacterMapping charMap) {
    this.charMap = charMap;
    this.base = new IntArrayList(charMap.getInitSize());
    this.check = new IntArrayList(charMap.getInitSize());

    this.base.add(0);

    this.check.add(0);

    this.base.add(1);
    this.check.add(0);
    expandArray(charMap.getInitSize());
    this.unuseCharValue = charMap.zeroId();
  }

  public int getBaseArraySize() {
    return base.size();
  }

  public int getCheckArraySize() {
    return check.size();
  }

  public int getFreeSize() {
    int count = 0;
    int chk = this.check.get(0);
    while (chk != 0) {
      count++;
      chk = this.check.get(-chk);
    }

    return count;
  }

  private boolean isLeaf(int value) {
    return (value > 0) && ((value & 0x40000000) != 0);
  }

  private int setLeafValue(int value) {
    return value | 0x40000000;
  }

  private int getLeafValue(int value) {
    return value ^ 0x40000000;
  }

  private int getBaseSize() {
    return this.base.size();
  }

  private int getBase(int position) {
    return this.base.get(position);
  }

  private int getCheck(int position) {
    return this.check.get(position);
  }

  private void setBase(int position, int value) {
    this.base.set(position, value);
  }

  private void setCheck(int position, int value) {
    this.check.set(position, value);
  }

  protected boolean isEmpty(int position) {
    return getCheck(position) <= 0;
  }

  private int getNextFreeBase(int nextChar) {
    int pos = -getCheck(0);
    while (pos != 0) {
      if (pos > nextChar + 1) {
        return pos - nextChar;
      }
      pos = -getCheck(pos);
    }
    int oldSize = getBaseSize();
    expandArray(oldSize + this.base.getExpandFactor());
    return oldSize;
  }

  private void addFreeLink(int position) {
    this.check.set(position, this.check.get(-this.base.get(0)));
    this.check.set(-this.base.get(0), -position);
    this.base.set(position, this.base.get(0));
    this.base.set(0, -position);
  }

  private void delFreeLink(int position) {
    this.base.set(-this.check.get(position), this.base.get(position));
    this.check.set(-this.base.get(position), this.check.get(position));
  }

  private void expandArray(int maxSize) {
    int curSize = getBaseSize();
    if (curSize > maxSize) {
      return;
    }
    if (maxSize >= leafBit) {
      throw new RuntimeException("Double Array Trie too large", null);
    }
    for (int i = curSize; i <= maxSize; i++) {
      this.base.add(0);
      this.check.add(0);
      addFreeLink(i);
    }
  }

  private boolean insert(String str, int value, boolean cover) {
    if ((null == str) || str.length() == 0 || (str.contains(String.valueOf(this.unuseChar)))) {
      return false;
    }
    if ((value < 0) || ((value & 0x40000000) != 0)) {
      return false;
    }
    value = setLeafValue(value);

    int[] ids = this.charMap.toIdList(str + this.unuseChar);

    int fromState = 1;
    int toState = 1;
    int ind = 0;
    while (ind < ids.length) {
      int c = ids[ind];

      toState = getBase(fromState) + c;

      expandArray(toState);
      if (isEmpty(toState)) {
        delFreeLink(toState);

        setCheck(toState, fromState);
        if (ind == ids.length - 1) {
          this.number++;
          setBase(toState, value);
        } else {
          int nextChar = ids[(ind + 1)];
          setBase(toState, getNextFreeBase(nextChar));
        }
      } else if (getCheck(toState) != fromState) {
        solveConflict(fromState, c);

        continue;
      }
      fromState = toState;
      ind++;
    }
    if (cover) {
      setBase(toState, value);
    }
    return true;
  }

  private int moveChildren(SortedSet<Integer> children) {
    int minChild = ((Integer) children.first()).intValue();
    int maxChild = ((Integer) children.last()).intValue();
    int cur = 0;
    while (getCheck(cur) != 0) {
      if (cur > minChild + 1) {
        int tempBase = cur - minChild;
        boolean ok = true;
        for (Iterator<Integer> itr = children.iterator(); itr.hasNext();) {
          int toPos = tempBase + ((Integer) itr.next()).intValue();
          if (toPos >= getBaseSize()) {
            ok = false;
            break;
          }
          if (!isEmpty(toPos)) {
            ok = false;
            break;
          }
        }
        if (ok) {
          return tempBase;
        }
      }
      cur = -getCheck(cur);
    }
    int oldSize = getBaseSize();
    expandArray(oldSize + maxChild);
    return oldSize;
  }

  private void solveConflict(int parent, int newChild) {
    TreeSet<Integer> children = new TreeSet<Integer>();

    children.add(new Integer(newChild));
    for (int c = 0; c < this.charMap.getCharsetSize(); c++) {
      int tempNext = getBase(parent) + c;
      if (tempNext >= getBaseSize()) {
        break;
      }
      if ((tempNext < getBaseSize()) && (getCheck(tempNext) == parent)) {
        children.add(new Integer(c));
      }
    }
    int newBase = moveChildren(children);

    children.remove(new Integer(newChild));
    for (Integer child : children) {
      int c = child.intValue();

      delFreeLink(newBase + c);

      setCheck(newBase + c, parent);

      setBase(newBase + c, getBase(getBase(parent) + c));

      int childBase = getBase(getBase(parent) + c);
      if (!isLeaf(childBase)) {
        for (int d = 0; d < this.charMap.getCharsetSize(); d++) {
          int nextPos = childBase + d;
          if (nextPos >= getBaseSize()) {
            break;
          }
          if ((nextPos < getBaseSize()) && (getCheck(nextPos) == getBase(parent) + c)) {
            setCheck(nextPos, newBase + c);
          }
        }
      }
      addFreeLink(getBase(parent) + c);
    }
    setBase(parent, newBase);
  }

  public int size() {
    return this.number;
  }

  public boolean coverInsert(String str, int value) {
    return insert(str, value, true);
  }

  public boolean uncoverInsert(String str, int value) {
    return insert(str, value, false);
  }

  private final static List<String> EMPYT_LIST;
  static {
    EMPYT_LIST = Collections.unmodifiableList(new ArrayList<String>(0));
  }

  public List<String> prefixMatch(String prefix) {
    int curState = 1;
    IntArrayList bytes = new IntArrayList(prefix.length() * 4);
    for (int i = 0; i < prefix.length(); i++) {
      int codePoint = prefix.charAt(i);
      if (curState < 1) {
        return EMPYT_LIST;
      }
      if ((curState != 1) && (isEmpty(curState))) {
        return EMPYT_LIST;
      }
      int[] ids = this.charMap.toIdList(codePoint);
      if (ids.length == 0) {
        return EMPYT_LIST;
      }
      for (int j = 0; j < ids.length; j++) {
        int c = ids[j];
        if ((getBase(curState) + c < getBaseSize())
            && (getCheck(getBase(curState) + c) == curState)) {
          bytes.add(c);
          curState = getBase(curState) + c;
        } else {
          return EMPYT_LIST;
        }
      }

    }
    List<String> result = new ArrayList<String>();
    recurAddSubTree(curState, result, bytes);

    return result;
  }

  private void recurAddSubTree(int curState, List<String> result, IntArrayList bytes) {
    if (getCheck(getBase(curState) + this.unuseCharValue) == curState) {
      byte[] array = new byte[bytes.size()];
      for (int i = 0; i < bytes.size(); i++) {
        array[i] = (byte) bytes.get(i);
      }
      try {
        result.add(new String(array, "UTF-8"));
      } catch (UnsupportedEncodingException e) {

      }
      
    }
    int base = getBase(curState);
    for (int c = 0; c < charMap.getCharsetSize(); c++) {
      if (c == unuseCharValue)
        continue;
      int check = getCheck(base + c);
      if (base + c < getBaseSize() && check == curState) {
        bytes.add(c);
        recurAddSubTree(base + c, result, bytes);
        bytes.removeLast();
      }
    }
  }
  
  private void recurAddSubTree(int curState, List<String> result, List<Integer> payloads, IntArrayList bytes) {
    if (getCheck(getBase(curState) + this.unuseCharValue) == curState) {
      byte[] array = new byte[bytes.size()];
      for (int i = 0; i < bytes.size(); i++) {
        array[i] = (byte) bytes.get(i);
      }
      try {
        result.add(new String(array, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        
      }
      int payload = getLeafValue(getBase(getBase(curState) + this.unuseCharValue));
      payloads.add(payload);
    }
    int base = getBase(curState);
    for (int c = 0; c < charMap.getCharsetSize(); c++) {
      if (c == unuseCharValue)
        continue;
      int check = getCheck(base + c);
      if (base + c < getBaseSize() && check == curState) {
        bytes.add(c);
        recurAddSubTree(base + c, result, payloads, bytes);
        bytes.removeLast();
      }
    }
  }

  public void prefixMatch(String prefix, List<String> result, List<Integer> payloads) {
    int curState = 1;
    IntArrayList bytes = new IntArrayList(prefix.length() * 4);
    for (int i = 0; i < prefix.length(); i++) {
      int codePoint = prefix.charAt(i);
      if (curState < 1) {
        return;
      }
      if ((curState != 1) && (isEmpty(curState))) {
        return;
      }
      int[] ids = this.charMap.toIdList(codePoint);
      if (ids.length == 0) {
        return;
      }
      for (int j = 0; j < ids.length; j++) {
        int c = ids[j];
        if ((getBase(curState) + c < getBaseSize())
            && (getCheck(getBase(curState) + c) == curState)) {
          bytes.add(c);
          curState = getBase(curState) + c;
        } else {
          return;
        }
      }

    }
    
    recurAddSubTree(curState, result, payloads, bytes);
  }

  public int[] find(String query, int start) {
    if ((query == null) || (start >= query.length())) {
      return new int[] { 0, -1 };
    }
    int curState = 1;
    int maxLength = 0;
    int lastVal = -1;
    for (int i = start; i < query.length(); i++) {
      int[] res = walkTrie(curState, query.charAt(i));
      if (res[0] == -1) {
        break;
      }
      curState = res[0];
      if (res[1] != -1) {
        maxLength = i - start + 1;
        lastVal = res[1];
      }
    }
    return new int[] { maxLength, lastVal };
  }

  public int[] findWithSupplementary(String query, int start) {
    if ((query == null) || (start >= query.length())) {
      return new int[] { 0, -1 };
    }
    int curState = 1;
    int maxLength = 0;
    int lastVal = -1;
    int charCount = 1;
    for (int i = start; i < query.length(); i += charCount) {
      int codePoint = query.codePointAt(i);
      charCount = Character.charCount(codePoint);
      int[] res = walkTrie(curState, codePoint);
      if (res[0] == -1) {
        break;
      }
      curState = res[0];
      if (res[1] != -1) {
        maxLength = i - start + 1;
        lastVal = res[1];
      }
    }
    return new int[] { maxLength, lastVal };

  }

  public List<int[]> findAllWithSupplementary(String query, int start) {
    List<int[]> ret = new ArrayList<int[]>(5);
    if ((query == null) || (start >= query.length())) {
      return ret;
    }
    int curState = 1;
    int charCount = 1;
    for (int i = start; i < query.length(); i += charCount) {
      int codePoint = query.codePointAt(i);
      charCount = Character.charCount(codePoint);
      int[] res = walkTrie(curState, codePoint);
      if (res[0] == -1) {
        break;
      }
      curState = res[0];
      if (res[1] != -1) {
        ret.add(new int[] { i - start + 1, res[1] });
      }
    }
    return ret;
  }

  public List<int[]> findAll(String query, int start) {
    List<int[]> ret = new ArrayList<int[]>(5);
    if ((query == null) || (start >= query.length())) {
      return ret;
    }
    int curState = 1;
    for (int i = start; i < query.length(); i++) {
      int[] res = walkTrie(curState, query.charAt(i));
      if (res[0] == -1) {
        break;
      }
      curState = res[0];
      if (res[1] != -1) {
        ret.add(new int[] { i - start + 1, res[1] });
      }
    }
    return ret;
  }

  public int getRoot() {
    return ROOT_INDEX;
  }

  public int[] walkTrie(int curState, int codepoint) {
    if (curState < 1) {
      return EMPTY_WALK_STATE;
    }
    if ((curState != 1) && (isEmpty(curState))) {
      return EMPTY_WALK_STATE;
    }
    int[] ids = this.charMap.toIdList(codepoint);
    if (ids.length == 0) {
      return EMPTY_WALK_STATE;
    }
    for (int i = 0; i < ids.length; i++) {
      int c = ids[i];
      if ((getBase(curState) + c < getBaseSize()) && (getCheck(getBase(curState) + c) == curState)) {
        curState = getBase(curState) + c;
      } else {
        return EMPTY_WALK_STATE;
      }
    }
    if (getCheck(getBase(curState) + this.unuseCharValue) == curState) {
      int value = getLeafValue(getBase(getBase(curState) + this.unuseCharValue));
      return new int[] { curState, value };
    }
    return new int[] { curState, -1 };
  }

  public int delete(String str) {
    if (str == null) {
      return -1;
    }
    int curState = 1;
    int[] ids = this.charMap.toIdList(str);

    int[] path = new int[ids.length + 1];
    int i = 0;
    for (; i < ids.length; i++) {
      int c = ids[i];
      if ((getBase(curState) + c >= getBaseSize()) || (getCheck(getBase(curState) + c) != curState)) {
        break;
      }
      curState = getBase(curState) + c;
      path[i] = curState;
    }
    int ret = -1;
    if (i == ids.length) {
      if (getCheck(getBase(curState) + this.unuseCharValue) == curState) {
        this.number--;
        ret = getLeafValue(getBase(getBase(curState) + this.unuseCharValue));
        path[(path.length - 1)] = (getBase(curState) + this.unuseCharValue);
        for (int j = path.length - 1; j >= 0; j--) {
          boolean isLeaf = true;
          int state = path[j];
          for (int k = 0; k < this.charMap.getCharsetSize(); k++) {
            if (isLeaf(getBase(state))) {
              break;
            }
            if ((getBase(state) + k < getBaseSize()) && (getCheck(getBase(state) + k) == state)) {
              isLeaf = false;
              break;
            }
          }
          if (!isLeaf) {
            break;
          }
          addFreeLink(state);
        }
      }
    }
    return ret;
  }

  public int getEmptySize() {
    int cnt = 0;
    for (int i = 0; i < getBaseSize(); i++) {
      if (isEmpty(i)) {
        cnt++;
      }
    }
    return cnt;
  }

  public int getMaximumValue() {
    return leafBit - 1;
  }

  /**
   * can't modified after get iterator or else the behavior is undefined.
   * 
   * @return DatrieIterator
   */
  public DatrieIterator iterator() {
    return new Itr();
  }

  private class Itr implements DatrieIterator {
    private IntArrayList path;
    private int curCount;
    private int value = -1;
    private String key = null;
    private int bs;

    public Itr() {
      path = new IntArrayList(20);
      path.add(1);
      int st = 1;
      int b = base.get(st);
      if (number > 0) {
        while (true) {
          for (int i = 0; i < charMap.getCharsetSize(); i++) {
            int c = check.get(b + i);
            if (c == st) {
              path.add(i);
              // path.add(c);
              st = b + i;
              path.add(st);
              b = base.get(st);
              if (getCheck(b + unuseCharValue) == st) {
                value = getLeafValue(getBase(b + unuseCharValue));
                int[] ids = new int[path.size() / 2];
                for (int k = 0, j = 1; j < path.size(); k++, j += 2) {
                  ids[k] = path.get(j);
                }
                key = charMap.toString(ids);
                path.add(unuseCharValue);
                bs = b;
                return;
              }
            }
          }

        }
      }
    }

    @Override
    public String key() {
      return key;
    }

    @Override
    public int value() {
      return value;
    }

    @Override
    public int setValue(int v) {
      int value = getLeafValue(v);
      setBase(bs + unuseCharValue, value);
      this.value = v;
      return v;
    }

    @Override
    public boolean hasNext() {
      return curCount < number;
    }

    @Override
    public void next() {
      if (curCount >= number) {
        throw new NoSuchElementException();
      } else if (curCount == 0) {
      } else {
        while (path.size() > 0) {
          int ch = path.pop();
          int s = path.getLast();
          int n = getNext(s, ch);
          if (n != -1)
            break;
          path.removeLast();
        }
      }

      curCount++;
    }

    private int getNext(int s, int ch) {
      int startChar = ch + 1;
      int b = getBase(s);
      int st = s;

      for (int i = startChar; i < charMap.getCharsetSize(); i++) {
        int c = check.get(b + i);
        if (c == st) {
          path.add(i);
          st = b + i;
          path.add(st);
          b = base.get(st);
          startChar = 0;
          if (getCheck(b + unuseCharValue) == st) {
            value = getLeafValue(getBase(b + unuseCharValue));
            int[] ids = new int[path.size() / 2];
            for (int k = 0, j = 1; j < path.size(); k++, j += 2) {
              ids[k] = path.get(j);
            }
            key = charMap.toString(ids);
            path.add(unuseCharValue);
            bs = b;
            return st;
          } else {
            return getNext(st, 0);
          }
        }
      }
      return -1;

    }
  }
  
  public boolean writeToOutputStream(OutputStream os) throws IOException{
    int[] baseArr=this.base.getData();
    os.write(Bytes.toBytes(this.base.size()));
    os.write(Bytes.toBytes(baseArr.length));
    for(int i=0;i<baseArr.length;i++){
      os.write(Bytes.toBytes(baseArr[i]));
    } 
    int[] checkArr=this.check.getData();
    os.write(Bytes.toBytes(this.check.size()));
    os.write(Bytes.toBytes(checkArr.length));
    for(int i=0;i<checkArr.length;i++){
      os.write(Bytes.toBytes(checkArr[i]));
    }
    return true;
  }
  
  public void readFromInputStream(InputStream is) throws IOException{
    byte[] bytes=new byte[4];
    is.read(bytes);
    int count=Bytes.toInt(bytes);
    is.read(bytes);
    int baseSize=Bytes.toInt(bytes);
    int[] baseArr=new int[baseSize];
    for(int i=0;i<baseSize;i++){
      is.read(bytes);
      baseArr[i]=Bytes.toInt(bytes);
    }
    this.base=new IntArrayList(baseArr, count);
    is.read(bytes);
    count=Bytes.toInt(bytes);
    is.read(bytes);
    int checkSize=Bytes.toInt(bytes);
    int[] checkArr=new int[checkSize];
    for(int i=0;i<checkSize;i++){
      is.read(bytes);
      checkArr[i]=Bytes.toInt(bytes);
    }
    this.check=new IntArrayList(checkArr, count);
  }
}
