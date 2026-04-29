/*
 * Open3270Client - Java / Spring Boot port of Open3270
 * Original C# library: https://github.com/Open3270/Open3270
 * Original authors: Mike Warriner, Francois Botha
 * Original license: MIT
 *
 * Java port copyright (c) 2026 Ivanlopezmolina
 * Released under the MIT License.
 */
package com.open3270client.tn3270e.x3270;

import java.util.ArrayList;
import java.util.List;

/**
 * Growable byte buffer used for assembling 3270 / Telnet protocol PDUs.
 * Port of {@code NetBuffer.cs}.
 */
public class NetBuffer {

    private final List<Byte> buf;

    public NetBuffer() {
        buf = new ArrayList<>();
    }

    public NetBuffer(byte[] data, int start, int len) {
        buf = new ArrayList<>(len);
        for (int i = 0; i < len; i++) buf.add(data[start + i]);
    }

    /** Returns a copy of the current contents as a byte array. */
    public byte[] getData() {
        byte[] out = new byte[buf.size()];
        for (int i = 0; i < out.length; i++) out[i] = buf.get(i);
        return out;
    }

    /** Returns the current number of bytes in the buffer. */
    public int getIndex() {
        return buf.size();
    }

    /** Returns a sub-range of this buffer as a new {@code NetBuffer}. */
    public NetBuffer copyFrom(int start, int len) {
        NetBuffer tmp = new NetBuffer();
        for (int i = 0; i < len; i++) tmp.add(buf.get(start + i));
        return tmp;
    }

    /** Decodes a sub-range of this buffer as a {@code String} (ISO-8859-1). */
    public String asString(int start, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append((char) (buf.get(start + i) & 0xff));
        return sb.toString();
    }

    /** Appends a single byte. */
    public void add(byte b)  { buf.add(b); }

    /** Appends a single byte (from int — truncates to low 8 bits). */
    public void add(int b)   { buf.add((byte) b); }

    /** Appends a single byte from a char. */
    public void add(char b)  { buf.add((byte) b); }

    /** Appends all characters in {@code s} as bytes. */
    public void add(String s) {
        for (int i = 0; i < s.length(); i++) buf.add((byte) s.charAt(i));
    }

    /** Increments the byte at the given index by {@code increment}. */
    public void incrementAt(int index, int increment) {
        buf.set(index, (byte) (buf.get(index) + increment));
    }

    /** Writes a big-endian 16-bit value at the given index (two bytes). */
    public void add16At(int index, int v16) {
        buf.set(index,     (byte) ((v16 & 0xFF00) >> 8));
        buf.set(index + 1, (byte)  (v16 & 0x00FF));
    }

    /** Appends a big-endian 16-bit value (two bytes). */
    public void add16(int v16) {
        buf.add((byte) ((v16 & 0xFF00) >> 8));
        buf.add((byte)  (v16 & 0x00FF));
    }

    /** Appends a big-endian 32-bit value (four bytes). */
    public void add32(int v32) {
        buf.add((byte) ((v32 & 0xFF000000) >> 24));
        buf.add((byte) ((v32 & 0x00FF0000) >> 16));
        buf.add((byte) ((v32 & 0x0000FF00) >>  8));
        buf.add((byte)  (v32 & 0x000000FF));
    }
}
