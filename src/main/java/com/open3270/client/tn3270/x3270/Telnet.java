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
package com.open3270.client.tn3270.x3270;

import com.open3270.client.ConnectionConfig;
import com.open3270.client.exceptions.TNHostException;
import com.open3270.client.tn3270.*;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Core TN3270/TN3270E protocol implementation.
 * Manages the TCP connection, Telnet option negotiation, and 3270 data stream processing.
 */
public class Telnet {

    // ── internal state ────────────────────────────────────────────────────────
    private Socket           socket;
    private OutputStream     out;
    private InputStream      in;
    private Thread           receiverThread;
    private volatile boolean running         = false;
    private volatile boolean socketConnected = false;

    // TN3270E negotiation state
    private boolean tn3270eEnabled   = false;
    private boolean tn3270eNegotiated= false;
    private boolean binaryMode       = false;
    private boolean eorMode          = false;
    private boolean ttypeMode        = false;

    private String disconnectReason  = "";

    // ── sub-components ────────────────────────────────────────────────────────
    Controller  controller;
    Keyboard    keyboard;
    Appres      appres;
    TNTrace     trace;
    Events      events;
    ConnectionConfig config;

    // ── synchronisation ───────────────────────────────────────────────────────
    private final Object connectionLock = new Object();

    // ── data stream accumulator ───────────────────────────────────────────────
    private final List<Byte> sbBuffer   = new ArrayList<>();
    private final List<Byte> recvBuffer = new ArrayList<>();

    // ── telnet state machine ─────────────────────────────────────────────────
    private TelnetState telnetState = TelnetState.Data;
    private int         currentOption = 0;

    // ── sequence number for TN3270E ─────────────────────────────────────────
    private int sequenceNumber = 0;

    public Telnet(ConnectionConfig config, Appres appres, TNTrace trace, Events events) {
        this.config = config;
        this.appres = appres;
        this.trace  = trace;
        this.events = events;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public void connect(String host, int port) throws TNHostException {
        try {
            if (config.isUseSSL()) {
                socket = SSLSocketFactory.getDefault().createSocket(host, port);
            } else {
                socket = new Socket(host, port);
            }
            socket.setTcpNoDelay(true);
            out = socket.getOutputStream();
            in  = socket.getInputStream();
            socketConnected = true;

            controller = new Controller(this, appres, trace);
            keyboard   = new Keyboard(this, trace, controller);
            keyboard.setKeyboardLock(KeyboardConstants.NOT_CONNECTED);

            running = true;
            receiverThread = new Thread(this::receiveLoop, "TN3270-Receiver");
            receiverThread.setDaemon(true);
            receiverThread.start();

            if (trace != null) trace.writeLine("Connected to " + host + ":" + port);
        } catch (IOException e) {
            throw new TNHostException("Connection failed to " + host + ":" + port,
                                      e.getMessage(), null);
        }
    }

    public void disconnect() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socketConnected = false;
        if (keyboard != null) keyboard.setKeyboardLock(KeyboardConstants.NOT_CONNECTED);
        synchronized (connectionLock) { connectionLock.notifyAll(); }
    }

    /** Wait up to timeoutMs for keyboard to be unlocked (NOT_CONNECTED cleared). */
    public boolean waitForConnect(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (connectionLock) {
            while (System.currentTimeMillis() < deadline) {
                if (keyboard != null &&
                    (keyboard.getKeyboardLock() & KeyboardConstants.NOT_CONNECTED) == 0) {
                    return true;
                }
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) break;
                try { connectionLock.wait(Math.min(remaining, 100)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }
        return keyboard != null &&
               (keyboard.getKeyboardLock() & KeyboardConstants.NOT_CONNECTED) == 0;
    }

    /** Wait up to timeoutMs for keyboard to be fully unlocked (all bits clear). */
    public boolean waitForUnlock(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (connectionLock) {
            while (System.currentTimeMillis() < deadline) {
                if (keyboard != null && keyboard.getKeyboardLock() == 0) return true;
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) break;
                try { connectionLock.wait(Math.min(remaining, 100)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }
        return keyboard != null && keyboard.getKeyboardLock() == 0;
    }

    /**
     * Send a data payload to the host, doubling any embedded IAC bytes,
     * and appending IAC EOR.
     */
    public synchronized void sendData(byte[] data, int length) throws IOException {
        if (!socketConnected || out == null) throw new IOException("Not connected");
        if (tn3270eNegotiated) {
            // TN3270E header
            byte seq1 = (byte) ((sequenceNumber >> 8) & 0xFF);
            byte seq2 = (byte) (sequenceNumber & 0xFF);
            sequenceNumber = (sequenceNumber + 1) & 0xFFFF;
            out.write(TnHeader.DataType.Data3270);
            out.write(0x00); // request flag
            out.write(0x00); // response flag
            out.write(seq1);
            out.write(seq2);
        }
        for (int i = 0; i < length; i++) {
            byte b = data[i];
            out.write(b & 0xFF);
            if ((b & 0xFF) == (TelnetConstants.IAC & 0xFF)) out.write(TelnetConstants.IAC & 0xFF);
        }
        out.write(TelnetConstants.IAC & 0xFF);
        out.write(TelnetConstants.EOR & 0xFF);
        out.flush();
    }

    public synchronized void sendByte(byte b) throws IOException {
        if (out == null) return;
        out.write(b & 0xFF);
        out.flush();
    }

    public synchronized void sendBytes(byte[] bytes) throws IOException {
        if (out == null) return;
        out.write(bytes);
        out.flush();
    }

    /** Send IAC IP (interrupt process). */
    public synchronized void interrupt() throws IOException {
        if (out == null) return;
        out.write(TelnetConstants.IAC & 0xFF);
        out.write(TelnetConstants.IP & 0xFF);
        out.flush();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Receiver loop
    // ─────────────────────────────────────────────────────────────────────────

    private void receiveLoop() {
        try {
            while (running) {
                int b = in.read();
                if (b < 0) { handleDisconnect("Server closed connection"); break; }
                processByte((byte) b);
            }
        } catch (IOException e) {
            if (running) handleDisconnect(e.getMessage());
        }
    }

    private void processByte(byte raw) throws IOException {
        int b = raw & 0xFF;

        switch (telnetState) {
            case Data:
                if (b == (TelnetConstants.IAC & 0xFF)) {
                    telnetState = TelnetState.IAC;
                } else {
                    recvBuffer.add((byte) b);
                }
                break;

            case IAC:
                switch (b) {
                    case 0xFF: // escaped IAC in data
                        recvBuffer.add((byte) 0xFF);
                        telnetState = TelnetState.Data;
                        break;
                    case 0xEF: // EOR
                        handleEOR();
                        telnetState = TelnetState.Data;
                        break;
                    case 0xFB: // WILL
                        telnetState = TelnetState.Will;
                        break;
                    case 0xFC: // WONT
                        telnetState = TelnetState.Wont;
                        break;
                    case 0xFD: // DO
                        telnetState = TelnetState.Do;
                        break;
                    case 0xFE: // DONT
                        telnetState = TelnetState.Dont;
                        break;
                    case 0xFA: // SB
                        sbBuffer.clear();
                        telnetState = TelnetState.SB;
                        break;
                    case 0xF1: // NOP
                        telnetState = TelnetState.Data;
                        break;
                    default:
                        telnetState = TelnetState.Data;
                        break;
                }
                break;

            case Will:
                handleWill(b);
                telnetState = TelnetState.Data;
                break;
            case Wont:
                handleWont(b);
                telnetState = TelnetState.Data;
                break;
            case Do:
                handleDo(b);
                telnetState = TelnetState.Data;
                break;
            case Dont:
                handleDont(b);
                telnetState = TelnetState.Data;
                break;

            case SB:
                if (b == (TelnetConstants.IAC & 0xFF)) {
                    telnetState = TelnetState.SbIac;
                } else {
                    sbBuffer.add((byte) b);
                }
                break;

            case SbIac:
                if (b == 0xF0) { // SE
                    handleSB(sbBuffer);
                    sbBuffer.clear();
                    telnetState = TelnetState.Data;
                } else if (b == (TelnetConstants.IAC & 0xFF)) {
                    sbBuffer.add((byte) 0xFF);
                    telnetState = TelnetState.SB;
                } else {
                    telnetState = TelnetState.Data;
                }
                break;

            default:
                telnetState = TelnetState.Data;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Telnet option negotiation
    // ─────────────────────────────────────────────────────────────────────────

    private void handleWill(int option) throws IOException {
        if (option == TelnetConstants.TELOPT_TN3270E && !config.isRefuseTN3270E()) {
            sendDo(option);
            tn3270eEnabled = true;
            sendTN3270EConnectRequest();
        } else if (option == TelnetConstants.TELOPT_BINARY ||
                   option == TelnetConstants.TELOPT_EOR) {
            sendDo(option);
            if (option == TelnetConstants.TELOPT_EOR)   eorMode    = true;
            if (option == TelnetConstants.TELOPT_BINARY) binaryMode = true;
        } else {
            sendDont(option);
        }
    }

    private void handleWont(int option) throws IOException {
        if (option == TelnetConstants.TELOPT_TN3270E) {
            tn3270eEnabled = false;
        }
    }

    private void handleDo(int option) throws IOException {
        if (option == TelnetConstants.TELOPT_BINARY ||
            option == TelnetConstants.TELOPT_EOR) {
            sendWill(option);
            if (option == TelnetConstants.TELOPT_EOR)   eorMode    = true;
            if (option == TelnetConstants.TELOPT_BINARY) binaryMode = true;
            checkConnectionReady();
        } else if (option == TelnetConstants.TELOPT_TTYPE) {
            sendWill(option);
            ttypeMode = true;
        } else {
            sendWont(option);
        }
    }

    private void handleDont(int option) throws IOException {
        if (option == TelnetConstants.TELOPT_BINARY ||
            option == TelnetConstants.TELOPT_EOR ||
            option == TelnetConstants.TELOPT_TTYPE) {
            sendWont(option);
        }
    }

    private void checkConnectionReady() {
        if (binaryMode && eorMode && !tn3270eEnabled) {
            markConnected();
        }
    }

    private void markConnected() {
        if (keyboard != null) {
            keyboard.setKeyboardLock(keyboard.getKeyboardLock() & ~KeyboardConstants.NOT_CONNECTED);
        }
        synchronized (connectionLock) { connectionLock.notifyAll(); }
    }

    private void handleSB(List<Byte> sb) throws IOException {
        if (sb.isEmpty()) return;
        int option = sb.get(0) & 0xFF;

        if (option == TelnetConstants.TELOPT_TTYPE) {
            // Host sends SB TTYPE SEND (1) → we respond SB TTYPE IS <type>
            if (sb.size() >= 2 && (sb.get(1) & 0xFF) == TelnetConstants.TELQUAL_SEND) {
                sendTerminalType();
            }
        } else if (option == TelnetConstants.TELOPT_TN3270E) {
            handleTN3270ESB(sb);
        }
    }

    private void sendTerminalType() throws IOException {
        String termType = config.getTermType();
        byte[] typeBytes = termType.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        List<Byte> resp = new ArrayList<>();
        resp.add(TelnetConstants.IAC);
        resp.add(TelnetConstants.SB);
        resp.add((byte) TelnetConstants.TELOPT_TTYPE);
        resp.add((byte) TelnetConstants.TELQUAL_IS);
        for (byte b : typeBytes) {
            resp.add(b);
            if ((b & 0xFF) == 0xFF) resp.add((byte) 0xFF); // double IAC
        }
        resp.add(TelnetConstants.IAC);
        resp.add(TelnetConstants.SE);
        writeList(resp);
    }

    private void handleTN3270ESB(List<Byte> sb) throws IOException {
        if (sb.size() < 2) return;
        int op = sb.get(1) & 0xFF;

        if (op == TnHeader.Ops.Send) {
            // Host: SB TN3270E SEND DEVICE-TYPE → reply with CONNECT or DEVICE-TYPE
            sendTN3270EConnectRequest();
        } else if (op == TnHeader.Ops.DeviceType) {
            // Host accepted our device type
            sendTN3270EFunctionsRequest();
        } else if (op == TnHeader.Ops.Functions) {
            if (sb.size() >= 3 && (sb.get(2) & 0xFF) == TnHeader.Ops.Is) {
                // Functions negotiation complete
                tn3270eNegotiated = true;
                markConnected();
            } else if (sb.size() >= 3 && (sb.get(2) & 0xFF) == TnHeader.Ops.Request) {
                // Host is requesting specific functions – just agree
                sendTN3270EFunctionsIs(sb.subList(3, sb.size()));
            }
        } else if (op == TnHeader.Ops.Is) {
            // Connected
            tn3270eNegotiated = true;
            markConnected();
        } else if (op == TnHeader.Ops.Reject) {
            // TN3270E rejected, fall back to standard 3270
            tn3270eEnabled     = false;
            tn3270eNegotiated  = false;
        }
    }

    private void sendTN3270EConnectRequest() throws IOException {
        String termType = config.getTermType();
        byte[] typeBytes = termType.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        List<Byte> resp = new ArrayList<>();
        resp.add(TelnetConstants.IAC);
        resp.add(TelnetConstants.SB);
        resp.add((byte) TelnetConstants.TELOPT_TN3270E);
        resp.add(TnHeader.Ops.DeviceType);
        resp.add(TnHeader.Ops.Request);
        for (byte b : typeBytes) resp.add(b);
        if (config.getHostLU() != null && !config.getHostLU().isEmpty()) {
            resp.add(TnHeader.Ops.Connect);
            for (byte b : config.getHostLU().getBytes(java.nio.charset.StandardCharsets.US_ASCII)) {
                resp.add(b);
            }
        }
        resp.add(TelnetConstants.IAC);
        resp.add(TelnetConstants.SE);
        writeList(resp);
    }

    private void sendTN3270EFunctionsRequest() throws IOException {
        List<Byte> resp = new ArrayList<>();
        resp.add(TelnetConstants.IAC);
        resp.add(TelnetConstants.SB);
        resp.add((byte) TelnetConstants.TELOPT_TN3270E);
        resp.add(TnHeader.Ops.Functions);
        resp.add(TnHeader.Ops.Request);
        resp.add((byte) TelnetConstants.TN3270E_FUNC_BIND_IMAGE);
        resp.add((byte) TelnetConstants.TN3270E_FUNC_RESPONSES);
        resp.add((byte) TelnetConstants.TN3270E_FUNC_SYSREQ);
        resp.add(TelnetConstants.IAC);
        resp.add(TelnetConstants.SE);
        writeList(resp);
    }

    private void sendTN3270EFunctionsIs(List<Byte> functions) throws IOException {
        List<Byte> resp = new ArrayList<>();
        resp.add(TelnetConstants.IAC);
        resp.add(TelnetConstants.SB);
        resp.add((byte) TelnetConstants.TELOPT_TN3270E);
        resp.add(TnHeader.Ops.Functions);
        resp.add(TnHeader.Ops.Is);
        resp.addAll(functions);
        resp.add(TelnetConstants.IAC);
        resp.add(TelnetConstants.SE);
        writeList(resp);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EOR handler – process a complete 3270 record
    // ─────────────────────────────────────────────────────────────────────────

    private void handleEOR() {
        byte[] data = toByteArray(recvBuffer);
        recvBuffer.clear();

        int start = 0;
        if (tn3270eNegotiated && data.length >= TnHeader.SIZE) {
            // Strip TN3270E header
            TnHeader hdr = new TnHeader(data);
            if (hdr.dataType != TnHeader.DataType.Data3270) return;
            start = TnHeader.SIZE;
        }

        if (start >= data.length) return;

        int len = data.length - start;
        byte[] payload = new byte[len];
        System.arraycopy(data, start, payload, 0, len);

        if (controller != null) {
            controller.processDataStream(payload, len);
            // After processing, lock keyboard while host may be updating
            if (keyboard != null) {
                keyboard.setKeyboardLock(keyboard.getKeyboardLock() | KeyboardConstants.OIA_LOCKED);
                // Unlock after a brief moment (simulate keyboard unlock)
                new Thread(() -> {
                    try { Thread.sleep(KeyboardConstants.UNLOCK_MS); } catch (InterruptedException ignored) {}
                    if (keyboard != null) keyboard.unlock();
                }, "TN3270-Unlock").start();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void handleDisconnect(String reason) {
        disconnectReason = reason != null ? reason : "";
        socketConnected  = false;
        if (keyboard != null) keyboard.setKeyboardLock(KeyboardConstants.NOT_CONNECTED);
        synchronized (connectionLock) { connectionLock.notifyAll(); }
        if (trace != null) trace.writeLine("Disconnected: " + reason);
    }

    private void sendDo(int option)   throws IOException { sendCmd(TelnetConstants.DO,   option); }
    private void sendDont(int option) throws IOException { sendCmd(TelnetConstants.DONT, option); }
    private void sendWill(int option) throws IOException { sendCmd(TelnetConstants.WILL, option); }
    private void sendWont(int option) throws IOException { sendCmd(TelnetConstants.WONT, option); }

    private synchronized void sendCmd(byte verb, int option) throws IOException {
        if (out == null) return;
        out.write(TelnetConstants.IAC & 0xFF);
        out.write(verb & 0xFF);
        out.write(option & 0xFF);
        out.flush();
    }

    private synchronized void writeList(List<Byte> bytes) throws IOException {
        if (out == null) return;
        for (byte b : bytes) out.write(b & 0xFF);
        out.flush();
    }

    private static byte[] toByteArray(List<Byte> list) {
        byte[] arr = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isConnected()       { return socketConnected; }
    public boolean isSocketConnected() { return socketConnected; }
    public Controller  getController() { return controller; }
    public Keyboard    getKeyboard()   { return keyboard; }
    public Events      getEvents()     { return events; }
    public String      getDisconnectReason() { return disconnectReason; }
    public Object      getConnectionLock()   { return connectionLock; }
}
