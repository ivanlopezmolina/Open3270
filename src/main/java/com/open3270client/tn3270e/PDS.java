package com.open3270client.tn3270e;

/** Return codes from the Partition Data Set (PDS) processing. */
public enum PDS {
    OKAY_NO_OUTPUT(0),
    OKAY_OUTPUT(1),
    BAD_COMMAND(-1),
    BAD_ADDRESS(-2);

    private final int value;
    PDS(int value) { this.value = value; }
    public int getValue() { return value; }

    public static PDS fromValue(int v) {
        for (PDS p : values()) if (p.value == v) return p;
        return v >= 0 ? OKAY_OUTPUT : BAD_COMMAND;
    }
}
