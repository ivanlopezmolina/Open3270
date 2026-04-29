/*
 * Open3270Client - Java / Spring Boot port of Open3270
 * Original C# library: https://github.com/Open3270/Open3270
 * Original authors: Mike Warriner, Francois Botha
 * Original license: MIT
 *
 * Java port copyright (c) 2026 Ivanlopezmolina
 * Released under the MIT License.
 */
package com.open3270client.interfaces;

import com.open3270client.engine.XMLScreenField;

import java.util.UUID;

/**
 * Read-only view of a TN3270 screen. Provides text extraction, field access,
 * XML serialisation, and screen-identity helpers.
 */
public interface IXMLScreen {

    /** Logical name of the screen as identified from the connection configuration. */
    String getName();

    /** Returns a formatted, human-readable text representation of the screen. */
    String dump();

    /** Writes a formatted text representation of the screen to the supplied audit stream. */
    void dump(IAudit stream);

    /**
     * Returns the text at the specified (x, y) coordinate with the given length.
     *
     * @param x      column (0-based)
     * @param y      row (0-based)
     * @param length number of characters to read
     */
    String getText(int x, int y, int length);

    /**
     * Returns the text at the given linear screen offset with the given length.
     *
     * @param offset linear offset (row * CX + col)
     * @param length number of characters to read
     */
    String getText(int offset, int length);

    /**
     * Searches the screen for any of the supplied strings.
     *
     * @param text array of strings to look for
     * @return index of the first matching string in {@code text}, or -1 if none found
     */
    int lookForTextStrings(String[] text);

    /**
     * Searches the screen for any of the supplied strings and returns position details.
     *
     * @param text array of strings to look for
     * @return a {@link StringPosition} describing the first match, or {@code null} if none found
     */
    StringPosition lookForTextStrings2(String[] text);

    /**
     * Returns the entire content of the specified row.
     *
     * @param row row index (0-based)
     */
    String getRow(int row);

    /**
     * Returns the character at the specified linear offset.
     *
     * @param offset linear offset
     */
    char getCharAt(int offset);

    /** Screen width in characters. */
    int getCX();

    /** Screen height in characters. */
    int getCY();

    /**
     * Serialises this screen to an XML string.
     *
     * @param refreshCachedValue {@code false} to force re-serialisation; {@code true} to use the
     *                           cached value (preferred)
     */
    String getXMLText(boolean refreshCachedValue);

    /** Returns the cached XML representation of this screen. */
    String getXMLText();

    /** Returns the raw unformatted text lines, or {@code null} if the screen is formatted. */
    String[] getUnformattedStrings();

    /**
     * A UUID that changes whenever the screen may have changed. It does not guarantee a content
     * change — only that the library believes the screen may differ from the previous state.
     */
    UUID getScreenGuid();

    /** All formatted fields present on the screen, or {@code null} / empty if unformatted. */
    XMLScreenField[] getFields();
}
