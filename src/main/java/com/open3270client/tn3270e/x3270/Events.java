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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Error / event accumulator for the Telnet layer.
 * Port of {@code Events.cs}.
 */
class Events {

    private static final Logger log = LoggerFactory.getLogger(Events.class);

    private final Telnet telnet;
    private final List<EventNotification> events = new ArrayList<>();

    Events(Telnet tn) {
        this.telnet = tn;
    }

    void clear() {
        events.clear();
    }

    String getErrorAsText() {
        if (events.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (EventNotification n : events) sb.append(n);
        return sb.toString();
    }

    boolean isError() {
        return !events.isEmpty();
    }

    void showError(String error, Object... args) {
        events.add(new EventNotification(error, args));
        log.error("ERROR {}", TraceFormatter.format(error, args));
    }

    void warning(String warning) {
        log.warn("warning=={}", warning);
    }

    void runScript(String where) {
        synchronized (telnet) {
            if ((telnet.getKeyboard().getKeyboardLock() | KeyboardConstants.DeferredUnlock) == KeyboardConstants.DeferredUnlock) {
                telnet.getKeyboard().keyboardLockClear(KeyboardConstants.DeferredUnlock, "defer_unlock");
                if (telnet.isConnected()) telnet.getController().processPendingInput();
            }
        }
        if (telnet.getTelnetApi() != null) telnet.getTelnetApi().runScript(where);
    }

    // ------------------------------------------------------------------ //
    // Inner notification record
    // ------------------------------------------------------------------ //
    private static class EventNotification {
        final String error;
        final Object[] data;

        EventNotification(String error, Object[] data) {
            this.error = error;
            this.data  = data;
        }

        @Override
        public String toString() {
            return TraceFormatter.format(error, data);
        }
    }
}
