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
package com.open3270.client;

import com.open3270.client.exceptions.TNHostException;
import com.open3270.client.interfaces.IAudit;
import com.open3270.client.interfaces.IXMLScreen;
import com.open3270.client.tn3270.*;
import com.open3270.client.tn3270.x3270.AID;
import com.open3270.client.tn3270.x3270.ControllerConstant;
import com.open3270.client.tn3270.x3270.KeyboardConstants;

/**
 * Main public API for Open3270. Create one instance per connection.
 *
 * <pre>{@code
 * try (TNEmulator em = new TNEmulator()) {
 *     em.connect("mainframe.example.com", 23);
 *     em.waitForConnect(5000);
 *     System.out.println(em.getScreen().dump());
 *     em.sendEnter();
 * }
 * }</pre>
 */
public class TNEmulator implements AutoCloseable {

    private final ConnectionConfig config;
    private IAudit audit;
    private TN3270API api;

    public TNEmulator() {
        this.config = new ConnectionConfig();
    }

    public TNEmulator(ConnectionConfig config) {
        this.config = config != null ? config : new ConnectionConfig();
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Connect to a TN3270 host using the current configuration.
     *
     * @param host hostname or IP address
     * @param port TCP port (usually 23)
     * @throws TNHostException if the connection attempt fails
     */
    public void connect(String host, int port) throws TNHostException {
        api = new TN3270API(config, audit);
        api.connect(host, port);
    }

    /**
     * Connect using an LU name (TN3270E).
     */
    public void connect(String host, int port, String lu) throws TNHostException {
        config.setHostLU(lu);
        connect(host, port);
    }

    public void disconnect() {
        if (api != null) api.disconnect();
    }

    public boolean isConnected() {
        return api != null && api.isConnected();
    }

    @Override
    public void close() {
        disconnect();
    }

    // ── waiting ───────────────────────────────────────────────────────────────

    /** Block until the terminal is connected (keyboard NOT_CONNECTED cleared). */
    public boolean waitForConnect(int timeoutMs) {
        return api != null && api.waitForConnect(timeoutMs);
    }

    /** Block until the keyboard is fully unlocked. */
    public boolean waitForUnlock(int timeoutMs) {
        return api != null && api.waitForUnlock(timeoutMs);
    }

    /**
     * Wait until the screen contains one of the given text strings.
     *
     * @param texts    strings to look for
     * @param timeoutMs maximum time to wait in milliseconds
     * @return index of the first matched string, or -1 on timeout
     */
    public int waitForText(String[] texts, int timeoutMs) {
        if (api == null || texts == null) return -1;
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String screen = api.getAllStringData();
            for (int i = 0; i < texts.length; i++) {
                if (texts[i] != null && screen.contains(texts[i])) return i;
            }
            try { Thread.sleep(100); } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); break;
            }
        }
        return -1;
    }

    public boolean waitForText(String text, int timeoutMs) {
        return waitForText(new String[]{text}, timeoutMs) >= 0;
    }

    // ── screen reading ────────────────────────────────────────────────────────

    /** Return the current screen as an IXMLScreen. */
    public IXMLScreen getScreen() {
        return api == null ? new XMLScreen(80, 24) : api.getXMLScreen();
    }

    /** Return the current screen as an XMLScreen. */
    public XMLScreen getCurrentScreen() {
        return api == null ? new XMLScreen(80, 24) : api.getXMLScreen();
    }

    /**
     * Get text at a 1-based position.
     *
     * @param row    1-based row
     * @param col    1-based column
     * @param length number of characters
     */
    public String getText(int row, int col, int length) {
        if (api == null) return "";
        return api.getText(col - 1, row - 1, length);
    }

    /** Get text using 0-based coordinates. */
    public String getTextZero(int x, int y, int length) {
        return api == null ? "" : api.getText(x, y, length);
    }

    public String getRow(int row) {
        return api == null ? "" : api.getRow(row);
    }

    public String getAllText() {
        return api == null ? "" : api.getAllStringData();
    }

    // ── keyboard input ────────────────────────────────────────────────────────

    public boolean sendKey(TnKey key) {
        if (api == null) return false;
        byte aidByte = aidForKey(key);
        return api.sendKey(aidByte);
    }

    public boolean sendEnter() {
        return sendKey(TnKey.Enter);
    }

    public boolean sendClear() {
        return sendKey(TnKey.Clear);
    }

    public boolean sendText(String text) {
        return api != null && api.sendText(text);
    }

    public boolean moveCursor(int x, int y) {
        return api != null && api.moveCursor(CursorOp.Exact, x, y);
    }

    public int getCursorX() {
        return api == null ? 0 : api.getCursorX();
    }

    public int getCursorY() {
        return api == null ? 0 : api.getCursorY();
    }

    public int getKeyboardLock() {
        return api == null ? KeyboardConstants.NOT_CONNECTED : api.getKeyboardLock();
    }

    public byte getFieldAttribute(int x, int y) {
        return api == null ? ControllerConstant.FA_BASE : api.getFieldAttribute(x, y);
    }

    // ── configuration ─────────────────────────────────────────────────────────

    public ConnectionConfig getConfig() { return config; }

    public void setAudit(IAudit audit) { this.audit = audit; }

    // ── private helpers ───────────────────────────────────────────────────────

    private byte aidForKey(TnKey key) {
        return switch (key) {
            case Enter  -> AID.ENTER;
            case Clear  -> AID.CLEAR;
            case PA1    -> AID.PA1;
            case PA2    -> AID.PA2;
            case PA3    -> AID.PA3;
            case F1     -> AID.F1;
            case F2     -> AID.F2;
            case F3     -> AID.F3;
            case F4     -> AID.F4;
            case F5     -> AID.F5;
            case F6     -> AID.F6;
            case F7     -> AID.F7;
            case F8     -> AID.F8;
            case F9     -> AID.F9;
            case F10    -> AID.F10;
            case F11    -> AID.F11;
            case F12    -> AID.F12;
            case F13    -> AID.F13;
            case F14    -> AID.F14;
            case F15    -> AID.F15;
            case F16    -> AID.F16;
            case F17    -> AID.F17;
            case F18    -> AID.F18;
            case F19    -> AID.F19;
            case F20    -> AID.F20;
            case F21    -> AID.F21;
            case F22    -> AID.F22;
            case F23    -> AID.F23;
            case F24    -> AID.F24;
            case SysReq -> AID.SYSREQ;
            default     -> AID.ENTER;
        };
    }
}
