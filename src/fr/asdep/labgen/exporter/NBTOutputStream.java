package fr.asdep.labgen.exporter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class NBTOutputStream implements AutoCloseable {
    private final DataOutputStream dos;

    public NBTOutputStream(OutputStream os) throws IOException {
        this(os, true);
    }

    public NBTOutputStream(OutputStream os, boolean gzip) throws IOException {
        OutputStream wrapped = new java.io.BufferedOutputStream(os, 65536);
        if (gzip) {
            this.dos = new DataOutputStream(new java.util.zip.GZIPOutputStream(wrapped) {
                {
                    def.setLevel(java.util.zip.Deflater.BEST_SPEED);
                }
            });
        } else {
            this.dos = new DataOutputStream(wrapped);
        }
    }

    public void writeTagByte(String name, byte value) throws IOException {
        writeTagHeader(1, name);
        dos.writeByte(value);
    }

    public void writeTagCompound(String name) throws IOException {
        writeTagHeader(10, name);
    }

    public void writeTagEnd() throws IOException {
        dos.writeByte(0);
    }

    public void writeTagShort(String name, short value) throws IOException {
        writeTagHeader(2, name);
        dos.writeShort(value);
    }

    public void writeTagInt(String name, int value) throws IOException {
        writeTagHeader(3, name);
        dos.writeInt(value);
    }

    public void writeTagLong(String name, long value) throws IOException {
        writeTagHeader(4, name);
        dos.writeLong(value);
    }

    public void writeTagString(String name, String value) throws IOException {
        writeTagHeader(8, name);
        writeString(value);
    }

    public void writeTagByteArray(String name, byte[] value) throws IOException {
        writeTagByteArray(name, value, null);
    }

    public void writeTagByteArray(String name, byte[] value, java.util.function.Consumer<Integer> progress) throws IOException {
        writeTagHeader(7, name);
        dos.writeInt(value.length);
        if (progress != null) {
            int chunkSize = 65536; // 64KB
            for (int i = 0; i < value.length; i += chunkSize) {
                int len = Math.min(chunkSize, value.length - i);
                dos.write(value, i, len);
                progress.accept(i + len);
            }
        } else {
            dos.write(value);
        }
    }

    public void writeTagListHeader(String name, byte type, int size) throws IOException {
        writeTagHeader(9, name);
        dos.writeByte(type);
        dos.writeInt(size);
    }

    public void writeTagIntArray(String name, int[] value) throws IOException {
        writeTagHeader(11, name);
        dos.writeInt(value.length);
        for (int i : value) {
            dos.writeInt(i);
        }
    }

    private void writeTagHeader(int type, String name) throws IOException {
        dos.writeByte(type);
        if (name != null) {
            writeString(name);
        }
    }

    private void writeString(String s) throws IOException {
        if (s == null) {
            dos.writeShort(0);
        } else {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            dos.writeShort(bytes.length);
            dos.write(bytes);
        }
    }

    @Override
    public void close() throws IOException {
        dos.close();
    }
}
