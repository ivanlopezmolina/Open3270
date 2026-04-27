/*
 * MIT License
 *
 * Copyright (c) 2026 ivanlopezmolina
 *
 * Originally based on Open3270 - A C# implementation of the TN3270/TN3270E protocol
 * Original authors: Michael Warriner and contributors (c) 2004-2020
 * Original project: https://github.com/Open3270/Open3270
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.open3270.client.tn3270;

public final class TelnetConstants {
    private TelnetConstants() {}

    public static final byte IAC   = (byte) 255;
    public static final byte DO    = (byte) 253;
    public static final byte DONT  = (byte) 254;
    public static final byte WILL  = (byte) 251;
    public static final byte WONT  = (byte) 252;
    public static final byte SB    = (byte) 250;
    public static final byte GA    = (byte) 249;
    public static final byte EL    = (byte) 248;
    public static final byte EC    = (byte) 247;
    public static final byte AYT   = (byte) 246;
    public static final byte AO    = (byte) 245;
    public static final byte IP    = (byte) 244;
    public static final byte BREAK = (byte) 243;
    public static final byte DM    = (byte) 242;
    public static final byte NOP   = (byte) 241;
    public static final byte SE    = (byte) 240;
    public static final byte EOR   = (byte) 239;
    public static final byte SUSP  = (byte) 237;
    public static final byte xEOF  = (byte) 236;
    public static final byte SYNCH = (byte) 242;

    public static final int TELQUAL_IS   = 0;
    public static final int TELQUAL_SEND = 1;
    public static final int LU_MAX       = 32;

    public static final int TELOPT_BINARY        = 0;
    public static final int TELOPT_ECHO          = 1;
    public static final int TELOPT_SGA           = 3;
    public static final int TELOPT_TTYPE         = 24;
    public static final int TELOPT_EOR           = 25;
    public static final int TELOPT_TN3270E       = 40;
    public static final int TELOPT_EXOPL         = 255;

    public static final int TN3270E_FUNC_BIND_IMAGE      = 0;
    public static final int TN3270E_FUNC_DATA_STREAM_CTL = 1;
    public static final int TN3270E_FUNC_RESPONSES       = 2;
    public static final int TN3270E_FUNC_SCS_CTL_CODES   = 3;
    public static final int TN3270E_FUNC_SYSREQ          = 4;

    public static final int TELCMD_FIRST = xEOF & 0xFF;

    public static final String[] TelnetCommands = {
        "EOF", "SUSP", "ABORT", "EOR", "SE", "NOP", "DMARK", "BRK", "IP",
        "AO", "AYT", "EC", "EL", "GA", "SB", "WILL", "WONT", "DO", "DONT", "IAC"
    };

    public static final String[] TelnetOptions = {
        "BINARY", "ECHO", "RCP", "SUPPRESS GO AHEAD", "NAME",
        "STATUS", "TIMING MARK", "RCTE", "NAOL", "NAOP",
        "NAOCRD", "NAOHTS", "NAOHTD", "NAOFFD", "NAOVTS",
        "NAOVTD", "NAOLFD", "EXTEND ASCII", "LOGOUT", "BYTE MACRO",
        "DATA ENTRY TERMINAL", "SUPDUP", "SUPDUP OUTPUT",
        "SEND LOCATION", "TERMINAL TYPE", "END OF RECORD",
        "TACACS UID", "OUTPUT MARKING", "TTYLOC",
        "3270 REGIME", "X.3 PAD", "NAWS", "TSPEED", "LFLOW",
        "LINEMODE", "XDISPLOC", "OLD-ENVIRON", "AUTHENTICATION",
        "ENCRYPT", "NEW-ENVIRON", "TN3270E"
    };

    public static final String[] FunctionNames = {
        "BIND-IMAGE", "DATA-STREAM-CTL", "RESPONSES", "SCS-CTL-CODES", "SYSREQ"
    };

    public static String getReason(int reasonCode) {
        return switch (reasonCode) {
            case 0 -> "CONN-PARTNER";
            case 1 -> "DEVICE-IN-USE";
            case 2 -> "INV-ASSOCIATE";
            case 3 -> "INV-NAME";
            case 4 -> "INV-DEVICE-TYPE";
            case 5 -> "TYPE-NAME-ERROR";
            case 6 -> "UNKNOWN-ERROR";
            case 7 -> "UNSUPPORTED-REQ";
            default -> "??";
        };
    }
}
