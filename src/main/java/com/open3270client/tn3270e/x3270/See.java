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
 * Named constant definitions for extended attributes, 3270 orders, SCS codes,
 * structured field codes, query replies, WCC flags, AID bytes, and color codes.
 * Port of {@code See.cs}.
 */
class See {

    private See() {}

    // Extended-attribute type codes
    static final byte XA_ALL            = 0x00;
    static final byte XA_3270           = (byte) 0xc0;
    static final byte XA_VALIDATION     = (byte) 0xc1;
    static final byte XAV_FILL          = 0x04;
    static final byte XAV_ENTRY         = 0x02;
    static final byte XAV_TRIGGER       = 0x01;
    static final byte XA_OUTLINING      = (byte) 0xc2;
    static final byte XAO_UNDERLINE     = 0x01;
    static final byte XAO_RIGHT         = 0x02;
    static final byte XAO_OVERLINE      = 0x04;
    static final byte XAO_LEFT          = 0x08;
    static final byte XA_HIGHLIGHTING   = 0x41;
    static final byte XAH_DEFAULT       = 0x00;
    static final byte XAH_NORMAL        = (byte) 0xf0;
    static final byte XAH_BLINK         = (byte) 0xf1;
    static final byte XAH_REVERSE       = (byte) 0xf2;
    static final byte XAH_UNDERSCORE    = (byte) 0xf4;
    static final byte XAH_INTENSIFY     = (byte) 0xf8;
    static final byte XA_FOREGROUND     = 0x42;
    static final byte XAC_DEFAULT       = 0x00;
    static final byte XA_CHARSET        = 0x43;
    static final byte XA_BACKGROUND     = 0x45;
    static final byte XA_TRANSPARENCY   = 0x46;
    static final byte XAT_DEFAULT       = 0x00;
    static final byte XAT_OR            = (byte) 0xf0;
    static final byte XAT_XOR           = (byte) 0xf1;
    static final byte XAT_OPAQUE        = (byte) 0xff;

    // 3270 orders
    static final byte ORDER_PT   = 0x05;
    static final byte ORDER_GE   = 0x08;
    static final byte ORDER_SBA  = 0x11;
    static final byte ORDER_EUA  = 0x12;
    static final byte ORDER_IC   = 0x13;
    static final byte ORDER_SF   = 0x1d;
    static final byte ORDER_SA   = 0x28;
    static final byte ORDER_SFE  = 0x29;
    static final byte ORDER_YALE = 0x2b;
    static final byte ORDER_MF   = 0x2c;
    static final byte ORDER_RA   = 0x3c;

    // Format-control orders
    static final byte FCORDER_NULL = 0x00;
    static final byte FCORDER_FF   = 0x0c;
    static final byte FCORDER_CR   = 0x0d;
    static final byte FCORDER_NL   = 0x15;
    static final byte FCORDER_EM   = 0x19;
    static final byte FCORDER_DUP  = 0x1c;
    static final byte FCORDER_FM   = 0x1e;
    static final byte FCORDER_SUB  = 0x3f;
    static final byte FCORDER_EO   = (byte) 0xff;

    // SCS control codes
    static final byte SCS_BS  = 0x16;
    static final byte SCS_BEL = 0x2f;
    static final byte SCS_CR  = 0x0d;
    static final byte SCS_ENP = 0x14;
    static final byte SCS_FF  = 0x0c;
    static final byte SCS_GE  = 0x08;
    static final byte SCS_HT  = 0x05;
    static final byte SCS_INP = 0x24;
    static final byte SCS_IRS = 0x1e;
    static final byte SCS_LF  = 0x25;
    static final byte SCS_NL  = 0x15;
    static final byte SCS_SA  = 0x28;
    static final byte SCS_SET = 0x2b;
    static final byte SCS_SHF = (byte) 0xc1;
    static final byte SCS_SLD = (byte) 0xc6;
    static final byte SCS_SVF = (byte) 0xc2;
    static final byte SCS_TRN = 0x35;
    static final byte SCS_VCS = 0x04;
    static final byte SCS_VT  = 0x0b;

    // Structured-field codes
    static final byte SF_READ_PART     = 0x01;
    static final byte SF_RP_QUERY      = 0x02;
    static final byte SF_RP_QLIST      = 0x03;
    static final byte SF_RPQ_LIST      = 0x00;
    static final byte SF_RPQ_EQUIV     = 0x40;
    static final byte SF_RPQ_ALL       = (byte) 0x80;
    static final byte SF_ERASE_RESET   = 0x03;
    static final byte SF_ER_DEFAULT    = 0x00;
    static final byte SF_ER_ALT        = (byte) 0x80;
    static final byte SF_SET_REPLY_MODE = 0x09;
    static final byte SF_SRM_FIELD     = 0x00;
    static final byte SF_SRM_XFIELD    = 0x01;
    static final byte SF_SRM_CHAR      = 0x02;
    static final byte SF_CREATE_PART   = 0x0c;
    static final byte CPFLAG_PROT      = 0x40;
    static final byte CPFLAG_COPY_PS   = 0x20;
    static final byte CPFLAG_BASE      = 0x07;
    static final byte SF_OUTBOUND_DS   = 0x40;
    static final byte SF_TRANSFER_DATA = (byte) 0xd0;

    // Query-reply codes
    static final byte QR_SUMMARY      = (byte) 0x80;
    static final byte QR_USABLE_AREA  = (byte) 0x81;
    static final byte QR_ALPHA_PART   = (byte) 0x84;
    static final byte QR_CHARSETS     = (byte) 0x85;
    static final byte QR_COLOR        = (byte) 0x86;
    static final byte QR_HIGHLIGHTING = (byte) 0x87;
    static final byte QR_REPLY_MODES  = (byte) 0x88;
    static final byte QR_PC3270       = (byte) 0x93;
    static final byte QR_DDM          = (byte) 0x95;
    static final byte QR_IMP_PART     = (byte) 0xa6;
    static final byte QR_NULL         = (byte) 0xff;

    // AID bytes
    static final byte AID_NO      = 0x60;
    static final byte AID_QREPLY  = 0x61;
    static final byte AID_ENTER   = 0x7d;
    static final byte AID_PF1     = (byte) 0xf1;
    static final byte AID_PF2     = (byte) 0xf2;
    static final byte AID_PF3     = (byte) 0xf3;
    static final byte AID_PF4     = (byte) 0xf4;
    static final byte AID_PF5     = (byte) 0xf5;
    static final byte AID_PF6     = (byte) 0xf6;
    static final byte AID_PF7     = (byte) 0xf7;
    static final byte AID_PF8     = (byte) 0xf8;
    static final byte AID_PF9     = (byte) 0xf9;
    static final byte AID_PF10    = 0x7a;
    static final byte AID_PF11    = 0x7b;
    static final byte AID_PF12    = 0x7c;
    static final byte AID_PF13    = (byte) 0xc1;
    static final byte AID_PF14    = (byte) 0xc2;
    static final byte AID_PF15    = (byte) 0xc3;
    static final byte AID_PF16    = (byte) 0xc4;
    static final byte AID_PF17    = (byte) 0xc5;
    static final byte AID_PF18    = (byte) 0xc6;
    static final byte AID_PF19    = (byte) 0xc7;
    static final byte AID_PF20    = (byte) 0xc8;
    static final byte AID_PF21    = (byte) 0xc9;
    static final byte AID_PF22    = 0x4a;
    static final byte AID_PF23    = 0x4b;
    static final byte AID_PF24    = 0x4c;
    static final byte AID_OICR    = (byte) 0xe6;
    static final byte AID_MSR_MHS = (byte) 0xe7;
    static final byte AID_SELECT  = 0x7e;
    static final byte AID_PA1     = 0x6c;
    static final byte AID_PA2     = 0x6e;
    static final byte AID_PA3     = 0x6b;
    static final byte AID_CLEAR   = 0x6d;
    static final byte AID_SYSREQ  = (byte) 0xf0;
    static final byte AID_SF      = (byte) 0x88;
    static final byte SFID_QREPLY = (byte) 0x81;

    // Colors
    static final byte COLOR_NEUTRAL_BLACK   = 0;
    static final byte COLOR_BLUE            = 1;
    static final byte COLOR_RED             = 2;
    static final byte COLOR_PINK            = 3;
    static final byte COLOR_GREEN           = 4;
    static final byte COLOR_TURQUOISE       = 5;
    static final byte COLOR_YELLOW          = 6;
    static final byte COLOR_NEUTRAL_WHITE   = 7;
    static final byte COLOR_BLACK           = 8;
    static final byte COLOR_DEEP_BLUE       = 9;
    static final byte COLOR_ORANGE          = 10;
    static final byte COLOR_PURPLE          = 11;
    static final byte COLOR_PALE_GREEN      = 12;
    static final byte COLOR_PALE_TURQUOISE  = 13;
    static final byte COLOR_GREY            = 14;
    static final byte COLOR_WHITE           = 15;

    // ------------------------------------------------------------------ //
    // WCC helpers
    // ------------------------------------------------------------------ //
    static boolean wccReset(byte c)           { return (c & 0x40) != 0; }
    static boolean wccStartPrinter(byte c)    { return (c & 0x08) != 0; }
    static boolean wccSoundAlarm(byte c)      { return (c & 0x04) != 0; }
    static boolean wccKeyboardRestore(byte c) { return (c & 0x02) != 0; }
    static boolean wccResetMDT(byte c)        { return (c & 0x01) != 0; }

    // ------------------------------------------------------------------ //
    // Display helpers
    // ------------------------------------------------------------------ //
    static String formatUnknown(byte v) { return "unknown[= 0x" + v + "]"; }

    static String getEbc(byte ch) {
        return switch (ch) {
            case FCORDER_NULL -> "NULL";
            case FCORDER_SUB  -> "SUB";
            case FCORDER_DUP  -> "DUP";
            case FCORDER_FM   -> "FM";
            case FCORDER_FF   -> "FF";
            case FCORDER_CR   -> "CR";
            case FCORDER_NL   -> "NL";
            case FCORDER_EM   -> "EM";
            case FCORDER_EO   -> "EO";
            default -> {
                int ascii = Tables.EBC_2_ASCII[ch & 0xff] & 0xff;
                yield ascii != 0 ? String.valueOf((char) ascii) : String.valueOf((char) ch);
            }
        };
    }

    static String getAidFromCode(byte code) {
        return switch (code) {
            case AID_NO      -> "NoAID";
            case AID_ENTER   -> "Enter";
            case AID_PF1     -> "PF1";
            case AID_PF2     -> "PF2";
            case AID_PF3     -> "PF3";
            case AID_PF4     -> "PF4";
            case AID_PF5     -> "PF5";
            case AID_PF6     -> "PF6";
            case AID_PF7     -> "PF7";
            case AID_PF8     -> "PF8";
            case AID_PF9     -> "PF9";
            case AID_PF10    -> "PF10";
            case AID_PF11    -> "PF11";
            case AID_PF12    -> "PF12";
            case AID_PF13    -> "PF13";
            case AID_PF14    -> "PF14";
            case AID_PF15    -> "PF15";
            case AID_PF16    -> "PF16";
            case AID_PF17    -> "PF17";
            case AID_PF18    -> "PF18";
            case AID_PF19    -> "PF19";
            case AID_PF20    -> "PF20";
            case AID_PF21    -> "PF21";
            case AID_PF22    -> "PF22";
            case AID_PF23    -> "PF23";
            case AID_PF24    -> "PF24";
            case AID_OICR    -> "OICR";
            case AID_MSR_MHS -> "MSR_MHS";
            case AID_SELECT  -> "Select";
            case AID_PA1     -> "PA1";
            case AID_PA2     -> "PA2";
            case AID_PA3     -> "PA3";
            case AID_CLEAR   -> "Clear";
            case AID_SYSREQ  -> "SysReq";
            case AID_QREPLY  -> "QueryReplyAID";
            default          -> formatUnknown(code);
        };
    }

    static String getSeeAttribute(byte fa) {
        StringBuilder sb = new StringBuilder();
        String paren = "(";
        if ((fa & 0x04) != 0) {
            sb.append(paren).append("protected"); paren = ",";
            if ((fa & 0x08) != 0) { sb.append(paren).append("skip"); paren = ","; }
        } else if ((fa & 0x08) != 0) { sb.append(paren).append("numeric"); paren = ","; }
        switch (fa & 0x03) {
            case 1 -> { sb.append(paren).append("detectable");  paren = ","; }
            case 2 -> { sb.append(paren).append("intensified"); paren = ","; }
            case 3 -> { sb.append(paren).append("nondisplay");  paren = ","; }
        }
        if ((fa & 0x20) != 0) { sb.append(paren).append("modified"); paren = ","; }
        if (!paren.equals("(")) sb.append(")"); else sb.append("(default)");
        return sb.toString();
    }

    static String getHighlight(byte setting) {
        return switch (setting) {
            case XAH_DEFAULT   -> "default";
            case XAH_NORMAL    -> "normal";
            case XAH_BLINK     -> "blink";
            case XAH_REVERSE   -> "reverse";
            case XAH_UNDERSCORE -> "underscore";
            case XAH_INTENSIFY  -> "intensify";
            default             -> formatUnknown(setting);
        };
    }

    private static final String[] COLOR_NAME = {
        "neutralBlack","blue","red","pink","green","turquoise","yellow","neutralWhite",
        "black","deepBlue","orange","purple","paleGreen","paleTurquoise","grey","white"
    };

    static String getColor(byte setting) {
        if (setting == XAC_DEFAULT) return "default";
        int idx = (setting & 0xff) - 0xf0;
        if (idx < 0 || idx >= COLOR_NAME.length) return formatUnknown(setting);
        return COLOR_NAME[idx];
    }

    static String getTransparency(byte setting) {
        return switch (setting) {
            case XAT_DEFAULT -> "default";
            case XAT_OR      -> "or";
            case XAT_XOR     -> "xor";
            case XAT_OPAQUE  -> "opaque";
            default          -> formatUnknown(setting);
        };
    }

    static String getValidation(byte setting) {
        StringBuilder sb = new StringBuilder();
        String paren = "(";
        if ((setting & XAV_FILL)    != 0) { sb.append(paren).append("fill");    paren = ","; }
        if ((setting & XAV_ENTRY)   != 0) { sb.append(paren).append("entry");   paren = ","; }
        if ((setting & XAV_TRIGGER) != 0) { sb.append(paren).append("trigger"); paren = ","; }
        if (!paren.equals("(")) sb.append(")"); else sb.append("(none)");
        return sb.toString();
    }

    static String getOutline(byte setting) {
        StringBuilder sb = new StringBuilder();
        String paren = "(";
        if ((setting & XAO_UNDERLINE) != 0) { sb.append(paren).append("underline"); paren = ","; }
        if ((setting & XAO_RIGHT)     != 0) { sb.append(paren).append("right");     paren = ","; }
        if ((setting & XAO_OVERLINE)  != 0) { sb.append(paren).append("overline");  paren = ","; }
        if ((setting & XAO_LEFT)      != 0) { sb.append(paren).append("left");      paren = ","; }
        if (!paren.equals("(")) sb.append(")"); else sb.append("(none)");
        return sb.toString();
    }

    static String getEfa(byte efa, byte value) {
        return switch (efa) {
            case XA_ALL          -> " all(" + value + ")";
            case XA_3270         -> " 3270" + getSeeAttribute(value);
            case XA_VALIDATION   -> " validation" + getValidation(value);
            case XA_OUTLINING    -> " outlining(" + getOutline(value) + ")";
            case XA_HIGHLIGHTING -> " highlighting(" + getHighlight(value) + ")";
            case XA_FOREGROUND   -> " foreground(" + getColor(value) + ")";
            case XA_CHARSET      -> " charset(" + value + ")";
            case XA_BACKGROUND   -> " background(" + getColor(value) + ")";
            case XA_TRANSPARENCY -> " transparency(" + getTransparency(value) + ")";
            default              -> " " + formatUnknown(efa) + "[0x" + value + "]";
        };
    }

    static String getEfaUnformatted(byte efa, byte value) {
        return switch (efa) {
            case XA_ALL          -> "" + value;
            case XA_3270         -> " 3270" + getSeeAttribute(value);
            case XA_VALIDATION   -> getValidation(value);
            case XA_OUTLINING    -> getOutline(value);
            case XA_HIGHLIGHTING -> getHighlight(value);
            case XA_FOREGROUND   -> getColor(value);
            case XA_CHARSET      -> "" + value;
            case XA_BACKGROUND   -> getColor(value);
            case XA_TRANSPARENCY -> getTransparency(value);
            default              -> formatUnknown(efa) + "[0x" + value + "]";
        };
    }

    static String getEfaOnly(byte efa) {
        return switch (efa) {
            case XA_ALL          -> "all";
            case XA_3270         -> "3270";
            case XA_VALIDATION   -> "validation";
            case XA_OUTLINING    -> "outlining";
            case XA_HIGHLIGHTING -> "highlighting";
            case XA_FOREGROUND   -> "foreground";
            case XA_CHARSET      -> "charset";
            case XA_BACKGROUND   -> "background";
            case XA_TRANSPARENCY -> "transparency";
            default              -> formatUnknown(efa);
        };
    }

    static String getQCodeode(byte id) {
        return switch (id) {
            case QR_CHARSETS     -> "CharacterSets";
            case QR_IMP_PART     -> "ImplicitPartition";
            case QR_SUMMARY      -> "Summary";
            case QR_USABLE_AREA  -> "UsableArea";
            case QR_COLOR        -> "Color";
            case QR_HIGHLIGHTING -> "Highlighting";
            case QR_REPLY_MODES  -> "ReplyModes";
            case QR_ALPHA_PART   -> "AlphanumericPartitions";
            case QR_DDM          -> "DistributedDataManagement";
            default              -> "unknown[0x" + id + "]";
        };
    }
}
