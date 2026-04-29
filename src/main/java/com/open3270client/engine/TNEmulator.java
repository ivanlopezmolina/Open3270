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

import com.open3270client.commframework.MySemaphore;
import com.open3270client.exceptions.TNHostException;
import com.open3270client.exceptions.TNIdentificationException;
import com.open3270client.interfaces.IAudit;
import com.open3270client.interfaces.IXMLScreen;
import com.open3270client.interfaces.StringPosition;
import com.open3270client.tn3270e.*;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * High-level TN3270/TN3270E emulator facade.
 *
 * <p>This is the primary public API for connecting to, interacting with,
 * and disconnecting from a mainframe TN3270 session.
 *
 * <p>Usage example:
 * <pre>{@code
 * TNEmulator emulator = new TNEmulator();
 * emulator.getConfig().setHostName("mainframe.example.com");
 * emulator.getConfig().setHostPort(23);
 * emulator.connect();
 * emulator.sendKey(true, TnKey.ENTER, 10_000);
 * String text = emulator.getText(0, 0, 80);
 * emulator.close();
 * }</pre>
 */
public class TNEmulator implements AutoCloseable {

    // ------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------

    private boolean debug = false;
    private boolean disposed = false;

    private final MySemaphore semaphore = new MySemaphore(0, 9999);
    private Object objectState;

    private IXMLScreen currentScreenXML;
    private String screenName;
    private IAudit sout;
    private boolean useSSL = false;
    private String localIP = "";

    private final ConnectionConfig config;
    private TN3270API currentConnection;

    // ------------------------------------------------------------------
    // Listeners (Java equivalents of C# events)
    // ------------------------------------------------------------------

    private final List<OnDisconnectListener> disconnectListeners = new ArrayList<>();
    private final List<EventListener> cursorChangedListeners = new ArrayList<>();

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public TNEmulator() {
        this.config = new ConnectionConfig();
        this.currentScreenXML = null;
        this.currentConnection = null;
    }

    // ------------------------------------------------------------------
    // Properties
    // ------------------------------------------------------------------

    /** Returns {@code true} if the session is currently connected to the host. */
    public boolean isConnected() {
        return currentConnection != null && currentConnection.isConnected();
    }

    /** Returns {@code true} if {@link #close()} has been called. */
    public boolean isDisposed() { return disposed; }

    /** The reason the session was disconnected, or empty string if still connected. */
    public String getDisconnectReason() {
        synchronized (this) {
            return (currentConnection != null) ? currentConnection.getDisconnectReason() : "";
        }
    }

    /**
     * Returns the current keyboard lock state.
     * A value of {@code 0} means the keyboard is locked (inhibited).
     */
    public int getKeyboardLocked() {
        requireConnected();
        return currentConnection.getKeyboardLock();
    }

    /** Zero-based X (column) position of the cursor. */
    public int getCursorX() {
        requireConnected();
        return currentConnection.getCursorX();
    }

    /** Zero-based Y (row) position of the cursor. */
    public int getCursorY() {
        requireConnected();
        return currentConnection.getCursorY();
    }

    /** IP address of the local end-point. */
    public String getLocalIP() { return localIP; }

    /** The connection configuration object for this session. */
    public ConnectionConfig getConfig() { return config; }

    /** When {@code true}, verbose debugging is written to the audit stream. */
    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }

    /** Whether SSL should be used for the connection. */
    public boolean isUseSSL() { return useSSL; }
    public void setUseSSL(boolean useSSL) { this.useSSL = useSSL; }

    /** Arbitrary state object that callers can attach to the emulator instance. */
    public Object getObjectState() { return objectState; }
    public void setObjectState(Object objectState) { this.objectState = objectState; }

    /** The audit / logging interface. Set before connecting to capture output. */
    public IAudit getAudit() { return sout; }
    public void setAudit(IAudit sout) { this.sout = sout; }

    /** Returns the current screen snapshot (lazy-loaded on first access). */
    public IXMLScreen getCurrentScreenXML() {
        if (currentScreenXML == null) {
            if (sout != null && debug) {
                sout.writeLine("CurrentScreenXML reloading by calling getScreenAsXML()");
                currentScreenXML = getScreenAsXML();
                if (currentScreenXML != null) currentScreenXML.dump(sout);
            } else {
                currentScreenXML = getScreenAsXML();
            }
        }
        return currentScreenXML;
    }

    // ------------------------------------------------------------------
    // Listener registration
    // ------------------------------------------------------------------

    public void addDisconnectListener(OnDisconnectListener l) { disconnectListeners.add(l); }
    public void removeDisconnectListener(OnDisconnectListener l) { disconnectListeners.remove(l); }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Sends a key stroke to the emulator.
     *
     * @param waitForScreenToUpdate when {@code true}, blocks until the host responds
     * @param key                   the key to send
     * @param timeout               maximum wait time in milliseconds
     * @return {@code true} on success
     */
    public boolean sendKey(boolean waitForScreenToUpdate, TnKey key, int timeout) {
        requireConnected();

        if (sout != null && debug) {
            sout.writeLine("sendKey(" + waitForScreenToUpdate + ", \"" + key + "\", " + timeout + ")");
        }

        String command;
        int functionInteger = -1;

        if (Constants.isFunctionKey(key)) {
            command = "PF";
            functionInteger = Constants.FUNCTION_KEY_INT_LUT.get(key);
        } else if (Constants.isAKey(key)) {
            command = "PA";
            functionInteger = Constants.FUNCTION_KEY_INT_LUT.get(key);
        } else {
            command = key.name();
        }

        boolean triggerSubmit = config.isSubmitAllKeyboardCommands() ||
                currentConnection.keyboardCommandCausesSubmit(command);

        if (triggerSubmit) {
            synchronized (this) {
                disposeCurrentScreenXML();
                currentScreenXML = null;
                if (sout != null && debug) {
                    sout.writeLine("semaphore.reset. Count was " + semaphore.getCount());
                }
                semaphore.reset();
            }
        }

        boolean success = currentConnection.executeAction(triggerSubmit, command, functionInteger);

        if (sout != null && debug) {
            sout.writeLine("sendKey - submit=" + triggerSubmit + " ok=" + success);
        }

        if (triggerSubmit && success && waitForScreenToUpdate) {
            success = refresh(true, timeout);
        }
        return success;
    }

    /**
     * Blocks until the keyboard is unlocked or the timeout expires.
     *
     * @param timeoutMs maximum wait time in milliseconds
     */
    public void waitTillKeyboardUnlocked(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (getKeyboardLocked() != 0 && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(10); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Waits for a valid screen to appear.
     *
     * @param waitForValidScreen {@code true} to wait for a fully unlocked screen
     * @param timeoutMs          maximum time to wait in milliseconds
     * @return {@code true} if a screen was obtained within the timeout
     */
    public boolean refresh(boolean waitForValidScreen, int timeoutMs) {
        requireConnected();

        if (sout != null && debug) {
            sout.writeLine("Refresh(" + waitForValidScreen + ", " + timeoutMs +
                    "). FastScreenMode=" + config.isFastScreenMode());
        }

        long end = System.currentTimeMillis() + timeoutMs;

        do {
            if (waitForValidScreen) {
                boolean acquired = false;
                int remaining;
                do {
                    remaining = (int) (end - System.currentTimeMillis());
                    if (remaining > 0) {
                        if (sout != null && debug) {
                            sout.writeLine("Refresh::acquire(" + remaining + " ms). count=" + semaphore.getCount());
                        }
                        acquired = semaphore.acquire(Math.min(remaining, 1000));

                        if (!isConnected()) {
                            throw new TNHostException("The TN3270 connection was lost",
                                    currentConnection.getDisconnectReason(), null);
                        }
                        if (acquired) {
                            if (sout != null && debug) sout.writeLine("Refresh::return true");
                            return true;
                        }
                    }
                } while (!acquired && remaining > 0);
            }

            if (config.isFastScreenMode() || getKeyboardLocked() == 0) {
                disposeCurrentScreenXML();
                currentScreenXML = null;
                return true;
            } else {
                try { Thread.sleep(10); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } while (System.currentTimeMillis() < end);

        if (sout != null) {
            sout.writeLine("Refresh::Timed out waiting for valid screen. Timeout was " + timeoutMs);
        }

        if (!config.isFastScreenMode() && config.isThrowExceptionOnLockedScreen() && getKeyboardLocked() != 0) {
            throw new IllegalStateException(
                    "Timeout waiting for new screen with keyboard inhibit false. " +
                    "Turn off 'ThrowExceptionOnLockedScreen' to suppress this. " +
                    "Timeout=" + timeoutMs + ", keyboardLock=" + getKeyboardLocked());
        }

        if (config.isIdentificationEngineOn()) {
            throw new TNIdentificationException(screenName, getScreenAsXML());
        }
        return false;
    }

    /**
     * Returns text at the specified screen coordinates.
     *
     * @param x      column (0-based)
     * @param y      row (0-based)
     * @param length number of characters
     */
    public String getText(int x, int y, int length) {
        return getCurrentScreenXML().getText(x, y, length);
    }

    /**
     * Sends text starting at the given position.
     *
     * @param text the text to enter
     * @param x    column (0-based)
     * @param y    row (0-based)
     * @return {@code true} on success
     */
    public boolean setText(String text, int x, int y) {
        requireConnected();
        setCursor(x, y);
        return setText(text);
    }

    /**
     * Sends text at the current cursor position.
     *
     * @param text the text to enter
     * @return {@code true} on success
     */
    public boolean setText(String text) {
        requireConnected();
        synchronized (this) {
            disposeCurrentScreenXML();
            currentScreenXML = null;
        }
        return currentConnection.executeAction(false, "String", -1, text);
    }

    /**
     * Waits for new screen data to stop flowing for at least {@code checkIntervalMs}.
     *
     * @param checkIntervalMs interval between polls in milliseconds
     * @param finalTimeoutMs  absolute maximum wait time in milliseconds
     * @return {@code true} if data settled; {@code false} on timeout
     */
    public boolean waitForHostSettle(int checkIntervalMs, int finalTimeoutMs) {
        int elapsed = 0;
        while (!refresh(true, checkIntervalMs)) {
            if (elapsed > finalTimeoutMs) return false;
            elapsed += checkIntervalMs;
        }
        return true;
    }

    /**
     * Returns the text of the last internal error.
     */
    public String getLastError() {
        requireConnected();
        return currentConnection.getLastError();
    }

    /**
     * Overrides the value of the field at the given index.
     *
     * @param index field index
     * @param text  new value
     */
    public void setField(int index, String text) {
        requireConnected();
        if (index == -1001) {
            if ("showparseerror".equals(text)) {
                currentConnection.setShowParseError(true);
            }
            return;
        }
        currentConnection.executeAction(false, "FieldSet", index, text);
        disposeCurrentScreenXML();
        currentScreenXML = null;
    }

    /**
     * Moves the cursor to the given position.
     *
     * @param x column (0-based)
     * @param y row (0-based)
     */
    public void setCursor(int x, int y) {
        requireConnected();
        currentConnection.moveCursor(CursorOp.EXACT, x, y);
    }

    /**
     * Connects to the host using the values in {@link #getConfig()}.
     */
    public void connect() {
        connect(config.getHostName(), config.getHostPort(), config.getHostLU());
    }

    /**
     * Connects to the host binding to a specific local IP address.
     *
     * @param localIP local IP address to bind to
     * @param host    hostname or IP
     * @param port    TCP port
     */
    public void connect(String localIP, String host, int port) {
        this.localIP = localIP;
        connect(host, port, "");
    }

    /**
     * Connects to the host.
     *
     * @param host hostname or IP address
     * @param port TCP port (default TN3270 port is 23)
     * @param lu   TN3270E LU name, or {@code null} for no LU
     */
    public void connect(String host, int port, String lu) {
        if (currentConnection != null) {
            currentConnection.disconnect();
        }

        semaphore.reset();
        currentConnection = null;

        TN3270API api = new TN3270API();
        api.setDebug(debug);
        api.setRunScriptListener(this::onRunScriptEvent);
        api.setDisconnectListener(this::onApiDisconnect);
        api.setUseSSL(useSSL);

        if (sout != null) {
            sout.writeLine("Open3270Client Java port — (c) 2026 Ivanlopezmolina");
            if (debug) {
                config.dump(sout);
                sout.writeLine("Connect to host \"" + host + "\"");
                sout.writeLine("           port \"" + port + "\"");
                sout.writeLine("           LU   \"" + lu + "\"");
                sout.writeLine("     Local IP   \"" + localIP + "\"");
            }
        }

        if (localIP != null && !localIP.isEmpty()) {
            api.connect(sout, localIP, host, port, config);
        } else {
            api.connect(sout, host, port, lu, config);
        }

        api.waitForConnect(-1);
        currentConnection = api;

        disposeCurrentScreenXML();
        currentScreenXML = null;

        screenName = "Start";
        refresh(true, 10_000);

        if (sout != null && debug) sout.writeLine("Debug::Connected");
    }

    /** Closes the current session. */
    public void disconnect() {
        if (currentConnection != null) {
            currentConnection.disconnect();
            currentConnection = null;
        }
    }

    /**
     * Waits for specific text to appear at a given location.
     *
     * @param x         column
     * @param y         row
     * @param text      expected text
     * @param timeoutMs max wait in milliseconds
     * @return {@code true} if the text appeared before the timeout
     */
    public boolean waitForText(int x, int y, String text, int timeoutMs) {
        requireConnected();
        long start = System.currentTimeMillis();

        if (config.isAlwaysRefreshWhenWaiting()) {
            synchronized (this) { disposeCurrentScreenXML(); currentScreenXML = null; }
        }

        do {
            if (getCurrentScreenXML() != null) {
                String screenText = getCurrentScreenXML().getText(x, y, text.length());
                if (text.equals(screenText)) {
                    if (sout != null) sout.writeLine("waitForText('" + text + "') Found!");
                    return true;
                }
            }
            if (timeoutMs == 0) {
                if (sout != null) sout.writeLine("waitForText('" + text + "') Not found");
                return false;
            }
            try { Thread.sleep(100); } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); break;
            }
            if (config.isAlwaysRefreshWhenWaiting()) {
                synchronized (this) { disposeCurrentScreenXML(); currentScreenXML = null; }
            }
            refresh(true, 1000);
        } while ((System.currentTimeMillis() - start) < timeoutMs);

        if (sout != null) sout.writeLine("waitForText('" + text + "') Timed out");
        return false;
    }

    /**
     * Searches the screen for any of the provided strings.
     *
     * @param timeoutMs max wait in milliseconds
     * @param text      strings to search for
     * @return index of the matched string, or {@code -1} on timeout
     */
    public int waitForTextOnScreen(int timeoutMs, String... text) {
        requireConnected();
        long start = System.currentTimeMillis();

        if (config.isAlwaysRefreshWhenWaiting()) {
            synchronized (this) { disposeCurrentScreenXML(); currentScreenXML = null; }
        }

        do {
            synchronized (this) {
                if (getCurrentScreenXML() != null) {
                    int index = getCurrentScreenXML().lookForTextStrings(text);
                    if (index != -1) {
                        if (sout != null) sout.writeLine("waitForTextOnScreen('" + text[index] + "') Found!");
                        return index;
                    }
                }
            }
            if (timeoutMs > 0) {
                try { Thread.sleep(100); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); break;
                }
                if (config.isAlwaysRefreshWhenWaiting()) {
                    synchronized (this) { disposeCurrentScreenXML(); currentScreenXML = null; }
                }
                refresh(true, 1000);
            }
        } while (timeoutMs > 0 && (System.currentTimeMillis() - start) < timeoutMs);

        if (sout != null) sout.writeLine("waitForTextOnScreen timed out");
        return -1;
    }

    /**
     * Searches the screen for any of the provided strings and returns position details.
     *
     * @param timeoutMs max wait in milliseconds
     * @param text      strings to search for
     * @return a {@link StringPosition} for the first match, or {@code null} on timeout
     */
    public StringPosition waitForTextOnScreen2(int timeoutMs, String... text) {
        if (waitForTextOnScreen(timeoutMs, text) != -1) {
            return getCurrentScreenXML().lookForTextStrings2(text);
        }
        return null;
    }

    /** Dumps the current screen to the audit output. */
    public void dump() {
        synchronized (this) {
            if (sout != null) getCurrentScreenXML().dump(sout);
        }
    }

    /** Invalidates the cached screen snapshot, forcing a refresh on next access. */
    public void refresh() {
        synchronized (this) {
            disposeCurrentScreenXML();
            currentScreenXML = null;
        }
    }

    /** Displays field information to the audit output. */
    public void showFields() {
        requireConnected();
        if (sout == null) throw new IllegalStateException("showFields requires an active Audit connection");
        sout.writeLine("-------------------dump screen data -----------------");
        currentConnection.executeAction(false, "Fields", -1, null);
        sout.writeLine(currentConnection.getAllStringData(false));
        getCurrentScreenXML().dump(sout);
        sout.writeLine("-------------------dump screen end -----------------");
    }

    // ------------------------------------------------------------------
    // AutoCloseable
    // ------------------------------------------------------------------

    @Override
    public void close() {
        synchronized (this) {
            if (disposed) return;
            disposed = true;

            if (sout != null && debug) sout.writeLine("TNEmulator.close()");

            if (currentConnection != null) {
                try {
                    currentConnection.disconnect();
                    currentConnection.close();
                } catch (Exception ignored) {
                }
                currentConnection = null;
            }

            disconnectListeners.clear();
            disposeCurrentScreenXML();
        }
    }

    // ------------------------------------------------------------------
    // Package-private / internal
    // ------------------------------------------------------------------

    IXMLScreen getScreenAsXML() {
        disposeCurrentScreenXML();
        requireConnected();

        if (currentConnection.executeAction(false, "DumpXML", -1, null)) {
            try {
                return XMLScreen.loadFromString(currentConnection.getAllStringData(false));
            } catch (Exception e) {
                if (sout != null) sout.writeLine("getScreenAsXML failed: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private void disposeCurrentScreenXML() {
        if (currentScreenXML instanceof AutoCloseable ac) {
            try { ac.close(); } catch (Exception ignored) {}
        }
        currentScreenXML = null;
    }

    private void requireConnected() {
        if (currentConnection == null) {
            throw new TNHostException("TNEmulator is not connected",
                    "There is no currently open TN3270 connection", null);
        }
    }

    private void onRunScriptEvent(String where) {
        synchronized (this) {
            disposeCurrentScreenXML();
            if (sout != null && debug) sout.writeLine("semaphore.release(1) from " + where);
            semaphore.release(1);
        }
    }

    private void onApiDisconnect(TNEmulator emulator, String reason) {
        for (OnDisconnectListener l : disconnectListeners) {
            l.onDisconnect(this, reason);
        }
    }
}
