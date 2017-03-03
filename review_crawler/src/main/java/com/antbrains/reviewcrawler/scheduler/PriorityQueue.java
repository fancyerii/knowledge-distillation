package com.antbrains.reviewcrawler.scheduler;

/*
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
 
public abstract class PriorityQueue<T> {
  private int size;
  private final int maxSize;
  private final T[] heap;

  public PriorityQueue(int maxSize) {
    this(maxSize, true);
  }

  @SuppressWarnings("unchecked")
  public PriorityQueue(int maxSize, boolean prepopulate) {
    size = 0;
    int heapSize;
    if (0 == maxSize) {
      // We allocate 1 extra to avoid if statement in top()
      heapSize = 2;
    } else {
      if (maxSize > Integer.MAX_VALUE - 256) {
        // Don't wrap heapSize to -1, in this case, which
        // causes a confusing NegativeArraySizeException.
        // Note that very likely this will simply then hit
        // an OOME, but at least that's more indicative to
        // caller that this values is too big. We don't +1
        // in this case, but it's very unlikely in practice
        // one will actually insert this many objects into
        // the PQ:
        // Throw exception to prevent confusing OOME:
        throw new IllegalArgumentException(
            "maxSize must be <= " + (Integer.MAX_VALUE - 256) + "; got: " + maxSize);
      } else {
        // NOTE: we add +1 because all access to heap is
        // 1-based not 0-based. heap[0] is unused.
        heapSize = maxSize + 1;
      }
    }
    heap = (T[]) new Object[heapSize]; // T is unbounded type, so this
    // unchecked cast works always
    this.maxSize = maxSize;

    if (prepopulate) {
      // If sentinel objects are supported, populate the queue with them
      T sentinel = getSentinelObject();
      if (sentinel != null) {
        heap[1] = sentinel;
        for (int i = 2; i < heap.length; i++) {
          heap[i] = getSentinelObject();
        }
        size = maxSize;
      }
    }
  }
 
  protected abstract boolean lessThan(T a, T b);
 
  protected T getSentinelObject() {
    return null;
  }
 
  public final T add(T element) {
    size++;
    heap[size] = element;
    upHeap();
    return heap[1];
  }
 
  public T insertWithOverflow(T element) {
    if (size < maxSize) {
      add(element);
      return null;
    } else if (size > 0 && !lessThan(element, heap[1])) {
      T ret = heap[1];
      heap[1] = element;
      updateTop();
      return ret;
    } else {
      return element;
    }
  }
 
  public final T top() {
    // We don't need to check size here: if maxSize is 0,
    // then heap is length 2 array with both entries null.
    // If size is 0 then heap[1] is already null.
    return heap[1];
  }
 
  public final T pop() {
    if (size > 0) {
      T result = heap[1]; // save first value
      heap[1] = heap[size]; // move last to first
      heap[size] = null; // permit GC of objects
      size--;
      downHeap(); // adjust heap
      return result;
    } else {
      return null;
    }
  }
 
  public final T updateTop() {
    downHeap();
    return heap[1];
  }
 
  public final int size() {
    return size;
  }
 
  public final void clear() {
    for (int i = 0; i <= size; i++) {
      heap[i] = null;
    }
    size = 0;
  }

  private final void upHeap() {
    int i = size;
    T node = heap[i]; // save bottom node
    int j = i >>> 1;
    while (j > 0 && lessThan(node, heap[j])) {
      heap[i] = heap[j]; // shift parents down
      i = j;
      j = j >>> 1;
    }
    heap[i] = node; // install saved node
  }

  private final void downHeap() {
    int i = 1;
    T node = heap[i]; // save top node
    int j = i << 1; // find smaller child
    int k = j + 1;
    if (k <= size && lessThan(heap[k], heap[j])) {
      j = k;
    }
    while (j <= size && lessThan(heap[j], node)) {
      heap[i] = heap[j]; // shift up child
      i = j;
      j = i << 1;
      k = j + 1;
      if (k <= size && lessThan(heap[k], heap[j])) {
        j = k;
      }
    }
    heap[i] = node; // install saved node
  }
 
  protected final Object[] getHeapArray() {
    return (Object[]) heap;
  }
}
