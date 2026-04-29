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

/**
 * Protocol utility helpers.
 * Port of {@code Util.cs}.
 */
class Util {

    private Util() {}

    /**
     * Expands a byte in the manner of "cat -v".
     */
    static String controlSee(byte c) {
        StringBuilder p = new StringBuilder();
        int v = c & 0xff;
        if ((v & 0x80) != 0 && v <= 0xa0) {
            p.append("M-");
            v &= 0x7f;
        }
        if (v >= ' ' && v != 0x7f) {
            p.append((char) v);
        } else {
            p.append('^');
            if (v == 0x7f) p.append('?');
            else           p.append((char) (v + '@'));
        }
        return p.toString();
    }

    /**
     * Decodes a 3270 12- or 14-bit buffer address from two bytes.
     */
    static int decodeBAddress(byte c1, byte c2) {
        if ((c1 & 0xC0) == 0x00) {
            return ((c1 & 0x3F) << 8) | (c2 & 0xff);
        } else {
            return ((c1 & 0x3F) << 6) | (c2 & 0x3F);
        }
    }

    /**
     * Encodes a buffer address into the output buffer using 3270 code-table encoding.
     */
    static void encodeBAddress(NetBuffer ptr, int addr) {
        if (addr > 0xfff) {
            ptr.add((addr >> 8) & 0x3F);
            ptr.add( addr      & 0xFF);
        } else {
            ptr.add(ControllerConstant.CODE_TABLE[(addr >> 6) & 0x3F]);
            ptr.add(ControllerConstant.CODE_TABLE[ addr       & 0x3F]);
        }
    }
}
