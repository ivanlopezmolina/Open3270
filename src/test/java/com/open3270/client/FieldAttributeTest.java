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

import com.open3270.client.tn3270.x3270.FieldAttribute;
import com.open3270.client.tn3270.x3270.ControllerConstant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldAttributeTest {

    @Test
    void faBaseIsFieldAttribute() {
        assertTrue(FieldAttribute.isFA(ControllerConstant.FA_BASE));
    }

    @Test
    void protectedFlag() {
        byte prot = (byte)(ControllerConstant.FA_BASE | ControllerConstant.FA_PROTECT);
        assertTrue(FieldAttribute.isFA(prot));
        assertTrue(FieldAttribute.isProtected(prot));
    }

    @Test
    void unprotectedFlag() {
        byte unprot = ControllerConstant.FA_BASE; // no PROTECT bit
        assertFalse(FieldAttribute.isProtected(unprot));
    }

    @Test
    void modifiedFlag() {
        byte mod = (byte)(ControllerConstant.FA_BASE | ControllerConstant.FA_MODIFY);
        assertTrue(FieldAttribute.isModified(mod));
        assertFalse(FieldAttribute.isModified(ControllerConstant.FA_BASE));
    }

    @Test
    void numericFlag() {
        byte num = (byte)(ControllerConstant.FA_BASE | ControllerConstant.FA_NUMERIC);
        assertTrue(FieldAttribute.isNumeric(num));
        assertFalse(FieldAttribute.isNumeric(ControllerConstant.FA_BASE));
    }

    @Test
    void regularDataByteIsNotFA() {
        // 0x41 = 'A' in EBCDIC, not a field attribute
        assertFalse(FieldAttribute.isFA((byte) 0x41));
        // 0x40 = EBCDIC space, not a field attribute (high nibble 0x4 != 0xC)
        assertFalse(FieldAttribute.isFA((byte) 0x40));
    }
}
