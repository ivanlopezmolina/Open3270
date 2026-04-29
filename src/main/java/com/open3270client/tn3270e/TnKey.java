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

/** All keyboard keys that can be sent to a TN3270 host. */
public enum TnKey {
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
    F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24,
    TAB,
    BACK_TAB,
    ENTER,
    BACKSPACE,
    CLEAR,
    DELETE,
    DELETE_FIELD,
    DELETE_WORD,
    LEFT,
    LEFT2,
    UP,
    RIGHT,
    RIGHT2,
    DOWN,
    ATTN,
    CIRCUM_NOT,
    CURSOR_SELECT,
    DUP,
    ERASE,
    ERASE_EOF,
    ERASE_INPUT,
    FIELD_END,
    FIELD_MARK,
    FIELD_EXIT,
    HOME,
    INSERT,
    INTERRUPT,
    KEY,
    NEWLINE,
    NEXT_WORD,
    PANN,
    PREVIOUS_WORD,
    RESET,
    SYS_REQ,
    TOGGLE,
    TOGGLE_INSERT,
    TOGGLE_REVERSE,
    PA1, PA2, PA3, PA4, PA5, PA6, PA7, PA8, PA9, PA10, PA11, PA12
}
