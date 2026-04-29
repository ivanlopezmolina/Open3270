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

/** State of the SMS (Script Management System) state machine. */
public enum SmsState {
    /** No command active. */
    IDLE,
    /** Command(s) buffered and ready to run. */
    INCOMPLETE,
    /** Command executing. */
    RUNNING,
    /** Command awaiting keyboard unlock. */
    KB_WAIT,
    /** Command awaiting connection to complete. */
    CONNECT_WAIT,
    /** Stopped in PauseScript action. */
    PAUSED,
    /** Awaiting completion of Wait(ANSI). */
    WAIT_ANSI,
    /** Awaiting completion of Wait(3270). */
    WAIT_3270,
    /** Awaiting completion of Wait(Output). */
    WAIT_OUTPUT,
    /** Awaiting completion of Snap(Wait). */
    SNAP_WAIT_OUTPUT,
    /** Awaiting completion of Wait(Disconnect). */
    WAIT_DISCONNECT,
    /** Awaiting completion of Wait(). */
    WAIT,
    /** Awaiting completion of Expect(). */
    EXPECTING,
    /** Awaiting completion of Close(). */
    CLOSING
}
