package com.antbrains.nlp.datrie;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;


/**
 * Created by lili on 15-3-25. copied from hbase but remove unsafe
 */
public class Bytes {
    //HConstants.UTF8_ENCODING should be updated if this changed
    /** When we encode strings, we always specify UTF8 encoding */
    private static final String UTF8_ENCODING = "UTF-8";

    //HConstants.UTF8_CHARSET should be updated if this changed
    /** When we encode strings, we always specify UTF8 encoding */
    private static final Charset UTF8_CHARSET = Charset.forName(UTF8_ENCODING);

    //HConstants.EMPTY_BYTE_ARRAY should be updated if this changed
    private static final byte [] EMPTY_BYTE_ARRAY = new byte [0];


    /**
     * Size of boolean in bytes
     */
    public static final int SIZEOF_BOOLEAN = Byte.SIZE / Byte.SIZE;

    /**
     * Size of byte in bytes
     */
    public static final int SIZEOF_BYTE = SIZEOF_BOOLEAN;

    /**
     * Size of char in bytes
     */
    public static final int SIZEOF_CHAR = Character.SIZE / Byte.SIZE;

    /**
     * Size of double in bytes
     */
    public static final int SIZEOF_DOUBLE = Double.SIZE / Byte.SIZE;

    /**
     * Size of float in bytes
     */
    public static final int SIZEOF_FLOAT = Float.SIZE / Byte.SIZE;

    /**
     * Size of int in bytes
     */
    public static final int SIZEOF_INT = Integer.SIZE / Byte.SIZE;

    /**
     * Size of long in bytes
     */
    public static final int SIZEOF_LONG = Long.SIZE / Byte.SIZE;

    /**
     * Size of short in bytes
     */
    public static final int SIZEOF_SHORT = Short.SIZE / Byte.SIZE;


    /**
     * Estimate of size cost to pay beyond payload in jvm for instance of byte [].
     * Estimate based on study of jhat and jprofiler numbers.
     */
    // JHat says BU is 56 bytes.
    // SizeOf which uses java.lang.instrument says 24 bytes. (3 longs?)
    public static final int ESTIMATED_HEAP_TAX = 16;


    /**
     * Returns length of the byte array, returning 0 if the array is null.
     * Useful for calculating sizes.
     * @param b byte array, which can be null
     * @return 0 if b is null, otherwise returns length
     */
    final public static int len(byte[] b) {
        return b == null ? 0 : b.length;
    }



    /**
     * Write byte-array from src to tgt with a vint length prefix.
     * @param tgt target array
     * @param tgtOffset offset into target array
     * @param src source array
     * @param srcOffset source offset
     * @param srcLength source length
     * @return New offset in src array.
     */
    public static int writeByteArray(final byte [] tgt, final int tgtOffset,
                                     final byte [] src, final int srcOffset, final int srcLength) {
        byte [] vint = vintToBytes(srcLength);
        System.arraycopy(vint, 0, tgt, tgtOffset, vint.length);
        int offset = tgtOffset + vint.length;
        System.arraycopy(src, srcOffset, tgt, offset, srcLength);
        return offset + srcLength;
    }

    /**
     * Put bytes at the specified byte array position.
     * @param tgtBytes the byte array
     * @param tgtOffset position in the array
     * @param srcBytes array to write out
     * @param srcOffset source offset
     * @param srcLength source length
     * @return incremented offset
     */
    public static int putBytes(byte[] tgtBytes, int tgtOffset, byte[] srcBytes,
                               int srcOffset, int srcLength) {
        System.arraycopy(srcBytes, srcOffset, tgtBytes, tgtOffset, srcLength);
        return tgtOffset + srcLength;
    }

    /**
     * Write a single byte out to the specified byte array position.
     * @param bytes the byte array
     * @param offset position in the array
     * @param b byte to write out
     * @return incremented offset
     */
    public static int putByte(byte[] bytes, int offset, byte b) {
        bytes[offset] = b;
        return offset + 1;
    }

    /**
     * Add the whole content of the ByteBuffer to the bytes arrays. The ByteBuffer is modified.
     * @param bytes the byte array
     * @param offset position in the array
     * @param buf ByteBuffer to write out
     * @return incremented offset
     */
    public static int putByteBuffer(byte[] bytes, int offset, ByteBuffer buf) {
        int len = buf.remaining();
        buf.get(bytes, offset, len);
        return offset + len;
    }

    /**
     * Returns a new byte array, copied from the given {@code buf},
     * from the index 0 (inclusive) to the limit (exclusive),
     * regardless of the current position.
     * The position and the other index parameters are not changed.
     *
     * @param buf a byte buffer
     * @return the byte array
     * @see #getBytes(java.nio.ByteBuffer)
     */
    public static byte[] toBytes(ByteBuffer buf) {
        ByteBuffer dup = buf.duplicate();
        dup.position(0);
        return readBytes(dup);
    }

    private static byte[] readBytes(ByteBuffer buf) {
        byte [] result = new byte[buf.remaining()];
        buf.get(result);
        return result;
    }

    /**
     * @param b Presumed UTF-8 encoded byte array.
     * @return String made from <code>b</code>
     */
    public static String toString(final byte [] b) {
        if (b == null) {
            return null;
        }
        return toString(b, 0, b.length);
    }

    /**
     * Joins two byte arrays together using a separator.
     * @param b1 The first byte array.
     * @param sep The separator to use.
     * @param b2 The second byte array.
     * @return join 2 string
     */
    public static String toString(final byte [] b1,
                                  String sep,
                                  final byte [] b2) {
        return toString(b1, 0, b1.length) + sep + toString(b2, 0, b2.length);
    }
 
    public static String toString(final byte [] b, int off, int len) {
        if (b == null) {
            return null;
        }
        if (len == 0) {
            return "";
        }
        return new String(b, off, len, UTF8_CHARSET);
    }
 
    public static String toStringBinary(final byte [] b) {
        if (b == null)
            return "null";
        return toStringBinary(b, 0, b.length);
    }
    
    public static String toStringBinary(ByteBuffer buf) {
        if (buf == null)
            return "null";
        if (buf.hasArray()) {
            return toStringBinary(buf.array(), buf.arrayOffset(), buf.limit());
        }
        return toStringBinary(toBytes(buf));
    }

 
    public static String toStringBinary(final byte [] b, int off, int len) {
        StringBuilder result = new StringBuilder();
        // Just in case we are passed a 'len' that is > buffer length...
        if (off >= b.length) return result.toString();
        if (off + len > b.length) len = b.length - off;
        for (int i = off; i < off + len ; ++i ) {
            int ch = b[i] & 0xFF;
            if ( (ch >= '0' && ch <= '9')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= 'a' && ch <= 'z')
                    || " `~!@#$%^&*()-_=+[]{}|;:'\",.<>/?".indexOf(ch) >= 0 ) {
                result.append((char)ch);
            } else {
                result.append(String.format("\\x%02X", ch));
            }
        }
        return result.toString();
    }

    private static boolean isHexDigit(char c) {
        return
                (c >= 'A' && c <= 'F') ||
                        (c >= '0' && c <= '9');
    }
 
    public static byte toBinaryFromHex(byte ch) {
        if ( ch >= 'A' && ch <= 'F' )
            return (byte) ((byte)10 + (byte) (ch - 'A'));
        // else
        return (byte) (ch - '0');
    }

    public static byte [] toBytesBinary(String in) {
        // this may be bigger than we need, but let's be safe.
        byte [] b = new byte[in.length()];
        int size = 0;
        for (int i = 0; i < in.length(); ++i) {
            char ch = in.charAt(i);
            if (ch == '\\' && in.length() > i+1 && in.charAt(i+1) == 'x') {
                // ok, take next 2 hex digits.
                char hd1 = in.charAt(i+2);
                char hd2 = in.charAt(i+3);

                // they need to be A-F0-9:
                if (!isHexDigit(hd1) ||
                        !isHexDigit(hd2)) {
                    // bogus escape code, ignore:
                    continue;
                }
                // turn hex ASCII digit -> number
                byte d = (byte) ((toBinaryFromHex((byte)hd1) << 4) + toBinaryFromHex((byte)hd2));

                b[size++] = d;
                i += 3; // skip 3
            } else {
                b[size++] = (byte) ch;
            }
        }
        // resize:
        byte [] b2 = new byte[size];
        System.arraycopy(b, 0, b2, 0, size);
        return b2;
    }

    /**
     * Converts a string to a UTF-8 byte array.
     * @param s string
     * @return the byte array
     */
    public static byte[] toBytes(String s) {
        return s.getBytes(UTF8_CHARSET);
    }

    /**
     * Convert a boolean to a byte array. True becomes -1
     * and false becomes 0.
     *
     * @param b value
     * @return <code>b</code> encoded in a byte array.
     */
    public static byte [] toBytes(final boolean b) {
        return new byte[] { b ? (byte) -1 : (byte) 0 };
    }

    /**
     * Reverses {@link #toBytes(boolean)}
     * @param b array
     * @return True or false.
     */
    public static boolean toBoolean(final byte [] b) {
        if (b.length != 1) {
            throw new IllegalArgumentException("Array has wrong size: " + b.length);
        }
        return b[0] != (byte) 0;
    }

    /**
     * Convert a long value to a byte array using big-endian.
     *
     * @param val value to convert
     * @return the byte array
     */
    public static byte[] toBytes(long val) {
        byte [] b = new byte[8];
        for (int i = 7; i > 0; i--) {
            b[i] = (byte) val;
            val >>>= 8;
        }
        b[0] = (byte) val;
        return b;
    }

    /**
     * Converts a byte array to a long value. Reverses
     * {@link #toBytes(long)}
     * @param bytes array
     * @return the long value
     */
    public static long toLong(byte[] bytes) {
        return toLong(bytes, 0, SIZEOF_LONG);
    }

    /**
     * Converts a byte array to a long value. Assumes there will be
     * {@link #SIZEOF_LONG} bytes available.
     *
     * @param bytes bytes
     * @param offset offset
     * @return the long value
     */
    public static long toLong(byte[] bytes, int offset) {
        return toLong(bytes, offset, SIZEOF_LONG);
    }

    /**
     * Converts a byte array to a long value.
     *
     * @param bytes array of bytes
     * @param offset offset into array
     * @param length length of data (must be {@link #SIZEOF_LONG})
     * @return the long value
     * @throws IllegalArgumentException if length is not {@link #SIZEOF_LONG} or
     * if there's not enough room in the array at the offset indicated.
     */
    public static long toLong(byte[] bytes, int offset, final int length) {
        if (length != SIZEOF_LONG || offset + length > bytes.length) {
            throw explainWrongLengthOrOffset(bytes, offset, length, SIZEOF_LONG);
        }

        long l = 0;
        for(int i = offset; i < offset + length; i++) {
            l <<= 8;
            l ^= bytes[i] & 0xFF;
        }
        return l;

    }

    private static IllegalArgumentException
    explainWrongLengthOrOffset(final byte[] bytes,
                               final int offset,
                               final int length,
                               final int expectedLength) {
        String reason;
        if (length != expectedLength) {
            reason = "Wrong length: " + length + ", expected " + expectedLength;
        } else {
            reason = "offset (" + offset + ") + length (" + length + ") exceed the"
                    + " capacity of the array: " + bytes.length;
        }
        return new IllegalArgumentException(reason);
    }

    /**
     * Put a long value out to the specified byte array position.
     * @param bytes the byte array
     * @param offset position in the array
     * @param val long to write out
     * @return incremented offset
     * @throws IllegalArgumentException if the byte array given doesn't have
     * enough room at the offset specified.
     */
    public static int putLong(byte[] bytes, int offset, long val) {
        if (bytes.length - offset < SIZEOF_LONG) {
            throw new IllegalArgumentException("Not enough room to put a long at"
                    + " offset " + offset + " in a " + bytes.length + " byte array");
        }

        for(int i = offset + 7; i > offset; i--) {
            bytes[i] = (byte) val;
            val >>>= 8;
        }
        bytes[offset] = (byte) val;
        return offset + SIZEOF_LONG;

    }



    /**
     * Presumes float encoded as IEEE 754 floating-point "single format"
     * @param bytes byte array
     * @return Float made from passed byte array.
     */
    public static float toFloat(byte [] bytes) {
        return toFloat(bytes, 0);
    }

    /**
     * Presumes float encoded as IEEE 754 floating-point "single format"
     * @param bytes array to convert
     * @param offset offset into array
     * @return Float made from passed byte array.
     */
    public static float toFloat(byte [] bytes, int offset) {
        return Float.intBitsToFloat(toInt(bytes, offset, SIZEOF_INT));
    }

    /**
     * @param bytes byte array
     * @param offset offset to write to
     * @param f float value
     * @return New offset in <code>bytes</code>
     */
    public static int putFloat(byte [] bytes, int offset, float f) {
        return putInt(bytes, offset, Float.floatToRawIntBits(f));
    }

    /**
     * @param f float value
     * @return the float represented as byte []
     */
    public static byte [] toBytes(final float f) {
        // Encode it as int
        return Bytes.toBytes(Float.floatToRawIntBits(f));
    }

    /**
     * @param bytes byte array
     * @return Return double made from passed bytes.
     */
    public static double toDouble(final byte [] bytes) {
        return toDouble(bytes, 0);
    }

    /**
     * @param bytes byte array
     * @param offset offset where double is
     * @return Return double made from passed bytes.
     */
    public static double toDouble(final byte [] bytes, final int offset) {
        return Double.longBitsToDouble(toLong(bytes, offset, SIZEOF_LONG));
    }

    /**
     * @param bytes byte array
     * @param offset offset to write to
     * @param d value
     * @return New offset into array <code>bytes</code>
     */
    public static int putDouble(byte [] bytes, int offset, double d) {
        return putLong(bytes, offset, Double.doubleToLongBits(d));
    }

    /**
     * Serialize a double as the IEEE 754 double format output. The resultant
     * array will be 8 bytes long.
     *
     * @param d value
     * @return the double represented as byte []
     */
    public static byte [] toBytes(final double d) {
        // Encode it as a long
        return Bytes.toBytes(Double.doubleToRawLongBits(d));
    }

    /**
     * Convert an int value to a byte array.  Big-endian.  Same as what DataOutputStream.writeInt
     * does.
     *
     * @param val value
     * @return the byte array
     */
    public static byte[] toBytes(int val) {
        byte [] b = new byte[4];
        for(int i = 3; i > 0; i--) {
            b[i] = (byte) val;
            val >>>= 8;
        }
        b[0] = (byte) val;
        return b;
    }

    /**
     * Converts a byte array to an int value
     * @param bytes byte array
     * @return the int value
     */
    public static int toInt(byte[] bytes) {
        return toInt(bytes, 0, SIZEOF_INT);
    }

    /**
     * Converts a byte array to an int value
     * @param bytes byte array
     * @param offset offset into array
     * @return the int value
     */
    public static int toInt(byte[] bytes, int offset) {
        return toInt(bytes, offset, SIZEOF_INT);
    }
 
    public static int toInt(byte[] bytes, int offset, final int length) {
        if (length != SIZEOF_INT || offset + length > bytes.length) {
            throw explainWrongLengthOrOffset(bytes, offset, length, SIZEOF_INT);
        }

        int n = 0;
        for(int i = offset; i < (offset + length); i++) {
            n <<= 8;
            n ^= bytes[i] & 0xFF;
        }
        return n;

    }
 
    public static int readAsInt(byte[] bytes, int offset, final int length) {
        if (offset + length > bytes.length) {
            throw new IllegalArgumentException("offset (" + offset + ") + length (" + length
                    + ") exceed the" + " capacity of the array: " + bytes.length);
        }
        int n = 0;
        for(int i = offset; i < (offset + length); i++) {
            n <<= 8;
            n ^= bytes[i] & 0xFF;
        }
        return n;
    }
 
    public static int putInt(byte[] bytes, int offset, int val) {
        if (bytes.length - offset < SIZEOF_INT) {
            throw new IllegalArgumentException("Not enough room to put an int at"
                    + " offset " + offset + " in a " + bytes.length + " byte array");
        }

        for(int i= offset + 3; i > offset; i--) {
            bytes[i] = (byte) val;
            val >>>= 8;
        }
        bytes[offset] = (byte) val;
        return offset + SIZEOF_INT;

    }
 
    public static byte[] toBytes(short val) {
        byte[] b = new byte[SIZEOF_SHORT];
        b[1] = (byte) val;
        val >>= 8;
        b[0] = (byte) val;
        return b;
    }
 
    public static short toShort(byte[] bytes) {
        return toShort(bytes, 0, SIZEOF_SHORT);
    }
 
    public static short toShort(byte[] bytes, int offset) {
        return toShort(bytes, offset, SIZEOF_SHORT);
    }
 
    public static short toShort(byte[] bytes, int offset, final int length) {
        if (length != SIZEOF_SHORT || offset + length > bytes.length) {
            throw explainWrongLengthOrOffset(bytes, offset, length, SIZEOF_SHORT);
        }

        short n = 0;
        n ^= bytes[offset] & 0xFF;
        n <<= 8;
        n ^= bytes[offset+1] & 0xFF;
        return n;

    }
 
    public static byte[] getBytes(ByteBuffer buf) {
        return readBytes(buf.duplicate());
    }
 
    public static int putShort(byte[] bytes, int offset, short val) {
        if (bytes.length - offset < SIZEOF_SHORT) {
            throw new IllegalArgumentException("Not enough room to put a short at"
                    + " offset " + offset + " in a " + bytes.length + " byte array");
        }

        bytes[offset+1] = (byte) val;
        val >>= 8;
        bytes[offset] = (byte) val;
        return offset + SIZEOF_SHORT;

    }
 
    public static int putAsShort(byte[] bytes, int offset, int val) {
        if (bytes.length - offset < SIZEOF_SHORT) {
            throw new IllegalArgumentException("Not enough room to put a short at"
                    + " offset " + offset + " in a " + bytes.length + " byte array");
        }
        bytes[offset+1] = (byte) val;
        val >>= 8;
        bytes[offset] = (byte) val;
        return offset + SIZEOF_SHORT;
    }
 
    public static byte[] toBytes(BigDecimal val) {
        byte[] valueBytes = val.unscaledValue().toByteArray();
        byte[] result = new byte[valueBytes.length + SIZEOF_INT];
        int offset = putInt(result, 0, val.scale());
        putBytes(result, offset, valueBytes, 0, valueBytes.length);
        return result;
    }
 
    public static BigDecimal toBigDecimal(byte[] bytes) {
        return toBigDecimal(bytes, 0, bytes.length);
    }
 
    public static BigDecimal toBigDecimal(byte[] bytes, int offset, final int length) {
        if (bytes == null || length < SIZEOF_INT + 1 ||
                (offset + length > bytes.length)) {
            return null;
        }

        int scale = toInt(bytes, offset);
        byte[] tcBytes = new byte[length - SIZEOF_INT];
        System.arraycopy(bytes, offset + SIZEOF_INT, tcBytes, 0, length - SIZEOF_INT);
        return new BigDecimal(new BigInteger(tcBytes), scale);
    }
 
    public static int putBigDecimal(byte[] bytes, int offset, BigDecimal val) {
        if (bytes == null) {
            return offset;
        }

        byte[] valueBytes = val.unscaledValue().toByteArray();
        byte[] result = new byte[valueBytes.length + SIZEOF_INT];
        offset = putInt(result, offset, val.scale());
        return putBytes(result, offset, valueBytes, 0, valueBytes.length);
    }

 
    public static byte [] vintToBytes(final long vint) {
        long i = vint;
        int size = WritableUtils.getVIntSize(i);
        byte [] result = new byte[size];
        int offset = 0;
        if (i >= -112 && i <= 127) {
            result[offset] = (byte) i;
            return result;
        }

        int len = -112;
        if (i < 0) {
            i ^= -1L; // take one's complement'
            len = -120;
        }

        long tmp = i;
        while (tmp != 0) {
            tmp = tmp >> 8;
            len--;
        }

        result[offset++] = (byte) len;

        len = (len < -120) ? -(len + 120) : -(len + 112);

        for (int idx = len; idx != 0; idx--) {
            int shiftbits = (idx - 1) * 8;
            long mask = 0xFFL << shiftbits;
            result[offset++] = (byte)((i & mask) >> shiftbits);
        }
        return result;
    }
 
    public static long bytesToVint(final byte [] buffer) {
        int offset = 0;
        byte firstByte = buffer[offset++];
        int len = WritableUtils.decodeVIntSize(firstByte);
        if (len == 1) {
            return firstByte;
        }
        long i = 0;
        for (int idx = 0; idx < len-1; idx++) {
            byte b = buffer[offset++];
            i = i << 8;
            i = i | (b & 0xFF);
        }
        return (WritableUtils.isNegativeVInt(firstByte) ? ~i : i);
    }
 
    public static long readVLong(final byte [] buffer, final int offset)
            throws IOException {
        byte firstByte = buffer[offset];
        int len = WritableUtils.decodeVIntSize(firstByte);
        if (len == 1) {
            return firstByte;
        }
        long i = 0;
        for (int idx = 0; idx < len-1; idx++) {
            byte b = buffer[offset + 1 + idx];
            i = i << 8;
            i = i | (b & 0xFF);
        }
        return (WritableUtils.isNegativeVInt(firstByte) ? ~i : i);
    }
 
    public static boolean equals(byte[] a, ByteBuffer buf) {
        if (a == null) return buf == null;
        if (buf == null) return false;
        if (a.length != buf.remaining()) return false;

        // Thou shalt not modify the original byte buffer in what should be read only operations.
        ByteBuffer b = buf.duplicate();
        for (byte anA : a) {
            if (anA != b.get()) {
                return false;
            }
        }
        return true;
    }
 
    public static byte [] add(final byte [] a, final byte [] b) {
        return add(a, b, EMPTY_BYTE_ARRAY);
    }
 
    public static byte [] add(final byte [] a, final byte [] b, final byte [] c) {
        byte [] result = new byte[a.length + b.length + c.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        System.arraycopy(c, 0, result, a.length + b.length, c.length);
        return result;
    }
 
    public static byte [] head(final byte [] a, final int length) {
        if (a.length < length) {
            return null;
        }
        byte [] result = new byte[length];
        System.arraycopy(a, 0, result, 0, length);
        return result;
    }
 
    public static byte [] tail(final byte [] a, final int length) {
        if (a.length < length) {
            return null;
        }
        byte [] result = new byte[length];
        System.arraycopy(a, a.length - length, result, 0, length);
        return result;
    }
 
    public static byte [] padHead(final byte [] a, final int length) {
        byte [] padding = new byte[length];
        for (int i = 0; i < length; i++) {
            padding[i] = 0;
        }
        return add(padding,a);
    }
 
    public static byte [] padTail(final byte [] a, final int length) {
        byte [] padding = new byte[length];
        for (int i = 0; i < length; i++) {
            padding[i] = 0;
        }
        return add(a,padding);
    }
 
    public static int hashCode(byte[] bytes, int offset, int length) {
        int hash = 1;
        for (int i = offset; i < offset + length; i++)
            hash = (31 * hash) + (int) bytes[i];
        return hash;
    }
 
    public static byte [][] toByteArrays(final String [] t) {
        byte [][] result = new byte[t.length][];
        for (int i = 0; i < t.length; i++) {
            result[i] = Bytes.toBytes(t[i]);
        }
        return result;
    }
 
    public static byte [][] toByteArrays(final String column) {
        return toByteArrays(toBytes(column));
    }
 
    public static byte [][] toByteArrays(final byte [] column) {
        byte [][] result = new byte[1][];
        result[0] = column;
        return result;
    }
 
    public static void writeStringFixedSize(final DataOutput out, String s,
                                            int size) throws IOException {
        byte[] b = toBytes(s);
        if (b.length > size) {
            throw new IOException("Trying to write " + b.length + " bytes (" +
                    toStringBinary(b) + ") into a field of length " + size);
        }

        out.writeBytes(s);
        for (int i = 0; i < size - s.length(); ++i)
            out.writeByte(0);
    }
 
    public static String readStringFixedSize(final DataInput in, int size)
            throws IOException {
        byte[] b = new byte[size];
        in.readFully(b);
        int n = b.length;
        while (n > 0 && b[n - 1] == 0)
            --n;

        return toString(b, 0, n);
    }
 
    public static byte [] copy(byte [] bytes) {
        if (bytes == null) return null;
        byte [] result = new byte[bytes.length];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        return result;
    }
 
    public static byte [] copy(byte [] bytes, final int offset, final int length) {
        if (bytes == null) return null;
        byte [] result = new byte[length];
        System.arraycopy(bytes, offset, result, 0, length);
        return result;
    }
 
    public static int unsignedBinarySearch(byte[] a, int fromIndex, int toIndex, byte key) {
        int unsignedKey = key & 0xff;
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid] & 0xff;

            if (midVal < unsignedKey) {
                low = mid + 1;
            } else if (midVal > unsignedKey) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1); // key not found.
    }

    /**
     * Treat the byte[] as an unsigned series of bytes, most significant bits first.  Start by adding
     * 1 to the rightmost bit/byte and carry over all overflows to the more significant bits/bytes.
     *
     * @param input The byte[] to increment.
     * @return The incremented copy of "in".  May be same length or 1 byte longer.
     */
    public static byte[] unsignedCopyAndIncrement(final byte[] input) {
        byte[] copy = copy(input);
        if (copy == null) {
            throw new IllegalArgumentException("cannot increment null array");
        }
        for (int i = copy.length - 1; i >= 0; --i) {
            if (copy[i] == -1) {// -1 is all 1-bits, which is the unsigned maximum
                copy[i] = 0;
            } else {
                ++copy[i];
                return copy;
            }
        }
        // we maxed out the array
        byte[] out = new byte[copy.length + 1];
        out[0] = 1;
        System.arraycopy(copy, 0, out, 1, copy.length);
        return out;
    }
 
    public static int indexOf(byte[] array, byte target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }
 
    public static int indexOf(byte[] array, byte[] target) {
        if (target.length == 0) {
            return 0;
        }

        outer:
        for (int i = 0; i < array.length - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
 
    public static boolean contains(byte[] array, byte target) {
        return indexOf(array, target) > -1;
    }
 
    public static boolean contains(byte[] array, byte[] target) {
        return indexOf(array, target) > -1;
    }
 
    public static void zero(byte[] b) {
        zero(b, 0, b.length);
    }
 
    public static void zero(byte[] b, int offset, int length) {
        Arrays.fill(b, offset, offset + length, (byte) 0);
    }

    private static final SecureRandom RNG = new SecureRandom();
 
    public static void random(byte[] b) {
        RNG.nextBytes(b);
    }
 
    public static void random(byte[] b, int offset, int length) {
        byte[] buf = new byte[length];
        RNG.nextBytes(buf);
        System.arraycopy(buf, 0, b, offset, length);
    }
 
    public static byte[] createMaxByteArray(int maxByteCount) {
        byte[] maxByteArray = new byte[maxByteCount];
        for (int i = 0; i < maxByteArray.length; i++) {
            maxByteArray[i] = (byte) 0xff;
        }
        return maxByteArray;
    }
 
    public static byte[] multiple(byte[] srcBytes, int multiNum) {
        if (multiNum <= 0) {
            return new byte[0];
        }
        byte[] result = new byte[srcBytes.length * multiNum];
        for (int i = 0; i < multiNum; i++) {
            System.arraycopy(srcBytes, 0, result, i * srcBytes.length,
                    srcBytes.length);
        }
        return result;
    }
 
    public static String toHex(byte[] b) {

        return String.format("%x", new BigInteger(1, b));
    }
 
    public static byte[] fromHex(String hex) {
        // Make sure letters are upper case
        hex = hex.toUpperCase();
        byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte)((toBinaryFromHex((byte)hex.charAt(2 * i)) << 4) +
                    toBinaryFromHex((byte)hex.charAt((2 * i + 1))));
        }
        return b;
    }

}