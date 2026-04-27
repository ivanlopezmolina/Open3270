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
package com.open3270.client.tn3270;

import com.open3270.client.ConnectionConfig;
import com.open3270.client.exceptions.TNHostException;
import com.open3270.client.interfaces.IAudit;
import com.open3270.client.tn3270.x3270.*;

/**
 * Mid-level API wrapping the Telnet/Controller/Keyboard triad.
 * Provides simple operations required by TNEmulator.
 */
public class TN3270API {

    private Telnet     telnet;
    private Appres     appres;
    private TNTrace    trace;
    private Events     events;
    private ConnectionConfig config;

    public TN3270API(ConnectionConfig config, IAudit audit) {
        this.config = config;
        this.appres = new Appres();
        this.trace  = new TNTrace(audit);
        this.events = new Events();
        this.telnet = new Telnet(config, appres, trace, events);
    }

    // ── connection ────────────────────────────────────────────────────────────

    public void connect(String host, int port) throws TNHostException {
        telnet.connect(host, port);
    }

    public void disconnect() {
        if (telnet != null) telnet.disconnect();
    }

    public boolean isConnected() {
        return telnet != null && telnet.isConnected();
    }

    public boolean waitForConnect(int timeoutMs) {
        return telnet != null && telnet.waitForConnect(timeoutMs);
    }

    public boolean waitForUnlock(int timeoutMs) {
        return telnet != null && telnet.waitForUnlock(timeoutMs);
    }

    // ── screen reading ────────────────────────────────────────────────────────

    public String getText(int x, int y, int length) {
        Controller c = controller();
        return c == null ? "" : c.getText(x, y, length);
    }

    public String getRow(int row) {
        Controller c = controller();
        return c == null ? "" : c.getRow(row);
    }

    public String getAllStringData() {
        Controller c = controller();
        return c == null ? "" : c.getAllText(true);
    }

    public XMLScreen getXMLScreen() {
        Controller c = controller();
        return c == null ? new XMLScreen(80, 24) : c.getXMLScreen();
    }

    // ── keyboard ─────────────────────────────────────────────────────────────

    public boolean sendKey(byte aidByte) {
        Keyboard kb = keyboard();
        return kb != null && kb.handleAttentionIdentifierKey(aidByte);
    }

    public boolean sendText(String text) {
        if (text == null || text.isEmpty()) return true;
        Keyboard kb = keyboard();
        if (kb == null) return false;
        for (char c : text.toCharArray()) {
            if (!kb.handleOrdinaryCharacter(c, false, false)) return false;
        }
        return true;
    }

    public boolean moveCursor(CursorOp op, int x, int y) {
        Controller c = controller();
        return c != null && c.moveCursor(op, x, y);
    }

    public int getCursorX() {
        Controller c = controller();
        return c == null ? 0 : c.getCursorX();
    }

    public int getCursorY() {
        Controller c = controller();
        return c == null ? 0 : c.getCursorY();
    }

    public int getKeyboardLock() {
        Keyboard kb = keyboard();
        return kb == null ? KeyboardConstants.NOT_CONNECTED : kb.getKeyboardLock();
    }

    public byte getFieldAttribute(int x, int y) {
        Controller c = controller();
        if (c == null) return ControllerConstant.FA_BASE;
        return c.getFieldAttribute(y * (c.getColumnCount()) + x);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Controller controller() {
        return telnet == null ? null : telnet.getController();
    }

    private Keyboard keyboard() {
        return telnet == null ? null : telnet.getKeyboard();
    }

    public Telnet getTelnet() { return telnet; }
}
