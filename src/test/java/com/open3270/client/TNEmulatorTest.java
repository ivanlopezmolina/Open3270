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

import com.open3270.client.tn3270.TnKey;
import com.open3270.client.tn3270.x3270.KeyboardConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TNEmulator that do not require a live host.
 */
class TNEmulatorTest {

    @Test
    void constructorSetsDefaultConfig() {
        TNEmulator em = new TNEmulator();
        assertNotNull(em.getConfig());
        assertEquals("IBM-3278-2-E", em.getConfig().getTermType());
    }

    @Test
    void notConnectedByDefault() {
        TNEmulator em = new TNEmulator();
        assertFalse(em.isConnected());
    }

    @Test
    void keyboardLockedWhenNotConnected() {
        TNEmulator em = new TNEmulator();
        int lock = em.getKeyboardLock();
        assertTrue((lock & KeyboardConstants.NOT_CONNECTED) != 0,
                   "Keyboard should be locked (NOT_CONNECTED) before connecting");
    }

    @Test
    void getScreenReturnsEmptyScreenWhenNotConnected() {
        TNEmulator em = new TNEmulator();
        assertNotNull(em.getScreen());
        assertNotNull(em.getCurrentScreen());
    }

    @Test
    void getTextReturnsEmptyWhenNotConnected() {
        TNEmulator em = new TNEmulator();
        assertEquals("", em.getText(1, 1, 10));
    }

    @Test
    void closeIsIdempotent() {
        TNEmulator em = new TNEmulator();
        em.close();
        em.close(); // should not throw
    }

    @Test
    void configCanBeCustomised() {
        ConnectionConfig cfg = new ConnectionConfig();
        cfg.setHostName("test.example.com");
        cfg.setHostPort(9999);
        TNEmulator em = new TNEmulator(cfg);
        assertEquals("test.example.com", em.getConfig().getHostName());
        assertEquals(9999, em.getConfig().getHostPort());
    }

    @Test
    void waitForConnectReturnsFalseWhenNotConnected() {
        TNEmulator em = new TNEmulator();
        assertFalse(em.waitForConnect(100));
    }

    @Test
    void autoCloseWithTryWithResources() {
        assertDoesNotThrow(() -> {
            try (TNEmulator em = new TNEmulator()) {
                assertFalse(em.isConnected());
            }
        });
    }
}
