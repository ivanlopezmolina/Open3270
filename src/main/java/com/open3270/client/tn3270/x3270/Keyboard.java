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

import com.open3270.client.tn3270.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles keyboard input and sends it to the 3270 host.
 */
public class Keyboard {

    public volatile int keyboardLock = KeyboardConstants.NOT_CONNECTED;

    private Telnet telnet;
    private TNTrace trace;
    private Controller controller;

    public Keyboard(Telnet telnet, TNTrace trace, Controller controller) {
        this.telnet     = telnet;
        this.trace      = trace;
        this.controller = controller;
    }

    /** Send an AID key (Enter, Fn, PAn, Clear, SysReq) to the host. */
    public boolean handleAttentionIdentifierKey(byte aidByte) {
        if ((keyboardLock & ~KeyboardConstants.NOT_CONNECTED) != 0) {
            if (trace != null) trace.writeLine("Keyboard locked, cannot send AID");
            return false;
        }
        try {
            sendAid(aidByte);
            return true;
        } catch (IOException e) {
            if (trace != null) trace.writeLine("Error sending AID: " + e.getMessage());
            return false;
        }
    }

    /** Type a regular character into the current cursor position. */
    public boolean handleOrdinaryCharacter(char c, boolean ge, boolean paste) {
        if ((keyboardLock & ~KeyboardConstants.NOT_CONNECTED) != 0) {
            return false;
        }
        if (controller != null) {
            int addr = controller.getCursorAddress();
            byte fa  = controller.getFieldAttribute(addr);
            if (FieldAttribute.isProtected(fa)) {
                return false;
            }
            byte ebcdic = CharacterGenerator.asciiToEbcdic(c);
            controller.getScreenBuffer()[addr] = ebcdic;
            controller.setCursorAddress(addr + 1);
        }
        return true;
    }

    private void sendAid(byte aidByte) throws IOException {
        if (telnet == null) return;

        // Build the modified fields data
        List<Byte> data = new ArrayList<>();
        data.add(aidByte);

        // Encode cursor address
        int cursor = controller != null ? controller.getCursorAddress() : 0;
        addBufferAddress(data, cursor);

        // Append modified fields content
        if (controller != null) {
            byte[] screen = controller.getScreenBuffer();
            int size = screen.length;
            int addr  = 0;
            // walk fields looking for modified ones
            for (int i = 0; i < size; i++) {
                if (FieldAttribute.isFA(screen[i]) && FieldAttribute.isModified(screen[i])) {
                    // SBA + address
                    data.add((byte) ControllerConstant.ORDER_SBA);
                    addBufferAddress(data, i + 1); // start of field data
                    // field content until next FA or end
                    int j = i + 1;
                    while (j < size && !FieldAttribute.isFA(screen[j])) {
                        data.add(screen[j]);
                        j++;
                    }
                }
            }
        }

        byte[] buf = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) buf[i] = data.get(i);
        telnet.sendData(buf, buf.length);
    }

    private void addBufferAddress(List<Byte> buf, int addr) {
        byte[] codeTable = ControllerConstant.CodeTable;
        buf.add(codeTable[(addr >> 6) & 0x3F]);
        buf.add(codeTable[addr & 0x3F]);
    }

    /** Unlock the keyboard (clear non-error lock bits). */
    public void unlock() {
        keyboardLock &= KeyboardConstants.ERROR_MASK;
        if (telnet != null) {
            synchronized (telnet.getConnectionLock()) {
                telnet.getConnectionLock().notifyAll();
            }
        }
    }

    public int getKeyboardLock() { return keyboardLock; }
    public void setKeyboardLock(int lock) { this.keyboardLock = lock; }
}
