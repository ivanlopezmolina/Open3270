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

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Idle-session timer — fires a keep-alive macro after a configurable inactivity period.
 * Port of {@code Idle.cs}.
 */
class Idle implements AutoCloseable {

    private static final int IDLE_MILLISECONDS = 7 * 60 * 1000; // 7 minutes

    private final Telnet telnet;
    private final Random rand = new Random();

    private Timer idleTimer;
    private boolean idleWasIn3270 = false;
    private boolean randomize     = false;
    private boolean isTicking     = false;
    private int     milliseconds  = 0;

    Idle(Telnet tn) {
        this.telnet = tn;
    }

    /** Register the 3270-connection listener and start the idle timer infrastructure. */
    void initialize() {
        telnet.addConnected3270Listener(this::idleIn3270);
    }

    private int processTimeoutValue(String t) {
        if (t == null || t.isEmpty()) {
            this.milliseconds = IDLE_MILLISECONDS;
            this.randomize    = true;
            return 0;
        }
        if (t.charAt(0) == '~') { randomize = true; t = t.substring(1); }
        throw new UnsupportedOperationException("process_timeout_value not implemented");
    }

    void idleIn3270(boolean in3270) {
        if (in3270 && !idleWasIn3270) {
            idleWasIn3270 = true;
        } else {
            cancelTimer();
            idleWasIn3270 = false;
        }
    }

    private void timedOut() {
        synchronized (telnet) {
            telnet.getTrace().trace_event("Idle timeout\n");
            resetIdleTimer();
        }
    }

    /** Reset (and re-enable) the idle timer.  Called when the user presses an AID key. */
    void resetIdleTimer() {
        if (milliseconds == 0) return;
        cancelTimer();
        int idleMsNow = milliseconds;
        if (randomize) {
            if ((rand.nextInt(100) & 1) != 0) idleMsNow += rand.nextInt(milliseconds / 10);
            else                              idleMsNow -= rand.nextInt(milliseconds / 10);
        }
        telnet.getTrace().trace_event("Setting idle timeout to " + idleMsNow);
        idleTimer = new Timer("idle-timer", true);
        idleTimer.schedule(new TimerTask() { @Override public void run() { timedOut(); } }, idleMsNow);
        isTicking = true;
    }

    private void cancelTimer() {
        if (isTicking && idleTimer != null) { idleTimer.cancel(); idleTimer = null; isTicking = false; }
    }

    @Override
    public void close() {
        cancelTimer();
        telnet.removeConnected3270Listener(this::idleIn3270);
    }
}
