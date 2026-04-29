package com.open3270client.tn3270e;

/** Source / reason for an emulated-input action. */
public enum EIAction {
    STRING, PASTE, REDRAW, KEYPAD, DEFAULT, KEY, MACRO, SCRIPT,
    PEEK, TYPE_AHEAD, FT, COMMAND, KEY_MAP
}
