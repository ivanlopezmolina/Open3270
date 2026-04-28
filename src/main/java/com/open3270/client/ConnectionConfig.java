/*
 * MIT License
 *
 * Copyright (c) 2026 ivanlopezmolina
 *
 * Originally based on Open3270 - A C# implementation of the TN3270/TN3270E protocol
 * Original authors: Michael Warriner and contributors (c) 2004-2020
 * Original project: https://github.com/Open3270/Open3270
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.open3270.client;

import com.open3270.client.interfaces.IAudit;

/**
 * Holds configuration options for a TN3270 connection.
 */
public class ConnectionConfig {
    private boolean fastScreenMode                = false;
    private boolean ignoreSequenceCount           = false;
    private boolean identificationEngineOn        = false;
    private boolean alwaysSkipToUnprotected       = true;
    private boolean lockScreenOnWriteToUnprotected= false;
    private boolean throwExceptionOnLockedScreen  = true;
    private int     defaultTimeout                = 40000;
    private String  hostName                      = null;
    private int     hostPort                      = 23;
    private String  hostLU                        = null;
    private String  termType                      = "IBM-3278-2-E";
    private boolean alwaysRefreshWhenWaiting      = false;
    private boolean submitAllKeyboardCommands     = false;
    private boolean refuseTN3270E                 = false;
    private boolean useSSL                        = false;

    public ConnectionConfig() {}

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
        sout.writeLine("Config.UseSSL " + useSSL);
    }

    public boolean isFastScreenMode() { return fastScreenMode; }
    public void setFastScreenMode(boolean fastScreenMode) { this.fastScreenMode = fastScreenMode; }

    public boolean isIgnoreSequenceCount() { return ignoreSequenceCount; }
    public void setIgnoreSequenceCount(boolean ignoreSequenceCount) { this.ignoreSequenceCount = ignoreSequenceCount; }

    public boolean isIdentificationEngineOn() { return identificationEngineOn; }
    public void setIdentificationEngineOn(boolean identificationEngineOn) { this.identificationEngineOn = identificationEngineOn; }

    public boolean isAlwaysSkipToUnprotected() { return alwaysSkipToUnprotected; }
    public void setAlwaysSkipToUnprotected(boolean alwaysSkipToUnprotected) { this.alwaysSkipToUnprotected = alwaysSkipToUnprotected; }

    public boolean isLockScreenOnWriteToUnprotected() { return lockScreenOnWriteToUnprotected; }
    public void setLockScreenOnWriteToUnprotected(boolean v) { this.lockScreenOnWriteToUnprotected = v; }

    public boolean isThrowExceptionOnLockedScreen() { return throwExceptionOnLockedScreen; }
    public void setThrowExceptionOnLockedScreen(boolean throwExceptionOnLockedScreen) { this.throwExceptionOnLockedScreen = throwExceptionOnLockedScreen; }

    public int getDefaultTimeout() { return defaultTimeout; }
    public void setDefaultTimeout(int defaultTimeout) { this.defaultTimeout = defaultTimeout; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public int getHostPort() { return hostPort; }
    public void setHostPort(int hostPort) { this.hostPort = hostPort; }

    public String getHostLU() { return hostLU; }
    public void setHostLU(String hostLU) { this.hostLU = hostLU; }

    public String getTermType() { return termType != null ? termType : "IBM-3278-2-E"; }
    public void setTermType(String termType) { this.termType = termType; }

    public boolean isAlwaysRefreshWhenWaiting() { return alwaysRefreshWhenWaiting; }
    public void setAlwaysRefreshWhenWaiting(boolean alwaysRefreshWhenWaiting) { this.alwaysRefreshWhenWaiting = alwaysRefreshWhenWaiting; }

    public boolean isSubmitAllKeyboardCommands() { return submitAllKeyboardCommands; }
    public void setSubmitAllKeyboardCommands(boolean submitAllKeyboardCommands) { this.submitAllKeyboardCommands = submitAllKeyboardCommands; }

    public boolean isRefuseTN3270E() { return refuseTN3270E; }
    public void setRefuseTN3270E(boolean refuseTN3270E) { this.refuseTN3270E = refuseTN3270E; }

    public boolean isUseSSL() { return useSSL; }
    public void setUseSSL(boolean useSSL) { this.useSSL = useSSL; }
}
