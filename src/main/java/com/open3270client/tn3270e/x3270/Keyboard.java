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

import com.open3270client.engine.ConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Timer;
import java.util.function.Consumer;

/**
 * Keyboard input handler and action dispatcher.
 * Faithful Java 21 port of {@code Keyboard.cs}.
 */
class Keyboard implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Keyboard.class);

    // ------------------------------------------------------------------ //
    // Character Generator (CG) codes — from C# CharacterGenerator.cs
    // ------------------------------------------------------------------ //
    static final byte CG_NULL           = 0x00;
    static final byte CG_SPACE          = 0x10;
    static final byte CG_GREATER_THAN   = 0x08;
    static final byte CG_QUESTION_MARK  = 0x18;
    static final byte CG_AMPERSAND      = 0x30;
    static final byte CG_NUMERAL0       = 0x20;
    static final byte CG_NUMERAL9       = 0x29;
    static final byte CG_MINUS          = 0x31;
    static final byte CG_PERIOD         = 0x32;
    static final byte CG_FM             = (byte) 0x9e;
    static final byte CG_DUP            = (byte) 0x9f;

    // ------------------------------------------------------------------ //
    // Fields
    // ------------------------------------------------------------------ //

    private final Telnet  telnet;
    private       Actions action;
    private final TNTrace trace;

    /** Package-private: other classes in the same package read/modify this directly. */
    int keyboardLock = KeyboardConstants.NotConnected;

    private boolean insertMode  = false;
    private boolean reverseMode = false;
    private boolean flipped     = false;

    private final int PF_SZ;
    private final int PA_SZ;

    private Composing composing = Composing.NONE;

    private final Deque<TAItem> taQueue = new ArrayDeque<>();

    private Timer unlockId = null;

    // Listeners kept for unregistration in close()
    private Consumer<Boolean> primaryConnectionChangedListener;
    private Consumer<Boolean> connected3270Listener;

    // ------------------------------------------------------------------ //
    // Inner types
    // ------------------------------------------------------------------ //

    private enum Composing { NONE, COMPOSE, FIRST }

    private enum EIState { BASE, BACKSLASH, BACK_P, BACK_PF, BACK_PA, BACK_X, OCTAL, HEX, XGE }

    private enum EIAction { KEY, STRING, PASTE }

    private enum KeyType { STANDARD, GE }

    private static class AKeySym {
        byte    keysym;
        KeyType keytype;
    }

    /** Typeahead item: a deferred action captured as a lambda. */
    private static class TAItem {
        final Runnable fn;
        TAItem(Runnable fn) { this.fn = fn; }
    }

    // ------------------------------------------------------------------ //
    // Constructor / AutoCloseable
    // ------------------------------------------------------------------ //

    Keyboard(Telnet telnetObject) {
        this.telnet = telnetObject;
        this.action = telnetObject.getAction();
        this.trace  = telnetObject.getTrace();
        this.PF_SZ  = KeyboardConstants.PfTranslation.length;
        this.PA_SZ  = KeyboardConstants.PaTranslation.length;
    }

    @Override
    public void close() {
        if (primaryConnectionChangedListener != null)
            telnet.removePrimaryConnectionChangedListener(primaryConnectionChangedListener);
        if (connected3270Listener != null)
            telnet.removeConnected3270Listener(connected3270Listener);
    }

    // ------------------------------------------------------------------ //
    // Setters / accessors
    // ------------------------------------------------------------------ //

    /** Package-private setter so Actions can inject itself after construction. */
    void setActions(Actions a) { this.action = a; }

    /** Package-private getter used by Events and TN3270API. */
    int getKeyboardLock() { return keyboardLock; }

    // ------------------------------------------------------------------ //
    // Utility helpers
    // ------------------------------------------------------------------ //

    boolean akEq(AKeySym k1, AKeySym k2) {
        return (k1.keysym == k2.keysym) && (k1.keytype == k2.keytype);
    }

    private byte fromHex(char c) {
        String dx1 = "0123456789abcdef";
        String dx2 = "0123456789ABCDEF";
        int index = dx1.indexOf(c);
        if (index == -1) index = dx2.indexOf(c);
        if (index == -1) throw new IllegalArgumentException("Not a valid hex digit: '" + c + "'");
        return (byte) index;
    }

    private boolean isXDigit(char ch) {
        return "0123456789ABCDEFabcdef".indexOf(ch) != -1;
    }

    private boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    // ------------------------------------------------------------------ //
    // Typeahead queue
    // ------------------------------------------------------------------ //

    private void enqueueTypeAheadAction(Runnable fn) {
        if (!telnet.isConnected()) {
            trace.trace_event("  dropped (not connected)\n");
            return;
        }
        if ((keyboardLock & KeyboardConstants.ErrorMask) != 0) {
            trace.trace_event("  dropped (operator error)\n");
            return;
        }
        if ((keyboardLock & KeyboardConstants.Scrolled) != 0) {
            trace.trace_event("  dropped (scrolled)\n");
            return;
        }
        if (!telnet.getAppres().typeahead) {
            trace.trace_event("  dropped (no typeahead)\n");
            return;
        }
        taQueue.addLast(new TAItem(fn));
        trace.trace_event("  action queued (kybdlock 0x%x)\n", keyboardLock);
    }

    boolean runTypeAhead() {
        if (keyboardLock == 0 && !taQueue.isEmpty()) {
            TAItem item = taQueue.pollFirst();
            item.fn.run();
            return true;
        }
        return false;
    }

    private boolean flushTypeAheadQueue() {
        boolean any = !taQueue.isEmpty();
        taQueue.clear();
        return any;
    }

    // ------------------------------------------------------------------ //
    // Keyboard lock management
    // ------------------------------------------------------------------ //

    void keyboardLockSet(int bits, String cause) {
        int n = keyboardLock | bits;
        if (n != keyboardLock) keyboardLock = n;
    }

    void keyboardLockClear(int bits, String debug) {
        if (bits == -1) bits = 0xFFFF;
        int n = keyboardLock & ~bits;
        if (n != keyboardLock) keyboardLock = n;
    }

    void toggleEnterInhibitMode(boolean inhibit) {
        if (inhibit) keyboardLockSet(KeyboardConstants.EnterInhibit, "kybd_inhibit");
        else         keyboardLockClear(KeyboardConstants.EnterInhibit, "kybd_inhibit");
    }

    void resetKeyboardLock(boolean explicitValue) {
        if (explicitValue) {
            boolean halfReset = false;
            if (flushTypeAheadQueue()) halfReset = true;
            if (composing != Composing.NONE) {
                composing = Composing.NONE;
                halfReset = true;
            }
            if (halfReset) return;
        }

        insertMode = false;

        if (!telnet.isConnected()) return;

        if ((keyboardLock & KeyboardConstants.DeferredUnlock) != 0)
            telnet.getController().removeTimeout(unlockId);

        if (explicitValue) {
            keyboardLockClear(-1, "resetKeyboardLock");
        } else if ((keyboardLock & (KeyboardConstants.DeferredUnlock | KeyboardConstants.OiaTWait
                | KeyboardConstants.OiaLocked | KeyboardConstants.AwaitingFirst)) != 0) {
            trace.writeLine("Clear lock in deferred path");
            keyboardLockClear(~KeyboardConstants.DeferredUnlock, "resetKeyboardLock");
            keyboardLockSet(KeyboardConstants.DeferredUnlock, "resetKeyboardLock");
            synchronized (telnet) {
                unlockId = telnet.getController().addTimeout(KeyboardConstants.UnlockMS, this::deferUnlock);
            }
        }

        composing = Composing.NONE;
    }

    private void deferUnlock() {
        synchronized (telnet) {
            if ((keyboardLock | KeyboardConstants.DeferredUnlock) == KeyboardConstants.DeferredUnlock) {
                trace.writeLine("--debug--defer_unlock");
                keyboardLockClear(KeyboardConstants.DeferredUnlock, "defer_unlock");
                if (telnet.isConnected()) telnet.getController().processPendingInput();
            } else {
                trace.writeLine("--debug--defer_unlock ignored");
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Connection / mode state callbacks
    // ------------------------------------------------------------------ //

    void connectedStateChanged(boolean connected) {
        if ((keyboardLock & KeyboardConstants.DeferredUnlock) != 0)
            telnet.getController().removeTimeout(unlockId);
        keyboardLockClear(-1, "kybd_connect");
        if (connected) {
            keyboardLockSet(KeyboardConstants.AwaitingFirst, "kybd_connect");
        } else {
            keyboardLockSet(KeyboardConstants.NotConnected, "kybd_connect");
            flushTypeAheadQueue();
        }
    }

    void switchMode3270(boolean in3270) {
        if ((keyboardLock & KeyboardConstants.DeferredUnlock) != 0)
            telnet.getController().removeTimeout(unlockId);
        keyboardLockClear(-1, "kybd_connect");
    }

    void initialize() {
        primaryConnectionChangedListener = this::connectedStateChanged;
        connected3270Listener            = this::switchMode3270;
        telnet.addPrimaryConnectionChangedListener(primaryConnectionChangedListener);
        telnet.addConnected3270Listener(connected3270Listener);
    }

    // ------------------------------------------------------------------ //
    // Operator error and AID key handling
    // ------------------------------------------------------------------ //

    void handleOperatorError(int address, int errorType) {
        Controller ctrl = telnet.getController();
        log.debug("cursor@{} - ROW={} COL={}", address,
                ctrl.addressToRow(address), ctrl.addressToColumn(address));
        telnet.getEvents().showError("Keyboard locked");
        log.warn("operator_error error_type={}", errorType);
        if (telnet.getConfig().isLockScreenOnWriteToUnprotected()) {
            keyboardLockSet(errorType, "operator_error");
            flushTypeAheadQueue();
        }
    }

    void handleAttentionIdentifierKey(byte aidCode) {
        if (telnet.isAnsi()) {
            if (aidCode == AID.ENTER) {
                telnet.sendChar('\r');
                return;
            }
            for (int i = 0; i < PF_SZ; i++) {
                if (aidCode == KeyboardConstants.PfTranslation[i]) {
                    telnet.getAnsi().ansi_send_pf(i + 1);
                    return;
                }
            }
            for (int i = 0; i < PA_SZ; i++) {
                if (aidCode == KeyboardConstants.PaTranslation[i]) {
                    telnet.getAnsi().ansi_send_pa(i + 1);
                    return;
                }
            }
            return;
        }
        if (telnet.isSscp()) {
            if ((keyboardLock & KeyboardConstants.OiaMinus) != 0) return;
            if (aidCode != AID.ENTER && aidCode != AID.CLEAR) {
                keyboardLockSet(KeyboardConstants.OiaMinus, "key_AID");
                return;
            }
        }
        if (telnet.isSscp() && aidCode == AID.ENTER) {
            telnet.getController().setBufferAddress(telnet.getController().getCursorAddress());
        }
        if (!telnet.isSscp() || aidCode != AID.CLEAR) {
            insertMode = false;
            keyboardLockSet(KeyboardConstants.OiaTWait | KeyboardConstants.OiaLocked, "key_AID");
        }
        telnet.getIdle().resetIdleTimer();
        telnet.getController().setAttentionID(aidCode);
        telnet.getController().processReadModifiedCommand(
                telnet.getController().getAttentionID(), false);
    }

    // ------------------------------------------------------------------ //
    // Character handling
    // ------------------------------------------------------------------ //

    private boolean wrapCharacter(int cgcode) {
        boolean withGE  = false;
        boolean pasting = false;
        if ((cgcode & KeyboardConstants.WFlag) != 0) {
            withGE = true;
            cgcode &= ~KeyboardConstants.WFlag;
        }
        if ((cgcode & KeyboardConstants.PasteWFlag) != 0) {
            pasting = true;
            cgcode  &= ~KeyboardConstants.PasteWFlag;
        }
        trace.trace_event(" nop -> Key(%s\"%s\")\n",
                withGE ? "GE " : "",
                Util.controlSee(Tables.CG_2_ASCII[cgcode & 0xFF]));
        return handleOrdinaryCharacter(cgcode, withGE, pasting);
    }

    boolean handleOrdinaryCharacter(int cgCode, boolean withGE, boolean pasting) {
        Controller ctrl = telnet.getController();
        int address;
        int endAddress;
        int fa;
        boolean noRoom = false;

        if (keyboardLock != 0) {
            log.debug("keyboard locked; dropping character (keyboardLock=0x{:x})", keyboardLock);
            return false;
        }

        address = ctrl.getCursorAddress();
        fa      = ctrl.getFieldAttribute(address);
        byte favalue = ctrl.getFakeFA();
        if (fa != -1) favalue = ctrl.getScreenBuffer()[fa];

        if (FieldAttribute.isFA(ctrl.getScreenBuffer()[address])
                || FieldAttribute.isProtected(favalue)) {
            handleOperatorError(address, KeyboardConstants.ErrorProtected);
            return false;
        }
        if (telnet.getAppres().numeric_lock && FieldAttribute.isNumeric(favalue)
                && !((cgCode >= (CG_NUMERAL0 & 0xFF) && cgCode <= (CG_NUMERAL9 & 0xFF))
                     || cgCode == (CG_MINUS & 0xFF) || cgCode == (CG_PERIOD & 0xFF))) {
            handleOperatorError(address, KeyboardConstants.ErrorNumeric);
            return false;
        }

        if (reverseMode || (insertMode && ctrl.getScreenBuffer()[address] != 0)) {
            int lastBlank = -1;
            endAddress = address;
            if (ctrl.getScreenBuffer()[endAddress] == CG_SPACE) lastBlank = endAddress;
            do {
                endAddress = ctrl.incrementAddress(endAddress);
                if (ctrl.getScreenBuffer()[endAddress] == CG_SPACE) lastBlank = endAddress;
                if (ctrl.getScreenBuffer()[endAddress] == CG_NULL
                        || FieldAttribute.isFA(ctrl.getScreenBuffer()[endAddress])) break;
            } while (endAddress != address);

            if (telnet.getAppres().toggled(Appres.BlankFill) && lastBlank != -1) {
                lastBlank = ctrl.incrementAddress(lastBlank);
                if (lastBlank == endAddress) {
                    endAddress = ctrl.decrementAddress(endAddress);
                    ctrl.addCharacter(endAddress, CG_NULL, (byte) 0);
                }
            }

            if (ctrl.getScreenBuffer()[endAddress] != CG_NULL) {
                if (insertMode) {
                    handleOperatorError(endAddress, KeyboardConstants.ErrorOverflow);
                    return false;
                } else {
                    noRoom = true;
                }
            } else {
                if (endAddress > address) {
                    ctrl.copyBlock(address, address + 1, endAddress - address, false);
                } else if (endAddress < address) {
                    ctrl.copyBlock(0, 1, endAddress, false);
                    ctrl.addCharacter(0, ctrl.getScreenBuffer()[(ctrl.getRowCount() * ctrl.getColumnCount()) - 1], (byte) 0);
                    ctrl.copyBlock(address, address + 1,
                            ((ctrl.getRowCount() * ctrl.getColumnCount()) - 1) - address, false);
                }
            }
        }

        // Replace leading nulls with blanks if BlankFill is active
        if (ctrl.isFormatted() && telnet.getAppres().toggled(Appres.BlankFill)) {
            int addrSOF     = fa;
            int addressFill = ctrl.decrementAddress(address);
            while (addressFill != addrSOF) {
                if ((addressFill % ctrl.getColumnCount()) == ctrl.getColumnCount() - 1) {
                    boolean aborted    = true;
                    int     addressScan = addressFill;
                    while (addressScan != addrSOF) {
                        if (ctrl.getScreenBuffer()[addressScan] != CG_NULL) {
                            aborted = false;
                            break;
                        }
                        if (0 == (addressScan % ctrl.getColumnCount())) break;
                        addressScan = ctrl.decrementAddress(addressScan);
                    }
                    if (aborted) break;
                }
                if (ctrl.getScreenBuffer()[addressFill] == CG_NULL)
                    ctrl.addCharacter(addressFill, CG_SPACE, (byte) 0);
                addressFill = ctrl.decrementAddress(addressFill);
            }
        }

        if (noRoom) {
            do {
                address = ctrl.incrementAddress(address);
            } while (!FieldAttribute.isFA(ctrl.getScreenBuffer()[address]));
        } else {
            ctrl.addCharacter(address, (byte) cgCode,
                    withGE ? (byte) ControllerConstant.CS_GE : (byte) 0);
            ctrl.setForegroundColor(address, (byte) 0);
            ctrl.addGr(address, (byte) 0);
            if (!reverseMode) address = ctrl.incrementAddress(address);
        }

        if (pasting || (cgCode != (CG_DUP & 0xFF))) {
            while (FieldAttribute.isFA(ctrl.getScreenBuffer()[address])) {
                if (FieldAttribute.isSkip(ctrl.getScreenBuffer()[address])) {
                    address = ctrl.getNextUnprotectedField(address);
                } else {
                    address = ctrl.incrementAddress(address);
                }
            }
            ctrl.setCursorAddress(address);
        }

        ctrl.setMDT(ctrl.getScreenBuffer(), fa);
        return true;
    }

    private void handleAsciiCharacter(byte character, KeyType keytype, EIAction cause) {
        switch (composing) {
            case NONE:   break;
            case COMPOSE: composing = Composing.NONE; return;
            case FIRST:   composing = Composing.NONE; return;
        }

        trace.trace_event(" %s -> Key(\"%s\")\n", cause.name(), Util.controlSee(character));

        if (telnet.is3270()) {
            if ((character & 0xFF) < ' ') {
                trace.trace_event("  dropped (control char)\n");
                return;
            }
            handleOrdinaryCharacter(Tables.ASCII_2_CG[character & 0xFF] & 0xFF,
                    keytype == KeyType.GE, false);
        } else if (telnet.isAnsi()) {
            telnet.sendChar((char) (character & 0xFF));
        } else {
            trace.trace_event("  dropped (not connected)\n");
        }
    }

    // ------------------------------------------------------------------ //
    // PsSet — set a pending string (from String/HexString actions)
    // ------------------------------------------------------------------ //

    private void psSet(String text, boolean isHex) {
        ConnectionConfig config = telnet.getConfig();
        boolean skipToUnprotected = config.isAlwaysSkipToUnprotected();
        Controller ctrl = telnet.getController();
        int address = ctrl.getCursorAddress();

        if (skipToUnprotected) {
            boolean ok;
            int faIdx;
            do {
                ok   = true;
                faIdx = ctrl.getFieldAttribute(address);
                if (faIdx == -1) break;
                if (FieldAttribute.isFA(ctrl.getScreenBuffer()[address])
                        || (faIdx >= 0 && FieldAttribute.isProtected(ctrl.getScreenBuffer()[faIdx]))) {
                    ok      = false;
                    address = ctrl.incrementAddress(address);
                    if (address == ctrl.getCursorAddress()) {
                        log.warn("Screen has no unprotected field!");
                        return;
                    }
                }
            } while (!ok);

            if (address != ctrl.getCursorAddress()) {
                log.debug("Moved cursor to {} to skip protected fields", address);
                ctrl.setCursorAddress(address);
            }
        }

        emulateInput(text, false);
    }

    // ------------------------------------------------------------------ //
    // Action methods  (void — used by Actions.java dispatch table)
    // ------------------------------------------------------------------ //

    void attnAction(Object[] args) {
        if (telnet.is3270()) telnet.interrupt();
    }

    void backSpaceAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> backSpaceAction(args)); return; }
        if (telnet.isAnsi()) { telnet.sendErase(); return; }
        if (reverseMode) {
            deleteCharacter();
        } else if (!flipped) {
            moveLeft();
        } else {
            Controller ctrl = telnet.getController();
            int address = ctrl.decrementAddress(ctrl.getCursorAddress());
            ctrl.setCursorAddress(address);
        }
    }

    void backTabAction(Object[] args) {
        if (!telnet.is3270()) return;
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> backTabAction(args)); return; }
        Controller ctrl = telnet.getController();
        int baddr  = ctrl.getCursorAddress();
        baddr = ctrl.decrementAddress(baddr);
        if (FieldAttribute.isFA(ctrl.getScreenBuffer()[baddr])) baddr = ctrl.decrementAddress(baddr);
        int sbaddr = baddr;
        while (true) {
            int nbaddr = ctrl.incrementAddress(baddr);
            if (FieldAttribute.isFA(ctrl.getScreenBuffer()[baddr])
                    && !FieldAttribute.isProtected(ctrl.getScreenBuffer()[baddr])
                    && !FieldAttribute.isFA(ctrl.getScreenBuffer()[nbaddr]))
                break;
            baddr = ctrl.decrementAddress(baddr);
            if (baddr == sbaddr) { ctrl.setCursorAddress(0); return; }
        }
        baddr = ctrl.incrementAddress(baddr);
        ctrl.setCursorAddress(baddr);
    }

    void circumNotAction(Object[] args) {
        if (telnet.is3270() && composing == Composing.NONE)
            handleAsciiCharacter((byte) 0xac, KeyType.STANDARD, EIAction.KEY);
        else
            handleAsciiCharacter((byte) '^', KeyType.STANDARD, EIAction.KEY);
    }

    void clearAction(Object[] args) {
        if ((keyboardLock & KeyboardConstants.OiaMinus) != 0) return;
        if (keyboardLock != 0 && telnet.isConnected()) {
            enqueueTypeAheadAction(() -> clearAction(args)); return;
        }
        if (telnet.isAnsi()) { telnet.getAnsi().ansi_send_clear(); return; }
        Controller ctrl = telnet.getController();
        ctrl.setBufferAddress(0);
        ctrl.clear(true);
        ctrl.setCursorAddress(0);
        if (telnet.isConnected()) handleAttentionIdentifierKey(AID.CLEAR);
    }

    void cursorSelectAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> cursorSelectAction(args)); return; }
        if (telnet.isAnsi()) return;
        lightPenSelect(telnet.getController().getCursorAddress());
    }

    void deleteAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> deleteAction(args)); return; }
        if (telnet.isAnsi()) { telnet.sendByte((byte) 0x7f); return; }
        if (!deleteCharacter()) return;
        if (reverseMode) {
            Controller ctrl = telnet.getController();
            int address = ctrl.decrementAddress(ctrl.getCursorAddress());
            if (!FieldAttribute.isFA(ctrl.getScreenBuffer()[address]))
                ctrl.setCursorAddress(address);
        }
    }

    void deleteFieldAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> deleteFieldAction(args)); return; }
        if (telnet.isAnsi()) { telnet.sendKill(); return; }
        Controller ctrl = telnet.getController();
        if (!ctrl.isFormatted()) return;
        int address = ctrl.getCursorAddress();
        int faIndex = ctrl.getFieldAttribute(address);
        byte fa     = ctrl.getFakeFA();
        if (faIndex != -1) fa = ctrl.getScreenBuffer()[faIndex];
        if (FieldAttribute.isProtected(fa) || FieldAttribute.isFA(ctrl.getScreenBuffer()[address])) {
            handleOperatorError(address, KeyboardConstants.ErrorProtected); return;
        }
        while (!FieldAttribute.isFA(ctrl.getScreenBuffer()[address]))
            address = ctrl.decrementAddress(address);
        address = ctrl.incrementAddress(address);
        ctrl.setCursorAddress(address);
        while (!FieldAttribute.isFA(ctrl.getScreenBuffer()[address])) {
            ctrl.addCharacter(address, CG_NULL, (byte) 0);
            address = ctrl.incrementAddress(address);
        }
        ctrl.setMDT(ctrl.getScreenBuffer(), faIndex);
    }

    void deleteWordAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> deleteWordAction(args)); return; }
        if (telnet.isAnsi()) { telnet.sendWErase(); return; }
        Controller ctrl = telnet.getController();
        if (!ctrl.isFormatted()) return;
        int address = ctrl.getCursorAddress();
        int faIndex = ctrl.getFieldAttribute(address);
        byte fa     = ctrl.getFakeFA();
        if (faIndex != -1) fa = ctrl.getScreenBuffer()[faIndex];
        if (FieldAttribute.isProtected(fa) || FieldAttribute.isFA(ctrl.getScreenBuffer()[address])) {
            handleOperatorError(address, KeyboardConstants.ErrorProtected); return;
        }
        int frontAddress = address;
        while (ctrl.getScreenBuffer()[frontAddress] == CG_SPACE
                || ctrl.getScreenBuffer()[frontAddress] == CG_NULL)
            frontAddress = ctrl.decrementAddress(frontAddress);
        if (FieldAttribute.isFA(ctrl.getScreenBuffer()[frontAddress])) {
            ctrl.setCursorAddress(frontAddress + 1); return;
        }
        while (!FieldAttribute.isFA(ctrl.getScreenBuffer()[frontAddress])
                && ctrl.getScreenBuffer()[frontAddress] != CG_SPACE
                && ctrl.getScreenBuffer()[frontAddress] != CG_NULL)
            frontAddress = ctrl.decrementAddress(frontAddress);
        frontAddress = ctrl.incrementAddress(frontAddress);
        int backAddress = frontAddress;
        while (!FieldAttribute.isFA(ctrl.getScreenBuffer()[backAddress])
                && ctrl.getScreenBuffer()[backAddress] != CG_SPACE
                && ctrl.getScreenBuffer()[backAddress] != CG_NULL)
            backAddress = ctrl.incrementAddress(backAddress);
        while (ctrl.getScreenBuffer()[backAddress] == CG_SPACE
                || ctrl.getScreenBuffer()[backAddress] == CG_NULL)
            backAddress = ctrl.incrementAddress(backAddress);
        int endAddress = backAddress;
        while (!FieldAttribute.isFA(ctrl.getScreenBuffer()[endAddress]))
            endAddress = ctrl.incrementAddress(endAddress);
        int addr  = frontAddress;
        int addr2 = backAddress;
        while (addr2 != endAddress) {
            ctrl.addCharacter(addr, ctrl.getScreenBuffer()[addr2], (byte) 0);
            addr  = ctrl.incrementAddress(addr);
            addr2 = ctrl.incrementAddress(addr2);
        }
        while (addr != endAddress) {
            ctrl.addCharacter(addr, CG_NULL, (byte) 0);
            addr = ctrl.incrementAddress(addr);
        }
        ctrl.setMDT(ctrl.getScreenBuffer(), faIndex);
        ctrl.setCursorAddress(frontAddress);
    }

    void moveCursorDown(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> moveCursorDown(args)); return; }
        if (telnet.isAnsi()) { telnet.getAnsi().ansi_send_down(); return; }
        Controller ctrl = telnet.getController();
        int address = (ctrl.getCursorAddress() + ctrl.getColumnCount())
                      % (ctrl.getColumnCount() * ctrl.getRowCount());
        ctrl.setCursorAddress(address);
    }

    void dupAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> dupAction(args)); return; }
        if (telnet.isAnsi()) return;
        if (handleOrdinaryCharacter(CG_DUP & 0xFF, false, false))
            telnet.getController().setCursorAddress(
                    telnet.getController().getNextUnprotectedField(
                            telnet.getController().getCursorAddress()));
    }

    void emulateInputAction(Object[] args) {
        if (args == null) return;
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) if (arg != null) sb.append(arg);
        emulateInput(sb.toString(), false);
    }

    void enterAction(Object[] args) {
        if ((keyboardLock & KeyboardConstants.OiaMinus) != 0) return;
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> enterAction(args)); return; }
        handleAttentionIdentifierKey(AID.ENTER);
    }

    void eraseAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> eraseAction(args)); return; }
        if (telnet.isAnsi()) { telnet.sendErase(); return; }
        Controller ctrl = telnet.getController();
        int address = ctrl.getCursorAddress();
        int faIndex = ctrl.getFieldAttribute(address);
        byte fa     = ctrl.getFakeFA();
        if (faIndex != -1) fa = ctrl.getScreenBuffer()[faIndex];
        if (faIndex == address || FieldAttribute.isProtected(fa)) {
            handleOperatorError(address, KeyboardConstants.ErrorProtected); return;
        }
        if (address != 0 && faIndex == address - 1) return;
        moveLeft();
        deleteCharacter();
    }

    void eraseEndOfFieldAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> eraseEndOfFieldAction(args)); return; }
        if (telnet.isAnsi()) return;
        Controller ctrl = telnet.getController();
        int address = ctrl.getCursorAddress();
        int faIndex = ctrl.getFieldAttribute(address);
        byte fa     = ctrl.getFakeFA();
        if (faIndex != -1) fa = ctrl.getScreenBuffer()[faIndex];
        if (FieldAttribute.isProtected(fa) || FieldAttribute.isFA(ctrl.getScreenBuffer()[address])) {
            handleOperatorError(address, KeyboardConstants.ErrorProtected); return;
        }
        if (ctrl.isFormatted()) {
            do {
                ctrl.addCharacter(address, CG_NULL, (byte) 0);
                address = ctrl.incrementAddress(address);
            } while (!FieldAttribute.isFA(ctrl.getScreenBuffer()[address]));
            ctrl.setMDT(ctrl.getScreenBuffer(), faIndex);
        } else {
            do {
                ctrl.addCharacter(address, CG_NULL, (byte) 0);
                address = ctrl.incrementAddress(address);
            } while (address != 0);
        }
    }

    void eraseInputAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> eraseInputAction(args)); return; }
        if (telnet.isAnsi()) return;
        Controller ctrl = telnet.getController();
        if (ctrl.isFormatted()) {
            int address = 0;
            do {
                if (FieldAttribute.isFA(ctrl.getScreenBuffer()[address])) break;
                address = ctrl.incrementAddress(address);
            } while (address != 0);
            int sbAddress = address;
            boolean f     = false;
            do {
                byte fa = ctrl.getScreenBuffer()[address];
                if (!FieldAttribute.isProtected(fa)) {
                    ctrl.mdtClear(ctrl.getScreenBuffer(), address);
                    do {
                        address = ctrl.incrementAddress(address);
                        if (!f) { ctrl.setCursorAddress(address); f = true; }
                        if (!FieldAttribute.isFA(ctrl.getScreenBuffer()[address]))
                            ctrl.addCharacter(address, CG_NULL, (byte) 0);
                    } while (!FieldAttribute.isFA(ctrl.getScreenBuffer()[address]));
                } else {
                    do { address = ctrl.incrementAddress(address); }
                    while (!FieldAttribute.isFA(ctrl.getScreenBuffer()[address]));
                }
            } while (address != sbAddress);
            if (!f) ctrl.setCursorAddress(0);
        } else {
            ctrl.clear(true);
            ctrl.setCursorAddress(0);
        }
    }

    void fieldEndAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> fieldEndAction(args)); return; }
        if (telnet.isAnsi()) return;
        Controller ctrl = telnet.getController();
        if (!ctrl.isFormatted()) return;
        int address = ctrl.getCursorAddress();
        int faIndex = ctrl.getFieldAttribute(address);
        byte fa     = ctrl.getFakeFA();
        if (faIndex != -1) fa = ctrl.getScreenBuffer()[faIndex];
        if (faIndex == (ctrl.getScreenBuffer()[address] & 0xFF) || FieldAttribute.isProtected(fa)) return;
        int lastNonBlank = -1;
        address = faIndex;
        while (true) {
            address = ctrl.incrementAddress(address);
            byte c = ctrl.getScreenBuffer()[address];
            if (FieldAttribute.isFA(c)) break;
            if (c != CG_NULL && c != CG_SPACE) lastNonBlank = address;
        }
        if (lastNonBlank == -1) {
            address = ctrl.incrementAddress(faIndex);
        } else {
            address = ctrl.incrementAddress(lastNonBlank);
            if (FieldAttribute.isFA(ctrl.getScreenBuffer()[address])) address = lastNonBlank;
        }
        ctrl.setCursorAddress(address);
    }

    void fieldExitAction(Object[] args) {
        if (telnet.isAnsi()) { telnet.sendChar('\n'); return; }
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> fieldExitAction(args)); return; }
        Controller ctrl = telnet.getController();
        int address = ctrl.getCursorAddress();
        int faIndex = ctrl.getFieldAttribute(address);
        byte fa     = ctrl.getFakeFA();
        if (faIndex != -1) fa = ctrl.getScreenBuffer()[faIndex];
        if (FieldAttribute.isProtected(fa) || FieldAttribute.isFA(ctrl.getScreenBuffer()[address])) {
            handleOperatorError(address, KeyboardConstants.ErrorProtected); return;
        }
        if (ctrl.isFormatted()) {
            do {
                ctrl.addCharacter(address, CG_NULL, (byte) 0);
                address = ctrl.incrementAddress(address);
            } while (!FieldAttribute.isFA(ctrl.getScreenBuffer()[address]));
            ctrl.setMDT(ctrl.getScreenBuffer(), faIndex);
            ctrl.setCursorAddress(ctrl.getNextUnprotectedField(ctrl.getCursorAddress()));
        } else {
            do {
                ctrl.addCharacter(address, CG_NULL, (byte) 0);
                address = ctrl.incrementAddress(address);
            } while (address != 0);
        }
    }

    void fieldsAction(Object[] args) {
        Controller ctrl = telnet.getController();
        int fieldpos = 0;
        int index    = 0;
        do {
            int newfield = ctrl.getNextUnprotectedField(fieldpos);
            if (newfield <= fieldpos) break;
            int end = newfield;
            while (!FieldAttribute.isFA(ctrl.getScreenBuffer()[end])) {
                end = ctrl.incrementAddress(end);
                if (end == 0) { end = ctrl.getColumnCount() * ctrl.getRowCount() - 1; break; }
            }
            action.actionOutput("data: field[" + index + "] at " + newfield + " to " + end
                    + " (x=" + ctrl.addressToColumn(newfield)
                    + ", y=" + ctrl.addressToRow(newfield)
                    + ", len=" + (end - newfield + 1) + ")\n");
            index++;
            fieldpos = newfield;
        } while (true);
    }

    void fieldGetAction(Object[] args) {
        int fieldnumber = (int) args[0];
        Controller ctrl = telnet.getController();
        if (!ctrl.isFormatted()) {
            telnet.getEvents().showError("FieldGet: Screen is not formatted"); return;
        }
        int fieldpos = 0;
        int index    = 0;
        do {
            int newfield = ctrl.getNextUnprotectedField(fieldpos);
            if (newfield <= fieldpos) break;
            if (fieldnumber == index) {
                int faIndex = ctrl.getFieldAttribute(newfield);
                byte fa     = ctrl.getFakeFA();
                if (faIndex != -1) fa = ctrl.getScreenBuffer()[faIndex];
                int start   = ctrl.incrementAddress(faIndex);
                int address = start;
                int length  = 0;
                do {
                    if (FieldAttribute.isFA(ctrl.getScreenBuffer()[address])) break;
                    length++;
                    address = ctrl.incrementAddress(address);
                } while (address != start);
                ctrl.dumpRange(start, length, true, ctrl.getScreenBuffer(),
                        ctrl.getRowCount(), ctrl.getColumnCount());
                return;
            }
            index++;
            fieldpos = newfield;
        } while (true);
        telnet.getEvents().showError("FieldGet: Field %d not found", fieldnumber);
    }

    void fieldMarkAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> fieldMarkAction(args)); return; }
        if (telnet.isAnsi()) return;
        handleOrdinaryCharacter(CG_FM & 0xFF, false, false);
    }

    void fieldSetAction(Object[] args) {
        int fieldnumber = (int) args[0];
        String fielddata = (String) args[1];
        Controller ctrl = telnet.getController();
        if (!ctrl.isFormatted()) {
            telnet.getEvents().showError("FieldSet: Screen is not formatted"); return;
        }
        int fieldpos = 0;
        int index    = 0;
        do {
            int newfield = ctrl.getNextUnprotectedField(fieldpos);
            if (newfield <= fieldpos) break;
            if (fieldnumber == index) {
                ctrl.setCursorAddress(newfield);
                deleteFieldAction(null);
                psSet(fielddata, false);
                return;
            }
            index++;
            fieldpos = newfield;
        } while (true);
        telnet.getEvents().showError("FieldSet: Field %d not found", fieldnumber);
    }

    void flipAction(Object[] args) {
        // screen_flip() — display flip not yet implemented
    }

    void hexStringAction(Object[] args) {
        if (args == null) return;
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            String t = (String) arg;
            if (t.length() > 2 && (t.startsWith("0x") || t.startsWith("0X")))
                t = t.substring(2);
            sb.append(t);
        }
        if (sb.length() == 0) return;
        psSet(sb.toString(), true);
    }

    void homeAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> homeAction(args)); return; }
        if (telnet.isAnsi()) { telnet.getAnsi().ansi_send_home(); return; }
        Controller ctrl = telnet.getController();
        if (!ctrl.isFormatted()) { ctrl.setCursorAddress(0); return; }
        ctrl.setCursorAddress(ctrl.getNextUnprotectedField(
                ctrl.getRowCount() * ctrl.getColumnCount() - 1));
    }

    void insertAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> insertAction(args)); return; }
        if (telnet.isAnsi()) return;
        insertMode = true;
    }

    void interruptAction(Object[] args) {
        if (telnet.is3270()) telnet.interrupt();
    }

    void sendKeyAction(Object[] args) {
        if (args == null) return;
        for (Object arg : args) {
            String s = (String) arg;
            if (s == null || s.isEmpty()) continue;
            // TODO: full StringToKeySymbol not implemented; basic hex literal handling only
            try {
                int k = Integer.parseInt(s, 16);
                if ((k & ~0xff) != 0) {
                    telnet.getEvents().showError("SendKey action: Invalid KeySym: " + s);
                    continue;
                }
                handleAsciiCharacter((byte) (k & 0xff), KeyType.STANDARD, EIAction.KEY);
            } catch (NumberFormatException e) {
                telnet.getEvents().showError("SendKey action: Nonexistent or invalid KeySym: " + s);
            }
        }
    }

    void leftAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> leftAction(args)); return; }
        if (telnet.isAnsi()) { telnet.getAnsi().ansi_send_left(); return; }
        if (!flipped) {
            moveLeft();
        } else {
            Controller ctrl = telnet.getController();
            ctrl.setCursorAddress(ctrl.incrementAddress(ctrl.getCursorAddress()));
        }
    }

    void moveCursorLeft2Positions(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> moveCursorLeft2Positions(args)); return; }
        if (telnet.isAnsi()) return;
        Controller ctrl = telnet.getController();
        int address = ctrl.decrementAddress(ctrl.decrementAddress(ctrl.getCursorAddress()));
        ctrl.setCursorAddress(address);
    }

    void monoCaseAction(Object[] args) {
        telnet.getAppres().toggleTheValue(Appres.MonoCase);
    }

    void moveCursorAction(Object[] args) {
        if (keyboardLock != 0) {
            if (args != null && args.length == 2) enqueueTypeAheadAction(() -> moveCursorAction(args));
            return;
        }
        if (args != null && args.length == 2) {
            Controller ctrl = telnet.getController();
            int row = (int) args[0];
            int col = (int) args[1];
            if (!telnet.is3270()) { row--; col--; }
            if (row < 0) row = 0;
            if (col < 0) col = 0;
            int address = ((row * ctrl.getColumnCount()) + col)
                          % (ctrl.getRowCount() * ctrl.getColumnCount());
            ctrl.setCursorAddress(address);
        } else {
            telnet.getEvents().showError("MoveCursor_action requires 2 arguments");
        }
    }

    void moveCursorToNewLine(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> moveCursorToNewLine(args)); return; }
        if (telnet.isAnsi()) { telnet.sendChar('\n'); return; }
        Controller ctrl = telnet.getController();
        int address = (ctrl.getCursorAddress() + ctrl.getColumnCount())
                      % (ctrl.getColumnCount() * ctrl.getRowCount());
        address = (address / ctrl.getColumnCount()) * ctrl.getColumnCount();
        int faIndex = ctrl.getFieldAttribute(address);
        byte fa     = ctrl.getFakeFA();
        if (faIndex != -1) fa = ctrl.getScreenBuffer()[faIndex];
        if (faIndex != address && !FieldAttribute.isProtected(fa)) {
            ctrl.setCursorAddress(address);
        } else {
            ctrl.setCursorAddress(ctrl.getNextUnprotectedField(address));
        }
    }

    void moveCursorToNextUnprotectedWord(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> moveCursorToNextUnprotectedWord(args)); return; }
        if (telnet.isAnsi()) return;
        Controller ctrl = telnet.getController();
        if (!ctrl.isFormatted()) return;

        if (FieldAttribute.isFA(ctrl.getScreenBuffer()[ctrl.getCursorAddress()])
                || FieldAttribute.isProtectedAt(ctrl.getScreenBuffer(), ctrl.getCursorAddress())) {
            int address = findNextUnprotectedWord(ctrl.getCursorAddress());
            if (address != -1) ctrl.setCursorAddress(address);
            return;
        }

        int address = findNextWordInField(ctrl.getCursorAddress());
        if (address != -1) { ctrl.setCursorAddress(address); return; }

        byte c = ctrl.getScreenBuffer()[ctrl.getCursorAddress()];
        if (c != CG_SPACE && c != CG_NULL) {
            address = ctrl.getCursorAddress();
            do {
                c = ctrl.getScreenBuffer()[address];
                if (c == CG_SPACE || c == CG_NULL) { ctrl.setCursorAddress(address); return; }
                else if (FieldAttribute.isFA(c)) {
                    address = findNextUnprotectedWord(address);
                    if (address != -1) ctrl.setCursorAddress(address);
                    return;
                }
                address = ctrl.incrementAddress(address);
            } while (address != ctrl.getCursorAddress());
        } else {
            address = findNextUnprotectedWord(ctrl.getCursorAddress());
            if (address != -1) ctrl.setCursorAddress(address);
        }
    }

    void paAction(Object[] args) {
        int k = (int) args[0];
        if (k < 1 || k > PA_SZ) {
            telnet.getEvents().showError("PA_action: Invalid argument '" + args[0] + "'"); return;
        }
        if ((keyboardLock & KeyboardConstants.OiaMinus) != 0) return;
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> paAction(args)); return; }
        handleAttentionIdentifierKey(KeyboardConstants.PaTranslation[k - 1]);
    }

    void pfAction(Object[] args) {
        int k = (int) args[0];
        if (k < 1 || k > PF_SZ) {
            telnet.getEvents().showError("PF_action: Invalid argument '" + args[0] + "'"); return;
        }
        if ((keyboardLock & KeyboardConstants.OiaMinus) != 0) return;
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> pfAction(args)); return; }
        handleAttentionIdentifierKey(KeyboardConstants.PfTranslation[k - 1]);
    }

    void previousWordAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> previousWordAction(args)); return; }
        if (telnet.isAnsi()) return;
        Controller ctrl = telnet.getController();
        if (!ctrl.isFormatted()) return;
        int address = ctrl.getCursorAddress();
        boolean prot = FieldAttribute.isProtectedAt(ctrl.getScreenBuffer(), address);

        if (!prot) {
            byte c = ctrl.getScreenBuffer()[address];
            while (!FieldAttribute.isFA(c) && c != CG_SPACE && c != CG_NULL) {
                address = ctrl.decrementAddress(address);
                if (address == ctrl.getCursorAddress()) return;
                c = ctrl.getScreenBuffer()[address];
            }
        }
        int address0 = address;
        do {
            byte c = ctrl.getScreenBuffer()[address];
            if (FieldAttribute.isFA(c)) {
                address = ctrl.decrementAddress(address);
                prot    = FieldAttribute.isProtectedAt(ctrl.getScreenBuffer(), address);
                continue;
            }
            if (!prot && c != CG_SPACE && c != CG_NULL) break;
            address = ctrl.decrementAddress(address);
        } while (address != address0);
        if (address == address0) return;

        // Scan back to front of word
        while (true) {
            address = ctrl.decrementAddress(address);
            byte c = ctrl.getScreenBuffer()[address];
            if (FieldAttribute.isFA(c) || c == CG_SPACE || c == CG_NULL) break;
        }
        address = ctrl.incrementAddress(address);
        ctrl.setCursorAddress(address);
    }

    void resetAction(Object[] args) {
        resetKeyboardLock(true);
    }

    void moveRight(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> moveRight(args)); return; }
        if (telnet.isAnsi()) { telnet.getAnsi().ansi_send_right(); return; }
        if (!flipped) {
            Controller ctrl = telnet.getController();
            ctrl.setCursorAddress(ctrl.incrementAddress(ctrl.getCursorAddress()));
        } else {
            moveLeft();
        }
    }

    void moveCursorRight2Positions(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> moveCursorRight2Positions(args)); return; }
        if (telnet.isAnsi()) return;
        Controller ctrl = telnet.getController();
        int address = ctrl.incrementAddress(ctrl.incrementAddress(ctrl.getCursorAddress()));
        ctrl.setCursorAddress(address);
    }

    void sendStringAction(Object[] args) {
        if (args == null) return;
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) sb.append(arg);
        psSet(sb.toString(), false);
        boolean ok = !telnet.getEvents().isError();
        if (!ok && telnet.getConfig().isThrowExceptionOnLockedScreen())
            throw new RuntimeException(telnet.getEvents().getErrorAsText());
    }

    void systemRequestAction(Object[] args) {
        if (telnet.isAnsi()) return;
        if (telnet.isE()) {
            telnet.abort();
        } else {
            if ((keyboardLock & KeyboardConstants.OiaMinus) != 0) return;
            if (keyboardLock != 0) { enqueueTypeAheadAction(() -> systemRequestAction(args)); return; }
            handleAttentionIdentifierKey(AID.SYS_REQ);
        }
    }

    void tabForwardAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> tabForwardAction(args)); return; }
        if (telnet.isAnsi()) { telnet.sendChar('\t'); return; }
        telnet.getController().setCursorAddress(
                telnet.getController().getNextUnprotectedField(
                        telnet.getController().getCursorAddress()));
    }

    void toggleInsertAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> toggleInsertAction(args)); return; }
        if (telnet.isAnsi()) return;
        insertMode = !insertMode;
    }

    void toggleReverseAction(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> toggleReverseAction(args)); return; }
        if (telnet.isAnsi()) return;
        reverseMode = !reverseMode;
    }

    void moveCursorUp(Object[] args) {
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> moveCursorUp(args)); return; }
        if (telnet.isAnsi()) { telnet.getAnsi().ansi_send_up(); return; }
        Controller ctrl = telnet.getController();
        int address = ctrl.getCursorAddress() - ctrl.getColumnCount();
        if (address < 0)
            address = (ctrl.getCursorAddress() + (ctrl.getRowCount() * ctrl.getColumnCount()))
                      - ctrl.getColumnCount();
        ctrl.setCursorAddress(address);
    }

    void waitAction(Object[] args) {
        // TODO: port from C# — Wait action
    }

    // ------------------------------------------------------------------ //
    // Required-signature aliases (different naming from Actions.java)
    // ------------------------------------------------------------------ //
    void nextWordAction(Object[] args)    { moveCursorToNextUnprotectedWord(args); }
    void sysReqAction(Object[] args)      { systemRequestAction(args); }
    void rightAction(Object[] args)       { moveRight(args); }
    void tabBackwardAction(Object[] args) { backTabAction(args); }

    // ------------------------------------------------------------------ //
    // Private helper methods
    // ------------------------------------------------------------------ //

    private void moveLeft() {
        Controller ctrl = telnet.getController();
        ctrl.setCursorAddress(ctrl.decrementAddress(ctrl.getCursorAddress()));
    }

    private boolean deleteCharacter() {
        Controller ctrl = telnet.getController();
        int address  = ctrl.getCursorAddress();
        int faIndex  = ctrl.getFieldAttribute(address);
        byte fa      = ctrl.getFakeFA();
        if (faIndex != -1) {
            fa = ctrl.getScreenBuffer()[faIndex];
            if (!FieldAttribute.isProtected(fa))
                ctrl.addCharacter(address, CG_NULL, (byte) 0);
        }
        if (FieldAttribute.isProtected(fa) || FieldAttribute.isFA(ctrl.getScreenBuffer()[address])) {
            handleOperatorError(address, KeyboardConstants.ErrorProtected);
            return false;
        }
        int endAddress;
        if (ctrl.isFormatted()) {
            endAddress = address;
            do {
                endAddress = ctrl.incrementAddress(endAddress);
                if (FieldAttribute.isFA(ctrl.getScreenBuffer()[endAddress])) break;
            } while (endAddress != address);
            endAddress = ctrl.decrementAddress(endAddress);
        } else {
            if ((address % ctrl.getColumnCount()) == ctrl.getColumnCount() - 1) return true;
            endAddress = address + (ctrl.getColumnCount() - (address % ctrl.getColumnCount())) - 1;
        }
        if (endAddress > address) {
            ctrl.copyBlock(address + 1, address, endAddress - address, false);
        } else if (endAddress != address) {
            ctrl.copyBlock(address + 1, address,
                    (ctrl.getRowCount() * ctrl.getColumnCount() - 1) - address, false);
            ctrl.addCharacter(ctrl.getRowCount() * ctrl.getColumnCount() - 1,
                    ctrl.getScreenBuffer()[0], (byte) 0);
            ctrl.copyBlock(1, 0, endAddress, false);
        }
        ctrl.addCharacter(endAddress, CG_NULL, (byte) 0);
        ctrl.setMDT(ctrl.getScreenBuffer(), faIndex);
        return false;
    }

    private void lightPenSelect(int address) {
        Controller ctrl = telnet.getController();
        int faIndex     = ctrl.getFieldAttribute(address);
        byte fa         = ctrl.getFakeFA();
        if (faIndex != -1) fa = ctrl.getScreenBuffer()[faIndex];
        if (!FieldAttribute.isSelectable(fa)) return;
        byte sel      = ctrl.getScreenBuffer()[faIndex + 1];
        int designator = faIndex + 1;
        if (sel == CG_GREATER_THAN) {
            ctrl.addCharacter(designator, CG_QUESTION_MARK, (byte) 0);
            ctrl.mdtClear(ctrl.getScreenBuffer(), faIndex);
        } else if (sel == CG_QUESTION_MARK) {
            ctrl.addCharacter(designator, CG_GREATER_THAN, (byte) 0);
            ctrl.mdtClear(ctrl.getScreenBuffer(), faIndex);
        } else if (sel == CG_SPACE || sel == CG_NULL) {
            handleAttentionIdentifierKey(AID.SELECT);
        } else if (sel == CG_AMPERSAND) {
            ctrl.setMDT(ctrl.getScreenBuffer(), faIndex);
            handleAttentionIdentifierKey(AID.ENTER);
        }
    }

    private int findNextUnprotectedWord(int baseAddress) {
        Controller ctrl = telnet.getController();
        int address0    = baseAddress;
        boolean prot    = FieldAttribute.isProtectedAt(ctrl.getScreenBuffer(), baseAddress);
        do {
            byte c = ctrl.getScreenBuffer()[baseAddress];
            if (FieldAttribute.isFA(c))
                prot = FieldAttribute.isProtected(c);
            else if (!prot && c != CG_SPACE && c != CG_NULL)
                return baseAddress;
            baseAddress = ctrl.incrementAddress(baseAddress);
        } while (baseAddress != address0);
        return -1;
    }

    private int findNextWordInField(int baseAddress) {
        Controller ctrl = telnet.getController();
        int address0    = baseAddress;
        boolean inWord  = true;
        do {
            byte c = ctrl.getScreenBuffer()[baseAddress];
            if (FieldAttribute.isFA(c)) return -1;
            if (inWord) {
                if (c == CG_SPACE || c == CG_NULL) inWord = false;
            } else {
                if (c != CG_SPACE && c != CG_NULL) return baseAddress;
            }
            baseAddress = ctrl.incrementAddress(baseAddress);
        } while (baseAddress != address0);
        return -1;
    }

    private void doPA(int n) {
        if (n < 1 || n > PA_SZ) { telnet.getEvents().showError("Unknown PA key %d", n); return; }
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> paAction(new Object[]{n})); return; }
        handleAttentionIdentifierKey(KeyboardConstants.PaTranslation[n - 1]);
    }

    private void doFunctionKey(int n) {
        if (n < 1 || n > PF_SZ) { telnet.getEvents().showError("Unknown PF key %d", n); return; }
        if (keyboardLock != 0) { enqueueTypeAheadAction(() -> pfAction(new Object[]{n})); return; }
        handleAttentionIdentifierKey(KeyboardConstants.PfTranslation[n - 1]);
    }

    private boolean remarginCursor(int lMargin) {
        Controller ctrl = telnet.getController();
        boolean ever    = false;
        int address     = ctrl.getCursorAddress();
        int b0          = 0;
        while (ctrl.addressToColumn(address) < lMargin) {
            address = ctrl.rowColumnToByteAddress(ctrl.addressToRow(address), lMargin);
            if (!ever) { b0 = address; ever = true; }
            int faIndex = ctrl.getFieldAttribute(address);
            byte fa     = ctrl.getFakeFA();
            if (faIndex != -1) fa = ctrl.getScreenBuffer()[faIndex];
            if (faIndex == address || FieldAttribute.isProtected(fa)) {
                address = ctrl.getNextUnprotectedField(address);
                if (address <= b0) return false;
            }
        }
        ctrl.setCursorAddress(address);
        return true;
    }

    // ------------------------------------------------------------------ //
    // EmulateInput — main string-input state machine
    // ------------------------------------------------------------------ //

    int emulateInput(String s, boolean pasting) {
        EIState  state   = EIState.BASE;
        int      literal = 0;
        int      nc      = 0;
        EIAction ia      = pasting ? EIAction.PASTE : EIAction.STRING;
        int originalAddress = telnet.getController().getCursorAddress();
        int originalColumn  = telnet.getController().addressToColumn(originalAddress);
        int length          = s.length();

        while (s.length() > 0) {
            if (keyboardLock != 0) {
                trace.trace_event("  keyboard locked, string dropped. kybdlock=%d\n", keyboardLock);
                if (telnet.getConfig().isThrowExceptionOnLockedScreen())
                    throw new RuntimeException(
                            "Keyboard locked typing data onto screen.");
                return 0;
            }

            if (pasting && telnet.is3270()) {
                if (telnet.getController().getCursorAddress() < originalAddress) return length - 1;
                if (telnet.getAppres().toggled(Appres.MarginedPaste)
                        && telnet.getController().addressToColumn(
                               telnet.getController().getCursorAddress()) < originalColumn) {
                    if (!remarginCursor(originalColumn)) return length - 1;
                }
            }

            char c = s.charAt(0);

            switch (state) {
                case BASE:
                    switch (c) {
                        case '\b': leftAction(null); break;
                        case '\f':
                            if (pasting) handleAsciiCharacter((byte) ' ', KeyType.STANDARD, ia);
                            else {
                                clearAction(null);
                                if (telnet.is3270()) return length - 1;
                            }
                            break;
                        case '\n':
                            if (pasting) moveCursorToNewLine(null);
                            else { enterAction(null); if (telnet.is3270()) return length - 1; }
                            break;
                        case '\r': break;
                        case '\t': tabForwardAction(null); break;
                        case '\\':
                            if (!pasting) { state = EIState.BACKSLASH; }
                            else handleAsciiCharacter((byte) c, KeyType.STANDARD, ia);
                            break;
                        case (char) 0x1b:
                            if (pasting) state = EIState.XGE;
                            break;
                        default:
                            handleAsciiCharacter((byte) c, KeyType.STANDARD, ia);
                            break;
                    }
                    break;

                case BACKSLASH:
                    switch (c) {
                        case 'a':
                            telnet.getEvents().showError("String_action: Bell not supported");
                            state = EIState.BASE; break;
                        case 'b': leftAction(null); state = EIState.BASE; break;
                        case 'f':
                            clearAction(null); state = EIState.BASE;
                            if (telnet.is3270()) return length - 1;
                            break;
                        case 'n':
                            enterAction(null); state = EIState.BASE;
                            if (telnet.is3270()) return length - 1;
                            break;
                        case 'p': state = EIState.BACK_P; break;
                        case 'r': moveCursorToNewLine(null); state = EIState.BASE; break;
                        case 't': tabForwardAction(null); state = EIState.BASE; break;
                        case 'T': backTabAction(null); state = EIState.BASE; break;
                        case 'v':
                            telnet.getEvents().showError("String_action: Vertical tab not supported");
                            state = EIState.BASE; break;
                        case 'x': state = EIState.BACK_X; break;
                        case '\\':
                            handleAsciiCharacter((byte) c, KeyType.STANDARD, ia);
                            state = EIState.BASE; break;
                        case '0': case '1': case '2': case '3':
                        case '4': case '5': case '6': case '7':
                            state = EIState.OCTAL; literal = 0; nc = 0;
                            s = s.substring(1); length--;
                            continue;
                        default:
                            state = EIState.BASE;
                            s = s.substring(1); length--;
                            continue;
                    }
                    break;

                case BACK_P:
                    switch (c) {
                        case 'a': literal = 0; nc = 0; state = EIState.BACK_PA; break;
                        case 'f': literal = 0; nc = 0; state = EIState.BACK_PF; break;
                        default:
                            telnet.getEvents().showError("StringAction: Unknown character after \\p");
                            state = EIState.BASE; break;
                    }
                    break;

                case BACK_PF:
                    if (nc < 2 && isDigit(c)) {
                        literal = (literal * 10) + (c - '0'); nc++;
                    } else if (nc == 0) {
                        telnet.getEvents().showError("StringAction: Unknown character after \\pf");
                        state = EIState.BASE;
                    } else {
                        doFunctionKey(literal);
                        if (telnet.is3270()) return length - 1;
                        state = EIState.BASE;
                        s = s.substring(1); length--;
                        continue;
                    }
                    break;

                case BACK_PA:
                    if (nc < 1 && isDigit(c)) {
                        literal = (literal * 10) + (c - '0'); nc++;
                    } else if (nc == 0) {
                        telnet.getEvents().showError("String_action: Unknown character after \\pa");
                        state = EIState.BASE;
                    } else {
                        doPA(literal);
                        if (telnet.is3270()) return length - 1;
                        state = EIState.BASE;
                        s = s.substring(1); length--;
                        continue;
                    }
                    break;

                case BACK_X:
                    if (isXDigit(c)) {
                        state = EIState.HEX; literal = 0; nc = 0;
                        s = s.substring(1); length--;
                        continue;
                    } else {
                        telnet.getEvents().showError("String_action: Missing hex digits after \\x");
                        state = EIState.BASE;
                        s = s.substring(1); length--;
                        continue;
                    }

                case OCTAL:
                    if (nc < 3 && isDigit(c) && c < '8') {
                        literal = (literal * 8) + fromHex(c); nc++;
                    } else {
                        handleAsciiCharacter((byte) literal, KeyType.STANDARD, ia);
                        state = EIState.BASE;
                        s = s.substring(1); length--;
                        continue;
                    }
                    break;

                case HEX:
                    if (nc < 2 && isXDigit(c)) {
                        literal = (literal * 16) + fromHex(c); nc++;
                    } else {
                        handleAsciiCharacter((byte) literal, KeyType.STANDARD, ia);
                        state = EIState.BASE;
                        s = s.substring(1); length--;
                        continue;
                    }
                    break;

                case XGE:
                    switch (c) {
                        case ';': handleOrdinaryCharacter(CG_FM  & 0xFF, false, true); break;
                        case '*': handleOrdinaryCharacter(CG_DUP & 0xFF, false, true); break;
                        default:  handleAsciiCharacter((byte) c, KeyType.GE, ia);      break;
                    }
                    state = EIState.BASE;
                    break;
            }

            s = s.substring(1);
            length--;
        }

        switch (state) {
            case OCTAL: case HEX:
                handleAsciiCharacter((byte) literal, KeyType.STANDARD, ia);
                break;
            case BACK_PF:
                if (nc > 0) doFunctionKey(literal);
                break;
            case BACK_PA:
                if (nc > 0) doPA(literal);
                break;
            default:
                break;
        }
        if (state != EIState.BASE)
            telnet.getEvents().showError("String_action: Missing data after \\");

        return length;
    }

    // ------------------------------------------------------------------ //
    // HexInput — send raw hex bytes into the session
    // ------------------------------------------------------------------ //

    private void hexInput(String s) {
        boolean escaped  = false;
        int byteCount    = 0;
        int index        = 0;

        if ((s.length() % 2) != 0) {
            telnet.getEvents().showError("HexStringAction: Odd number of characters in specification");
            return;
        }

        while (index < s.length()) {
            if (isXDigit(s.charAt(index)) && isXDigit(s.charAt(index + 1))) {
                escaped = false; byteCount++;
            } else if (s.substring(index, index + 2).equalsIgnoreCase("\\e")) {
                if (escaped) { telnet.getEvents().showError("HexString_action: Double \\E"); return; }
                if (!telnet.is3270()) { telnet.getEvents().showError("HexString_action: \\E in ANSI mode"); return; }
                escaped = true;
            } else {
                telnet.getEvents().showError("HexString_action: Illegal character in specification");
                return;
            }
            index += 2;
        }
        if (escaped) { telnet.getEvents().showError("HexString_action: Nothing follows \\E"); return; }

        byte[] xBuffer  = null;
        int bufferIndex = 0;
        if (!telnet.is3270() && byteCount != 0) xBuffer = new byte[byteCount];

        index   = 0;
        escaped = false;
        while (index < s.length()) {
            if (isXDigit(s.charAt(index)) && isXDigit(s.charAt(index + 1))) {
                byte bv = (byte) ((fromHex(s.charAt(index)) * 16) + fromHex(s.charAt(index + 1)));
                if (telnet.is3270()) {
                    handleOrdinaryCharacter(Tables.EBC_2_CG[bv & 0xFF] & 0xFF, escaped, true);
                } else {
                    if (xBuffer != null) xBuffer[bufferIndex++] = bv;
                }
                escaped = false;
            } else if (s.substring(index, index + 2).equalsIgnoreCase("\\e")) {
                escaped = true;
            }
            index += 2;
        }
        if (!telnet.is3270() && byteCount != 0 && xBuffer != null)
            telnet.sendHexAnsiOut(xBuffer, byteCount);
    }
}
