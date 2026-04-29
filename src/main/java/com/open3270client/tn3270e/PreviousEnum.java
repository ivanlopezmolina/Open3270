package com.open3270client.tn3270e;

/** Tracks what token type immediately preceded the current position in a 3270 data stream. */
public enum PreviousEnum { NONE, ORDER, SBA, TEXT, NULL_CHARACTER }
