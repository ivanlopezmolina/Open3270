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
 * Holds the result of a screen-text search, including the position at which
 * the matched string was found.
 */
public class StringPosition {
    public int x;
    public int y;
    public String str;
    public int indexInStringArray;
}
