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
package com.open3270.client.tn3270.x3270;

public final class ControllerConstant {
    private ControllerConstant() {}

    // Configuration change masks
    public static final int NO_CHANGE     = 0x0000;
    public static final int MODEL_CHANGE  = 0x0001;
    public static final int FONT_CHANGE   = 0x0002;
    public static final int COLOR_CHANGE  = 0x0004;
    public static final int SCROLL_CHANGE = 0x0008;
    public static final int CHARSET_CHANGE= 0x0010;
    public static final int ALL_CHANGE    = 0xffff;

    // Field attribute definitions
    public static final byte FA_BASE         = (byte) 0xC0;
    public static final byte FA_MASK         = (byte) 0xD0;
    public static final byte FA_MODIFY       = (byte) 0x20;
    public static final byte FA_MODIFY_MASK  = (byte) 0xDF;
    public static final byte FA_NUMERIC      = (byte) 0x08;
    public static final byte FA_PROTECT      = (byte) 0x04;
    public static final byte FA_INTENSITY    = (byte) 0x03;
    public static final byte FA_INT_NORM_NSEL = (byte) 0x00;
    public static final byte FA_INT_NORM_SEL  = (byte) 0x01;
    public static final byte FA_INT_HIGH_SEL  = (byte) 0x02;
    public static final byte FA_INT_ZERO_NSEL = (byte) 0x03;

    // 3270 commands
    public static final int CMD_W   = 0x01;
    public static final int CMD_RB  = 0x02;
    public static final int CMD_NOP = 0x03;
    public static final int CMD_EW  = 0x05;
    public static final int CMD_RM  = 0x06;
    public static final int CMD_EWA = 0x0d;
    public static final int CMD_RMA = 0x0e;
    public static final int CMD_EAU = 0x0f;
    public static final int CMD_WSF = 0x11;

    // SNA 3270 commands
    public static final int SNA_CMD_RMA = 0x6e;
    public static final int SNA_CMD_EAU = 0x6f;
    public static final int SNA_CMD_EWA = 0x7e;
    public static final int SNA_CMD_W   = 0xf1;
    public static final int SNA_CMD_RB  = 0xf2;
    public static final int SNA_CMD_WSF = 0xf3;
    public static final int SNA_CMD_EW  = 0xf5;
    public static final int SNA_CMD_RM  = 0xf6;

    // 3270 orders
    public static final int ORDER_PT   = 0x05;
    public static final int ORDER_GE   = 0x08;
    public static final int ORDER_SBA  = 0x11;
    public static final int ORDER_EUA  = 0x12;
    public static final int ORDER_IC   = 0x13;
    public static final int ORDER_SF   = 0x1d;
    public static final int ORDER_SA   = 0x28;
    public static final int ORDER_SFE  = 0x29;
    public static final int ORDER_YALE = 0x2b;
    public static final int ORDER_MF   = 0x2c;
    public static final int ORDER_RA   = 0x3c;

    public static final int FCORDER_NULL = 0x00;
    public static final int FCORDER_FF   = 0x0c;
    public static final int FCORDER_CR   = 0x0d;
    public static final int FCORDER_NL   = 0x15;
    public static final int FCORDER_EM   = 0x19;
    public static final int FCORDER_DUP  = 0x1c;
    public static final int FCORDER_FM   = 0x1e;
    public static final int FCORDER_SUB  = 0x3f;
    public static final int FCORDER_EO   = 0xff;

    // Structured fields
    public static final int SF_READ_PART     = 0x01;
    public static final int SF_RP_QUERY      = 0x02;
    public static final int SF_RP_QLIST      = 0x03;
    public static final int SF_RPQ_LIST      = 0x00;
    public static final int SF_RPQ_EQUIV     = 0x40;
    public static final int SF_RPQ_ALL       = 0x80;
    public static final int SF_ERASE_RESET   = 0x03;
    public static final int SF_ER_DEFAULT    = 0x00;
    public static final int SF_ER_ALT        = 0x80;
    public static final int SF_SET_REPLY_MODE= 0x09;
    public static final int SF_SRM_FIELD     = 0x00;
    public static final int SF_SRM_XFIELD    = 0x01;
    public static final int SF_SRM_CHAR      = 0x02;
    public static final int SF_CREATE_PART   = 0x0c;
    public static final int CPFLAG_PROT      = 0x40;
    public static final int CPFLAG_COPY_PS   = 0x20;
    public static final int CPFLAG_BASE      = 0x07;
    public static final int SF_OUTBOUND_DS   = 0x40;
    public static final int SF_TRANSFER_DATA = 0xd0;

    // Query replies
    public static final int QR_SUMMARY    = 0x80;
    public static final int QR_USABLE_AREA= 0x81;
    public static final int QR_ALPHA_PART = 0x84;
    public static final int QR_CHARSETS   = 0x85;
    public static final int QR_COLOR      = 0x86;
    public static final int QR_HIGHLIGHTING= 0x87;
    public static final int QR_REPLY_MODES = 0x88;
    public static final int QR_PC3270     = 0x93;
    public static final int QR_DDM        = 0x95;
    public static final int QR_IMP_PART   = 0xa6;
    public static final int QR_NULL       = 0xff;

    public static final int CS_GE = 0x04;

    /** Code table used to translate buffer addresses to 3270 data stream representation. */
    public static final byte[] CodeTable = new byte[] {
        (byte)0x40, (byte)0xC1, (byte)0xC2, (byte)0xC3, (byte)0xC4, (byte)0xC5, (byte)0xC6, (byte)0xC7,
        (byte)0xC8, (byte)0xC9, (byte)0x4A, (byte)0x4B, (byte)0x4C, (byte)0x4D, (byte)0x4E, (byte)0x4F,
        (byte)0x50, (byte)0xD1, (byte)0xD2, (byte)0xD3, (byte)0xD4, (byte)0xD5, (byte)0xD6, (byte)0xD7,
        (byte)0xD8, (byte)0xD9, (byte)0x5A, (byte)0x5B, (byte)0x5C, (byte)0x5D, (byte)0x5E, (byte)0x5F,
        (byte)0x60, (byte)0x61, (byte)0xE2, (byte)0xE3, (byte)0xE4, (byte)0xE5, (byte)0xE6, (byte)0xE7,
        (byte)0xE8, (byte)0xE9, (byte)0x6A, (byte)0x6B, (byte)0x6C, (byte)0x6D, (byte)0x6E, (byte)0x6F,
        (byte)0xF0, (byte)0xF1, (byte)0xF2, (byte)0xF3, (byte)0xF4, (byte)0xF5, (byte)0xF6, (byte)0xF7,
        (byte)0xF8, (byte)0xF9, (byte)0x7A, (byte)0x7B, (byte)0x7C, (byte)0x7D, (byte)0x7E, (byte)0x7F,
    };
}
