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
 * Application resources (configuration / option-toggles).
 * Port of {@code Appres.cs}.
 */
class Appres {

    // ------------------------------------------------------------------ //
    // Toggle indices
    // ------------------------------------------------------------------ //
    static final int MonoCase        = 0;
    static final int AltCursor       = 1;
    static final int CursorBlink     = 2;
    static final int ShowTiming      = 3;
    static final int CursorPos       = 4;
    static final int DSTrace         = 5;
    static final int ScrollBar       = 6;
    static final int LINE_WRAP       = 7;
    static final int BlankFill       = 8;
    static final int ScreenTrace     = 9;
    static final int EventTrace      = 10;
    static final int MarginedPaste   = 11;
    static final int RectangleSelect = 12;
    private static final int N_TOGGLES = 14;

    // ------------------------------------------------------------------ //
    // Inner classes
    // ------------------------------------------------------------------ //
    static class Toggle {
        boolean toggleValue;
        boolean changed;
        String[] labels;

        Toggle() {
            toggleValue = false;
            changed     = false;
            labels      = new String[2];
        }
    }

    // ------------------------------------------------------------------ //
    // Toggle array
    // ------------------------------------------------------------------ //
    private final Toggle[] toggles;

    Appres() {
        toggles = new Toggle[N_TOGGLES];
        for (int i = 0; i < N_TOGGLES; i++) toggles[i] = new Toggle();

        // Defaults
        mono            = false;
        extended        = true;
        m3279           = false;
        modified_sel    = false;
        apl_mode        = false;
        scripted        = true;
        numeric_lock    = false;
        secure          = false;
        typeahead       = true;
        debug_tracing   = true;
        disconnect_clear = false;
        color8          = false;

        hostsfile  = null;
        port       = "telnet";
        charset    = "bracket";
        termname   = null;
        macros     = null;
        trace_dir  = "/tmp";
        oversize   = null;

        icrnl  = true;
        inlcr  = false;
        onlcr  = true;
        erase  = "^H";
        kill   = "^U";
        werase = "^W";
        rprnt  = "^R";
        lnext  = "^V";
        intr   = "^C";
        quit   = "^\\";
        eof    = "^D";
    }

    boolean toggled(int ix)              { return toggles[ix].toggleValue; }
    void toggleTheValue(int ix)          { toggles[ix].toggleValue = !toggles[ix].toggleValue; toggles[ix].changed = true; }
    void setToggle(int ix, boolean v)    { toggles[ix].toggleValue = v; toggles[ix].changed = true; }

    // ------------------------------------------------------------------ //
    // Public fields  (mirrors C# public fields — deliberate design)
    // ------------------------------------------------------------------ //
    boolean mono;
    boolean extended;
    boolean m3279;
    boolean modified_sel;
    boolean apl_mode;
    boolean scripted;
    boolean numeric_lock;
    boolean secure;
    boolean typeahead;
    boolean debug_tracing;
    boolean disconnect_clear;
    boolean color8;

    String hostsfile;
    String port;
    String charset;
    String termname;
    String macros;
    String trace_dir;
    String oversize;

    // Line-mode TTY parameters
    boolean icrnl;
    boolean inlcr;
    boolean onlcr;
    String erase;
    String kill;
    String werase;
    String rprnt;
    String lnext;
    String intr;
    String quit;
    String eof;
}
