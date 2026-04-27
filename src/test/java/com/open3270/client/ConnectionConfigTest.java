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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionConfigTest {

    @Test
    void defaultValues() {
        ConnectionConfig cfg = new ConnectionConfig();
        assertEquals("IBM-3278-2-E", cfg.getTermType());
        assertEquals(23, cfg.getHostPort());
        assertFalse(cfg.isUseSSL());
        assertFalse(cfg.isRefuseTN3270E());
        assertEquals(40000, cfg.getDefaultTimeout());
    }

    @Test
    void settersAndGetters() {
        ConnectionConfig cfg = new ConnectionConfig();
        cfg.setHostName("mainframe.example.com");
        cfg.setHostPort(1234);
        cfg.setHostLU("LU001");
        cfg.setTermType("IBM-3279-3-E");
        cfg.setUseSSL(true);

        assertEquals("mainframe.example.com", cfg.getHostName());
        assertEquals(1234, cfg.getHostPort());
        assertEquals("LU001", cfg.getHostLU());
        assertEquals("IBM-3279-3-E", cfg.getTermType());
        assertTrue(cfg.isUseSSL());
    }

    @Test
    void termTypeNeverNull() {
        ConnectionConfig cfg = new ConnectionConfig();
        cfg.setTermType(null);
        assertNotNull(cfg.getTermType());
        assertEquals("IBM-3278-2-E", cfg.getTermType());
    }
}
