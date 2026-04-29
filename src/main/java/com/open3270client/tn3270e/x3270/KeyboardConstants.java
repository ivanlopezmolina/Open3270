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
 * Keyboard lock-bit constants.
 * Port of {@code KeyboardConstants.cs}.
 */
class KeyboardConstants {

    // ------------------------------------------------------------------ //
    // Error codes (low nibble of keyboardLock)
    // ------------------------------------------------------------------ //
    static final int ErrorMask       = 0x000f;
    static final int ErrorProtected  = 1;
    static final int ErrorNumeric    = 2;
    static final int ErrorOverflow   = 3;

    // ------------------------------------------------------------------ //
    // Keyboard-lock bits
    // ------------------------------------------------------------------ //
    static final int NotConnected    = 0x0010;
    static final int AwaitingFirst   = 0x0020;
    static final int OiaTWait        = 0x0040;
    static final int OiaLocked       = 0x0080;
    static final int DeferredUnlock  = 0x0100;
    static final int EnterInhibit    = 0x0200;
    static final int Scrolled        = 0x0400;
    static final int OiaMinus        = 0x0800;

    // ------------------------------------------------------------------ //
    // Miscellaneous constants
    // ------------------------------------------------------------------ //
    static final int NoSymbol        = 0;
    static final int WFlag           = 0x100;
    static final int PasteWFlag      = 0x200;
    static final int UnlockMS        = 350;

    // ------------------------------------------------------------------ //
    // AID translation tables
    // ------------------------------------------------------------------ //
    static final byte[] PfTranslation = {
        AID.F1,  AID.F2,  AID.F3,  AID.F4,  AID.F5,  AID.F6,
        AID.F7,  AID.F8,  AID.F9,  AID.F10, AID.F11, AID.F12,
        AID.F13, AID.F14, AID.F15, AID.F16, AID.F17, AID.F18,
        AID.F19, AID.F20, AID.F21, AID.F22, AID.F23, AID.F24
    };

    static final byte[] PaTranslation = {
        AID.PA1, AID.PA2, AID.PA3
    };

    private KeyboardConstants() { /* static-only */ }
}
