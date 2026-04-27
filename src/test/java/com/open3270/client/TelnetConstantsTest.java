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

import com.open3270.client.tn3270.TelnetConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TelnetConstantsTest {

    @Test
    void iacValue() {
        assertEquals((byte) 255, TelnetConstants.IAC);
    }

    @Test
    void doValue() {
        assertEquals((byte) 253, TelnetConstants.DO);
    }

    @Test
    void tn3270eOption() {
        assertEquals(40, TelnetConstants.TELOPT_TN3270E);
    }

    @Test
    void ttypeOption() {
        assertEquals(24, TelnetConstants.TELOPT_TTYPE);
    }

    @Test
    void eorOption() {
        assertEquals(25, TelnetConstants.TELOPT_EOR);
    }

    @Test
    void binaryOption() {
        assertEquals(0, TelnetConstants.TELOPT_BINARY);
    }

    @Test
    void telqualValues() {
        assertEquals(0, TelnetConstants.TELQUAL_IS);
        assertEquals(1, TelnetConstants.TELQUAL_SEND);
    }
}
