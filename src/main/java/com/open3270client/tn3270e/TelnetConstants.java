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

/**
 * Telnet and TN3270E protocol byte constants.
 *
 * <p>Direct translation of {@code TelnetConstants.cs}.
 */
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
    public static final byte X_EOF = (byte) 236;
    public static final byte SYNCH = (byte) 242;

    public static final char IS     = '0';
    public static final char SEND   = '1';
    public static final char INFO   = '2';
    public static final char VAR    = '0';
    public static final char VALUE  = '1';
    public static final char ESC    = '2';
    public static final char USERVAR = '3';

    public static final int TELQUAL_IS   = 0;
    public static final int TELQUAL_SEND = 1;
    public static final int LU_MAX       = 32;

    public static final String[] TEL_QUALS = {"IS", "SEND"};

    // Telnet options
    public static final int TELOPT_BINARY       = 0;
    public static final int TELOPT_ECHO         = 1;
    public static final int TELOPT_RCP          = 2;
    public static final int TELOPT_SGA          = 3;
    public static final int TELOPT_NAMS         = 4;
    public static final int TELOPT_STATUS       = 5;
    public static final int TELOPT_TM           = 6;
    public static final int TELOPT_RCTE         = 7;
    public static final int TELOPT_NAOL         = 8;
    public static final int TELOPT_NAOP         = 9;
    public static final int TELOPT_NAOCRD       = 10;
    public static final int TELOPT_NAOHTS       = 11;
    public static final int TELOPT_NAOHTD       = 12;
    public static final int TELOPT_NAOFFD       = 13;
    public static final int TELOPT_NAOVTS       = 14;
    public static final int TELOPT_NAOVTD       = 15;
    public static final int TELOPT_NAOLFD       = 16;
    public static final int TELOPT_XASCII       = 17;
    public static final int TELOPT_LOGOUT       = 18;
    public static final int TELOPT_BM           = 19;
    public static final int TELOPT_DET          = 20;
    public static final int TELOPT_SUPDUP       = 21;
    public static final int TELOPT_SUPDUPOUTPUT = 22;
    public static final int TELOPT_SNDLOC       = 23;
    public static final int TELOPT_TTYPE        = 24;
    public static final int TELOPT_EOR          = 25;
    public static final int TELOPT_TUID         = 26;
    public static final int TELOPT_OUTMRK       = 27;
    public static final int TELOPT_TTYLOC       = 28;
    public static final int TELOPT_3270REGIME   = 29;
    public static final int TELOPT_X3PAD        = 30;
    public static final int TELOPT_NAWS         = 31;
    public static final int TELOPT_TSPEED       = 32;
    public static final int TELOPT_LFLOW        = 33;
    public static final int TELOPT_LINEMODE     = 34;
    public static final int TELOPT_XDISPLOC     = 35;
    public static final int TELOPT_OLD_ENVIRON  = 36;
    public static final int TELOPT_AUTHENTICATION = 37;
    public static final int TELOPT_ENCRYPT      = 38;
    public static final int TELOPT_NEW_ENVIRON  = 39;
    public static final int TELOPT_TN3270E      = 40;
    public static final int TELOPT_EXOPL        = 255;

    // TN3270E function codes
    public static final int TN3270E_FUNC_BIND_IMAGE      = 0;
    public static final int TN3270E_FUNC_DATA_STREAM_CTL = 1;
    public static final int TN3270E_FUNC_RESPONSES       = 2;
    public static final int TN3270E_FUNC_SCS_CTL_CODES   = 3;
    public static final int TN3270E_FUNC_SYSREQ          = 4;

    public static final int TELCMD_FIRST = X_EOF & 0xFF;

    public static final String[] TELNET_COMMANDS = {
        "EOF", "SUSP", "ABORT", "EOR", "SE", "NOP", "DMARK", "BRK", "IP",
        "AO", "AYT", "EC", "EL", "GA", "SB", "WILL", "WONT", "DO", "DONT", "IAC"
    };

    public static final String[] TELNET_OPTIONS = {
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

    public static final String[] FUNCTION_NAMES = {
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
