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

import com.open3270client.commframework.MySemaphore;
import com.open3270client.engine.ConnectionConfig;
import com.open3270client.exceptions.TNHostException;
import com.open3270client.interfaces.IAudit;
import com.open3270client.tn3270e.ConnectionState;
import com.open3270client.tn3270e.DataType3270;
import com.open3270client.tn3270e.SmsState;
import com.open3270client.tn3270e.TelnetState;
import com.open3270client.tn3270e.TN3270API;
import com.open3270client.tn3270e.TN3270ESubmode;
import com.open3270client.tn3270e.TN3270State;
import com.open3270client.tn3270e.TNEvent;
import com.open3270client.tn3270e.PDS;
import com.open3270client.tn3270e.TelnetConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;
import java.util.function.*;

/**
 * Core TN3270/TN3270E Telnet protocol driver.
 * Port of {@code Telnet.cs}.
 */
public class Telnet implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Telnet.class);

    // ------------------------------------------------------------------ //
    // Events (listener lists replacing C# EventHandler delegates)
    // ------------------------------------------------------------------ //

    /** telnetDataEventOccurred */
    private final List<TelnetDataListener> telnetDataListeners = new ArrayList<>();
    /** Connected3270 */
    private final List<Consumer<Boolean>> connected3270Listeners = new ArrayList<>();
    /** ConnectedLineMode */
    private final List<Runnable> connectedLineModeListeners = new ArrayList<>();
    /** ConnectionPending */
    private final List<Runnable> connectionPendingListeners = new ArrayList<>();
    /** PrimaryConnectionChanged */
    private final List<Consumer<Boolean>> primaryConnectionChangedListeners = new ArrayList<>();
    /** CursorLocationChanged */
    private final List<Runnable> cursorLocationChangedListeners = new ArrayList<>();

    public interface TelnetDataListener {
        void onTelnetData(Object parentData, TNEvent eventType, String text);
    }

    public void addTelnetDataListener(TelnetDataListener l)            { telnetDataListeners.add(l); }
    public void removeTelnetDataListener(TelnetDataListener l)         { telnetDataListeners.remove(l); }
    public void addConnected3270Listener(Consumer<Boolean> l)          { connected3270Listeners.add(l); }
    public void removeConnected3270Listener(Consumer<Boolean> l)       { connected3270Listeners.remove(l); }
    public void addConnectedLineModeListener(Runnable l)               { connectedLineModeListeners.add(l); }
    public void addConnectionPendingListener(Runnable l)               { connectionPendingListeners.add(l); }
    public void removeConnectionPendingListener(Runnable l)            { connectionPendingListeners.remove(l); }
    public void addPrimaryConnectionChangedListener(Consumer<Boolean> l)   { primaryConnectionChangedListeners.add(l); }
    public void removePrimaryConnectionChangedListener(Consumer<Boolean> l){ primaryConnectionChangedListeners.remove(l); }
    public void addCursorLocationChangedListener(Runnable l)           { cursorLocationChangedListeners.add(l); }
    public void removeCursorLocationChangedListener(Runnable l)        { cursorLocationChangedListeners.remove(l); }

    // ------------------------------------------------------------------ //
    // State fields
    // ------------------------------------------------------------------ //

    private SmsState waitState = SmsState.IDLE;
    private TN3270State tnState = TN3270State.IN_NEITHER;
    private ConnectionState connectionState = ConnectionState.NOT_CONNECTED;
    private TN3270ESubmode tn3270eSubmode = TN3270ESubmode.NONE;
    private TelnetState telnetState = TelnetState.DATA;

    private TN3270API telnetApi = null;
    private ConnectionConfig connectionConfig = null;

    // Services
    private Controller controller = null;
    private Print print = null;
    private Idle idle = null;
    private Actions action = null;
    private Ansi ansi = null;
    private Appres appres;
    private Events events = null;
    private Keyboard keyboard = null;
    private TNTrace trace = null;

    private boolean nonTn3270eHost = false;
    private boolean parseLogFileOnly = false;
    private boolean showParseError = false;
    private boolean isValid = false;
    private boolean tn3270eBound = false;
    private boolean linemode = false;
    private boolean syncing = false;
    private boolean tn3270e_negotiated = false;
    private volatile boolean logFileProcessorThread_Quit = false;
    private boolean closeRequested = false;
    private boolean isDisposed = false;

    // ANSI terminal characters
    private byte vintr, vquit, verase, vkill, veof, vwerase, vrprnt, vlnext;

    private String address;
    private int port;
    private int bytesReceived;
    private int bytesSent;
    private int currentLUIndex = 0;
    private int eTransmitSequence = 0;
    private int ansiData = 0;
    private int currentOptionMask;
    private int responseRequired = TnHeader.HeaderResponseFlags.NO_RESPONSE;
    private int inputBufferIndex = 0;
    private int startedReceivingCount = 0;

    private int[] clientOptions = new int[256];
    private int[] hostOptions = new int[256];

    private List<String> lus = null;

    private String termType = null;
    private String connectedType = null;
    private String reportedType = null;
    private String connectedLu = null;
    private String reportedLu = null;
    private String sourceIP = "";
    private String disconnectReason = null;

    // Buffers
    private NetBuffer sbBuffer = null;
    private final byte[] byteBuffer = new byte[32767];
    private byte[] inputBuffer = null;

    // Telnet predefined messages
    private final byte[] doOption      = { TelnetConstants.IAC, TelnetConstants.DO,   0 };
    private final byte[] dontOption    = { TelnetConstants.IAC, TelnetConstants.DONT, 0 };
    private final byte[] willDoOption  = { TelnetConstants.IAC, TelnetConstants.WILL, 0 };
    private final byte[] wontDoOption  = { TelnetConstants.IAC, TelnetConstants.WONT, 0 };

    // Sockets
    private Socket socketBase = null;
    private InputStream socketInput = null;
    private OutputStream socketOutput = null;
    private Thread readerThread = null;
    private Thread logFileProcessorThread = null;

    // Synchronization
    private final Object receivingPadlock = new Object();
    private final ReentrantLock waitLock = new ReentrantLock();
    private final Condition waitCondition = waitLock.newCondition();
    private boolean waitEventSet = false;

    // Log-file simulation
    private MySemaphore logFileSemaphore = null;
    private Queue<Byte> logClientData = null;

    private Object parentData;

    // ------------------------------------------------------------------ //
    // Constructor
    // ------------------------------------------------------------------ //

    public Telnet(TN3270API api, IAudit audit, ConnectionConfig config) {
        this.connectionConfig = config;
        this.telnetApi = api;

        if (config.isIgnoreSequenceCount()) {
            this.currentOptionMask = shift(TelnetConstants.TN3270E_FUNC_BIND_IMAGE)
                    | shift(TelnetConstants.TN3270E_FUNC_SYSREQ);
        } else {
            this.currentOptionMask = shift(TelnetConstants.TN3270E_FUNC_BIND_IMAGE)
                    | shift(TelnetConstants.TN3270E_FUNC_RESPONSES)
                    | shift(TelnetConstants.TN3270E_FUNC_SYSREQ);
        }

        this.disconnectReason = null;

        this.trace      = new TNTrace(this, audit);
        this.appres     = new Appres();
        this.events     = new Events(this);
        this.ansi       = new Ansi(this);
        this.print      = new Print(this);
        this.controller = new Controller(this, appres);
        this.keyboard   = new Keyboard(this);
        this.action     = new Actions(this);
        this.keyboard.setActions(action);
        this.idle       = new Idle(this);

        // Register cursor-location listener from controller → telnet
        this.controller.addCursorLocationListener(this::onControllerCursorLocationChanged);

        if (!isValid) {
            this.vintr  = parseControlCharacter(appres.intr);
            this.vquit  = parseControlCharacter(appres.quit);
            this.verase = parseControlCharacter(appres.erase);
            this.vkill  = parseControlCharacter(appres.kill);
            this.veof   = parseControlCharacter(appres.eof);
            this.vwerase= parseControlCharacter(appres.werase);
            this.vrprnt = parseControlCharacter(appres.rprnt);
            this.vlnext = parseControlCharacter(appres.lnext);
            this.isValid = true;
        }

        Arrays.fill(this.hostOptions, 0);
    }

    @Override
    public void close() {
        dispose(true);
    }

    private void dispose(boolean disposing) {
        if (isDisposed) return;
        isDisposed = true;
        if (disposing) {
            disconnect();
            if (controller != null) {
                controller.removeCursorLocationListener(this::onControllerCursorLocationChanged);
                controller.close();
            }
            if (idle != null)       idle.close();
            if (keyboard != null)   keyboard.close();
            if (ansi != null)       ansi.close();
        }
    }

    // ------------------------------------------------------------------ //
    // Accessors
    // ------------------------------------------------------------------ //

    public TNTrace getTrace()               { return trace; }
    public Controller getController()       { return controller; }
    public Print getPrint()                 { return print; }
    public Actions getAction()              { return action; }
    public Idle getIdle()                   { return idle; }
    public Ansi getAnsi()                   { return ansi; }
    public Events getEvents()               { return events; }
    public Keyboard getKeyboard()           { return keyboard; }
    public Appres getAppres()               { return appres; }
    public TN3270API getTelnetApi()         { return telnetApi; }
    public void setTelnetApi(TN3270API a)   { telnetApi = a; }
    public ConnectionConfig getConfig()     { return connectionConfig; }
    public SmsState getWaitState()          { return waitState; }
    public void setWaitState(SmsState s)    { waitState = s; }
    public List<String> getLus()            { return lus; }
    public void setLus(List<String> l)      { lus = l; }
    public String getTermType()             { return termType; }
    public void setTermType(String t)       { termType = t; }
    public boolean isShowParseError()       { return showParseError; }
    public void setShowParseError(boolean b){ showParseError = b; }
    public String getDisconnectReason()     { return disconnectReason; }
    public boolean isParseLogFileOnly()     { return parseLogFileOnly; }

    public int getStartedReceivingCount() {
        synchronized (receivingPadlock) { return startedReceivingCount; }
    }

    // ------------------------------------------------------------------ //
    // Computed state properties
    // ------------------------------------------------------------------ //

    public boolean isKeyboardInWait() {
        return 0 != (keyboard.keyboardLock & (KeyboardConstants.OiaLocked | KeyboardConstants.OiaTWait | KeyboardConstants.DeferredUnlock));
    }

    public boolean canProceed() {
        return isSscp()
                || (is3270() && controller.isFormatted() && controller.getCursorAddress() != 0 && !isKeyboardInWait())
                || (isAnsi() && 0 == (keyboard.keyboardLock & KeyboardConstants.AwaitingFirst));
    }

    public boolean isSocketConnected() {
        if (connectionConfig.getLogFile() != null) return true;
        if (socketBase != null && socketBase.isConnected()) return true;
        if (disconnectReason == null) disconnectReason = "Server disconnected socket";
        return false;
    }

    public boolean isResolving()   { return connectionState.ordinal() >= ConnectionState.RESOLVING.ordinal(); }
    public boolean isPending()     { return connectionState == ConnectionState.RESOLVING || connectionState == ConnectionState.PENDING; }
    public boolean isConnected()   { return connectionState.ordinal() >= ConnectionState.CONNECTED_INITIAL.ordinal(); }
    public boolean isAnsi()        { return connectionState == ConnectionState.CONNECTED_ANSI || connectionState == ConnectionState.CONNECTED_NVT; }
    public boolean is3270()        { return connectionState == ConnectionState.CONNECTED_3270 || connectionState == ConnectionState.CONNECTED_3270E || connectionState == ConnectionState.CONNECTED_SSCP; }
    public boolean isSscp()        { return connectionState == ConnectionState.CONNECTED_SSCP; }
    public boolean isTn3270E()     { return connectionState == ConnectionState.CONNECTED_3270E; }
    public boolean isE()           { return connectionState.ordinal() >= ConnectionState.CONNECTED_INITIAL_3270E.ordinal(); }

    // ------------------------------------------------------------------ //
    // Wait-event (replaces ManualResetEvent)
    // ------------------------------------------------------------------ //

    public void signalWaitEvent() {
        waitLock.lock();
        try { waitEventSet = true; waitCondition.signalAll(); }
        finally { waitLock.unlock(); }
    }

    private void resetWaitEvent() {
        waitLock.lock();
        try { waitEventSet = false; }
        finally { waitLock.unlock(); }
    }

    private boolean waitForEvent(int timeoutMs) throws InterruptedException {
        waitLock.lock();
        try {
            if (waitEventSet) return true;
            return waitCondition.await(timeoutMs, TimeUnit.MILLISECONDS) && waitEventSet;
        } finally { waitLock.unlock(); }
    }

    // ------------------------------------------------------------------ //
    // Public methods
    // ------------------------------------------------------------------ //

    public void connect(Object parentDataObj, String hostAddress, int hostPort) {
        connect(parentDataObj, hostAddress, hostPort, null);
    }

    public void connect(Object parentDataObj, String hostAddress, int hostPort, String srcIP) {
        this.parentData = parentDataObj;
        this.address = hostAddress;
        this.port = hostPort;
        this.sourceIP = srcIP == null ? "" : srcIP;
        this.disconnectReason = null;
        this.closeRequested = false;

        if (connectionConfig.getTermType() == null) {
            this.termType = "IBM-3278-2";
        } else {
            this.termType = connectionConfig.getTermType();
        }

        this.controller.initialize(-1);
        this.controller.reinitialize(-1);
        this.keyboard.initialize();
        this.ansi.ansi_init();

        appres.mono   = false;
        appres.m3279  = true;
        appres.debug_tracing = true;

        if (!appres.debug_tracing) {
            appres.setToggle(Appres.DSTrace, false);
            appres.setToggle(Appres.EventTrace, false);
        }
        appres.setToggle(Appres.DSTrace, true);

        if (connectionConfig.getLogFile() != null) {
            // Log-file replay mode
            parseLogFileOnly = true;
            logFileSemaphore = new MySemaphore(0, 9999);
            logClientData = new LinkedList<>();
            logFileProcessorThread_Quit = false;
            logFileProcessorThread = new Thread(this::logFileProcessorThreadHandler, "logfile-processor");
            logFileProcessorThread.start();
        } else {
            // Real TCP connection
            InetSocketAddress remote;
            try {
                remote = new InetSocketAddress(InetAddress.getByName(hostAddress), hostPort);
            } catch (UnknownHostException e) {
                throw new TNHostException("Unable to resolve host '" + hostAddress + "'", e.getMessage(), null);
            }

            InetSocketAddress local;
            if (sourceIP != null && !sourceIP.isEmpty()) {
                try {
                    local = new InetSocketAddress(InetAddress.getByName(sourceIP), 0);
                } catch (UnknownHostException e) {
                    throw new TNHostException("Invalid Source TCP/IP address '" + sourceIP + "'", e.getMessage(), null);
                }
            } else {
                local = new InetSocketAddress(0);
            }

            try {
                socketBase = new Socket();
                socketBase.bind(local);
                connectionState = ConnectionState.RESOLVING;
                final InetSocketAddress remoteRef = remote;
                Thread connectThread = new Thread(() -> connectCallback(remoteRef), "telnet-connect");
                connectThread.setDaemon(true);
                connectThread.start();
            } catch (IOException e) {
                throw new TNHostException("An error occurred connecting to host '" + hostAddress + "' on port " + hostPort, e.getMessage(), null);
            }
        }
    }

    public synchronized void disconnect() {
        if (!parseLogFileOnly) {
            synchronized (this) {
                if (socketInput != null) {
                    closeRequested = true;
                    try { socketInput.close(); } catch (IOException ignored) {}
                    socketInput = null;
                    if (disconnectReason == null || disconnectReason.isEmpty())
                        disconnectReason = "telnet.disconnect socket-stream requested";
                }
                if (socketOutput != null) {
                    try { socketOutput.close(); } catch (IOException ignored) {}
                    socketOutput = null;
                }
                if (socketBase != null) {
                    closeRequested = true;
                    try { socketBase.close(); } catch (IOException ignored) {}
                    socketBase = null;
                    if (disconnectReason == null || disconnectReason.isEmpty())
                        disconnectReason = "telnet.disconnect socket-base requested";
                }
            }
        }
        if (logFileProcessorThread != null) {
            logFileProcessorThread_Quit = true;
            try { logFileProcessorThread.join(2000); } catch (InterruptedException ignored) {}
            logFileProcessorThread = null;
        }
    }

    public boolean parseByte(byte b) {
        synchronized (this) {
            if (this.tnState == TN3270State.IN_NEITHER) {
                keyboard.keyboardLockClear(KeyboardConstants.AwaitingFirst, "telnet_fsm");
                this.tnState = TN3270State.ANSI;
            }
            if (telnetProcessFiniteStateMachine(b) != 0) {
                hostDisconnect(true);
                disconnect();
                disconnectReason = "Telnet state machine error during parseByte";
                return false;
            }
        }
        return true;
    }

    public int telnetProcessFiniteStateMachine(byte currentByte) {
        int sl = 0;
        if (sbBuffer == null) sbBuffer = new NetBuffer();

        switch (this.telnetState) {
            case DATA: {
                if (currentByte == TelnetConstants.IAC) {
                    telnetState = TelnetState.IAC;
                    if (ansiData != 0) { trace.trace_dsn("\n"); ansiData = 0; }
                    break;
                }
                if (connectionState == ConnectionState.CONNECTED_INITIAL) {
                    setHostState(ConnectionState.CONNECTED_ANSI);
                    keyboard.keyboardLockClear(KeyboardConstants.AwaitingFirst, "telnet_fsm");
                    controller.processPendingInput();
                }
                if (isAnsi() && !isE()) {
                    if (ansiData == 0) { trace.trace_dsn("<.. "); ansiData = 4; }
                    String see_chr = Util.controlSee(currentByte);
                    ansiData += (sl = see_chr.length());
                    if (ansiData >= TNTrace.TRACELINE) { trace.trace_dsn(" ...\n... "); ansiData = 4 + sl; }
                    trace.trace_dsn(see_chr);
                    if (!syncing) {
                        if (linemode && appres.onlcr && currentByte == '\n') ansi.ansi_process((byte)'\r');
                        ansi.ansi_process(currentByte);
                    }
                } else {
                    store3270Input(currentByte);
                }
                break;
            }
            case IAC: {
                if (currentByte != TelnetConstants.EOR && currentByte != TelnetConstants.IAC) {
                    trace.trace_dsn("RCVD " + getCommand(currentByte) + " ");
                }
                switch (currentByte) {
                    case TelnetConstants.IAC:
                        if (isAnsi() && !isE()) {
                            if (ansiData == 0) { trace.trace_dsn("<.. "); ansiData = 4; }
                            String see_chr = Util.controlSee(currentByte);
                            ansiData += (sl = see_chr.length());
                            if (ansiData >= TNTrace.TRACELINE) { trace.trace_dsn(" ...\n ..."); ansiData = 4 + sl; }
                            trace.trace_dsn(see_chr);
                            ansi.ansi_process(currentByte);
                        } else {
                            store3270Input(currentByte);
                        }
                        telnetState = TelnetState.DATA;
                        break;
                    case TelnetConstants.EOR:
                        trace.trace_dsn("RCVD EOR\n");
                        if (is3270() || (isE() && tn3270e_negotiated)) {
                            if (processEOR()) return -1;
                        } else {
                            events.warning("EOR received when not in 3270 mode, ignored.");
                        }
                        inputBufferIndex = 0;
                        telnetState = TelnetState.DATA;
                        break;
                    case TelnetConstants.WILL: telnetState = TelnetState.WILL; break;
                    case TelnetConstants.WONT: telnetState = TelnetState.WONT; break;
                    case TelnetConstants.DO:   telnetState = TelnetState.DO;   break;
                    case TelnetConstants.DONT: telnetState = TelnetState.DONT; break;
                    case TelnetConstants.SB:   telnetState = TelnetState.SB; sbBuffer = new NetBuffer(); break;
                    case TelnetConstants.DM:
                        trace.trace_dsn("\n");
                        if (syncing) syncing = false;
                        telnetState = TelnetState.DATA;
                        break;
                    case TelnetConstants.GA:
                    case TelnetConstants.NOP:
                        trace.trace_dsn("\n");
                        telnetState = TelnetState.DATA;
                        break;
                    default:
                        trace.trace_dsn("???\n");
                        telnetState = TelnetState.DATA;
                        break;
                }
                break;
            }
            case WILL: {
                trace.trace_dsn("" + getOption(currentByte) + "\n");
                byte cb = currentByte;
                if (cb == TelnetConstants.TELOPT_SGA || cb == TelnetConstants.TELOPT_BINARY ||
                    cb == TelnetConstants.TELOPT_EOR  || cb == TelnetConstants.TELOPT_TTYPE  ||
                    cb == TelnetConstants.TELOPT_ECHO || cb == TelnetConstants.TELOPT_TN3270E) {
                    if (cb != TelnetConstants.TELOPT_TN3270E || !nonTn3270eHost) {
                        if (hostOptions[cb & 0xFF] == 0) {
                            hostOptions[cb & 0xFF] = 1;
                            doOption[2] = cb;
                            sendRawOutput(doOption);
                            trace.trace_dsn("SENT DO " + getOption(cb) + "\n");
                            if (cb == TelnetConstants.TELOPT_EOR && clientOptions[cb & 0xFF] == 0) {
                                clientOptions[cb & 0xFF] = 1;
                                willDoOption[2] = cb;
                                sendRawOutput(willDoOption);
                                trace.trace_dsn("SENT WILL " + getOption(cb) + "\n");
                            }
                            checkIn3270();
                            checkLineMode(false);
                        }
                    }
                } else {
                    dontOption[2] = cb;
                    sendRawOutput(dontOption);
                    trace.trace_dsn("SENT DONT " + getOption(cb) + "\n");
                }
                telnetState = TelnetState.DATA;
                break;
            }
            case WONT: {
                trace.trace_dsn("" + getOption(currentByte) + "\n");
                if (hostOptions[currentByte & 0xFF] != 0) {
                    hostOptions[currentByte & 0xFF] = 0;
                    dontOption[2] = currentByte;
                    sendRawOutput(dontOption);
                    trace.trace_dsn("SENT DONT " + getOption(currentByte) + "\n");
                    checkIn3270();
                    checkLineMode(false);
                }
                telnetState = TelnetState.DATA;
                break;
            }
            case DO: {
                trace.trace_dsn("" + getOption(currentByte) + "\n");
                byte cb = currentByte;
                if (cb == TelnetConstants.TELOPT_BINARY || cb == TelnetConstants.TELOPT_EOR ||
                    cb == TelnetConstants.TELOPT_TTYPE  || cb == TelnetConstants.TELOPT_SGA ||
                    cb == TelnetConstants.TELOPT_NAWS   || cb == TelnetConstants.TELOPT_TM  ||
                    (cb == TelnetConstants.TELOPT_TN3270E && !connectionConfig.isRefuseTN3270E())) {
                    boolean fallthrough = true;
                    if (cb != TelnetConstants.TELOPT_TN3270E || !nonTn3270eHost) {
                        if (clientOptions[cb & 0xFF] == 0) {
                            if (cb != TelnetConstants.TELOPT_TM) clientOptions[cb & 0xFF] = 1;
                            willDoOption[2] = cb;
                            sendRawOutput(willDoOption);
                            trace.trace_dsn("SENT WILL " + getOption(cb) + "\n");
                            checkIn3270();
                            checkLineMode(false);
                        }
                        if (cb == TelnetConstants.TELOPT_NAWS) sendNaws();
                        fallthrough = false;
                    }
                    if (fallthrough) {
                        wontDoOption[2] = cb;
                        sendRawOutput(wontDoOption);
                        trace.trace_dsn("SENT WONT " + getOption(cb) + "\n");
                    }
                } else {
                    wontDoOption[2] = currentByte;
                    sendRawOutput(wontDoOption);
                    trace.trace_dsn("SENT WONT " + getOption(currentByte) + "\n");
                }
                telnetState = TelnetState.DATA;
                break;
            }
            case DONT: {
                trace.trace_dsn("" + getOption(currentByte) + "\n");
                if (clientOptions[currentByte & 0xFF] != 0) {
                    clientOptions[currentByte & 0xFF] = 0;
                    wontDoOption[2] = currentByte;
                    sendRawOutput(wontDoOption);
                    trace.trace_dsn("SENT WONT " + getOption(currentByte) + "\n");
                    checkIn3270();
                    checkLineMode(false);
                }
                telnetState = TelnetState.DATA;
                break;
            }
            case SB: {
                if (currentByte == TelnetConstants.IAC) {
                    telnetState = TelnetState.SB_IAC;
                } else {
                    sbBuffer.add(currentByte);
                }
                break;
            }
            case SB_IAC: {
                sbBuffer.add(currentByte);
                if (currentByte == TelnetConstants.SE) {
                    telnetState = TelnetState.DATA;
                    byte[] sbData = sbBuffer.getData();
                    if (sbData[0] == TelnetConstants.TELOPT_TTYPE && sbData[1] == TelnetConstants.TELQUAL_SEND) {
                        trace.trace_dsn("" + getOption(sbData[0]) + " " + TelnetConstants.TEL_QUALS[sbData[1]] + "\n");
                        if (lus != null && currentLUIndex >= lus.size()) {
                            events.showError("Cannot connect to specified LU");
                            return -1;
                        }
                        if (lus != null) connectedLu = lus.get(currentLUIndex);
                        else connectedLu = null;

                        NetBuffer tt_out = new NetBuffer();
                        tt_out.add(TelnetConstants.IAC);
                        tt_out.add(TelnetConstants.SB);
                        tt_out.add(TelnetConstants.TELOPT_TTYPE);
                        tt_out.add(TelnetConstants.TELQUAL_IS);
                        tt_out.add(termType);
                        if (lus != null) {
                            tt_out.add((byte)'@');
                            byte[] luBytes = lus.get(currentLUIndex).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
                            for (byte b2 : luBytes) tt_out.add(b2);
                        }
                        tt_out.add(TelnetConstants.IAC);
                        tt_out.add(TelnetConstants.SE);
                        sendRawOutput(tt_out);
                        trace.trace_dsn("SENT SB " + getOption(TelnetConstants.TELOPT_TTYPE) + " " + termType + " " + getCommand(TelnetConstants.SE));
                        currentLUIndex++;
                    } else if (clientOptions[TelnetConstants.TELOPT_TN3270E & 0xFF] != 0 && sbData[0] == TelnetConstants.TELOPT_TN3270E) {
                        if (tn3270e_Negotiate(sbBuffer) != 0) return -1;
                    }
                } else {
                    telnetState = TelnetState.SB;
                }
                break;
            }
            default:
                break;
        }
        return 0;
    }

    public void output(NetBuffer obptr) {
        NetBuffer outputBuffer = new NetBuffer();

        if (isTn3270E() || isSscp()) {
            TnHeader header = new TnHeader();
            if (responseRequired == TnHeader.HeaderResponseFlags.ALWAYS_RESPONSE) {
                sendAck();
                responseRequired = TnHeader.HeaderResponseFlags.NO_RESPONSE;
            }
            header.setDataType(isTn3270E() ? DataType3270.DATA_3270 : DataType3270.SSCP_LU_DATA);
            header.setRequestFlag((byte)0);
            header.setResponseFlag((byte)0);
            header.getSequenceNumber()[0] = (byte)((eTransmitSequence >> 8) & 0xff);
            header.getSequenceNumber()[1] = (byte)(eTransmitSequence & 0xff);
            trace.trace_dsn("SENT TN3270E(%s %s %d)\n",
                isTn3270E() ? "3270-DATA" : "SSCP-LU-DATA", "NO-RESPONSE", eTransmitSequence);
            if (!connectionConfig.isIgnoreSequenceCount() &&
                (currentOptionMask & shift(TelnetConstants.TN3270E_FUNC_RESPONSES)) != 0) {
                eTransmitSequence = (eTransmitSequence + 1) & 0x7fff;
            }
            byte[] hdrBytes = header.toBytes();
            for (byte hb : hdrBytes) outputBuffer.add(hb);
        }

        byte[] data = obptr.getData();
        int len = obptr.getIndex();
        for (int i = 0; i < len; i++) {
            outputBuffer.add(data[i]);
            if (data[i] == TelnetConstants.IAC) outputBuffer.add(TelnetConstants.IAC);
        }
        outputBuffer.add(TelnetConstants.IAC);
        outputBuffer.add(TelnetConstants.EOR);
        sendRawOutput(outputBuffer);
        trace.trace_dsn("SENT EOR\n");
        bytesSent++;
    }

    public void sendString(String s) {
        for (int i = 0; i < s.length(); i++) sendChar(s.charAt(i));
    }

    public void sendChar(char c) { sendByte((byte)c); }

    public void sendByte(byte c) {
        byte[] buf = new byte[2];
        if (c == '\r' && !linemode) {
            buf[0] = (byte)'\r'; buf[1] = 0;
            cook(buf, 2);
        } else {
            buf[0] = c;
            cook(buf, 1);
        }
    }

    public void abort() {
        byte[] buf = { TelnetConstants.IAC, TelnetConstants.AO };
        if ((currentOptionMask & shift(TelnetConstants.TN3270E_FUNC_SYSREQ)) != 0) {
            switch (tn3270eSubmode) {
                case SSCP:
                    sendRawOutput(buf, buf.length);
                    trace.trace_dsn("SENT AO\n");
                    if (tn3270eBound || 0 == (currentOptionMask & shift(TelnetConstants.TN3270E_FUNC_BIND_IMAGE))) {
                        tn3270eSubmode = TN3270ESubmode.MODE_3270;
                        checkIn3270();
                    }
                    break;
                case MODE_3270:
                    sendRawOutput(buf, buf.length);
                    trace.trace_dsn("SENT AO\n");
                    tn3270eSubmode = TN3270ESubmode.SSCP;
                    checkIn3270();
                    break;
                default: break;
            }
        }
    }

    public void sendErase()  { cook(new byte[]{ verase }, 1); }
    public void sendKill()   { cook(new byte[]{ vkill }, 1); }
    public void sendWErase() { cook(new byte[]{ vwerase }, 1); }

    public void sendHexAnsiOut(byte[] buffer, int length) {
        if (length <= 0) return;
        if (appres.toggled(Appres.DSTrace)) {
            trace.trace_dsn(">");
            for (int i = 0; i < length; i++) trace.trace_dsn(" " + Util.controlSee(buffer[i]));
            trace.trace_dsn("\n");
        }
        byte[] tempBuffer = new byte[2 * length];
        int index = 0, bindex = 0, rem = length;
        while (rem > 0) {
            byte c = buffer[bindex++];
            tempBuffer[index++] = c; rem--;
            if (c == TelnetConstants.IAC) tempBuffer[index++] = TelnetConstants.IAC;
            else if (c == (byte)'\r' && (rem == 0 || buffer[bindex] != (byte)'\n')) tempBuffer[index++] = 0;
        }
        sendRawOutput(tempBuffer, index);
    }

    public void breakCmd() {
        byte[] buf = { TelnetConstants.IAC, TelnetConstants.BREAK };
        sendRawOutput(buf, buf.length);
        trace.trace_dsn("SENT BREAK\n");
    }

    public void interrupt() {
        byte[] buf = { TelnetConstants.IAC, TelnetConstants.IP };
        sendRawOutput(buf, buf.length);
        trace.trace_dsn("SENT IP\n");
    }

    public void tn3270e_SendRequest() {
        String try_lu = (lus != null) ? lus.get(currentLUIndex) : null;
        NetBuffer buffer = new NetBuffer();
        buffer.add(TelnetConstants.IAC);
        buffer.add(TelnetConstants.SB);
        buffer.add(TelnetConstants.TELOPT_TN3270E);
        buffer.add(TnHeader.Ops.DEVICE_TYPE);
        buffer.add(TnHeader.Ops.REQUEST);
        String tt = termType.replace("3279", "3278");
        buffer.add(tt);
        if (try_lu != null) {
            buffer.add(TnHeader.Ops.CONNECT);
            buffer.add(try_lu);
        }
        buffer.add(TelnetConstants.IAC);
        buffer.add(TelnetConstants.SE);
        sendRawOutput(buffer);
        trace.trace_dsn("SENT SB TN3270E DEVICE-TYPE REQUEST %s%s%s SE\n",
            termType, (try_lu != null) ? " CONNECT " : "", (try_lu != null) ? try_lu : "");
    }

    public String getFunctionCodesAsText(NetBuffer buffer) {
        byte[] bufferData = buffer.getData();
        int len = buffer.getIndex();
        if (len == 0) return "(null)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(getFunctionName(bufferData[i] & 0xFF));
        }
        return sb.toString();
    }

    public boolean waitForConnect() {
        while (!isAnsi() && !is3270()) {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
            if (!isResolving()) { disconnectReason = "Timeout waiting for connection"; return false; }
        }
        return true;
    }

    public boolean waitFor(SmsState what, int timeoutMs) {
        synchronized (this) {
            waitState = what;
            resetWaitEvent();
            controller.continueProcessing();
        }
        try {
            if (waitForEvent(timeoutMs)) return true;
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        synchronized (this) { waitState = SmsState.IDLE; }
        return false;
    }

    public void restartReceive() {
        // In Java we keep the reader thread running; this is a no-op placeholder.
        log.debug("restartReceive called");
    }

    // ------------------------------------------------------------------ //
    // Private/internal methods
    // ------------------------------------------------------------------ //

    private void connectCallback(InetSocketAddress remote) {
        try {
            socketBase.connect(remote, 30000);
            if (socketBase.isConnected()) {
                fireTelnetData(parentData, TNEvent.CONNECT, null);
                connectionState = ConnectionState.CONNECTED_INITIAL;
                onPrimaryConnectionChanged(true);
                netConnected();
                setHostState(ConnectionState.CONNECTED_ANSI);

                socketInput  = connectionConfig.isUseSSL() ? createSslInput() : socketBase.getInputStream();
                socketOutput = socketBase.getOutputStream();

                readerThread = new Thread(this::receiverLoop, "telnet-reader");
                readerThread.setDaemon(true);
                readerThread.start();
            } else {
                disconnectReason = "Unable to connect to host - timeout on connect";
                fireTelnetData(parentData, TNEvent.ERROR, "Connect callback returned 'not connected'");
                connectionState = ConnectionState.NOT_CONNECTED;
                onPrimaryConnectionChanged(false);
            }
        } catch (Exception ex) {
            trace.trace_event("%s", "Exception occurred connecting to host.\n" + ex);
            disconnect();
            disconnectReason = "exception during telnet connect callback";
        }
    }

    private InputStream createSslInput() throws Exception {
        // For SSL support — basic pass-through placeholder
        log.warn("SSL support not fully implemented; using plain socket");
        return socketBase.getInputStream();
    }

    private void receiverLoop() {
        boolean disconnectMe = false;
        while (!disconnectMe) {
            disconnectReason = null;
            if (socketBase == null || !socketBase.isConnected()) {
                disconnectMe = true;
                if (disconnectReason == null || disconnectReason.isEmpty())
                    disconnectReason = "Host dropped connection in receiverLoop";
                break;
            }
            try {
                synchronized (receivingPadlock) { startedReceivingCount++; }
                int nBytesRec = socketInput.read(byteBuffer, 0, byteBuffer.length);
                ansiData = 0;
                if (nBytesRec > 0) {
                    if (isPending()) { hostConnected(); netConnected(); }
                    trace.trace_netdata('<', byteBuffer, nBytesRec);
                    bytesReceived += nBytesRec;
                    if (showParseError) throw new RuntimeException("ShowParseError exception test requested");

                    if (nBytesRec >= 5 && (currentOptionMask & shift(TelnetConstants.TN3270E_FUNC_RESPONSES)) != 0) {
                        eTransmitSequence = (byteBuffer[3] << 8) | (byteBuffer[4] & 0xFF);
                        eTransmitSequence = (eTransmitSequence + 1) & 0x7FFF;
                    }
                    synchronized (this) {
                        for (int i = 0; i < nBytesRec; i++) {
                            if (tnState == TN3270State.IN_NEITHER) {
                                keyboard.keyboardLockClear(KeyboardConstants.AwaitingFirst, "telnet_fsm");
                                tnState = TN3270State.ANSI;
                            }
                            if (telnetProcessFiniteStateMachine(byteBuffer[i]) != 0) {
                                hostDisconnect(true);
                                disconnect();
                                disconnectReason = "telnet_fsm error in receiverLoop";
                                return;
                            }
                        }
                    }
                } else {
                    disconnectMe = true;
                    disconnectReason = "No data received in receiverLoop";
                }
            } catch (java.io.InterruptedIOException ignored) {
                disconnectMe = true;
            } catch (Exception e) {
                trace.trace_event("%s", "Exception processing Telnet buffer.\n" + e);
                disconnectMe = true;
                disconnectReason = "Exception in data stream (" + e.getMessage() + ")";
            }
        }
        boolean closeWasRequested = closeRequested;
        trace.trace_dsn("RCVD disconnect\n");
        hostDisconnect(false);
        disconnect();
        if (closeWasRequested) fireTelnetData(parentData, TNEvent.DISCONNECT, null);
        else fireTelnetData(parentData, TNEvent.DISCONNECT_UNEXPECTED, null);
        closeRequested = false;
    }

    private void logFileProcessorThreadHandler() {
        try {
            fireTelnetData(parentData, TNEvent.CONNECT, null);
            connectionState = ConnectionState.CONNECTED_INITIAL;
            onPrimaryConnectionChanged(true);
            netConnected();
            setHostState(ConnectionState.CONNECTED_ANSI);

            java.io.BufferedReader logFile = connectionConfig.getLogFile();
            while (!logFileProcessorThread_Quit) {
                String text = logFile.readLine();
                if (text == null) {
                    trace.trace_dsn("RCVD disconnect\n");
                    fireTelnetData(parentData, TNEvent.DISCONNECT, null);
                    break;
                } else if (text.length() >= 11) {
                    trace.writeLine("\n" + text.substring(7));
                    if (text.substring(9, 11).equals("H ")) {
                        text = text.substring(18);
                        if (isPending()) { hostConnected(); netConnected(); }
                        synchronized (this) {
                            while (text.length() > 1) {
                                byte v = (byte)Integer.parseInt(text.substring(0, 2), 16);
                                if (tnState == TN3270State.IN_NEITHER) {
                                    keyboard.keyboardLockClear(KeyboardConstants.AwaitingFirst, "telnet_fsm");
                                    tnState = TN3270State.ANSI;
                                }
                                if (telnetProcessFiniteStateMachine(v) != 0) {
                                    hostDisconnect(true);
                                    disconnect();
                                    disconnectReason = "logfile processor telnet_fsm error";
                                    return;
                                }
                                text = text.substring(2).trim();
                            }
                        }
                    } else if (text.substring(9, 11).equals("C ")) {
                        text = text.substring(18);
                        while (text.length() > 1) {
                            byte v = (byte)Integer.parseInt(text.substring(0, 2), 16);
                            Byte netoutByte = logClientData.poll();
                            while (!logFileProcessorThread_Quit) {
                                if (logFileSemaphore.acquire(1000)) break;
                            }
                            if (logFileProcessorThread_Quit) break;
                            text = text.substring(2).trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Telnet logfile parser exception", e);
        }
    }

    private void sendRawOutput(NetBuffer smk) {
        sendRawOutput(smk.getData(), smk.getIndex());
    }

    private void sendRawOutput(byte[] buffer) {
        sendRawOutput(buffer, buffer.length);
    }

    private void sendRawOutput(byte[] buffer, int length) {
        if (parseLogFileOnly) {
            trace.writeLine("\nnet_rawout2 [" + length + "]" + dumpToString(buffer, length) + "\n");
            if (logFileSemaphore != null) {
                for (int i = 0; i < length; i++) logClientData.add(buffer[i]);
                logFileSemaphore.release(length);
            }
        } else {
            trace.trace_netdata('>', buffer, length);
            if (socketOutput != null) {
                try { socketOutput.write(buffer, 0, length); socketOutput.flush(); }
                catch (IOException e) { log.error("sendRawOutput error", e); }
            }
        }
    }

    private void store3270Input(byte c) {
        if (inputBuffer == null) { inputBuffer = new byte[256]; inputBufferIndex = 0; }
        if (inputBufferIndex >= inputBuffer.length) {
            inputBuffer = Arrays.copyOf(inputBuffer, inputBuffer.length + 256);
        }
        inputBuffer[inputBufferIndex++] = c;
    }

    private void setHostState(ConnectionState newState) {
        boolean now3270 = (newState == ConnectionState.CONNECTED_3270
                || newState == ConnectionState.CONNECTED_SSCP
                || newState == ConnectionState.CONNECTED_3270E);
        this.connectionState = newState;
        this.controller.setIs3270(now3270);
        onConnected3270(now3270);
    }

    private void checkIn3270() {
        ConnectionState newCS = ConnectionState.NOT_CONNECTED;
        if (clientOptions[TelnetConstants.TELOPT_TN3270E & 0xFF] != 0) {
            if (!tn3270e_negotiated) {
                newCS = ConnectionState.CONNECTED_INITIAL_3270E;
            } else {
                switch (tn3270eSubmode) {
                    case NONE:    newCS = ConnectionState.CONNECTED_INITIAL_3270E; break;
                    case NVT:     newCS = ConnectionState.CONNECTED_NVT; break;
                    case MODE_3270:newCS = ConnectionState.CONNECTED_3270E; break;
                    case SSCP:    newCS = ConnectionState.CONNECTED_SSCP; break;
                }
            }
        } else if (clientOptions[TelnetConstants.TELOPT_BINARY & 0xFF] != 0
                && clientOptions[TelnetConstants.TELOPT_EOR & 0xFF] != 0
                && clientOptions[TelnetConstants.TELOPT_TTYPE & 0xFF] != 0
                && hostOptions[TelnetConstants.TELOPT_BINARY & 0xFF] != 0
                && hostOptions[TelnetConstants.TELOPT_EOR & 0xFF] != 0) {
            newCS = ConnectionState.CONNECTED_3270;
        } else if (connectionState == ConnectionState.CONNECTED_INITIAL) {
            return;
        } else {
            newCS = ConnectionState.CONNECTED_ANSI;
        }

        if (newCS != connectionState) {
            boolean wasInE = (connectionState.ordinal() >= ConnectionState.CONNECTED_INITIAL_3270E.ordinal());
            trace.trace_dsn("Now operating in " + newCS + " mode\n");
            setHostState(newCS);
            if (lus != null && wasInE != isE()) currentLUIndex = 0;
            if (newCS.ordinal() >= ConnectionState.CONNECTED_INITIAL.ordinal() && inputBuffer == null) {
                inputBuffer = new byte[256]; inputBufferIndex = 0;
            }
            if (clientOptions[TelnetConstants.TELOPT_TN3270E & 0xFF] == 0) {
                tn3270e_negotiated = false;
                tn3270eSubmode = TN3270ESubmode.NONE;
                tn3270eBound = false;
            }
            controller.continueProcessing();
        }
    }

    private boolean processEOR() {
        if (syncing || inputBufferIndex == 0) return false;

        if (connectionState.ordinal() >= ConnectionState.CONNECTED_INITIAL_3270E.ordinal()) {
            TnHeader h = TnHeader.fromBytes(inputBuffer, 0);
            PDS rv;
            trace.trace_dsn("RCVD TN3270E(datatype: " + h.getDataType() + ", request: " + h.getRequestFlag() +
                    ", response: " + h.getResponseFlag() + ", seq: " +
                    ((h.getSequenceNumber()[0] << 8) | (h.getSequenceNumber()[1] & 0xFF)) + ")\n");

            switch (h.getDataType()) {
                case DATA_3270:
                    if ((currentOptionMask & shift(TelnetConstants.TN3270E_FUNC_BIND_IMAGE)) == 0 || tn3270eBound) {
                        tn3270eSubmode = TN3270ESubmode.MODE_3270;
                        checkIn3270();
                        responseRequired = h.getResponseFlag();
                        rv = controller.processDS(inputBuffer, TnHeader.EH_SIZE, inputBufferIndex - TnHeader.EH_SIZE);
                        if (rv != null && rv.ordinal() < 0 && responseRequired != TnHeader.HeaderResponseFlags.NO_RESPONSE) {
                            sendNak();
                        } else if (rv == PDS.OKAY_NO_OUTPUT && responseRequired == TnHeader.HeaderResponseFlags.ALWAYS_RESPONSE) {
                            sendAck();
                        }
                        responseRequired = TnHeader.HeaderResponseFlags.NO_RESPONSE;
                    }
                    break;
                case BIND_IMAGE:
                    if ((currentOptionMask & shift(TelnetConstants.TN3270E_FUNC_BIND_IMAGE)) != 0) {
                        tn3270eBound = true;
                        checkIn3270();
                    }
                    break;
                case UNBIND:
                    if ((currentOptionMask & shift(TelnetConstants.TN3270E_FUNC_BIND_IMAGE)) != 0) {
                        tn3270eBound = false;
                        if (tn3270eSubmode == TN3270ESubmode.MODE_3270) tn3270eSubmode = TN3270ESubmode.NONE;
                        checkIn3270();
                    }
                    break;
                case NVT_DATA:
                    tn3270eSubmode = TN3270ESubmode.NVT;
                    checkIn3270();
                    for (int i = 0; i < inputBufferIndex; i++) ansi.ansi_process(inputBuffer[i]);
                    break;
                case SSCP_LU_DATA:
                    if ((currentOptionMask & shift(TelnetConstants.TN3270E_FUNC_BIND_IMAGE)) != 0) {
                        tn3270eSubmode = TN3270ESubmode.SSCP;
                        checkIn3270();
                        controller.writeSspcLuData(inputBuffer, TnHeader.EH_SIZE, inputBufferIndex - TnHeader.EH_SIZE);
                    }
                    break;
                default:
                    break;
            }
        } else {
            controller.processDS(inputBuffer, 0, inputBufferIndex);
        }
        return false;
    }

    private void sendAck() { ack(true); }
    private void sendNak() { ack(false); }

    private void ack(boolean positive) {
        byte[] responseBuffer = new byte[9];
        TnHeader header = new TnHeader();
        TnHeader headerIn = TnHeader.fromBytes(inputBuffer, 0);
        int responseLength = TnHeader.EH_SIZE;

        header.setDataType(DataType3270.RESPONSE);
        header.setRequestFlag((byte)0);
        header.setResponseFlag(positive ? (byte)TnHeader.HeaderResponseFlags.POSITIVE_RESPONSE : (byte)TnHeader.HeaderResponseFlags.NEGATIVE_RESPONSE);
        header.getSequenceNumber()[0] = headerIn.getSequenceNumber()[0];
        header.getSequenceNumber()[1] = headerIn.getSequenceNumber()[1];
        byte[] hdrOut = header.toBytes();
        System.arraycopy(hdrOut, 0, responseBuffer, 0, hdrOut.length);

        if (header.getSequenceNumber()[1] == TelnetConstants.IAC) responseBuffer[responseLength++] = TelnetConstants.IAC;
        responseBuffer[responseLength++] = positive ? TnHeader.HeaderResponseData.POS_DEVICE_END : TnHeader.HeaderResponseData.NEG_COMMAND_REJECT;
        responseBuffer[responseLength++] = TelnetConstants.IAC;
        responseBuffer[responseLength++] = TelnetConstants.EOR;

        trace.trace_dsn("SENT TN3270E(RESPONSE " + (positive ? "POSITIVE" : "NEGATIVE") + "-RESPONSE: " +
                ((headerIn.getSequenceNumber()[0] << 8) | (headerIn.getSequenceNumber()[1] & 0xFF)) + ")\n");
        sendRawOutput(responseBuffer, responseLength);
    }

    private void checkLineMode(boolean init) {
        boolean wasline = linemode;
        linemode = (hostOptions[TelnetConstants.TELOPT_ECHO & 0xFF] == 0);
        if (init || linemode != wasline) {
            onConnectedLineMode();
            if (!init) trace.trace_dsn("Operating in " + (linemode ? "line" : "character-at-a-time") + " mode.\n");
            if (isAnsi() && linemode) cookedInitialized();
        }
    }

    private void sendNaws() {
        NetBuffer buffer = new NetBuffer();
        buffer.add(TelnetConstants.IAC);
        buffer.add(TelnetConstants.SB);
        buffer.add(TelnetConstants.TELOPT_NAWS);
        buffer.add16(controller.getMaxColumns());
        buffer.add16(controller.getMaxRows());
        buffer.add(TelnetConstants.IAC);
        buffer.add(TelnetConstants.SE);
        sendRawOutput(buffer);
        trace.trace_dsn("SENT SB NAWS %d %d SE\n", controller.getMaxColumns(), controller.getMaxRows());
    }

    private int tn3270e_Negotiate(NetBuffer buffer) {
        int bufLen;
        byte[] sbData = buffer.getData();
        for (bufLen = 0; ; bufLen++) { if (sbData[bufLen] == TelnetConstants.SE) break; }
        trace.trace_dsn("TN3270E ");
        switch (sbData[1]) {
            case TnHeader.Ops.SEND:
                if (sbData[2] == TnHeader.Ops.DEVICE_TYPE) {
                    trace.trace_dsn("SEND DEVICE-TYPE SE\n");
                    tn3270e_SendRequest();
                } else trace.trace_dsn("SEND ??%d SE\n", sbData[2]);
                break;
            case TnHeader.Ops.DEVICE_TYPE:
                trace.trace_dsn("DEVICE-TYPE ");
                switch (sbData[2]) {
                    case TnHeader.Ops.IS: {
                        int tnLen = 0;
                        while (sbData[3 + tnLen] != TelnetConstants.SE && sbData[3 + tnLen] != TnHeader.Ops.CONNECT) tnLen++;
                        int snLen = 0;
                        if (sbData[3 + tnLen] == TnHeader.Ops.CONNECT) {
                            while (sbData[3 + tnLen + 1 + snLen] != TelnetConstants.SE) snLen++;
                        }
                        trace.trace_dsn("IS " + buffer.asString(3, tnLen) + " CONNECT " + buffer.asString(3 + tnLen + 1, snLen) + " SE\n");
                        if (tnLen != 0) { if (tnLen > TelnetConstants.LU_MAX) tnLen = TelnetConstants.LU_MAX; reportedType = connectedType = buffer.asString(3, tnLen); }
                        if (snLen != 0) { if (snLen > TelnetConstants.LU_MAX) snLen = TelnetConstants.LU_MAX; reportedLu = connectedLu = buffer.asString(3 + tnLen + 1, snLen); }
                        tn3270e_Subneg_Send((byte)TnHeader.Ops.REQUEST, currentOptionMask);
                        break;
                    }
                    case TnHeader.Ops.REJECT:
                        trace.trace_dsn("REJECT REASON %d SE\n", sbData[4]);
                        if (sbData[4] == TnHeader.NegotiationReasonCodes.INV_DEVICE_TYPE || sbData[4] == TnHeader.NegotiationReasonCodes.UNSUPPORTED_REQ) {
                            backoff_TN3270e("Host rejected device type or request type"); break;
                        }
                        currentLUIndex++;
                        if (lus != null && currentLUIndex < lus.size()) tn3270e_SendRequest();
                        else if (lus != null) backoff_TN3270e("Host rejected resource(s)");
                        else backoff_TN3270e("Device type rejected");
                        break;
                    default: trace.trace_dsn("??%d SE\n", sbData[2]); break;
                }
                break;
            case TnHeader.Ops.FUNCTIONS:
                trace.trace_dsn("FUNCTIONS ");
                switch (sbData[2]) {
                    case TnHeader.Ops.REQUEST: {
                        NetBuffer hostWants = buffer.copyFrom(3, bufLen - 3);
                        trace.trace_dsn("REQUEST %s SE\n", getFunctionCodesAsText(hostWants));
                        int caps = tn3270e_fdecode(hostWants);
                        if (caps == currentOptionMask || (currentOptionMask & ~caps) != 0) {
                            currentOptionMask = caps;
                            tn3270e_Subneg_Send((byte)TnHeader.Ops.IS, currentOptionMask);
                            tn3270e_negotiated = true;
                            trace.trace_dsn("TN3270E option negotiation complete.\n");
                            checkIn3270();
                        } else {
                            currentOptionMask &= caps;
                            tn3270e_Subneg_Send((byte)TnHeader.Ops.REQUEST, currentOptionMask);
                        }
                        break;
                    }
                    case TnHeader.Ops.IS: {
                        NetBuffer hostWants = buffer.copyFrom(3, bufLen - 3);
                        trace.trace_dsn("IS %s SE\n", getFunctionCodesAsText(hostWants));
                        int caps = tn3270e_fdecode(hostWants);
                        if (caps != currentOptionMask) {
                            if ((currentOptionMask & ~caps) != 0) currentOptionMask = caps;
                            else { backoff_TN3270e("Host illegally added function(s)"); break; }
                        }
                        tn3270e_negotiated = true;
                        trace.trace_dsn("TN3270E option negotiation complete.\n");
                        checkIn3270();
                        break;
                    }
                    default: trace.trace_dsn("??%d SE\n", sbData[2]); break;
                }
                break;
            default: trace.trace_dsn("??%d SE\n", sbData[1]); break;
        }
        return 0;
    }

    private void tn3270e_Subneg_Send(byte op, int funcs) {
        byte[] proto = new byte[7 + 32];
        proto[0] = TelnetConstants.IAC;
        proto[1] = TelnetConstants.SB;
        proto[2] = (byte)TelnetConstants.TELOPT_TN3270E;
        proto[3] = (byte)TnHeader.Ops.FUNCTIONS;
        proto[4] = op;
        int length = 5;
        for (int i = 0; i < 32; i++) {
            if ((funcs & shift(i)) != 0) proto[length++] = (byte)i;
        }
        proto[length++] = TelnetConstants.IAC;
        proto[length++] = TelnetConstants.SE;
        sendRawOutput(proto, length);
        trace.trace_dsn("SENT SB TN3270E FUNCTIONS %s SE\n", (op == TnHeader.Ops.REQUEST) ? "REQUEST" : "IS");
    }

    private int tn3270e_fdecode(NetBuffer netbuf) {
        int r = 0;
        byte[] buf = netbuf.getData();
        int len = netbuf.getIndex();
        for (int i = 0; i < len; i++) { if ((buf[i] & 0xFF) < 32) r |= shift(buf[i] & 0xFF); }
        return r;
    }

    private void backoff_TN3270e(String why) {
        trace.trace_dsn("Aborting TN3270E: %s\n", why);
        wontDoOption[2] = TelnetConstants.TELOPT_TN3270E;
        sendRawOutput(wontDoOption, wontDoOption.length);
        trace.trace_dsn("SENT WONT TN3270E\n");
        currentLUIndex = 0;
        clientOptions[TelnetConstants.TELOPT_TN3270E & 0xFF] = 0;
        checkIn3270();
    }

    private void cook(byte[] buffer, int length) {
        if (!isAnsi() || (keyboard.keyboardLock & KeyboardConstants.AwaitingFirst) != 0) return;
        if (linemode) { log.warn("net_cookedout not implemented for line mode"); return; }
        sendCookedOut(buffer, length);
    }

    private void sendCookedOut(byte[] buf, int len) {
        if (appres.toggled(Appres.DSTrace)) {
            trace.trace_dsn(">");
            for (int i = 0; i < len; i++) trace.trace_dsn(" %s", Util.controlSee(buf[i]));
            trace.trace_dsn("\n");
        }
        sendRawOutput(buf, len);
    }

    private void cookedInitialized() { log.debug("cooked_init called"); }

    private void reactToConnectionChange(boolean success) {
        if (isConnected() || appres.disconnect_clear) controller.erase(true);
    }

    private void hostConnected() {
        connectionState = ConnectionState.CONNECTED_INITIAL;
        onPrimaryConnectionChanged(true);
    }

    private void netConnected() {
        trace.trace_dsn("NETCONNECTED Connected to %s, port %d.\n", address, port);
        Arrays.fill(clientOptions, 0);
        Arrays.fill(hostOptions, 0);
        if (connectionConfig.isIgnoreSequenceCount()) {
            currentOptionMask = shift(TelnetConstants.TN3270E_FUNC_BIND_IMAGE) | shift(TelnetConstants.TN3270E_FUNC_SYSREQ);
        } else {
            currentOptionMask = shift(TelnetConstants.TN3270E_FUNC_BIND_IMAGE) | shift(TelnetConstants.TN3270E_FUNC_RESPONSES) | shift(TelnetConstants.TN3270E_FUNC_SYSREQ);
        }
        eTransmitSequence = 0;
        responseRequired = TnHeader.HeaderResponseFlags.NO_RESPONSE;
        telnetState = TelnetState.DATA;
        bytesReceived = 0; bytesSent = 0;
        syncing = false; tn3270e_negotiated = false;
        tn3270eSubmode = TN3270ESubmode.NONE; tn3270eBound = false;
        checkLineMode(true);
    }

    void hostDisconnect(boolean failed) {
        if (isConnected() || isPending()) {
            disconnect();
            trace.stop(isAnsi());
            connectionState = ConnectionState.NOT_CONNECTED;
            onPrimaryConnectionChanged(false);
        }
    }

    private String getCommand(int index) {
        int idx = index - TelnetConstants.TELCMD_FIRST;
        if (idx >= 0 && idx < TelnetConstants.TELNET_COMMANDS.length) return TelnetConstants.TELNET_COMMANDS[idx];
        return "?" + index;
    }

    private String getOption(int index) {
        if (index >= 0 && index < TelnetConstants.TELNET_OPTIONS.length) return TelnetConstants.TELNET_OPTIONS[index];
        return index == TelnetConstants.TELOPT_TN3270E ? "TN3270E" : String.valueOf(index);
    }

    private String getOption(byte b) { return getOption(b & 0xFF); }

    private String getFunctionName(int i) {
        if (i >= 0 && i < TelnetConstants.FUNCTION_NAMES.length) return TelnetConstants.FUNCTION_NAMES[i];
        return "?[function_name=" + i + "]?";
    }

    private static byte parseControlCharacter(String s) {
        if (s == null || s.isEmpty()) return 0;
        if (s.length() > 1) {
            if (s.charAt(0) != '^') return 0;
            if (s.charAt(1) == '?') return (byte)0x7f;
            return (byte)(s.charAt(1) - '@');
        }
        return (byte)s.charAt(0);
    }

    private static int shift(int n) { return (1 << n); }

    private static String dumpToString(byte[] data, int length) {
        StringBuilder sb = new StringBuilder(" ");
        for (int i = 0; i < length; i++) sb.append(String.format("%02x ", data[i]));
        return sb.toString();
    }

    // ------------------------------------------------------------------ //
    // Event-fire helpers
    // ------------------------------------------------------------------ //

    private void fireTelnetData(Object pd, TNEvent ev, String text) {
        for (TelnetDataListener l : telnetDataListeners) l.onTelnetData(pd, ev, text);
    }

    protected void onPrimaryConnectionChanged(boolean success) {
        reactToConnectionChange(success);
        for (Consumer<Boolean> l : primaryConnectionChangedListeners) l.accept(success);
    }

    protected void onConnected3270(boolean is3270) {
        reactToConnectionChange(is3270);
        for (Consumer<Boolean> l : connected3270Listeners) l.accept(is3270);
    }

    protected void onConnectionPending() {
        for (Runnable r : connectionPendingListeners) r.run();
    }

    protected void onConnectedLineMode() {
        for (Runnable r : connectedLineModeListeners) r.run();
    }

    protected void onCursorLocationChanged() {
        for (Runnable r : cursorLocationChangedListeners) r.run();
    }

    private void onControllerCursorLocationChanged() {
        onCursorLocationChanged();
    }
}
