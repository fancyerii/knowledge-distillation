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

import java.util.Collection;
import java.util.Comparator;
 
public final class ArrayUtil {

	private ArrayUtil() {
	} // no instance
 
	public static int parseInt(char[] chars) throws NumberFormatException {
		return parseInt(chars, 0, chars.length, 10);
	}
 
	public static int parseInt(char[] chars, int offset, int len) throws NumberFormatException {
		return parseInt(chars, offset, len, 10);
	}
 
	public static int parseInt(char[] chars, int offset, int len, int radix) throws NumberFormatException {
		if (chars == null || radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
			throw new NumberFormatException();
		}
		int i = 0;
		if (len == 0) {
			throw new NumberFormatException("chars length is 0");
		}
		boolean negative = chars[offset + i] == '-';
		if (negative && ++i == len) {
			throw new NumberFormatException("can't convert to an int");
		}
		if (negative == true) {
			offset++;
			len--;
		}
		return parse(chars, offset, len, radix, negative);
	}

	private static int parse(char[] chars, int offset, int len, int radix, boolean negative)
			throws NumberFormatException {
		int max = Integer.MIN_VALUE / radix;
		int result = 0;
		for (int i = 0; i < len; i++) {
			int digit = Character.digit(chars[i + offset], radix);
			if (digit == -1) {
				throw new NumberFormatException("Unable to parse");
			}
			if (max > result) {
				throw new NumberFormatException("Unable to parse");
			}
			int next = result * radix - digit;
			if (next > result) {
				throw new NumberFormatException("Unable to parse");
			}
			result = next;
		}
		/*
		 * while (offset < len) {
		 * 
		 * }
		 */
		if (!negative) {
			result = -result;
			if (result < 0) {
				throw new NumberFormatException("Unable to parse");
			}
		}
		return result;
	}
 
	public static int oversize(int minTargetSize, int bytesPerElement) {

		if (minTargetSize < 0) {
			// catch usage that accidentally overflows int
			throw new IllegalArgumentException("invalid array size " + minTargetSize);
		}

		if (minTargetSize == 0) {
			// wait until at least one element is requested
			return 0;
		}

		// asymptotic exponential growth by 1/8th, favors
		// spending a bit more CPU to not tie up too much wasted
		// RAM:
		int extra = minTargetSize >> 3;

		if (extra < 3) {
			// for very small arrays, where constant overhead of
			// realloc is presumably relatively high, we grow
			// faster
			extra = 3;
		}

		int newSize = minTargetSize + extra;

		// add 7 to allow for worst case byte alignment addition below:
		if (newSize + 7 < 0) {
			// int overflowed -- return max allowed array size
			return Integer.MAX_VALUE;
		}

		if (Constants.JRE_IS_64BIT) {
			// round up to 8 byte alignment in 64bit env
			switch (bytesPerElement) {
			case 4:
				// round up to multiple of 2
				return (newSize + 1) & 0x7ffffffe;
			case 2:
				// round up to multiple of 4
				return (newSize + 3) & 0x7ffffffc;
			case 1:
				// round up to multiple of 8
				return (newSize + 7) & 0x7ffffff8;
			case 8:
				// no rounding
			default:
				// odd (invalid?) size
				return newSize;
			}
		} else {
			// round up to 4 byte alignment in 64bit env
			switch (bytesPerElement) {
			case 2:
				// round up to multiple of 2
				return (newSize + 1) & 0x7ffffffe;
			case 1:
				// round up to multiple of 4
				return (newSize + 3) & 0x7ffffffc;
			case 4:
			case 8:
				// no rounding
			default:
				// odd (invalid?) size
				return newSize;
			}
		}
	}

	public static int getShrinkSize(int currentSize, int targetSize, int bytesPerElement) {
		final int newSize = oversize(targetSize, bytesPerElement);
		// Only reallocate if we are "substantially" smaller.
		// This saves us from "running hot" (constantly making a
		// bit bigger then a bit smaller, over and over):
		if (newSize < currentSize / 2)
			return newSize;
		else
			return currentSize;
	}

	public final static int NUM_BYTES_SHORT = 2;
	public final static int NUM_BYTES_INT = 4;
	public final static int NUM_BYTES_LONG = 8;
	public final static int NUM_BYTES_FLOAT = 4;
	public final static int NUM_BYTES_DOUBLE = 8;
	public final static int NUM_BYTES_CHAR = 2;
	public final static int NUM_BYTES_OBJECT_HEADER = 8;
	public final static int NUM_BYTES_OBJECT_REF = Constants.JRE_IS_64BIT ? 8 : 4;
	public final static int NUM_BYTES_ARRAY_HEADER = NUM_BYTES_OBJECT_HEADER + NUM_BYTES_INT + NUM_BYTES_OBJECT_REF;

	public static short[] grow(short[] array, int minSize) {
		if (array.length < minSize) {
			short[] newArray = new short[oversize(minSize, NUM_BYTES_SHORT)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static short[] grow(short[] array) {
		return grow(array, 1 + array.length);
	}

	public static float[] grow(float[] array, int minSize) {
		if (array.length < minSize) {
			float[] newArray = new float[oversize(minSize, NUM_BYTES_FLOAT)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static float[] grow(float[] array) {
		return grow(array, 1 + array.length);
	}

	public static double[] grow(double[] array, int minSize) {
		if (array.length < minSize) {
			double[] newArray = new double[oversize(minSize, NUM_BYTES_DOUBLE)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static double[] grow(double[] array) {
		return grow(array, 1 + array.length);
	}

	public static short[] shrink(short[] array, int targetSize) {
		final int newSize = getShrinkSize(array.length, targetSize, NUM_BYTES_SHORT);
		if (newSize != array.length) {
			short[] newArray = new short[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}

	public static int[] grow(int[] array, int minSize) {
		if (array.length < minSize) {
			int[] newArray = new int[oversize(minSize, NUM_BYTES_INT)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static int[] grow(int[] array) {
		return grow(array, 1 + array.length);
	}

	public static int[] shrink(int[] array, int targetSize) {
		final int newSize = getShrinkSize(array.length, targetSize, NUM_BYTES_INT);
		if (newSize != array.length) {
			int[] newArray = new int[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}

	public static long[] grow(long[] array, int minSize) {
		if (array.length < minSize) {
			long[] newArray = new long[oversize(minSize, NUM_BYTES_LONG)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static long[] grow(long[] array) {
		return grow(array, 1 + array.length);
	}

	public static long[] shrink(long[] array, int targetSize) {
		final int newSize = getShrinkSize(array.length, targetSize, NUM_BYTES_LONG);
		if (newSize != array.length) {
			long[] newArray = new long[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}

	public static byte[] grow(byte[] array, int minSize) {
		if (array.length < minSize) {
			byte[] newArray = new byte[oversize(minSize, 1)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static byte[] grow(byte[] array) {
		return grow(array, 1 + array.length);
	}

	public static byte[] shrink(byte[] array, int targetSize) {
		final int newSize = getShrinkSize(array.length, targetSize, 1);
		if (newSize != array.length) {
			byte[] newArray = new byte[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}

	public static boolean[] grow(boolean[] array, int minSize) {
		if (array.length < minSize) {
			boolean[] newArray = new boolean[oversize(minSize, 1)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static boolean[] grow(boolean[] array) {
		return grow(array, 1 + array.length);
	}

	public static boolean[] shrink(boolean[] array, int targetSize) {
		final int newSize = getShrinkSize(array.length, targetSize, 1);
		if (newSize != array.length) {
			boolean[] newArray = new boolean[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}

	public static char[] grow(char[] array, int minSize) {
		if (array.length < minSize) {
			char[] newArray = new char[oversize(minSize, NUM_BYTES_CHAR)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static char[] grow(char[] array) {
		return grow(array, 1 + array.length);
	}

	public static char[] shrink(char[] array, int targetSize) {
		final int newSize = getShrinkSize(array.length, targetSize, NUM_BYTES_CHAR);
		if (newSize != array.length) {
			char[] newArray = new char[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}
 
	public static int hashCode(char[] array, int start, int end) {
		int code = 0;
		for (int i = end - 1; i >= start; i--)
			code = code * 31 + array[i];
		return code;
	}
 
	public static int hashCode(byte[] array, int start, int end) {
		int code = 0;
		for (int i = end - 1; i >= start; i--)
			code = code * 31 + array[i];
		return code;
	}
 
	public static boolean equals(char[] left, int offsetLeft, char[] right, int offsetRight, int length) {
		if ((offsetLeft + length <= left.length) && (offsetRight + length <= right.length)) {
			for (int i = 0; i < length; i++) {
				if (left[offsetLeft + i] != right[offsetRight + i]) {
					return false;
				}

			}
			return true;
		}
		return false;
	}
 
	public static boolean equals(int[] left, int offsetLeft, int[] right, int offsetRight, int length) {
		if ((offsetLeft + length <= left.length) && (offsetRight + length <= right.length)) {
			for (int i = 0; i < length; i++) {
				if (left[offsetLeft + i] != right[offsetRight + i]) {
					return false;
				}

			}
			return true;
		}
		return false;
	}

	public static int[] toIntArray(Collection<Integer> ints) {

		final int[] result = new int[ints.size()];
		int upto = 0;
		for (int v : ints) {
			result[upto++] = v;
		}

		// paranoia:
		assert upto == result.length;

		return result;
	}

	
	private static <T> SorterTemplate getSorter(final T[] a, final Comparator<? super T> comp) {
		return new SorterTemplate() {
			@Override
			protected void swap(int i, int j) {
				final T o = a[i];
				a[i] = a[j];
				a[j] = o;
			}

			@Override
			protected int compare(int i, int j) {
				return comp.compare(a[i], a[j]);
			}

			@Override
			protected void setPivot(int i) {
				pivot = a[i];
			}

			@Override
			protected int comparePivot(int j) {
				return comp.compare(pivot, a[j]);
			}

			private T pivot;
		};
	}

	
	private static <T extends Comparable<? super T>> SorterTemplate getSorter(final T[] a) {
		return new SorterTemplate() {
			@Override
			protected void swap(int i, int j) {
				final T o = a[i];
				a[i] = a[j];
				a[j] = o;
			}

			@Override
			protected int compare(int i, int j) {
				return a[i].compareTo(a[j]);
			}

			@Override
			protected void setPivot(int i) {
				pivot = a[i];
			}

			@Override
			protected int comparePivot(int j) {
				return pivot.compareTo(a[j]);
			}

			private T pivot;
		};
	}

	// quickSorts (endindex is exclusive!):
 
	public static <T> void quickSort(T[] a, int fromIndex, int toIndex, Comparator<? super T> comp) {
		if (toIndex - fromIndex <= 1)
			return;
		getSorter(a, comp).quickSort(fromIndex, toIndex - 1);
	}
 
	public static <T> void quickSort(T[] a, Comparator<? super T> comp) {
		quickSort(a, 0, a.length, comp);
	}
 
	public static <T extends Comparable<? super T>> void quickSort(T[] a, int fromIndex, int toIndex) {
		if (toIndex - fromIndex <= 1)
			return;
		getSorter(a).quickSort(fromIndex, toIndex - 1);
	}
 
	public static <T extends Comparable<? super T>> void quickSort(T[] a) {
		quickSort(a, 0, a.length);
	}

	// mergeSorts:
 
	public static <T> void mergeSort(T[] a, int fromIndex, int toIndex, Comparator<? super T> comp) {
		if (toIndex - fromIndex <= 1)
			return;
		getSorter(a, comp).mergeSort(fromIndex, toIndex - 1);
	}
 
	public static <T> void mergeSort(T[] a, Comparator<? super T> comp) {
		mergeSort(a, 0, a.length, comp);
	}
 
	public static <T extends Comparable<? super T>> void mergeSort(T[] a, int fromIndex, int toIndex) {
		if (toIndex - fromIndex <= 1)
			return;
		getSorter(a).mergeSort(fromIndex, toIndex - 1);
	}
 
	public static <T extends Comparable<? super T>> void mergeSort(T[] a) {
		mergeSort(a, 0, a.length);
	}

	// insertionSorts:
 
	public static <T> void insertionSort(T[] a, int fromIndex, int toIndex, Comparator<? super T> comp) {
		if (toIndex - fromIndex <= 1)
			return;
		getSorter(a, comp).insertionSort(fromIndex, toIndex - 1);
	}
 
	public static <T> void insertionSort(T[] a, Comparator<? super T> comp) {
		insertionSort(a, 0, a.length, comp);
	}
 
	public static <T extends Comparable<? super T>> void insertionSort(T[] a, int fromIndex, int toIndex) {
		if (toIndex - fromIndex <= 1)
			return;
		getSorter(a).insertionSort(fromIndex, toIndex - 1);
	}
 
	public static <T extends Comparable<? super T>> void insertionSort(T[] a) {
		insertionSort(a, 0, a.length);
	}

}