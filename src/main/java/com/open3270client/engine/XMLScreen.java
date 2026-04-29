/*
 * Open3270Client - Java / Spring Boot port of Open3270
 * Original C# library: https://github.com/Open3270/Open3270
 * Original authors: Mike Warriner, Francois Botha
 * Original license: MIT
 *
 * Java port copyright (c) 2026 Ivanlopezmolina
 * Released under the MIT License.
 */
package com.open3270client.engine;

import com.open3270client.interfaces.IAudit;
import com.open3270client.interfaces.IXMLScreen;
import com.open3270client.interfaces.StringAudit;
import com.open3270client.interfaces.StringPosition;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

/**
 * The primary implementation of {@link IXMLScreen}. Holds a rendered snapshot of a
 * TN3270 screen decoded from the XML wire format.
 *
 * <p>Prefer the {@link IXMLScreen} interface in consuming code rather than
 * referencing this class directly.
 */
@XmlRootElement(name = "XMLScreen")
@XmlAccessorType(XmlAccessType.FIELD)
public class XMLScreen implements IXMLScreen, AutoCloseable {

    // ------------------------------------------------------------------
    // XML-mapped fields
    // ------------------------------------------------------------------

    @XmlElement(name = "Field")
    private XMLScreenField[] field;

    @XmlElement(name = "Unformatted")
    private XMLUnformattedScreen unformatted;

    @XmlElement(name = "Formatted")
    private boolean formatted;

    // ------------------------------------------------------------------
    // Transient / non-serialised state
    // ------------------------------------------------------------------

    @XmlTransient private UUID screenGuid;
    @XmlTransient private int cx = 80;
    @XmlTransient private int cy = 25;
    @XmlTransient private char[] screenBuffer;
    @XmlTransient private String[] screenRows;
    @XmlTransient private String stringValueCache;
    @XmlTransient private String matchListIdentified;
    @XmlTransient private String hash;
    @XmlTransient private boolean disposed = false;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    public XMLScreen() {
    }

    // ------------------------------------------------------------------
    // Factory methods
    // ------------------------------------------------------------------

    public static XMLScreen loadFromStream(InputStream in) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(XMLScreen.class);
        Unmarshaller um = ctx.createUnmarshaller();
        XMLScreen screen = (XMLScreen) um.unmarshal(in);
        screen.render();
        return screen;
    }

    public static XMLScreen loadFromString(String xmlText) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(XMLScreen.class);
        Unmarshaller um = ctx.createUnmarshaller();
        XMLScreen screen;
        try (StringReader sr = new StringReader(xmlText)) {
            screen = (XMLScreen) um.unmarshal(sr);
        }
        screen.stringValueCache = xmlText;
        screen.render();
        return screen;
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    /**
     * Rebuilds the in-memory screen buffer from the XML field data. Called
     * automatically after deserialisation.
     */
    public void render() {
        stringValueCache = null;

        if (cx == 0 || cy == 0) {
            cx = 132;
            cy = 43;
        }
        if (cx < 80) cx = 80;
        if (cy < 25) cy = 25;

        matchListIdentified = null;

        screenBuffer = new char[cx * cy];
        Arrays.fill(screenBuffer, ' ');
        screenRows = new String[cy];

        String blankRow = " ".repeat(cx);

        if (unformatted == null || unformatted.text == null) {
            Arrays.fill(screenRows, blankRow);
        } else {
            for (int i = 0; i < unformatted.text.length; i++) {
                String text = unformatted.text[i];
                if (text == null) text = "";
                text = text.replace("&lt;", "<");

                // Sanitise non-printable characters
                StringBuilder sb = new StringBuilder(text.length());
                for (int p = 0; p < text.length(); p++) {
                    char ch = text.charAt(p);
                    sb.append((ch < 32 || ch > 126) ? ' ' : ch);
                }
                text = sb.toString();

                for (int chIndex = 0; chIndex < text.length(); chIndex++) {
                    int bufIdx = chIndex + (i * cx);
                    if (bufIdx < screenBuffer.length) {
                        screenBuffer[bufIdx] = text.charAt(chIndex);
                    }
                }
                if (i < screenRows.length) {
                    screenRows[i] = padRight(text, cx);
                }
            }
        }

        // Fill any remaining blank rows
        for (int i = 0; i < screenRows.length; i++) {
            if (screenRows[i] == null || screenRows[i].isEmpty()) {
                screenRows[i] = blankRow;
            }
        }

        // Superimpose formatted fields
        if (field != null && field.length > 0) {
            for (XMLScreenField f : field) {
                if (f.text != null) {
                    for (int chIndex = 0; chIndex < f.text.length(); chIndex++) {
                        char ch = f.text.charAt(chIndex);
                        if (ch < 32 || ch > 126) ch = ' ';
                        int bufIdx = chIndex + f.location.left + f.location.top * cx;
                        if (bufIdx >= 0 && bufIdx < screenBuffer.length) {
                            screenBuffer[bufIdx] = ch;
                        }
                    }
                }
            }
            for (int i = 0; i < cy; i++) {
                screenRows[i] = new String(screenBuffer, i * cx, cx);
            }
        }

        // Compute MD5 hash for identity
        StringBuilder allRows = new StringBuilder();
        for (String row : screenRows) allRows.append(row);
        this.hash = md5Hex(allRows.toString());
        this.screenGuid = UUID.randomUUID();
    }

    // ------------------------------------------------------------------
    // IXMLScreen implementation
    // ------------------------------------------------------------------

    @Override
    public String getName() {
        return matchListIdentified;
    }

    @Override
    public String dump() {
        StringAudit audit = new StringAudit();
        dump(audit);
        return audit.toString();
    }

    @Override
    public void dump(IAudit stream) {
        stream.writeLine("-----");
        StringBuilder tens = new StringBuilder("  ");
        StringBuilder singles = new StringBuilder("  ");
        for (int i = 0; i < cx; i += 10) {
            tens.append(String.format("%-10s", i / 10));
            singles.append("0123456789");
        }
        stream.writeLine(tens.substring(0, 2 + cx));
        stream.writeLine(singles.substring(0, 2 + cx));

        for (int i = 0; i < cy; i++) {
            String line = getText(0, i, cx);
            line = String.format(" %02d%s", i, line);
            stream.writeLine(line);
        }
        stream.writeLine("-----");
    }

    @Override
    public String getText(int x, int y, int length) {
        return getText(x + y * cx, length);
    }

    @Override
    public String getText(int offset, int length) {
        if (screenBuffer == null) return null;
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = offset + i;
            if (idx < screenBuffer.length) {
                result.append(screenBuffer[idx]);
            }
        }
        return result.toString();
    }

    @Override
    public int lookForTextStrings(String[] text) {
        String buffer = new String(screenBuffer);
        for (int i = 0; i < text.length; i++) {
            if (buffer.contains(text[i])) return i;
        }
        return -1;
    }

    @Override
    public StringPosition lookForTextStrings2(String[] text) {
        String buffer = new String(screenBuffer);
        for (int i = 0; i < text.length; i++) {
            int idx = buffer.indexOf(text[i]);
            if (idx >= 0) {
                StringPosition sp = new StringPosition();
                sp.indexInStringArray = i;
                sp.str = text[i];
                sp.x = idx % cx;
                sp.y = idx / cx;
                return sp;
            }
        }
        return null;
    }

    @Override
    public String getRow(int row) {
        return screenRows[row];
    }

    @Override
    public char getCharAt(int offset) {
        return screenBuffer[offset];
    }

    @Override
    public int getCX() { return cx; }

    @Override
    public int getCY() { return cy; }

    @Override
    public String getXMLText() {
        return getXMLText(true);
    }

    @Override
    public String getXMLText(boolean useCache) {
        if (useCache && stringValueCache != null) return stringValueCache;
        try {
            JAXBContext ctx = JAXBContext.newInstance(XMLScreen.class);
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            m.marshal(this, sw);
            stringValueCache = sw.toString();
            return stringValueCache;
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to serialise XMLScreen to XML", e);
        }
    }

    @Override
    public String[] getUnformattedStrings() {
        if (unformatted != null && unformatted.text != null) return unformatted.text;
        return null;
    }

    @Override
    public UUID getScreenGuid() { return screenGuid; }

    @Override
    public XMLScreenField[] getFields() { return field; }

    // ------------------------------------------------------------------
    // Additional accessors
    // ------------------------------------------------------------------

    public boolean isFormatted() { return formatted; }
    public String getHash() { return hash; }

    // ------------------------------------------------------------------
    // AutoCloseable
    // ------------------------------------------------------------------

    @Override
    public void close() {
        if (!disposed) {
            disposed = true;
            field = null;
            unformatted = null;
            screenBuffer = null;
            screenRows = null;
            hash = null;
            matchListIdentified = null;
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String padRight(String s, int length) {
        if (s.length() >= length) return s.substring(0, length);
        return s + " ".repeat(length - s.length());
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_16LE));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02X-", b));
            if (sb.length() > 0) sb.setLength(sb.length() - 1);
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
