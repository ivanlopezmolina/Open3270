package com.open3270client.tn3270e;

/** State for the emulated-input (EI) string parser. */
public enum EIState {
    BASE, BACKSLASH, BACK_X, BACK_P, BACK_PA, BACK_PF, OCTAL, HEX, XGE
}
