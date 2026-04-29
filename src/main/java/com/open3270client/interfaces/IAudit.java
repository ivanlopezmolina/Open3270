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
 * Audit interface — receives diagnostic / tracing output from the library.
 * Implement this interface to capture log messages at runtime.
 */
public interface IAudit {

    /** Write text without a trailing newline. */
    void write(String text);

    /** Write text followed by a newline. */
    void writeLine(String text);
}
