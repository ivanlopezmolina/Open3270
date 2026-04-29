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

/**
 * An {@link IAudit} implementation that accumulates all output in memory
 * as a single string. Useful for capturing diagnostic output during testing.
 */
public class StringAudit implements IAudit {

    private final StringBuilder data = new StringBuilder();

    @Override
    public void write(String text) {
        data.append(text);
    }

    @Override
    public void writeLine(String text) {
        data.append(text).append('\n');
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
