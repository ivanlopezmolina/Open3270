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

import com.open3270client.interfaces.IAudit;

import java.time.Instant;

/**
 * Protocol-layer trace logger.
 * Port of {@code TNTrace.cs}.
 */
class TNTrace {

    static final int TRACELINE    = 72;
    private static final int LINEDUMP_MAX = 32;

    private final Telnet telnet;

    boolean optionTraceNetworkData = false;
    boolean optionTraceDS          = false;
    boolean optionTraceDSN         = false;
    boolean optionTraceAnsi        = false;
    boolean optionTraceEvent       = false;

    private IAudit audit;
    private long   ds_ts = 0;

    TNTrace(Telnet telnet, IAudit audit) {
        this.telnet = telnet;
        this.audit  = audit;
    }

    void start() { /* nothing */ }
    void stop(boolean ansi) { /* nothing */ }

    private void traceEvent(TraceType type, String text) {
        if (audit != null) audit.write(text);
    }

    void writeLine(String text) {
        if (!optionTraceDS) return;
        if (audit != null) audit.writeLine(text);
    }

    /** Trace DS-layer (3270 command-level) messages. */
    void trace_ds(String fmt, Object... args) {
        if (!optionTraceDS) return;
        traceEvent(TraceType.DS, TraceFormatter.format(fmt, args));
    }

    /** Trace DS-layer in English (byte descriptions). */
    void trace_dsn(String fmt, Object... args) {
        if (!optionTraceDSN) return;
        traceEvent(TraceType.DSN, TraceFormatter.format(fmt, args));
    }

    /** Trace a single ANSI character. */
    void trace_char(char c) {
        if (!optionTraceAnsi) return;
        traceEvent(TraceType.ANSI_CHAR, String.valueOf(c));
    }

    /** Trace a high-level event. */
    void trace_event(String fmt, Object... args) {
        if (!optionTraceEvent) return;
        traceEvent(TraceType.EVENT, TraceFormatter.format(fmt, args));
    }

    /** Hex-dump of raw network bytes. */
    void trace_netdata(char direction, byte[] buf, int len) {
        if (!optionTraceNetworkData) return;
        long ts = Instant.now().toEpochMilli();
        if (telnet.is3270()) {
            trace_dsn("%c +%f\n", direction, (double) ((ts - ds_ts) / 1000.0));
        }
        ds_ts = ts;
        for (int offset = 0; offset < len; offset++) {
            if ((offset % LINEDUMP_MAX) == 0) {
                String prefix = (offset != 0 ? "\n" : "") + direction + " 0x" + String.format("%03x ", offset);
                trace_dsn(prefix);
            }
            trace_dsn(String.format("%02x", buf[offset]));
        }
        trace_dsn("\n");
    }

    /** Display a (row,col) from a buffer address. */
    String rcba(int baddr) {
        int cols = telnet.getController().getColumnCount();
        int y = baddr / cols + 1;
        int x = baddr % cols + 1;
        return "(baddr=" + baddr + ",cols=" + cols + ", y=" + y + ",x=" + x + ")";
    }
}
