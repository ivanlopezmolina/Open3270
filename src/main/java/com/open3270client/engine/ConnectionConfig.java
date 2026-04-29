/*
 * Open3270Client - Java / Spring Boot port of Open3270
 * Original C# library: https://github.com/Open3270/Open3270
 * Original authors: Mike Warriner, Francois Botha
 * Original license: MIT
 *
 * Java port copyright (c) 2026 Ivanlopezmolina
 * Released under the MIT License.
 */
package com.open3270client.engine;

import com.open3270client.interfaces.IAudit;

import java.io.BufferedReader;

/**
 * Configuration parameters for a TN3270/TN3270E connection.
 */
public class ConnectionConfig {

    private String hostName;
    private int hostPort = 23;
    private String hostLU;
    private String termType;
    private boolean useSSL = false;
    private boolean fastScreenMode = false;
    private BufferedReader logFile = null;
    private boolean ignoreSequenceCount = false;
    private boolean identificationEngineOn = false;
    private boolean alwaysSkipToUnprotected = true;
    private boolean lockScreenOnWriteToUnprotected = false;
    private boolean throwExceptionOnLockedScreen = true;
    private int defaultTimeout = 40_000;
    private boolean alwaysRefreshWhenWaiting = false;
    private boolean submitAllKeyboardCommands = false;
    private boolean refuseTN3270E = false;

    public ConnectionConfig() {
    }

    /** Dumps the current configuration to the audit stream. */
    public void dump(IAudit sout) {
        if (sout == null) return;
        sout.writeLine("Config.FastScreenMode " + fastScreenMode);
        sout.writeLine("Config.IgnoreSequenceCount " + ignoreSequenceCount);
        sout.writeLine("Config.IdentificationEngineOn " + identificationEngineOn);
        sout.writeLine("Config.AlwaysSkipToUnprotected " + alwaysSkipToUnprotected);
        sout.writeLine("Config.LockScreenOnWriteToUnprotected " + lockScreenOnWriteToUnprotected);
        sout.writeLine("Config.ThrowExceptionOnLockedScreen " + throwExceptionOnLockedScreen);
        sout.writeLine("Config.DefaultTimeout " + defaultTimeout);
        sout.writeLine("Config.hostName " + hostName);
        sout.writeLine("Config.hostPort " + hostPort);
        sout.writeLine("Config.hostLU " + hostLU);
        sout.writeLine("Config.termType " + termType);
        sout.writeLine("Config.AlwaysRefreshWhenWaiting " + alwaysRefreshWhenWaiting);
        sout.writeLine("Config.SubmitAllKeyboardCommands " + submitAllKeyboardCommands);
        sout.writeLine("Config.RefuseTN3270E " + refuseTN3270E);
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public int getHostPort() { return hostPort; }
    public void setHostPort(int hostPort) { this.hostPort = hostPort; }

    public String getHostLU() { return hostLU; }
    public void setHostLU(String hostLU) { this.hostLU = hostLU; }

    public String getTermType() { return termType; }
    public void setTermType(String termType) { this.termType = termType; }

    public boolean isUseSSL() { return useSSL; }
    public void setUseSSL(boolean useSSL) { this.useSSL = useSSL; }

    public boolean isFastScreenMode() { return fastScreenMode; }
    public void setFastScreenMode(boolean fastScreenMode) { this.fastScreenMode = fastScreenMode; }

    public BufferedReader getLogFile() { return logFile; }
    public void setLogFile(BufferedReader logFile) { this.logFile = logFile; }

    public boolean isIgnoreSequenceCount() { return ignoreSequenceCount; }
    public void setIgnoreSequenceCount(boolean ignoreSequenceCount) { this.ignoreSequenceCount = ignoreSequenceCount; }

    public boolean isIdentificationEngineOn() { return identificationEngineOn; }
    public void setIdentificationEngineOn(boolean identificationEngineOn) { this.identificationEngineOn = identificationEngineOn; }

    public boolean isAlwaysSkipToUnprotected() { return alwaysSkipToUnprotected; }
    public void setAlwaysSkipToUnprotected(boolean alwaysSkipToUnprotected) { this.alwaysSkipToUnprotected = alwaysSkipToUnprotected; }

    public boolean isLockScreenOnWriteToUnprotected() { return lockScreenOnWriteToUnprotected; }
    public void setLockScreenOnWriteToUnprotected(boolean v) { this.lockScreenOnWriteToUnprotected = v; }

    public boolean isThrowExceptionOnLockedScreen() { return throwExceptionOnLockedScreen; }
    public void setThrowExceptionOnLockedScreen(boolean v) { this.throwExceptionOnLockedScreen = v; }

    public int getDefaultTimeout() { return defaultTimeout; }
    public void setDefaultTimeout(int defaultTimeout) { this.defaultTimeout = defaultTimeout; }

    public boolean isAlwaysRefreshWhenWaiting() { return alwaysRefreshWhenWaiting; }
    public void setAlwaysRefreshWhenWaiting(boolean v) { this.alwaysRefreshWhenWaiting = v; }

    public boolean isSubmitAllKeyboardCommands() { return submitAllKeyboardCommands; }
    public void setSubmitAllKeyboardCommands(boolean v) { this.submitAllKeyboardCommands = v; }

    public boolean isRefuseTN3270E() { return refuseTN3270E; }
    public void setRefuseTN3270E(boolean refuseTN3270E) { this.refuseTN3270E = refuseTN3270E; }
}
