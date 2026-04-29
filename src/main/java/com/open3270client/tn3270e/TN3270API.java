/*
 * Open3270Client - Java / Spring Boot port of Open3270
 * Original C# library: https://github.com/Open3270/Open3270
 * Original authors: Mike Warriner, Francois Botha
 * Original license: MIT
 *
 * Java port copyright (c) 2026 Ivanlopezmolina
 * Released under the MIT License.
 */
package com.open3270client.tn3270e;

import com.open3270client.engine.ConnectionConfig;
import com.open3270client.engine.TNEmulator;
import com.open3270client.interfaces.IAudit;

/**
 * Mid-tier connection adapter that wraps the raw Telnet protocol layer and
 * exposes a command-oriented API to {@link com.open3270client.engine.TNEmulator}.
 *
 * <p>Equivalent to {@code Open3270Library/TN3270E/TN3270API.cs}.
 */
public class TN3270API implements AutoCloseable {

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private boolean debug;
    private boolean useSSL;
    private boolean showParseError;
    private boolean connected;
    private String disconnectReason = "";
    private String lastError = "";

    private IAudit audit;
    private ConnectionConfig config;

    private RunScriptListener runScriptListener;
    private OnDisconnectListener disconnectListener;

    // Internal protocol driver — populated by connect()
    private Object telnet; // Telnet instance (stub — filled in when Telnet.java is implemented)

    // Snapshot of raw string data from the last command
    private String lastStringData = "";

    // Cursor state, delegated from the protocol layer
    private volatile int cursorX;
    private volatile int cursorY;
    private volatile int keyboardLock;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public TN3270API() {}

    // ------------------------------------------------------------------
    // Properties
    // ------------------------------------------------------------------

    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }

    public boolean isUseSSL() { return useSSL; }
    public void setUseSSL(boolean useSSL) { this.useSSL = useSSL; }

    public boolean isShowParseError() { return showParseError; }
    public void setShowParseError(boolean showParseError) { this.showParseError = showParseError; }

    public boolean isConnected() { return connected; }

    public String getDisconnectReason() { return disconnectReason; }
    public String getLastError() { return lastError; }

    public int getCursorX() { return cursorX; }
    public int getCursorY() { return cursorY; }
    public int getKeyboardLock() { return keyboardLock; }

    public void setRunScriptListener(RunScriptListener l) { this.runScriptListener = l; }
    public void setDisconnectListener(OnDisconnectListener l) { this.disconnectListener = l; }

    /**
     * Notifies any registered script listener that a scriptable event occurred.
     * @param where description of the location/event
     */
    public void runScript(String where) {
        if (runScriptListener != null) runScriptListener.runScript(where);
    }

    // ------------------------------------------------------------------
    // Connection management
    // ------------------------------------------------------------------

    /**
     * Connects to a TN3270 host.
     *
     * @param audit  optional audit / logging target
     * @param host   hostname or IP address
     * @param port   TCP port
     * @param lu     TN3270E LU name, or {@code null}
     * @param config connection configuration
     */
    public void connect(IAudit audit, String host, int port, String lu, ConnectionConfig config) {
        this.audit = audit;
        this.config = config;

        if (audit != null && debug) {
            audit.writeLine("TN3270API.connect(host=" + host + ", port=" + port + ", lu=" + lu + ")");
        }

        // TODO: instantiate and start Telnet here once Telnet.java is implemented
        connected = true;
    }

    /**
     * Connects to a TN3270 host binding to a specific local IP.
     *
     * @param audit   optional audit / logging target
     * @param localIP local IP address to bind to
     * @param host    hostname or IP address
     * @param port    TCP port
     * @param config  connection configuration
     */
    public void connect(IAudit audit, String localIP, String host, int port, ConnectionConfig config) {
        connect(audit, host, port, null, config);
    }

    /**
     * Blocks until the connection is fully established.
     *
     * @param timeoutMs max wait in milliseconds; use {@code -1} to wait indefinitely
     */
    public void waitForConnect(int timeoutMs) {
        // TODO: block until the TN3270E negotiation is complete
    }

    /** Disconnects from the host. */
    public void disconnect() {
        if (connected) {
            connected = false;
            disconnectReason = "disconnected";
            if (disconnectListener != null) {
                disconnectListener.onDisconnect(null, disconnectReason);
            }
        }
    }

    // ------------------------------------------------------------------
    // Screen and keyboard interaction
    // ------------------------------------------------------------------

    /**
     * Executes a protocol action.
     *
     * @param triggerSubmit {@code true} if this action is expected to submit the screen
     * @param command       action name (e.g., {@code "PF"}, {@code "String"}, {@code "DumpXML"})
     * @param functionInt   function key number (1-24), or {@code -1} for non-function actions
     * @return {@code true} on success
     */
    public boolean executeAction(boolean triggerSubmit, String command, int functionInt) {
        return executeAction(triggerSubmit, command, functionInt, null);
    }

    /**
     * Executes a protocol action with an optional text parameter.
     *
     * @param triggerSubmit {@code true} if this action is expected to submit the screen
     * @param command       action name
     * @param functionInt   function key number, or {@code -1}
     * @param parameter     optional text parameter for "String" / "FieldSet" actions
     * @return {@code true} on success
     */
    public boolean executeAction(boolean triggerSubmit, String command, int functionInt, String parameter) {
        if (!connected) {
            lastError = "Not connected";
            return false;
        }
        // TODO: delegate to Telnet / Controller once those are implemented
        return true;
    }

    /**
     * Moves the cursor using the specified operation.
     *
     * @param op cursor movement mode
     * @param x  target column
     * @param y  target row
     */
    public void moveCursor(CursorOp op, int x, int y) {
        // TODO: delegate to Keyboard/Controller
        this.cursorX = x;
        this.cursorY = y;
    }

    /**
     * Returns raw string data produced by the last {@code DumpXML} action.
     *
     * @param discardOld when {@code true}, clears the buffer after returning
     * @return accumulated string data
     */
    public String getAllStringData(boolean discardOld) {
        String data = lastStringData;
        if (discardOld) lastStringData = "";
        return data;
    }

    /**
     * Returns {@code true} when the given action command name normally causes
     * a screen submission (e.g., function keys, ENTER, PA keys).
     *
     * @param command action command string
     * @return whether the command submits the screen
     */
    public boolean keyboardCommandCausesSubmit(String command) {
        if (command == null) return false;
        return switch (command.toUpperCase()) {
            case "PF", "PA", "ENTER", "CLEAR", "ATTN", "SYSREQ" -> true;
            default -> false;
        };
    }

    // ------------------------------------------------------------------
    // AutoCloseable
    // ------------------------------------------------------------------

    @Override
    public void close() {
        disconnect();
    }
}
