/*
 * MIT License
 *
 * Copyright (c) 2026 ivanlopezmolina
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
package com.open3270.client;

import com.open3270.client.tn3270.x3270.AID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AIDTest {

    @Test
    void enterKey() {
        assertEquals((byte) 0x7d, AID.ENTER);
    }

    @Test
    void clearKey() {
        assertEquals((byte) 0x6d, AID.CLEAR);
    }

    @Test
    void paKeys() {
        assertEquals((byte) 0x6c, AID.PA1);
        assertEquals((byte) 0x6e, AID.PA2);
        assertEquals((byte) 0x6b, AID.PA3);
    }

    @Test
    void f1Key() {
        assertEquals((byte) 0xf1, AID.F1);
    }

    @Test
    void f10Key() {
        assertEquals((byte) 0x7a, AID.F10);
    }

    @Test
    void noAid() {
        assertEquals((byte) 0x60, AID.NONE);
    }
}
