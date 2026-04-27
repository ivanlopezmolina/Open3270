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

import com.open3270.client.tn3270.x3270.ControllerConstant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ControllerConstantTest {

    @Test
    void faBaseValue() {
        assertEquals((byte) 0xC0, ControllerConstant.FA_BASE);
    }

    @Test
    void faMaskValue() {
        assertEquals((byte) 0xD0, ControllerConstant.FA_MASK);
    }

    @Test
    void codeTableSize() {
        assertEquals(64, ControllerConstant.CodeTable.length);
    }

    @Test
    void codeTableFirstEntry() {
        // First entry (index 0) should be 0x40
        assertEquals((byte) 0x40, ControllerConstant.CodeTable[0]);
    }

    @Test
    void commandValues() {
        assertEquals(0x01, ControllerConstant.CMD_W);
        assertEquals(0x05, ControllerConstant.CMD_EW);
        assertEquals(0x0d, ControllerConstant.CMD_EWA);
    }

    @Test
    void orderValues() {
        assertEquals(0x11, ControllerConstant.ORDER_SBA);
        assertEquals(0x13, ControllerConstant.ORDER_IC);
        assertEquals(0x1d, ControllerConstant.ORDER_SF);
    }
}
