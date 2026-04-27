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

import com.open3270.client.interfaces.IAudit;
import com.open3270.client.interfaces.IXMLScreen;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the current 3270 screen state as an XML-describable structure.
 */
public class XMLScreen implements IXMLScreen {

    private XMLScreenField[] fields;
    private XMLUnformattedScreen unformatted;
    private boolean formatted;
    private char[] screenBuffer;
    private int cx = 80;
    private int cy = 24;
    private UUID screenGuid = UUID.randomUUID();
    private String hash;
    private String name;
    private String matchListIdentified;
    private String cachedXml;

    public XMLScreen() {}

    public XMLScreen(int cols, int rows) {
        this.cx = cols;
        this.cy = rows;
        this.screenBuffer = new char[cols * rows];
        java.util.Arrays.fill(screenBuffer, ' ');
    }

    /** Build this screen from an EBCDIC screen buffer provided by the Controller. */
    public void buildFromBuffer(byte[] ebcdicBuffer, int cols, int rows,
                                int cursorX, int cursorY) {
        this.cx = cols;
        this.cy = rows;
        this.screenBuffer = new char[cols * rows];
        String[] rowStrings = new String[rows];
        for (int i = 0; i < ebcdicBuffer.length && i < screenBuffer.length; i++) {
            byte b = ebcdicBuffer[i];
            char ch = com.open3270.client.tn3270.x3270.CharacterGenerator.ebcdicToAscii(b);
            screenBuffer[i] = ch;
        }
        for (int r = 0; r < rows; r++) {
            StringBuilder sb = new StringBuilder(cols);
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx < screenBuffer.length) sb.append(screenBuffer[idx]);
                else sb.append(' ');
            }
            rowStrings[r] = sb.toString();
        }
        this.unformatted = new XMLUnformattedScreen(rowStrings);
        this.screenGuid = UUID.randomUUID();
        this.cachedXml = null;
        computeHash();
    }

    private void computeHash() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (screenBuffer != null) {
                byte[] bytes = new String(screenBuffer).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] digest = md.digest(bytes);
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) sb.append(String.format("%02x", b));
                this.hash = sb.toString();
            }
        } catch (Exception e) {
            this.hash = "";
        }
    }

    @Override
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String dump() {
        StringBuilder sb = new StringBuilder();
        if (unformatted != null && unformatted.text != null) {
            for (String row : unformatted.text) {
                sb.append(row).append('\n');
            }
        } else if (screenBuffer != null) {
            for (int r = 0; r < cy; r++) {
                sb.append(getRow(r)).append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public void dump(IAudit stream) {
        if (stream != null) stream.writeLine(dump());
    }

    @Override
    public String getText(int x, int y, int length) {
        int offset = y * cx + x;
        return getText(offset, length);
    }

    @Override
    public String getText(int offset, int length) {
        if (screenBuffer == null) return "";
        int start = Math.max(0, offset);
        int end = Math.min(screenBuffer.length, start + length);
        if (start >= end) return "";
        return new String(screenBuffer, start, end - start);
    }

    @Override
    public String getRow(int row) {
        if (unformatted != null && unformatted.text != null && row < unformatted.text.length) {
            return unformatted.text[row];
        }
        return getText(row * cx, cx);
    }

    @Override
    public char getCharAt(int offset) {
        if (screenBuffer == null || offset < 0 || offset >= screenBuffer.length) return ' ';
        return screenBuffer[offset];
    }

    @Override
    public int lookForTextStrings(String[] text) {
        StringPosition sp = lookForTextStrings2(text);
        return sp != null ? sp.indexInStringArray : -1;
    }

    @Override
    public StringPosition lookForTextStrings2(String[] text) {
        if (text == null || screenBuffer == null) return null;
        String fullScreen = new String(screenBuffer);
        for (int i = 0; i < text.length; i++) {
            if (text[i] != null && fullScreen.contains(text[i])) {
                int pos = fullScreen.indexOf(text[i]);
                StringPosition sp = new StringPosition();
                sp.indexInStringArray = i;
                sp.str = text[i];
                sp.x = pos % cx;
                sp.y = pos / cx;
                return sp;
            }
        }
        return null;
    }

    @Override
    public int getCX() { return cx; }

    @Override
    public int getCY() { return cy; }

    @Override
    public String getXMLText(boolean refreshCachedValue) {
        if (refreshCachedValue || cachedXml == null) {
            cachedXml = buildXml();
        }
        return cachedXml;
    }

    @Override
    public String getXMLText() {
        return getXMLText(false);
    }

    private String buildXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<XMLScreen>\n");
        sb.append("  <CX>").append(cx).append("</CX>\n");
        sb.append("  <CY>").append(cy).append("</CY>\n");
        if (unformatted != null && unformatted.text != null) {
            sb.append("  <Unformatted>\n");
            for (int i = 0; i < unformatted.text.length; i++) {
                sb.append("    <Row index=\"").append(i).append("\">")
                  .append(xmlEscape(unformatted.text[i]))
                  .append("</Row>\n");
            }
            sb.append("  </Unformatted>\n");
        }
        sb.append("</XMLScreen>");
        return sb.toString();
    }

    private String xmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public String[] getUnformattedStrings() {
        if (unformatted != null && unformatted.text != null) return unformatted.text;
        return new String[0];
    }

    @Override
    public UUID getScreenGuid() { return screenGuid; }

    @Override
    public XMLScreenField[] getFields() {
        return fields != null ? fields : new XMLScreenField[0];
    }

    public void setFields(XMLScreenField[] fields) { this.fields = fields; }

    public boolean isFormatted() { return formatted; }
    public void setFormatted(boolean formatted) { this.formatted = formatted; }

    public String getHash() { return hash; }

    public char[] getScreenBuffer() { return screenBuffer; }
}
