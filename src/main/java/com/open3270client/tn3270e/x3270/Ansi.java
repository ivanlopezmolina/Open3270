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
 * ANSI terminal emulator.
 * Port of {@code Ansi.cs}.
 */
@SuppressWarnings("unchecked")
class Ansi implements AutoCloseable {

    // ------------------------------------------------------------------ //
    // Character-set constants
    // ------------------------------------------------------------------ //
    static final int CS_G0 = 0;
    static final int CS_G1 = 1;
    static final int CS_G2 = 2;
    static final int CS_G3 = 3;

    static final int CSD_LD = 0;
    static final int CSD_UK = 1;
    static final int CSD_US = 2;

    static final int DEFAULT_CGEN = 0x02b90000;
    static final int DEFAULT_CSET = 0x00000025;

    // ------------------------------------------------------------------ //
    // Extended-attribute bit-masks (from ExtendedAttribute.cs)
    // ------------------------------------------------------------------ //
    private static final byte GR_BLINK     = 0x01;
    private static final byte GR_REVERSE   = 0x02;
    private static final byte GR_UNDERLINE = 0x04;
    private static final byte GR_INTENSIFY = 0x08;

    // ------------------------------------------------------------------ //
    // Action / state-table dispatch codes (byte fields, not static)
    // ------------------------------------------------------------------ //
    byte SC = 1;   /* save cursor position */
    byte RC = 2;   /* restore cursor position */
    byte NL = 3;   /* new line */
    byte UP = 4;   /* cursor up */
    byte E2 = 5;   /* second level of ESC processing */
    byte rS = 6;   /* reset */
    byte IC = 7;   /* insert chars */
    byte DN = 8;   /* cursor down */
    byte RT = 9;   /* cursor right */
    byte LT = 10;  /* cursor left */
    byte CM = 11;  /* cursor motion */
    byte ED = 12;  /* erase in display */
    byte EL = 13;  /* erase in line */
    byte IL = 14;  /* insert lines */
    byte DL = 15;  /* delete lines */
    byte DC = 16;  /* delete characters */
    byte SG = 17;  /* set graphic rendition */
    byte BL = 18;  /* ring bell */
    byte NP = 19;  /* new page */
    byte BS = 20;  /* backspace */
    byte CR = 21;  /* carriage return */
    byte LF = 22;  /* line feed */
    byte HT = 23;  /* horizontal tab */
    byte E1 = 24;  /* first level of ESC processing */
    byte Xx = 25;  /* undefined control character (nop) */
    byte Pc = 26;  /* printing character */
    byte Sc = 27;  /* semicolon (after ESC [) */
    byte Dg = 28;  /* digit (after ESC [ or ESC [ ?) */
    byte RI = 29;  /* reverse index */
    byte DA = 30;  /* send device attributes */
    byte SM = 31;  /* set mode */
    byte RM = 32;  /* reset mode */
    byte DO = 33;  /* return terminal ID (obsolete) */
    byte SR = 34;  /* device status report */
    byte CS = 35;  /* character set designate */
    byte E3 = 36;  /* third level of ESC processing */
    byte DS = 37;  /* DEC private set */
    byte DR = 38;  /* DEC private reset */
    byte DV = 39;  /* DEC private save */
    byte DT = 40;  /* DEC private restore */
    byte SS = 41;  /* set scrolling region */
    byte TM = 42;  /* text mode (ESC ]) */
    byte T2 = 43;  /* semicolon (after ESC ]) */
    byte TX = 44;  /* text parameter (after ESC ] n ;) */
    byte TB = 45;  /* text parameter done (ESC ] n ; xxx BEL) */
    byte TS = 46;  /* tab set */
    byte TC = 47;  /* tab clear */
    byte C2 = 48;  /* character set designate (finish) */
    byte G0 = 49;  /* select G0 character set */
    byte G1 = 50;  /* select G1 character set */
    byte G2 = 51;  /* select G2 character set */
    byte G3 = 52;  /* select G3 character set */
    byte S2 = 53;  /* select G2 for next character */
    byte S3 = 54;  /* select G3 for next character */

    // ------------------------------------------------------------------ //
    // Function-pointer dispatch table
    // ------------------------------------------------------------------ //
    @FunctionalInterface
    interface AnsiFn { AnsiState apply(int a, int b); }
    private AnsiFn[] ansi_fn;

    @SuppressWarnings("unchecked")
    private void initializeAnsiFn() {
        ansi_fn = new AnsiFn[] {
            /* 0  */ this::ansi_data_mode,
            /* 1  */ this::dec_save_cursor,
            /* 2  */ this::dec_restore_cursor,
            /* 3  */ this::ansi_newline,
            /* 4  */ this::ansi_cursor_up,
            /* 5  */ this::ansi_esc2,
            /* 6  */ this::ansi_reset,
            /* 7  */ this::ansi_insert_chars,
            /* 8  */ this::ansi_cursor_down,
            /* 9  */ this::ansi_cursor_right,
            /* 10 */ this::ansi_cursor_left,
            /* 11 */ this::ansi_cursor_motion,
            /* 12 */ this::ansi_erase_in_display,
            /* 13 */ this::ansi_erase_in_line,
            /* 14 */ this::ansi_insert_lines,
            /* 15 */ this::ansi_delete_lines,
            /* 16 */ this::ansi_delete_chars,
            /* 17 */ this::ansi_sgr,
            /* 18 */ this::ansi_bell,
            /* 19 */ this::ansi_newpage,
            /* 20 */ this::ansi_backspace,
            /* 21 */ this::ansi_cr,
            /* 22 */ this::ansi_lf,
            /* 23 */ this::ansi_htab,
            /* 24 */ this::ansi_escape,
            /* 25 */ this::ansi_nop,
            /* 26 */ this::ansi_printing,
            /* 27 */ this::ansi_semicolon,
            /* 28 */ this::ansi_digit,
            /* 29 */ this::ansi_reverse_index,
            /* 30 */ this::ansi_send_attributes,
            /* 31 */ this::ansi_set_mode,
            /* 32 */ this::ansi_reset_mode,
            /* 33 */ this::dec_return_terminal_id,
            /* 34 */ this::ansi_status_report,
            /* 35 */ this::ansi_cs_designate,
            /* 36 */ this::ansi_esc3,
            /* 37 */ this::dec_set,
            /* 38 */ this::dec_reset,
            /* 39 */ this::dec_save,
            /* 40 */ this::dec_restore,
            /* 41 */ this::dec_scrolling_region,
            /* 42 */ this::xterm_text_mode,
            /* 43 */ this::xterm_text_semicolon,
            /* 44 */ this::xterm_text,
            /* 45 */ this::xterm_text_do,
            /* 46 */ this::ansi_htab_set,
            /* 47 */ this::ansi_htab_clear,
            /* 48 */ this::ansi_cs_designate2,
            /* 49 */ this::ansi_select_g0,
            /* 50 */ this::ansi_select_g1,
            /* 51 */ this::ansi_select_g2,
            /* 52 */ this::ansi_select_g3,
            /* 53 */ this::ansi_one_g2,
            /* 54 */ this::ansi_one_g3
        };
    }

    // ------------------------------------------------------------------ //
    // State tables [7][256]
    // ------------------------------------------------------------------ //
    private byte[][] st = new byte[7][];

    private void initializeST() {
        /* State table for base processing (state == DATA) */
        st[0] = new byte[] {
            /* 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f  */
            /* 00 */ Xx,Xx,Xx,Xx,Xx,Xx,Xx,BL,BS,HT,LF,LF,NP,CR,G1,G0,
            /* 10 */ Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,E1,Xx,Xx,Xx,Xx,
            /* 20 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,
            /* 30 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,
            /* 40 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,
            /* 50 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,
            /* 60 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,
            /* 70 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Xx,
            /* 80 */ Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,
            /* 90 */ Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,Xx,
            /* a0 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,
            /* b0 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,
            /* c0 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,
            /* d0 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,
            /* e0 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,
            /* f0 */ Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc,Pc
        };

        /* State table for ESC processing (state == ESC) */
        st[1] = new byte[] {
            /* 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f  */
            /* 00 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 10 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 20 */ 0, 0, 0, 0, 0, 0, 0, 0,CS,CS,CS,CS, 0, 0, 0, 0,
            /* 30 */ 0, 0, 0, 0, 0, 0, 0,SC,RC, 0, 0, 0, 0, 0, 0, 0,
            /* 40 */ 0, 0, 0, 0, 0,NL, 0, 0,TS, 0, 0, 0, 0,RI,S2,S3,
            /* 50 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,E2, 0,TM, 0, 0,
            /* 60 */ 0, 0, 0,rS, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,G2,G3,
            /* 70 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 80 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 90 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* a0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* b0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* c0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* d0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* e0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* f0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        /* State table for ESC ()*+ C processing (state == CSDES) */
        st[2] = new byte[] {
            /* 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f  */
            /* 00 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 10 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 20 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 30 */ C2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 40 */ 0,C2,C2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 50 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 60 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 70 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 80 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 90 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* a0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* b0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* c0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* d0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* e0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* f0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        /* State table for ESC [ processing (state == N1) */
        st[3] = new byte[] {
            /* 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f  */
            /* 00 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 10 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 20 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 30 */ Dg,Dg,Dg,Dg,Dg,Dg,Dg,Dg,Dg,Dg, 0,Sc, 0, 0, 0,E3,
            /* 40 */ IC,UP,DN,RT,LT, 0, 0, 0,CM, 0,ED,EL,IL,DL, 0, 0,
            /* 50 */ DC, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 60 */  0, 0, 0,DA, 0, 0,CM,TC,SM, 0, 0, 0,RM,SG,SR, 0,
            /* 70 */  0, 0,SS, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 80 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 90 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* a0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* b0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* c0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* d0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* e0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* f0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        /* State table for ESC [ ? processing (state == DECP) */
        st[4] = new byte[] {
            /* 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f  */
            /* 00 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 10 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 20 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 30 */ Dg,Dg,Dg,Dg,Dg,Dg,Dg,Dg,Dg,Dg, 0, 0, 0, 0, 0, 0,
            /* 40 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 50 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 60 */ 0, 0, 0, 0, 0, 0, 0, 0,DS, 0, 0, 0,DR, 0, 0, 0,
            /* 70 */ 0, 0,DT,DV, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 80 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 90 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* a0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* b0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* c0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* d0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* e0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* f0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        /* State table for ESC ] processing (state == TEXT) */
        st[5] = new byte[] {
            /* 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f  */
            /* 00 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 10 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 20 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 30 */ Dg,Dg,Dg,Dg,Dg,Dg,Dg,Dg,Dg,Dg, 0,T2, 0, 0, 0, 0,
            /* 40 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 50 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 60 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 70 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 80 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 90 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* a0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* b0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* c0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* d0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* e0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* f0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        /* State table for ESC ] n ; processing (state == TEXT2) */
        st[6] = new byte[] {
            /* 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f  */
            /* 00 */ 0, 0, 0, 0, 0, 0, 0,TB, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 10 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /* 20 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* 30 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* 40 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* 50 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* 60 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* 70 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,Xx,
            /* 80 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* 90 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* a0 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* b0 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* c0 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* d0 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* e0 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,
            /* f0 */ TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX,TX
        };
    }

    // ------------------------------------------------------------------ //
    // Instance state
    // ------------------------------------------------------------------ //
    private final Telnet telnet;

    int  saved_cursor = 0;
    static final int NN = 20;
    int[] n = new int[NN];
    int  nx = 0;
    static final int NT = 256;
    String text = "";
    int  tx = 0;
    char ansi_ch;
    byte gr = 0;
    byte saved_gr = 0;
    byte fg = 0;
    byte saved_fg = 0;
    byte bg = 0;
    byte saved_bg = 0;
    int  cset = CS_G0;
    int  saved_cset = CS_G0;
    int[] csd       = new int[] { CSD_US, CSD_US, CSD_US, CSD_US };
    int[] saved_csd = new int[] { CSD_US, CSD_US, CSD_US, CSD_US };
    int  once_cset = -1;
    boolean ansi_insert_mode    = false;
    boolean auto_newline_mode   = false;
    int  appl_cursor = 0;
    int  saved_appl_cursor = 0;
    boolean wraparound_mode          = true;
    boolean saved_wraparound_mode    = true;
    boolean rev_wraparound_mode      = false;
    boolean saved_rev_wraparound_mode = false;
    boolean allow_wide_mode          = false;
    boolean saved_allow_wide_mode    = false;
    boolean wide_mode                = false;
    boolean saved_wide_mode          = false;
    boolean saved_altbuffer          = false;
    int  scroll_top    = -1;
    int  scroll_bottom = -1;
    byte[] tabs = null;
    String gnnames = "()*+";
    String csnames  = "0AB";
    int  cs_to_change = 0;
    boolean held_wrap = false;

    AnsiState state = AnsiState.DATA;

    // ------------------------------------------------------------------ //
    // Constructor
    // ------------------------------------------------------------------ //
    Ansi(Telnet telnet) {
        this.telnet = telnet;
        initializeAnsiFn();
        initializeST();
    }

    // ------------------------------------------------------------------ //
    // Public API
    // ------------------------------------------------------------------ //

    /** Register the 3270-connection listener. */
    void ansi_init() {
        telnet.addConnected3270Listener(this::telnetConnected3270);
    }

    private void telnetConnected3270(boolean is3270) {
        ansi_in3270(is3270);
    }

    /** Callback for when we enter / leave ANSI mode. */
    void ansi_in3270(boolean in3270) {
        if (!in3270) {
            ansi_reset(0, 0);
        }
    }

    /**
     * External entry point — process a buffer of ANSI bytes.
     * Corresponds to {@code Ansi3270(byte[], int)} in the C# source.
     */
    void process(byte[] data, int length) {
        for (int i = 0; i < length; i++) {
            ansi_process(data[i]);
        }
    }

    void ansi_process(byte c) {
        int ci = c & 0xff;
        ansi_ch = (char) ci;

        if (telnet.getAppres().toggled(Appres.ScreenTrace)) {
            telnet.getTrace().trace_char((char) ci);
        }

        int fnindex = st[state.ordinal()][ci] & 0xff;
        AnsiFn fn = ansi_fn[fnindex];
        state = fn.apply(n[0], n[1]);
    }

    // ------------------------------------------------------------------ //
    // Cursor navigation
    // ------------------------------------------------------------------ //

    void ansi_send_up() {
        if (appl_cursor != 0) telnet.sendString("\033OA");
        else                   telnet.sendString("\033[A");
    }

    void ansi_send_down() {
        if (appl_cursor != 0) telnet.sendString("\033OB");
        else                   telnet.sendString("\033[B");
    }

    void ansi_send_right() {
        if (appl_cursor != 0) telnet.sendString("\033OC");
        else                   telnet.sendString("\033[C");
    }

    void ansi_send_left() {
        if (appl_cursor != 0) telnet.sendString("\033OD");
        else                   telnet.sendString("\033[D");
    }

    void ansi_send_home() {
        telnet.sendString("\033[H");
    }

    void ansi_send_clear() {
        telnet.sendString("\033[2K");
    }

    void ansi_send_pf(int nn) {
        throw new UnsupportedOperationException("ansi_send_pf not implemented");
    }

    void ansi_send_pa(int nn) {
        throw new UnsupportedOperationException("ansi_send_pa not implemented");
    }

    // ------------------------------------------------------------------ //
    // AutoCloseable
    // ------------------------------------------------------------------ //

    @Override
    public void close() {
        telnet.removeConnected3270Listener(this::telnetConnected3270);
    }

    // ================================================================== //
    // Private action methods — correspond 1-to-1 with C# action methods
    // ================================================================== //

    private AnsiState ansi_data_mode(int ig1, int ig2) {
        return AnsiState.DATA;
    }

    private AnsiState dec_save_cursor(int ig1, int ig2) {
        saved_cursor = telnet.getController().getCursorAddress();
        saved_cset   = cset;
        for (int i = 0; i < 4; i++) saved_csd[i] = csd[i];
        saved_fg = fg;
        saved_bg = bg;
        saved_gr = gr;
        return AnsiState.DATA;
    }

    private AnsiState dec_restore_cursor(int ig1, int ig2) {
        cset = saved_cset;
        for (int i = 0; i < 4; i++) csd[i] = saved_csd[i];
        fg = saved_fg;
        bg = saved_bg;
        gr = saved_gr;
        telnet.getController().setCursorAddress(saved_cursor);
        held_wrap = false;
        return AnsiState.DATA;
    }

    private AnsiState ansi_newline(int ig1, int ig2) {
        int cols = telnet.getController().getColumnCount();
        int cur  = telnet.getController().getCursorAddress();
        telnet.getController().setCursorAddress(cur - (cur % cols));
        int nc = telnet.getController().getCursorAddress() + cols;
        if (nc < scroll_bottom * cols)
            telnet.getController().setCursorAddress(nc);
        else
            ansi_scroll();
        held_wrap = false;
        return AnsiState.DATA;
    }

    private AnsiState ansi_cursor_up(int nn, int ig2) {
        if (nn < 1) nn = 1;
        int rr = telnet.getController().getCursorAddress() / telnet.getController().getColumnCount();
        if (rr - nn < 0)
            telnet.getController().setCursorAddress(
                telnet.getController().getCursorAddress() % telnet.getController().getColumnCount());
        else
            telnet.getController().setCursorAddress(
                telnet.getController().getCursorAddress() - (nn * telnet.getController().getColumnCount()));
        held_wrap = false;
        return AnsiState.DATA;
    }

    private AnsiState ansi_esc2(int ig1, int ig2) {
        for (int i = 0; i < NN; i++) n[i] = 0;
        nx = 0;
        return AnsiState.N1;
    }

    private boolean ansi_reset__first = false;

    private AnsiState ansi_reset(int ig1, int ig2) {
        gr = 0; saved_gr = 0;
        fg = 0; saved_fg = 0;
        bg = 0; saved_bg = 0;
        cset       = CS_G0;
        saved_cset = CS_G0;
        csd[0] = csd[1] = csd[2] = csd[3] = CSD_US;
        saved_csd[0] = saved_csd[1] = saved_csd[2] = saved_csd[3] = CSD_US;
        once_cset         = -1;
        saved_cursor      = 0;
        ansi_insert_mode  = false;
        auto_newline_mode = false;
        appl_cursor       = 0;
        saved_appl_cursor = 0;
        wraparound_mode          = true;
        saved_wraparound_mode    = true;
        rev_wraparound_mode      = false;
        saved_rev_wraparound_mode = false;
        allow_wide_mode          = false;
        saved_allow_wide_mode    = false;
        wide_mode                = false;
        allow_wide_mode          = false;
        saved_altbuffer          = false;
        scroll_top    = 1;
        scroll_bottom = telnet.getController().getRowCount();

        int cols = telnet.getController().getColumnCount();
        tabs = new byte[(cols + 7) / 8];
        for (int i = 0; i < (cols + 7) / 8; i++) tabs[i] = 0x01;

        held_wrap = false;

        if (!ansi_reset__first) {
            telnet.getController().swapAltBuffers(true);
            telnet.getController().eraseRegion(0, telnet.getController().getRowCount() * cols, true);
            telnet.getController().swapAltBuffers(false);
            telnet.getController().clear(false);
        }
        ansi_reset__first = false;
        return AnsiState.DATA;
    }

    private AnsiState ansi_insert_chars(int nn, int ig2) {
        int cc = telnet.getController().getCursorAddress() % telnet.getController().getColumnCount();
        int mc = telnet.getController().getColumnCount() - cc;
        if (nn < 1) nn = 1;
        if (nn > mc) nn = mc;
        int ns = mc - nn;
        if (ns != 0)
            telnet.getController().copyBlock(
                telnet.getController().getCursorAddress(),
                telnet.getController().getCursorAddress() + nn,
                ns, true);
        telnet.getController().eraseRegion(telnet.getController().getCursorAddress(), nn, true);
        return AnsiState.DATA;
    }

    private AnsiState ansi_cursor_down(int nn, int ig2) {
        if (nn < 1) nn = 1;
        int rr   = telnet.getController().getCursorAddress() / telnet.getController().getColumnCount();
        int rows = telnet.getController().getRowCount();
        int cols = telnet.getController().getColumnCount();
        if (rr + nn >= rows)
            telnet.getController().setCursorAddress(
                (rows - 1) * cols + (telnet.getController().getCursorAddress() % cols));
        else
            telnet.getController().setCursorAddress(
                telnet.getController().getCursorAddress() + (nn * cols));
        held_wrap = false;
        return AnsiState.DATA;
    }

    private AnsiState ansi_cursor_right(int nn, int ig2) {
        if (nn < 1) nn = 1;
        int cols = telnet.getController().getColumnCount();
        int cc   = telnet.getController().getCursorAddress() % cols;
        if (cc == cols - 1) return AnsiState.DATA;
        if (cc + nn >= cols) nn = cols - 1 - cc;
        telnet.getController().setCursorAddress(telnet.getController().getCursorAddress() + nn);
        held_wrap = false;
        return AnsiState.DATA;
    }

    private AnsiState ansi_cursor_left(int nn, int ig2) {
        if (held_wrap) {
            held_wrap = false;
            return AnsiState.DATA;
        }
        if (nn < 1) nn = 1;
        int cols = telnet.getController().getColumnCount();
        int cc   = telnet.getController().getCursorAddress() % cols;
        if (cc == 0) return AnsiState.DATA;
        if (nn > cc) nn = cc;
        telnet.getController().setCursorAddress(telnet.getController().getCursorAddress() - nn);
        return AnsiState.DATA;
    }

    private AnsiState ansi_cursor_motion(int n1, int n2) {
        int rows = telnet.getController().getRowCount();
        int cols = telnet.getController().getColumnCount();
        if (n1 < 1) n1 = 1;
        if (n1 > rows) n1 = rows;
        if (n2 < 1) n2 = 1;
        if (n2 > cols) n2 = cols;
        telnet.getController().setCursorAddress((n1 - 1) * cols + (n2 - 1));
        held_wrap = false;
        return AnsiState.DATA;
    }

    private AnsiState ansi_erase_in_display(int nn, int ig2) {
        int rows = telnet.getController().getRowCount();
        int cols = telnet.getController().getColumnCount();
        int cur  = telnet.getController().getCursorAddress();
        switch (nn) {
            case 0: /* below */
                telnet.getController().eraseRegion(cur, (rows * cols) - cur, true);
                break;
            case 1: /* above */
                telnet.getController().eraseRegion(0, cur + 1, true);
                break;
            case 2: /* all (without moving cursor) */
                // scroll_save omitted (not implemented in C# either)
                telnet.getController().eraseRegion(0, rows * cols, true);
                break;
        }
        return AnsiState.DATA;
    }

    private AnsiState ansi_erase_in_line(int nn, int ig2) {
        int cols = telnet.getController().getColumnCount();
        int cur  = telnet.getController().getCursorAddress();
        int nc   = cur % cols;
        switch (nn) {
            case 0: /* to right */
                telnet.getController().eraseRegion(cur, cols - nc, true);
                break;
            case 1: /* to left */
                telnet.getController().eraseRegion(cur - nc, nc + 1, true);
                break;
            case 2: /* all */
                telnet.getController().eraseRegion(cur - nc, cols, true);
                break;
        }
        return AnsiState.DATA;
    }

    private AnsiState ansi_insert_lines(int nn, int ig2) {
        int cols = telnet.getController().getColumnCount();
        int rr   = telnet.getController().getCursorAddress() / cols;
        int mr   = scroll_bottom - rr;
        if (rr < scroll_top - 1 || rr >= scroll_bottom) return AnsiState.DATA;
        if (nn < 1) nn = 1;
        if (nn > mr) nn = mr;
        int ns = mr - nn;
        if (ns != 0)
            telnet.getController().copyBlock(rr * cols, (rr + nn) * cols, ns * cols, true);
        telnet.getController().eraseRegion(rr * cols, nn * cols, true);
        return AnsiState.DATA;
    }

    private AnsiState ansi_delete_lines(int nn, int ig2) {
        int cols = telnet.getController().getColumnCount();
        int rr   = telnet.getController().getCursorAddress() / cols;
        int mr   = scroll_bottom - rr;
        if (rr < scroll_top - 1 || rr >= scroll_bottom) return AnsiState.DATA;
        if (nn < 1) nn = 1;
        if (nn > mr) nn = mr;
        int ns = mr - nn;
        if (ns != 0)
            telnet.getController().copyBlock((rr + nn) * cols, rr * cols, ns * cols, true);
        telnet.getController().eraseRegion((rr + ns) * cols, nn * cols, true);
        return AnsiState.DATA;
    }

    private AnsiState ansi_delete_chars(int nn, int ig2) {
        int cols = telnet.getController().getColumnCount();
        int cur  = telnet.getController().getCursorAddress();
        int cc   = cur % cols;
        int mc   = cols - cc;
        if (nn < 1) nn = 1;
        if (nn > mc) nn = mc;
        int ns = mc - nn;
        if (ns != 0)
            telnet.getController().copyBlock(cur + nn, cur, ns, true);
        telnet.getController().eraseRegion(cur + ns, nn, true);
        return AnsiState.DATA;
    }

    private AnsiState ansi_sgr(int ig1, int ig2) {
        for (int i = 0; i <= nx && i < NN; i++) {
            switch (n[i]) {
                case 0:  gr = 0; fg = 0; bg = 0; break;
                case 1:  gr |= GR_INTENSIFY; break;
                case 4:  gr |= GR_UNDERLINE; break;
                case 5:  gr |= GR_BLINK;     break;
                case 7:  gr |= GR_REVERSE;   break;
                case 30: fg = (byte) 0xf0; /* black   */ break;
                case 31: fg = (byte) 0xf2; /* red     */ break;
                case 32: fg = (byte) 0xf4; /* green   */ break;
                case 33: fg = (byte) 0xf6; /* yellow  */ break;
                case 34: fg = (byte) 0xf1; /* blue    */ break;
                case 35: fg = (byte) 0xf3; /* magenta */ break;
                case 36: fg = (byte) 0xfd; /* cyan    */ break;
                case 37: fg = (byte) 0xff; /* white   */ break;
                case 39: fg = 0;           /* default */ break;
                case 40: bg = (byte) 0xf0; /* black   */ break;
                case 41: bg = (byte) 0xf2; /* red     */ break;
                case 42: bg = (byte) 0xf4; /* green   */ break;
                case 43: bg = (byte) 0xf6; /* yellow  */ break;
                case 44: bg = (byte) 0xf1; /* blue    */ break;
                case 45: bg = (byte) 0xf3; /* magenta */ break;
                case 46: bg = (byte) 0xfd; /* cyan    */ break;
                case 47: bg = (byte) 0xff; /* white   */ break;
                case 49: bg = 0;           /* default */ break;
            }
        }
        return AnsiState.DATA;
    }

    private AnsiState ansi_bell(int ig1, int ig2) {
        // ring_bell() — not implemented
        return AnsiState.DATA;
    }

    private AnsiState ansi_newpage(int ig1, int ig2) {
        telnet.getController().clear(false);
        return AnsiState.DATA;
    }

    private AnsiState ansi_backspace(int ig1, int ig2) {
        if (held_wrap) {
            held_wrap = false;
            return AnsiState.DATA;
        }
        int cols = telnet.getController().getColumnCount();
        int cur  = telnet.getController().getCursorAddress();
        if (rev_wraparound_mode) {
            if (cur > (scroll_top - 1) * cols)
                telnet.getController().setCursorAddress(cur - 1);
        } else {
            if ((cur % cols) != 0)
                telnet.getController().setCursorAddress(cur - 1);
        }
        return AnsiState.DATA;
    }

    private AnsiState ansi_cr(int ig1, int ig2) {
        int cols = telnet.getController().getColumnCount();
        int cur  = telnet.getController().getCursorAddress();
        if ((cur % cols) != 0)
            telnet.getController().setCursorAddress(cur - (cur % cols));
        if (auto_newline_mode)
            ansi_lf(0, 0);
        held_wrap = false;
        return AnsiState.DATA;
    }

    private AnsiState ansi_lf(int ig1, int ig2) {
        int cols   = telnet.getController().getColumnCount();
        int rows   = telnet.getController().getRowCount();
        int cur    = telnet.getController().getCursorAddress();
        int nc     = cur + cols;
        held_wrap = false;

        /* If we're below the scrolling region, don't scroll. */
        if ((cur / cols) >= scroll_bottom) {
            if (nc < rows * cols)
                telnet.getController().setCursorAddress(nc);
            return AnsiState.DATA;
        }

        if (nc < scroll_bottom * cols)
            telnet.getController().setCursorAddress(nc);
        else
            ansi_scroll();
        return AnsiState.DATA;
    }

    private AnsiState ansi_htab(int ig1, int ig2) {
        int cols = telnet.getController().getColumnCount();
        int col  = telnet.getController().getCursorAddress() % cols;
        held_wrap = false;
        if (col == cols - 1) return AnsiState.DATA;
        int i;
        for (i = col + 1; i < cols - 1; i++) {
            if ((tabs[i / 8] & (1 << (i % 8))) != 0) break;
        }
        telnet.getController().setCursorAddress(
            telnet.getController().getCursorAddress() - col + i);
        return AnsiState.DATA;
    }

    private AnsiState ansi_escape(int ig1, int ig2) {
        return AnsiState.ESC;
    }

    private AnsiState ansi_nop(int ig1, int ig2) {
        return AnsiState.DATA;
    }

    /** Perform a wrap-advance of the cursor after printing. */
    private void pwrap() {
        int cols = telnet.getController().getColumnCount();
        int cur  = telnet.getController().getCursorAddress();
        int nc   = cur + 1;
        if (nc < scroll_bottom * cols) {
            telnet.getController().setCursorAddress(nc);
        } else {
            if (cur / cols >= scroll_bottom) {
                telnet.getController().setCursorAddress((cur / cols) * cols);
            } else {
                ansi_scroll();
                telnet.getController().setCursorAddress(nc - cols);
            }
        }
    }

    private AnsiState ansi_printing(int ig1, int ig2) {
        if (held_wrap) {
            pwrap();
            held_wrap = false;
        }

        if (ansi_insert_mode)
            ansi_insert_chars(1, 0);

        int effectiveCset = (once_cset != -1) ? once_cset : cset;
        int cur = telnet.getController().getCursorAddress();

        switch (csd[effectiveCset]) {
            case CSD_LD: /* line drawing "0" */
                if (ansi_ch >= 0x5f && ansi_ch <= 0x7e)
                    telnet.getController().addCharacter(cur, (byte) (ansi_ch - 0x5f), (byte) 2);
                else
                    telnet.getController().addCharacter(cur, Tables.ASCII_2_CG[ansi_ch & 0xff], (byte) 0);
                break;
            case CSD_UK: /* UK "A" */
                if (ansi_ch == '#')
                    telnet.getController().addCharacter(cur, (byte) 0x1e, (byte) 2);
                else
                    telnet.getController().addCharacter(cur, Tables.ASCII_2_CG[ansi_ch & 0xff], (byte) 0);
                break;
            case CSD_US: /* US "B" */
            default:
                telnet.getController().addCharacter(cur, Tables.ASCII_2_CG[ansi_ch & 0xff], (byte) 0);
                break;
        }

        once_cset = -1;
        telnet.getController().addGr(cur, (byte) gr);
        telnet.getController().setForegroundColor(cur, fg);
        telnet.getController().setBackgroundColor(cur, bg);

        int cols = telnet.getController().getColumnCount();
        cur = telnet.getController().getCursorAddress();

        if (wraparound_mode) {
            /*
             * xterm behaviour: when a character is printed in the last column the
             * cursor sticks there.  The next printing character will put the cursor
             * in column 2 of the next line.
             */
            if (0 == ((cur + 1) % cols)) {
                held_wrap = true;
            } else {
                pwrap();
            }
        } else {
            if ((cur % cols) != (cols - 1))
                telnet.getController().setCursorAddress(cur + 1);
        }
        return AnsiState.DATA;
    }

    private AnsiState ansi_semicolon(int ig1, int ig2) {
        if (nx >= NN) return AnsiState.DATA;
        nx++;
        return state;
    }

    private AnsiState ansi_digit(int ig1, int ig2) {
        n[nx] = (n[nx] * 10) + (ansi_ch - '0');
        return state;
    }

    private AnsiState ansi_reverse_index(int ig1, int ig2) {
        int cols = telnet.getController().getColumnCount();
        int rr   = telnet.getController().getCursorAddress() / cols;
        int np   = (scroll_top - 1) - rr;
        int nn   = 1;
        int ns;

        held_wrap = false;

        /* If the cursor is above the scrolling region, do a simple margined cursor up. */
        if (np < 0) {
            ansi_cursor_up(nn, 0);
            return AnsiState.DATA;
        }

        if (nn > np) {
            ns = nn - np;
            nn = np;
        } else {
            ns = 0;
        }

        if (nn != 0) ansi_cursor_up(nn, 0);
        if (ns != 0) ansi_insert_lines(ns, 0);

        return AnsiState.DATA;
    }

    private AnsiState ansi_send_attributes(int nn, int ig2) {
        if (nn == 0) telnet.sendString("\033[?1;2c");
        return AnsiState.DATA;
    }

    private AnsiState dec_return_terminal_id(int ig1, int ig2) {
        return ansi_send_attributes(0, 0);
    }

    private AnsiState ansi_set_mode(int nn, int ig2) {
        switch (nn) {
            case 4:  ansi_insert_mode  = true; break;
            case 20: auto_newline_mode = true; break;
        }
        return AnsiState.DATA;
    }

    private AnsiState ansi_reset_mode(int nn, int ig2) {
        switch (nn) {
            case 4:  ansi_insert_mode  = false; break;
            case 20: auto_newline_mode = false; break;
        }
        return AnsiState.DATA;
    }

    private AnsiState ansi_status_report(int nn, int ig2) {
        switch (nn) {
            case 5:
                telnet.sendString("\033[0n");
                break;
            case 6: {
                int cols = telnet.getController().getColumnCount();
                int cur  = telnet.getController().getCursorAddress();
                String cpr = "\033[" + (cur / cols + 1) + ";" + (cur % cols + 1) + "R";
                telnet.sendString(cpr);
                break;
            }
        }
        return AnsiState.DATA;
    }

    private AnsiState ansi_cs_designate(int ig1, int ig2) {
        cs_to_change = gnnames.indexOf((char) ansi_ch);
        return AnsiState.CSDES;
    }

    private AnsiState ansi_cs_designate2(int ig1, int ig2) {
        csd[cs_to_change] = csnames.indexOf((char) ansi_ch);
        return AnsiState.DATA;
    }

    private AnsiState ansi_select_g0(int ig1, int ig2) { cset = CS_G0; return AnsiState.DATA; }
    private AnsiState ansi_select_g1(int ig1, int ig2) { cset = CS_G1; return AnsiState.DATA; }
    private AnsiState ansi_select_g2(int ig1, int ig2) { cset = CS_G2; return AnsiState.DATA; }
    private AnsiState ansi_select_g3(int ig1, int ig2) { cset = CS_G3; return AnsiState.DATA; }

    private AnsiState ansi_one_g2(int ig1, int ig2) { once_cset = CS_G2; return AnsiState.DATA; }
    private AnsiState ansi_one_g3(int ig1, int ig2) { once_cset = CS_G3; return AnsiState.DATA; }

    private AnsiState ansi_esc3(int ig1, int ig2) {
        return AnsiState.DECP;
    }

    private AnsiState dec_set(int ig1, int ig2) {
        for (int i = 0; i <= nx && i < NN; i++) {
            switch (n[i]) {
                case 1:  appl_cursor = 1; break;
                case 2:  csd[0] = csd[1] = csd[2] = csd[3] = CSD_US; break;
                case 3:
                    if (allow_wide_mode) { wide_mode = true; /* screen_132() */ }
                    break;
                case 7:  wraparound_mode = true; break;
                case 40: allow_wide_mode = true; break;
                case 45: rev_wraparound_mode = true; break;
                case 47: telnet.getController().swapAltBuffers(true); break;
            }
        }
        return AnsiState.DATA;
    }

    private AnsiState dec_reset(int ig1, int ig2) {
        for (int i = 0; i <= nx && i < NN; i++) {
            switch (n[i]) {
                case 1:  appl_cursor = 0; break;
                case 3:
                    if (allow_wide_mode) { wide_mode = false; /* screen_80() */ }
                    break;
                case 7:  wraparound_mode = false; break;
                case 40: allow_wide_mode = false; break;
                case 45: rev_wraparound_mode = false; break;
                case 47: telnet.getController().swapAltBuffers(false); break;
            }
        }
        return AnsiState.DATA;
    }

    private AnsiState dec_save(int ig1, int ig2) {
        for (int i = 0; i <= nx && i < NN; i++) {
            switch (n[i]) {
                case 1:  saved_appl_cursor       = appl_cursor;       break;
                case 3:  saved_wide_mode         = wide_mode;          break;
                case 7:  saved_wraparound_mode   = wraparound_mode;    break;
                case 40: saved_allow_wide_mode   = allow_wide_mode;    break;
                case 45: saved_rev_wraparound_mode = rev_wraparound_mode; break;
                case 47: saved_altbuffer         = telnet.getController().getIsAltBuffer(); break;
            }
        }
        return AnsiState.DATA;
    }

    private AnsiState dec_restore(int ig1, int ig2) {
        for (int i = 0; i <= nx && i < NN; i++) {
            switch (n[i]) {
                case 1:  appl_cursor = saved_appl_cursor; break;
                case 3:
                    if (allow_wide_mode) wide_mode = saved_wide_mode;
                    break;
                case 7:  wraparound_mode    = saved_wraparound_mode;    break;
                case 40: allow_wide_mode    = saved_allow_wide_mode;     break;
                case 45: rev_wraparound_mode = saved_rev_wraparound_mode; break;
                case 47: telnet.getController().swapAltBuffers(saved_altbuffer); break;
            }
        }
        return AnsiState.DATA;
    }

    private AnsiState dec_scrolling_region(int top, int bottom) {
        int rows = telnet.getController().getRowCount();
        if (top < 1) top = 1;
        if (bottom > rows) bottom = rows;
        if (top <= bottom && (top > 1 || bottom < rows)) {
            scroll_top    = top;
            scroll_bottom = bottom;
            telnet.getController().setCursorAddress(0);
        } else {
            scroll_top    = 1;
            scroll_bottom = rows;
        }
        return AnsiState.DATA;
    }

    private AnsiState xterm_text_mode(int ig1, int ig2) {
        nx   = 0;
        n[0] = 0;
        return AnsiState.TEXT;
    }

    private AnsiState xterm_text_semicolon(int ig1, int ig2) {
        tx = 0;
        return AnsiState.TEXT2;
    }

    private AnsiState xterm_text(int ig1, int ig2) {
        if (tx < NT) {
            text += ansi_ch;
            tx++;
        }
        return state;
    }

    private AnsiState xterm_text_do(int ig1, int ig2) {
        return AnsiState.DATA;
    }

    private AnsiState ansi_htab_set(int ig1, int ig2) {
        int col = telnet.getController().getCursorAddress() % telnet.getController().getColumnCount();
        tabs[col / 8] = (byte) (tabs[col / 8] | (1 << (col % 8)));
        return AnsiState.DATA;
    }

    private AnsiState ansi_htab_clear(int nn, int ig2) {
        int cols = telnet.getController().getColumnCount();
        switch (nn) {
            case 0: {
                int col = telnet.getController().getCursorAddress() % cols;
                tabs[col / 8] = (byte) (tabs[col / 8] & ~(1 << (col % 8)));
                break;
            }
            case 3:
                for (int i = 0; i < (cols + 7) / 8; i++) tabs[i] = 0;
                break;
        }
        return AnsiState.DATA;
    }

    // ------------------------------------------------------------------ //
    // Scroll helper
    // ------------------------------------------------------------------ //

    private void ansi_scroll() {
        int cols = telnet.getController().getColumnCount();
        held_wrap = false;

        /* Full-screen scroll — delegate to controller's scroll-one */
        if (scroll_top == 1 && scroll_bottom == telnet.getController().getRowCount()) {
            telnet.getController().scrollOne();
            return;
        }

        /* Scroll all but the last line up */
        if (scroll_bottom > scroll_top)
            telnet.getController().copyBlock(
                scroll_top * cols,
                (scroll_top - 1) * cols,
                (scroll_bottom - scroll_top) * cols,
                true);

        /* Clear the last line */
        telnet.getController().eraseRegion((scroll_bottom - 1) * cols, cols, true);
    }
}
