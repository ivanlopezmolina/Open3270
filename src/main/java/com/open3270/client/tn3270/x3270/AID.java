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

public final class AID {
    private AID() {}

    public static final byte NONE     = 0x60;
    public static final byte QREPLY   = 0x61;
    public static final byte ENTER    = 0x7d;
    public static final byte F1       = (byte) 0xf1;
    public static final byte F2       = (byte) 0xf2;
    public static final byte F3       = (byte) 0xf3;
    public static final byte F4       = (byte) 0xf4;
    public static final byte F5       = (byte) 0xf5;
    public static final byte F6       = (byte) 0xf6;
    public static final byte F7       = (byte) 0xf7;
    public static final byte F8       = (byte) 0xf8;
    public static final byte F9       = (byte) 0xf9;
    public static final byte F10      = 0x7a;
    public static final byte F11      = 0x7b;
    public static final byte F12      = 0x7c;
    public static final byte F13      = (byte) 0xc1;
    public static final byte F14      = (byte) 0xc2;
    public static final byte F15      = (byte) 0xc3;
    public static final byte F16      = (byte) 0xc4;
    public static final byte F17      = (byte) 0xc5;
    public static final byte F18      = (byte) 0xc6;
    public static final byte F19      = (byte) 0xc7;
    public static final byte F20      = (byte) 0xc8;
    public static final byte F21      = (byte) 0xc9;
    public static final byte F22      = 0x4a;
    public static final byte F23      = 0x4b;
    public static final byte F24      = 0x4c;
    public static final byte OICR     = (byte) 0xe6;
    public static final byte MSR_MHS  = (byte) 0xe7;
    public static final byte SELECT   = 0x7e;
    public static final byte PA1      = 0x6c;
    public static final byte PA2      = 0x6e;
    public static final byte PA3      = 0x6b;
    public static final byte CLEAR    = 0x6d;
    public static final byte SYSREQ   = (byte) 0xf0;
    public static final byte SF       = (byte) 0x88;
    public static final byte SF_QREPLY = (byte) 0x81;
}
