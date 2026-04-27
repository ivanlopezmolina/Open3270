/*
 * MIT License
 *
 * Copyright (c) 2026 ivanlopezmolina
 *
 * Originally based on Open3270 - A C# implementation of the TN3270/TN3270E protocol
 * Original authors: Michael Warriner and contributors (c) 2004-2020
 * Original project: https://github.com/Open3270/Open3270
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.open3270.client.tn3270.x3270;

/**
 * EBCDIC to ASCII character translation (Code Page 037).
 */
public final class CharacterGenerator {
    private CharacterGenerator() {}

    // Standard EBCDIC Code Page 037 to ASCII mapping
    private static final char[] EBCDIC_TO_ASCII = new char[256];
    private static final byte[] ASCII_TO_EBCDIC = new byte[256];

    static {
        // Initialize with dots for non-printable
        for (int i = 0; i < 256; i++) EBCDIC_TO_ASCII[i] = ' ';
        for (int i = 0; i < 256; i++) ASCII_TO_EBCDIC[i] = 0x3F; // '?'

        // Standard EBCDIC 037 to ASCII mappings
        EBCDIC_TO_ASCII[0x00] = '\0';
        EBCDIC_TO_ASCII[0x01] = '\u0001';
        EBCDIC_TO_ASCII[0x02] = '\u0002';
        EBCDIC_TO_ASCII[0x03] = '\u0003';
        EBCDIC_TO_ASCII[0x04] = ' ';
        EBCDIC_TO_ASCII[0x05] = '\t';
        EBCDIC_TO_ASCII[0x06] = ' ';
        EBCDIC_TO_ASCII[0x07] = '\u007f';
        EBCDIC_TO_ASCII[0x0b] = '\u000b';
        EBCDIC_TO_ASCII[0x0c] = '\f';
        EBCDIC_TO_ASCII[0x0d] = '\r';
        EBCDIC_TO_ASCII[0x0e] = '\u000e';
        EBCDIC_TO_ASCII[0x0f] = '\u000f';
        EBCDIC_TO_ASCII[0x10] = '\u0010';
        EBCDIC_TO_ASCII[0x11] = '\u0011';
        EBCDIC_TO_ASCII[0x12] = '\u0012';
        EBCDIC_TO_ASCII[0x13] = '\u0013';
        EBCDIC_TO_ASCII[0x15] = '\n';
        EBCDIC_TO_ASCII[0x16] = '\u0008';
        EBCDIC_TO_ASCII[0x17] = '\u0017';
        EBCDIC_TO_ASCII[0x18] = '\u0018';
        EBCDIC_TO_ASCII[0x19] = '\u0019';
        EBCDIC_TO_ASCII[0x1a] = '\u001c';
        EBCDIC_TO_ASCII[0x1b] = '\u001a';
        EBCDIC_TO_ASCII[0x1c] = '\u001c';
        EBCDIC_TO_ASCII[0x1d] = '\u001d';
        EBCDIC_TO_ASCII[0x1e] = '\u001e';
        EBCDIC_TO_ASCII[0x1f] = '\u001f';
        EBCDIC_TO_ASCII[0x20] = ' ';
        EBCDIC_TO_ASCII[0x21] = ' ';
        EBCDIC_TO_ASCII[0x22] = ' ';
        EBCDIC_TO_ASCII[0x23] = ' ';
        EBCDIC_TO_ASCII[0x24] = ' ';
        EBCDIC_TO_ASCII[0x25] = '\n';
        EBCDIC_TO_ASCII[0x26] = '\u0017';
        EBCDIC_TO_ASCII[0x27] = '\u001b';
        EBCDIC_TO_ASCII[0x28] = ' ';
        EBCDIC_TO_ASCII[0x29] = ' ';
        EBCDIC_TO_ASCII[0x2a] = ' ';
        EBCDIC_TO_ASCII[0x2b] = ' ';
        EBCDIC_TO_ASCII[0x2c] = ' ';
        EBCDIC_TO_ASCII[0x2d] = '\u0005';
        EBCDIC_TO_ASCII[0x2e] = '\u0006';
        EBCDIC_TO_ASCII[0x2f] = '\u0007';
        EBCDIC_TO_ASCII[0x30] = ' ';
        EBCDIC_TO_ASCII[0x31] = ' ';
        EBCDIC_TO_ASCII[0x32] = '\u0016';
        EBCDIC_TO_ASCII[0x33] = ' ';
        EBCDIC_TO_ASCII[0x34] = ' ';
        EBCDIC_TO_ASCII[0x35] = ' ';
        EBCDIC_TO_ASCII[0x36] = ' ';
        EBCDIC_TO_ASCII[0x37] = '\u0004';
        EBCDIC_TO_ASCII[0x38] = ' ';
        EBCDIC_TO_ASCII[0x39] = ' ';
        EBCDIC_TO_ASCII[0x3a] = ' ';
        EBCDIC_TO_ASCII[0x3b] = ' ';
        EBCDIC_TO_ASCII[0x3c] = '\u0014';
        EBCDIC_TO_ASCII[0x3d] = '\u0015';
        EBCDIC_TO_ASCII[0x3e] = ' ';
        EBCDIC_TO_ASCII[0x3f] = '\u001a';
        EBCDIC_TO_ASCII[0x40] = ' ';
        EBCDIC_TO_ASCII[0x4a] = '\u00a2';  // cent sign - fallback to '¢'
        EBCDIC_TO_ASCII[0x4b] = '.';
        EBCDIC_TO_ASCII[0x4c] = '<';
        EBCDIC_TO_ASCII[0x4d] = '(';
        EBCDIC_TO_ASCII[0x4e] = '+';
        EBCDIC_TO_ASCII[0x4f] = '|';
        EBCDIC_TO_ASCII[0x50] = '&';
        EBCDIC_TO_ASCII[0x5a] = '!';
        EBCDIC_TO_ASCII[0x5b] = '$';
        EBCDIC_TO_ASCII[0x5c] = '*';
        EBCDIC_TO_ASCII[0x5d] = ')';
        EBCDIC_TO_ASCII[0x5e] = ';';
        EBCDIC_TO_ASCII[0x5f] = '\u00ac'; // not sign
        EBCDIC_TO_ASCII[0x60] = '-';
        EBCDIC_TO_ASCII[0x61] = '/';
        EBCDIC_TO_ASCII[0x6a] = '\u00a6'; // broken bar
        EBCDIC_TO_ASCII[0x6b] = ',';
        EBCDIC_TO_ASCII[0x6c] = '%';
        EBCDIC_TO_ASCII[0x6d] = '_';
        EBCDIC_TO_ASCII[0x6e] = '>';
        EBCDIC_TO_ASCII[0x6f] = '?';
        EBCDIC_TO_ASCII[0x79] = '`';
        EBCDIC_TO_ASCII[0x7a] = ':';
        EBCDIC_TO_ASCII[0x7b] = '#';
        EBCDIC_TO_ASCII[0x7c] = '@';
        EBCDIC_TO_ASCII[0x7d] = '\'';
        EBCDIC_TO_ASCII[0x7e] = '=';
        EBCDIC_TO_ASCII[0x7f] = '"';
        // lowercase a-i: 0x81-0x89
        EBCDIC_TO_ASCII[0x81] = 'a'; EBCDIC_TO_ASCII[0x82] = 'b'; EBCDIC_TO_ASCII[0x83] = 'c';
        EBCDIC_TO_ASCII[0x84] = 'd'; EBCDIC_TO_ASCII[0x85] = 'e'; EBCDIC_TO_ASCII[0x86] = 'f';
        EBCDIC_TO_ASCII[0x87] = 'g'; EBCDIC_TO_ASCII[0x88] = 'h'; EBCDIC_TO_ASCII[0x89] = 'i';
        // lowercase j-r: 0x91-0x99
        EBCDIC_TO_ASCII[0x91] = 'j'; EBCDIC_TO_ASCII[0x92] = 'k'; EBCDIC_TO_ASCII[0x93] = 'l';
        EBCDIC_TO_ASCII[0x94] = 'm'; EBCDIC_TO_ASCII[0x95] = 'n'; EBCDIC_TO_ASCII[0x96] = 'o';
        EBCDIC_TO_ASCII[0x97] = 'p'; EBCDIC_TO_ASCII[0x98] = 'q'; EBCDIC_TO_ASCII[0x99] = 'r';
        // lowercase s-z: 0xa2-0xa9
        EBCDIC_TO_ASCII[0xa2] = 's'; EBCDIC_TO_ASCII[0xa3] = 't'; EBCDIC_TO_ASCII[0xa4] = 'u';
        EBCDIC_TO_ASCII[0xa5] = 'v'; EBCDIC_TO_ASCII[0xa6] = 'w'; EBCDIC_TO_ASCII[0xa7] = 'x';
        EBCDIC_TO_ASCII[0xa8] = 'y'; EBCDIC_TO_ASCII[0xa9] = 'z';
        // uppercase A-I: 0xC1-0xC9
        EBCDIC_TO_ASCII[0xC1] = 'A'; EBCDIC_TO_ASCII[0xC2] = 'B'; EBCDIC_TO_ASCII[0xC3] = 'C';
        EBCDIC_TO_ASCII[0xC4] = 'D'; EBCDIC_TO_ASCII[0xC5] = 'E'; EBCDIC_TO_ASCII[0xC6] = 'F';
        EBCDIC_TO_ASCII[0xC7] = 'G'; EBCDIC_TO_ASCII[0xC8] = 'H'; EBCDIC_TO_ASCII[0xC9] = 'I';
        // uppercase J-R: 0xD1-0xD9
        EBCDIC_TO_ASCII[0xD1] = 'J'; EBCDIC_TO_ASCII[0xD2] = 'K'; EBCDIC_TO_ASCII[0xD3] = 'L';
        EBCDIC_TO_ASCII[0xD4] = 'M'; EBCDIC_TO_ASCII[0xD5] = 'N'; EBCDIC_TO_ASCII[0xD6] = 'O';
        EBCDIC_TO_ASCII[0xD7] = 'P'; EBCDIC_TO_ASCII[0xD8] = 'Q'; EBCDIC_TO_ASCII[0xD9] = 'R';
        // uppercase S-Z: 0xE2-0xE9
        EBCDIC_TO_ASCII[0xE2] = 'S'; EBCDIC_TO_ASCII[0xE3] = 'T'; EBCDIC_TO_ASCII[0xE4] = 'U';
        EBCDIC_TO_ASCII[0xE5] = 'V'; EBCDIC_TO_ASCII[0xE6] = 'W'; EBCDIC_TO_ASCII[0xE7] = 'X';
        EBCDIC_TO_ASCII[0xE8] = 'Y'; EBCDIC_TO_ASCII[0xE9] = 'Z';
        // digits 0-9: 0xF0-0xF9
        EBCDIC_TO_ASCII[0xF0] = '0'; EBCDIC_TO_ASCII[0xF1] = '1'; EBCDIC_TO_ASCII[0xF2] = '2';
        EBCDIC_TO_ASCII[0xF3] = '3'; EBCDIC_TO_ASCII[0xF4] = '4'; EBCDIC_TO_ASCII[0xF5] = '5';
        EBCDIC_TO_ASCII[0xF6] = '6'; EBCDIC_TO_ASCII[0xF7] = '7'; EBCDIC_TO_ASCII[0xF8] = '8';
        EBCDIC_TO_ASCII[0xF9] = '9';

        // Build reverse (ASCII to EBCDIC) table
        for (int i = 0; i < 256; i++) {
            char ascii = EBCDIC_TO_ASCII[i];
            if (ascii != ' ' || i == 0x40) {
                int asciiIdx = (int) ascii;
                if (asciiIdx < 256) {
                    ASCII_TO_EBCDIC[asciiIdx] = (byte) i;
                }
            }
        }
        // Key ASCII to EBCDIC overrides
        ASCII_TO_EBCDIC[' ']  = 0x40;
        ASCII_TO_EBCDIC['.']  = 0x4B;
        ASCII_TO_EBCDIC['<']  = 0x4C;
        ASCII_TO_EBCDIC['(']  = 0x4D;
        ASCII_TO_EBCDIC['+']  = 0x4E;
        ASCII_TO_EBCDIC['!']  = 0x5A;
        ASCII_TO_EBCDIC['$']  = 0x5B;
        ASCII_TO_EBCDIC['*']  = 0x5C;
        ASCII_TO_EBCDIC[')']  = 0x5D;
        ASCII_TO_EBCDIC[';']  = 0x5E;
        ASCII_TO_EBCDIC['-']  = 0x60;
        ASCII_TO_EBCDIC['/']  = 0x61;
        ASCII_TO_EBCDIC[',']  = 0x6B;
        ASCII_TO_EBCDIC['%']  = 0x6C;
        ASCII_TO_EBCDIC['_']  = 0x6D;
        ASCII_TO_EBCDIC['>']  = 0x6E;
        ASCII_TO_EBCDIC['?']  = 0x6F;
        ASCII_TO_EBCDIC[':']  = 0x7A;
        ASCII_TO_EBCDIC['#']  = 0x7B;
        ASCII_TO_EBCDIC['@']  = 0x7C;
        ASCII_TO_EBCDIC['\''] = 0x7D;
        ASCII_TO_EBCDIC['=']  = 0x7E;
        ASCII_TO_EBCDIC['"']  = 0x7F;
        ASCII_TO_EBCDIC['&']  = 0x50;
    }

    public static char ebcdicToAscii(byte ebcdic) {
        return EBCDIC_TO_ASCII[ebcdic & 0xFF];
    }

    public static byte asciiToEbcdic(char ascii) {
        if (ascii < 256) return ASCII_TO_EBCDIC[ascii];
        return 0x3F;
    }
}
