/*
 * Open3270Client - Java / Spring Boot port of Open3270
 * Original C# library: https://github.com/Open3270/Open3270
 * Original authors: Mike Warriner, Francois Botha
 * Original license: MIT
 *
 * Java port copyright (c) 2026 Ivanlopezmolina
 * Released under the MIT License.
 */
package com.open3270client.tn3270e;

/** Current state of the Telnet / TN3270(E) connection. */
public enum ConnectionState {
    /** No socket, unknown mode. */
    NOT_CONNECTED(0),
    /** Resolving hostname. */
    RESOLVING(1),
    /** Connection pending. */
    PENDING(2),
    /** Connected, mode not yet negotiated. */
    CONNECTED_INITIAL(3),
    /** Connected in NVT ANSI mode. */
    CONNECTED_ANSI(4),
    /** Connected in old-style 3270 mode. */
    CONNECTED_3270(5),
    /** Connected in TN3270E mode, unnegotiated. */
    CONNECTED_INITIAL_3270E(6),
    /** Connected in TN3270E mode, NVT mode. */
    CONNECTED_NVT(7),
    /** Connected in TN3270E mode, SSCP-LU mode. */
    CONNECTED_SSCP(8),
    /** Connected in TN3270E mode, 3270 mode. */
    CONNECTED_3270E(9);

    private final int value;

    ConnectionState(int value) { this.value = value; }

    public int getValue() { return value; }
}
