package com.open3270client.tn3270e;

/** State of the VT/ANSI control-character parser. */
public enum ControllerState {
    DATA(0), ESC(1), CSDES(2), N1(3), DECP(4), TEXT(5), TEXT2(6);

    private final int value;
    ControllerState(int value) { this.value = value; }
    public int getValue() { return value; }
}
