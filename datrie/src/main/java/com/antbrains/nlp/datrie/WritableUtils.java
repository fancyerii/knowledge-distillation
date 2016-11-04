package com.antbrains.nlp.datrie;
/**
 * Created by lili on 15-3-25. copy from hadoop
 */
public class WritableUtils {
    /**
     * Parse the first byte of a vint/vlong to determine the number of bytes
     * @param value the first byte of the vint/vlong
     * @return the total number of bytes (1 to 9)
     */
    public static int decodeVIntSize(byte value) {
        if (value >= -112) {
            return 1;
        } else if (value < -120) {
            return -119 - value;
        }
        return -111 - value;
    }

    /**
     * Given the first byte of a vint/vlong, determine the sign
     * @param value the first byte
     * @return is the value negative
     */
    public static boolean isNegativeVInt(byte value) {
        return value < -120 || (value >= -112 && value < 0);
    }

    /**
     * Get the encoded length if an integer is stored in a variable-length format
     * @param i long value
     * @return the encoded length
     */
    public static int getVIntSize(long i) {
        if (i >= -112 && i <= 127) {
            return 1;
        }

        if (i < 0) {
            i ^= -1L; // take one's complement'
        }
        // find the number of bytes with non-leading zeros
        int dataBits = Long.SIZE - Long.numberOfLeadingZeros(i);
        // find the number of data bytes + length byte
        return (dataBits + 7) / 8 + 1;
    }
}
