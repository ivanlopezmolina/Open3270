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

/** Location metadata for a single formatted field on a TN3270 screen. */
public class XMLScreenLocation {
    public int position;
    public int left;
    public int top;
    public int length;
}
