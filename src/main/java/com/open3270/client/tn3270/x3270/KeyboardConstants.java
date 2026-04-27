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

public final class KeyboardConstants {
    private KeyboardConstants() {}

    public static final int ERROR_MASK        = 0x000f;
    public static final int ERROR_PROTECTED   = 1;
    public static final int ERROR_NUMERIC     = 2;
    public static final int ERROR_OVERFLOW    = 3;
    public static final int NOT_CONNECTED     = 0x0010;
    public static final int AWAITING_FIRST    = 0x0020;
    public static final int OIA_TWAIT        = 0x0040;
    public static final int OIA_LOCKED       = 0x0080;
    public static final int DEFERRED_UNLOCK   = 0x0100;
    public static final int ENTER_INHIBIT     = 0x0200;
    public static final int SCROLLED          = 0x0400;
    public static final int OIA_MINUS        = 0x0800;

    public static final int NO_SYMBOL = 0;
    public static final int W_FLAG    = 0x100;
    public static final int PASTE_W_FLAG = 0x200;
    public static final int UNLOCK_MS = 350;

    public static final byte[] PfTranslation = new byte[] {
        AID.F1,  AID.F2,  AID.F3,  AID.F4,  AID.F5,  AID.F6,
        AID.F7,  AID.F8,  AID.F9,  AID.F10, AID.F11, AID.F12,
        AID.F13, AID.F14, AID.F15, AID.F16, AID.F17, AID.F18,
        AID.F19, AID.F20, AID.F21, AID.F22, AID.F23, AID.F24
    };

    public static final byte[] PaTranslation = new byte[] {
        AID.PA1, AID.PA2, AID.PA3
    };
}
