package fr.asdep.labgen.exporter;

public class ExporterUtils {
    public static void setNibble(byte[] data, int index, int value) {
        int byteIndex = index / 2;
        int nibble = value & 0x0F;
        if (index % 2 == 0) {
            data[byteIndex] = (byte) ((data[byteIndex] & 0xF0) | nibble);
        } else {
            data[byteIndex] = (byte) ((data[byteIndex] & 0x0F) | (nibble << 4));
        }
    }
}
