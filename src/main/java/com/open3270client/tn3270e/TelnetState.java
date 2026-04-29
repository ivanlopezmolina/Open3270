package com.open3270client.tn3270e;

/** State of the Telnet protocol parser. */
public enum TelnetState { DATA, IAC, WILL, WONT, DO, DONT, SB, SB_IAC }
