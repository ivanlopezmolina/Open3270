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

/** Holds the unformatted (raw) text rows for an unformatted TN3270 screen. */
public class XMLUnformattedScreen {
    public String[] text;
}
