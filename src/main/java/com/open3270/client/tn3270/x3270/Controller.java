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

/**
 * 3270 screen buffer controller. Processes the 3270 data stream and maintains
 * the current screen state.
 */
public class Controller {

    private byte[] screenBuffer;
    private int rowCount    = 24;
    private int columnCount = 80;
    private int cursorAddress = 0;
    private int bufferAddress = 0;
    private boolean isFormatted   = false;
    private boolean screenChanged = false;
    private Telnet telnet;
    private Appres appres;
    private TNTrace trace;

    public Controller(Telnet telnet, Appres appres, TNTrace trace) {
        this.telnet  = telnet;
        this.appres  = appres;
        this.trace   = trace;
        initBuffer(24, 80);
    }

    private void initBuffer(int rows, int cols) {
        this.rowCount    = rows;
        this.columnCount = cols;
        this.screenBuffer = new byte[rows * cols];
        eraseScreen();
    }

    /** Fill screen buffer with EBCDIC space (0x40). */
    public void eraseScreen() {
        java.util.Arrays.fill(screenBuffer, (byte) 0x40);
        isFormatted   = false;
        screenChanged = true;
    }

    /**
     * Process a complete 3270 data stream record.
     * @param data   raw bytes (may include TN3270E header already stripped)
     * @param length number of valid bytes in data
     */
    public void processDataStream(byte[] data, int length) {
        if (length == 0) return;

        int cmd = data[0] & 0xFF;
        int pos  = 1;

        // Normalise SNA command codes
        if (cmd == ControllerConstant.SNA_CMD_W)   cmd = ControllerConstant.CMD_W;
        else if (cmd == ControllerConstant.SNA_CMD_EW)  cmd = ControllerConstant.CMD_EW;
        else if (cmd == ControllerConstant.SNA_CMD_EWA) cmd = ControllerConstant.CMD_EWA;
        else if (cmd == ControllerConstant.SNA_CMD_RB)  cmd = ControllerConstant.CMD_RB;
        else if (cmd == ControllerConstant.SNA_CMD_RM)  cmd = ControllerConstant.CMD_RM;
        else if (cmd == ControllerConstant.SNA_CMD_RMA) cmd = ControllerConstant.CMD_RMA;
        else if (cmd == ControllerConstant.SNA_CMD_WSF) cmd = ControllerConstant.CMD_WSF;
        else if (cmd == ControllerConstant.SNA_CMD_EAU) cmd = ControllerConstant.CMD_EAU;

        switch (cmd) {
            case ControllerConstant.CMD_EW:
            case ControllerConstant.CMD_EWA:
                eraseScreen();
                if (pos < length) pos++; // skip WCC
                processOrders(data, pos, length);
                break;

            case ControllerConstant.CMD_W:
                if (pos < length) pos++; // skip WCC
                processOrders(data, pos, length);
                break;

            case ControllerConstant.CMD_EAU:
                eraseAllUnprotected();
                break;

            case ControllerConstant.CMD_WSF:
                // Structured fields – minimal handling
                break;

            case ControllerConstant.CMD_RB:
            case ControllerConstant.CMD_RM:
            case ControllerConstant.CMD_RMA:
            case ControllerConstant.CMD_NOP:
            default:
                break;
        }
        screenChanged = true;
    }

    private void processOrders(byte[] data, int start, int length) {
        int i = start;
        bufferAddress = 0;

        while (i < length) {
            int b = data[i] & 0xFF;

            switch (b) {
                case ControllerConstant.ORDER_SBA: {
                    if (i + 2 >= length) { i = length; break; }
                    bufferAddress = decodeBfrAddr(data[i + 1], data[i + 2]);
                    i += 3;
                    break;
                }
                case ControllerConstant.ORDER_IC: {
                    cursorAddress = bufferAddress;
                    i++;
                    break;
                }
                case ControllerConstant.ORDER_SF: {
                    if (i + 1 >= length) { i = length; break; }
                    byte fa = data[i + 1];
                    if (bufferAddress < screenBuffer.length) {
                        screenBuffer[bufferAddress] = fa;
                    }
                    isFormatted = true;
                    bufferAddress = incrementAddress(bufferAddress);
                    i += 2;
                    break;
                }
                case ControllerConstant.ORDER_SFE: {
                    if (i + 1 >= length) { i = length; break; }
                    int pairCount = data[i + 1] & 0xFF;
                    // First attribute pair (type/value) is the FA
                    byte fa = 0;
                    if (pairCount > 0 && i + 3 < length) {
                        fa = data[i + 3]; // value byte of first pair
                    }
                    if (bufferAddress < screenBuffer.length) {
                        screenBuffer[bufferAddress] = fa;
                    }
                    isFormatted = true;
                    bufferAddress = incrementAddress(bufferAddress);
                    i += 2 + pairCount * 2;
                    break;
                }
                case ControllerConstant.ORDER_SA: {
                    i += 3; // skip type + value
                    break;
                }
                case ControllerConstant.ORDER_RA: {
                    if (i + 3 >= length) { i = length; break; }
                    int stopAddr = decodeBfrAddr(data[i + 1], data[i + 2]);
                    byte fillChar = data[i + 3];
                    fillRange(bufferAddress, stopAddr, fillChar);
                    bufferAddress = stopAddr;
                    i += 4;
                    break;
                }
                case ControllerConstant.ORDER_EUA: {
                    if (i + 2 >= length) { i = length; break; }
                    int stopAddr = decodeBfrAddr(data[i + 1], data[i + 2]);
                    eraseUnprotectedRange(bufferAddress, stopAddr);
                    bufferAddress = stopAddr;
                    i += 3;
                    break;
                }
                case ControllerConstant.ORDER_GE: {
                    if (i + 1 >= length) { i = length; break; }
                    // Graphic escape character – store as-is
                    if (bufferAddress < screenBuffer.length) {
                        screenBuffer[bufferAddress] = data[i + 1];
                    }
                    bufferAddress = incrementAddress(bufferAddress);
                    i += 2;
                    break;
                }
                case ControllerConstant.ORDER_PT:
                    i++;
                    break;

                case ControllerConstant.ORDER_MF: {
                    if (i + 1 >= length) { i = length; break; }
                    int pairCount = data[i + 1] & 0xFF;
                    i += 2 + pairCount * 2;
                    break;
                }
                default: {
                    // Data byte
                    if (bufferAddress < screenBuffer.length) {
                        screenBuffer[bufferAddress] = (byte) b;
                    }
                    bufferAddress = incrementAddress(bufferAddress);
                    i++;
                    break;
                }
            }
        }
    }

    /** Decode a 12-bit buffer address from two EBCDIC code-table bytes. */
    private int decodeBfrAddr(byte b1, byte b2) {
        int hi = b1 & 0x3F;
        int lo = b2 & 0x3F;
        return (hi << 6) | lo;
    }

    private int incrementAddress(int addr) {
        addr++;
        if (addr >= screenBuffer.length) addr = 0;
        return addr;
    }

    private void fillRange(int start, int stop, byte fillChar) {
        int addr = start;
        while (addr != stop) {
            if (addr < screenBuffer.length) screenBuffer[addr] = fillChar;
            addr = incrementAddress(addr);
        }
    }

    private void eraseUnprotectedRange(int start, int stop) {
        int addr = start;
        while (addr != stop) {
            if (addr < screenBuffer.length) {
                byte fa = getFieldAttribute(addr);
                if (!FieldAttribute.isProtected(fa)) {
                    screenBuffer[addr] = (byte) 0x40; // EBCDIC space
                }
            }
            addr = incrementAddress(addr);
        }
    }

    private void eraseAllUnprotected() {
        for (int i = 0; i < screenBuffer.length; i++) {
            byte fa = getFieldAttribute(i);
            if (!FieldAttribute.isProtected(fa)) {
                screenBuffer[i] = (byte) 0x40;
            }
        }
    }

    /** Find the field attribute (FA byte) that governs the given buffer address. */
    public byte getFieldAttribute(int addr) {
        if (screenBuffer == null) return ControllerConstant.FA_BASE;
        int a = addr;
        for (int i = 0; i < screenBuffer.length; i++) {
            a--;
            if (a < 0) a = screenBuffer.length - 1;
            if (FieldAttribute.isFA(screenBuffer[a])) return screenBuffer[a];
        }
        return ControllerConstant.FA_BASE;
    }

    /** Return the address of the next unprotected field after the given address. */
    public int getNextUnprotectedField(int from) {
        int a = from;
        for (int i = 0; i < screenBuffer.length; i++) {
            a = incrementAddress(a);
            if (FieldAttribute.isFA(screenBuffer[a]) && !FieldAttribute.isProtected(screenBuffer[a])) {
                return incrementAddress(a); // position after the FA byte
            }
        }
        return -1;
    }

    public String getText(int offset, int length) {
        if (screenBuffer == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int idx = offset + i;
            if (idx >= screenBuffer.length) break;
            sb.append(CharacterGenerator.ebcdicToAscii(screenBuffer[idx]));
        }
        return sb.toString();
    }

    public String getText(int x, int y, int length) {
        return getText(y * columnCount + x, length);
    }

    public String getRow(int row) {
        return getText(row * columnCount, columnCount);
    }

    public String getAllText(boolean crlf) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rowCount; r++) {
            sb.append(getRow(r));
            if (crlf) sb.append("\r\n");
        }
        return sb.toString();
    }

    public boolean moveCursor(CursorOp op, int x, int y) {
        switch (op) {
            case Exact:
                cursorAddress = y * columnCount + x;
                return true;
            case NearestUnprotectedField: {
                int start = y * columnCount + x;
                int next = getNextUnprotectedField(start);
                if (next >= 0) { cursorAddress = next; return true; }
                return false;
            }
            case Tab: {
                int next = getNextUnprotectedField(cursorAddress);
                if (next >= 0) { cursorAddress = next; return true; }
                return false;
            }
            default:
                return false;
        }
    }

    public void setCursorAddress(int addr) { this.cursorAddress = addr; }
    public int getCursorAddress() { return cursorAddress; }
    public int getCursorX() { return cursorAddress % columnCount; }
    public int getCursorY() { return cursorAddress / columnCount; }
    public int getRowCount() { return rowCount; }
    public int getColumnCount() { return columnCount; }
    public boolean isFormatted() { return isFormatted; }
    public boolean isScreenChanged() { return screenChanged; }
    public void clearScreenChanged() { screenChanged = false; }

    /** Build an XMLScreen snapshot from the current buffer state. */
    public XMLScreen getXMLScreen() {
        XMLScreen screen = new XMLScreen(columnCount, rowCount);
        screen.buildFromBuffer(screenBuffer, columnCount, rowCount, getCursorX(), getCursorY());
        screen.setFormatted(isFormatted);
        return screen;
    }

    public void setDimensions(int cols, int rows) {
        if (cols != columnCount || rows != rowCount) {
            initBuffer(rows, cols);
        }
    }

    public byte[] getScreenBuffer() { return screenBuffer; }
}
