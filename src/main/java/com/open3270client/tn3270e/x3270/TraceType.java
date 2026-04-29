/*
 * Open3270Client - Java / Spring Boot port of Open3270
 * Original C# library: https://github.com/Open3270/Open3270
 * Original authors: Mike Warriner, Francois Botha
 * Original license: MIT
 *
 * Java port copyright (c) 2026 Ivanlopezmolina
 * Released under the MIT License.
 */
package com.open3270client.tn3270e.x3270;

/**
 * Trace-type discriminator.
 * Port of {@code TraceType} enum in {@code TNTrace.cs}.
 */
enum TraceType {
    DS, DSN, ANSI_CHAR, EVENT, NET_DATA, SCREEN
}
