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

import com.open3270client.tn3270e.CursorOp;
import com.open3270client.tn3270e.PDS;
import com.open3270client.tn3270e.PreviousEnum;
import com.open3270client.tn3270e.SmsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

class Controller implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    // ------------------------------------------------------------------ //
    // CharacterGenerator substitutes (from CharacterGenerator.cs)
    // ------------------------------------------------------------------ //
    static final byte CG_Null      = 0x00;
    static final byte CG_Space     = 0x40;
    static final byte CG_Asterisk  = (byte) 0xbf; // '*' in CG encoding
    static final byte CG_Semicolon = (byte) 0xbe; // ';' in CG encoding

    // ------------------------------------------------------------------ //
    // ExtendedAttribute inner class
    // ------------------------------------------------------------------ //
    static final class ExtendedAttribute {
        static final int CS_MASK = 0x03;
        byte fg = 0;
        byte bg = 0;
        byte gr = 0;
        byte cs = 0;

        boolean isZero() { return fg == 0 && bg == 0 && gr == 0 && cs == 0; }

        void clear() { fg = 0; bg = 0; gr = 0; cs = 0; }
    }

    // ------------------------------------------------------------------ //
    // Fields
    // ------------------------------------------------------------------ //
    private byte[] screenBuffer;
    private byte[] crmAttributes;
    private byte[] aScreenBuffer = null;

    private byte attentionID = AID.NONE;
    private byte defaultCs   = 0;
    private byte defaultFg   = 0;
    private byte defaultGr   = 0;
    private byte replyMode   = 0;
    private byte fakeFA      = 0;

    private boolean is3270        = false;
    private boolean isAltBuffer   = false;
    private boolean screenAlt     = true;
    private boolean isFormatted   = false;
    private boolean tracePrimed   = false;
    private boolean traceSkipping = false;
    private boolean screenChanged = false;
    private boolean debuggingFont = false;

    private int cursorAddress    = 0;
    private int bufferAddress    = 0;
    private int currentFaIndex   = 0;
    private int maxColumns       = 80;
    private int maxRows          = 25;
    private int modelNumber;
    private int crmnAttribute;
    private int rowCount         = 25;
    private int columnCount      = 80;
    private int sscpStart        = 0;
    private int firstChanged     = 0;
    private int lastChanged      = 0;
    private int dataAvailableCount = 0;
    private long startTime       = 0;

    private String modelName = null;

    private ExtendedAttribute[] extendedAttributes;
    private ExtendedAttribute[] aExtendedAttributeBuffer    = null;
    private ExtendedAttribute[] extendedAttributesZeroBuffer = null;

    private final Telnet        telnet;
    private final TNTrace       trace;
    private final Appres        appres;
    private final StructuredField sf;

    private PreviousEnum previous = PreviousEnum.NONE;

    private final Object dataAvailablePadlock = new Object();

    // Cursor-location-changed listeners (replaces C# event EventHandler CursorLocationChanged)
    private final List<Runnable> cursorLocationListeners = new ArrayList<>();

    // Telnet listener references (kept for unregistration in close())
    private Runnable            connectionPendingListener;
    private Consumer<Boolean>   primaryConnectionChangedListener;
    private Consumer<Boolean>   connected3270Listener;

    // ------------------------------------------------------------------ //
    // Properties (getters / setters)
    // ------------------------------------------------------------------ //

    boolean isFormatted()                 { return isFormatted; }
    void    setFormatted(boolean v)       { isFormatted = v; }

    byte[]  getCrmAttributes()            { return crmAttributes; }
    void    setCrmAttributes(byte[] v)    { crmAttributes = v; }

    int     getCrmnAttribute()            { return crmnAttribute; }
    void    setCrmnAttribute(int v)       { crmnAttribute = v; }

    byte    getFakeFA()                   { return fakeFA; }
    void    setFakeFA(byte v)             { fakeFA = v; }

    int     getRowCount()                 { return rowCount; }
    void    setRowCount(int v)            { rowCount = v; }

    int     getColumnCount()              { return columnCount; }
    void    setColumnCount(int v)         { log.debug("SET COLUMN COUNT={}", v); columnCount = v; }

    boolean getIs3270()                   { return is3270; }
    void    setIs3270(boolean v)          { is3270 = v; }

    int     getMaxColumns()               { return maxColumns; }
    void    setMaxColumns(int v)          { maxColumns = v; }

    int     getMaxRows()                  { return maxRows; }
    void    setMaxRows(int v)             { maxRows = v; }

    byte    getAttentionID()              { return attentionID; }
    void    setAttentionID(byte v)        { attentionID = v; }

    int     getBufferAddress()            { return bufferAddress; }
    void    setBufferAddress(int v)       { bufferAddress = v; }

    int     getCursorAddress()            { return cursorAddress; }

    byte[]  getScreenBuffer()             { return screenBuffer; }
    void    setScreenBuffer(byte[] v)     { screenBuffer = v; }

    boolean getIsAltBuffer()              { return isAltBuffer; }
    void    setIsAltBuffer(boolean v)     { isAltBuffer = v; }

    boolean isScreenAlt()                 { return screenAlt; }
    void    setScreenAlt(boolean v)       { screenAlt = v; }

    boolean isScreenChanged()             { return screenChanged; }

    int getDataAvailableCount() {
        synchronized (dataAvailablePadlock) {
            return dataAvailableCount;
        }
    }

    // ------------------------------------------------------------------ //
    // Constructor
    // ------------------------------------------------------------------ //
    Controller(Telnet tn, Appres appres) {
        this.sf          = new StructuredField(tn);
        this.crmAttributes = new byte[16];
        this.crmnAttribute = 0;
        this.telnet      = tn;
        this.trace       = tn.getTrace();
        this.appres      = appres;
        this.startTime   = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------ //
    // AutoCloseable
    // ------------------------------------------------------------------ //
    @Override
    public void close() {
        if (extendedAttributes != null) {
            for (int i = 0; i < extendedAttributes.length; i++) extendedAttributes[i] = null;
        }
        if (aExtendedAttributeBuffer != null) {
            for (int i = 0; i < aExtendedAttributeBuffer.length; i++) aExtendedAttributeBuffer[i] = null;
        }
        if (extendedAttributesZeroBuffer != null) {
            for (int i = 0; i < extendedAttributesZeroBuffer.length; i++) extendedAttributesZeroBuffer[i] = null;
        }
        if (connectionPendingListener != null) {
            telnet.removeConnectionPendingListener(connectionPendingListener);
        }
        if (primaryConnectionChangedListener != null) {
            telnet.removePrimaryConnectionChangedListener(primaryConnectionChangedListener);
        }
        if (connected3270Listener != null) {
            telnet.removeConnected3270Listener(connected3270Listener);
        }
    }

    // ------------------------------------------------------------------ //
    // Cursor-location listener helpers
    // ------------------------------------------------------------------ //
    void addCursorLocationListener(Runnable listener) {
        cursorLocationListeners.add(listener);
    }

    void removeCursorLocationListener(Runnable listener) {
        cursorLocationListeners.remove(listener);
    }

    private void fireCursorLocationChanged() {
        for (Runnable r : cursorLocationListeners) r.run();
    }

    // ------------------------------------------------------------------ //
    // Telnet connection event handlers
    // ------------------------------------------------------------------ //
    private void onPrimaryConnectionChanged(boolean success) {
        reactToConnectionChange(success);
    }

    private void onConnectionPending() {
        // Not doing anything here, yet.
    }

    private void onConnected3270(boolean is3270conn) {
        reactToConnectionChange(is3270conn);
    }

    // ------------------------------------------------------------------ //
    // Screen-change notification helpers
    // ------------------------------------------------------------------ //
    private void onAllChanged() {
        screenChanged = true;
        if (telnet.isAnsi()) {
            firstChanged = 0;
            lastChanged  = rowCount * columnCount;
        }
    }

    private void onRegionChanged(int f, int l) {
        screenChanged = true;
        if (telnet.isAnsi()) {
            if (firstChanged == -1 || f < firstChanged) firstChanged = f;
            if (lastChanged  == -1 || l > lastChanged)  lastChanged  = l;
        }
    }

    private void onOneChanged(int n) {
        onRegionChanged(n, n + 1);
    }

    // ------------------------------------------------------------------ //
    // Initialize / reactToConnectionChange
    // ------------------------------------------------------------------ //
    void initialize(int cmask) {
        connectionPendingListener        = this::onConnectionPending;
        primaryConnectionChangedListener = this::onPrimaryConnectionChanged;
        connected3270Listener            = this::onConnected3270;
        telnet.addConnectionPendingListener(connectionPendingListener);
        telnet.addPrimaryConnectionChangedListener(primaryConnectionChangedListener);
        telnet.addConnected3270Listener(connected3270Listener);
    }

    private void reactToConnectionChange(boolean success) {
        if (is3270) {
            fakeFA = (byte) 0xE0;
        } else {
            fakeFA = (byte) 0xC4;
        }
        if (!telnet.is3270() || (telnet.isSscp()
                && ((telnet.getKeyboard().getKeyboardLock() & KeyboardConstants.OiaTWait) != 0))) {
            telnet.getKeyboard().keyboardLockClear(KeyboardConstants.OiaTWait, "ctlr_connect");
        }
        defaultFg     = 0x00;
        defaultGr     = 0x00;
        defaultCs     = 0x00;
        replyMode     = (byte) ControllerConstant.SF_SRM_FIELD;
        crmnAttribute = 0;
    }

    // ------------------------------------------------------------------ //
    // Reinitialize
    // ------------------------------------------------------------------ //
    void reinitialize(int cmask) {
        if ((cmask & ControllerConstant.MODEL_CHANGE) != 0) {
            screenBuffer               = new byte[maxRows * maxColumns];
            extendedAttributes          = new ExtendedAttribute[maxRows * maxColumns];
            aScreenBuffer               = new byte[maxRows * maxColumns];
            aExtendedAttributeBuffer    = new ExtendedAttribute[maxRows * maxColumns];
            extendedAttributesZeroBuffer = new ExtendedAttribute[maxRows * maxColumns];
            for (int i = 0; i < maxRows * maxColumns; i++) {
                extendedAttributes[i]          = new ExtendedAttribute();
                aExtendedAttributeBuffer[i]    = new ExtendedAttribute();
                extendedAttributesZeroBuffer[i] = new ExtendedAttribute();
            }
            cursorAddress = 0;
            bufferAddress = 0;
        }
    }

    // ------------------------------------------------------------------ //
    // setRowsAndColumns
    // ------------------------------------------------------------------ //
    void setRowsAndColumns(int mn, int ovc, int ovr) {
        switch (mn) {
            case 2: maxColumns = columnCount = 80;  maxRows = rowCount = 24;  modelNumber = 2; break;
            case 3: maxColumns = columnCount = 80;  maxRows = rowCount = 32;  modelNumber = 3; break;
            case 4: maxColumns = columnCount = 80;  maxRows = rowCount = 43;  modelNumber = 4; break;
            case 5: maxColumns = columnCount = 132; maxRows = rowCount = 27;  modelNumber = 5; break;
            default:
                int defmod = 4;
                telnet.getEvents().showError("Unknown model: %d\nDefaulting to %d", mn, defmod);
                setRowsAndColumns(defmod, ovc, ovr);
                return;
        }
        if (ovc != 0 || ovr != 0) {
            throw new RuntimeException("oops - oversize");
        }
        modelName = "327" + (appres.m3279 ? "9" : "8") + "-" + modelNumber + (appres.extended ? "-E" : "");
        reinitialize(255);
    }

    // ------------------------------------------------------------------ //
    // Calculated properties
    // ------------------------------------------------------------------ //
    boolean isBlank(byte c) {
        return (c == CG_Null || c == CG_Space);
    }

    private boolean streamHasData() {
        for (int i = 0; i < rowCount * columnCount; i++) {
            byte oc = screenBuffer[i];
            if (!FieldAttribute.isFA(oc) && !isBlank(oc)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ //
    // setFormattedFlag
    // ------------------------------------------------------------------ //
    private void setFormattedFlag() {
        isFormatted = false;
        int baddr = 0;
        do {
            if (FieldAttribute.isFA(screenBuffer[baddr])) {
                isFormatted = true;
                break;
            }
            baddr = incrementAddress(baddr);
        } while (baddr != 0);
    }

    // ------------------------------------------------------------------ //
    // Field attribute lookup
    // ------------------------------------------------------------------ //
    int getFieldAttribute(int baddr) {
        if (!isFormatted) return -1;
        int sbaddr = baddr;
        do {
            if (FieldAttribute.isFA(screenBuffer[baddr])) return baddr;
            baddr = decrementAddress(baddr);
        } while (baddr != sbaddr);
        return -1;
    }

    boolean getBoundedFieldAttribute(int bAddr, int bound, int[] faOutIndex) {
        if (!isFormatted) {
            faOutIndex[0] = -1;
            return true;
        }
        int sbaddr = bAddr;
        do {
            if (FieldAttribute.isFA(screenBuffer[bAddr])) {
                faOutIndex[0] = bAddr;
                return true;
            }
            bAddr = decrementAddress(bAddr);
        } while (bAddr != sbaddr && bAddr != bound);
        if (bAddr == sbaddr) {
            faOutIndex[0] = -1;
            return true;
        }
        return false;
    }

    int getNextUnprotectedField(int fromAddress) {
        int baddr, nbaddr;
        nbaddr = fromAddress;
        do {
            baddr  = nbaddr;
            nbaddr = incrementAddress(nbaddr);
            if (FieldAttribute.isFA(screenBuffer[baddr])
                    && !FieldAttribute.isProtected(screenBuffer[baddr])
                    && !FieldAttribute.isFA(screenBuffer[nbaddr])) {
                return nbaddr;
            }
        } while (nbaddr != fromAddress);
        return 0;
    }

    // ------------------------------------------------------------------ //
    // erase
    // ------------------------------------------------------------------ //
    void erase(boolean alt) {
        telnet.getKeyboard().toggleEnterInhibitMode(false);
        clear(true);
        if (alt == screenAlt) return;
        if (alt) {
            rowCount    = maxRows;
            columnCount = maxColumns;
        } else {
            rowCount    = 24;
            columnCount = 80;
        }
        screenAlt = alt;
    }

    // ------------------------------------------------------------------ //
    // processDS
    // ------------------------------------------------------------------ //
    PDS processDS(byte[] buf, int start, int bufferLength) {
        if (buf.length == 0 || bufferLength == 0) return PDS.OKAY_NO_OUTPUT;
        trace.trace_ds("< ");
        int cmd = buf[start] & 0xff;
        if (cmd == ControllerConstant.CMD_EAU || cmd == ControllerConstant.SNA_CMD_EAU) {
            trace.trace_ds("EraseAllUnprotected\n");
            processEraseAllUnprotectedCommand();
            return PDS.OKAY_NO_OUTPUT;
        } else if (cmd == ControllerConstant.CMD_EWA || cmd == ControllerConstant.SNA_CMD_EWA) {
            trace.trace_ds("EraseWriteAlternate\n");
            erase(true);
            processWriteCommand(buf, start, bufferLength, true);
            return PDS.OKAY_NO_OUTPUT;
        } else if (cmd == ControllerConstant.CMD_EW || cmd == ControllerConstant.SNA_CMD_EW) {
            trace.trace_ds("EraseWrite\n");
            erase(false);
            processWriteCommand(buf, start, bufferLength, true);
            return PDS.OKAY_NO_OUTPUT;
        } else if (cmd == ControllerConstant.CMD_W || cmd == ControllerConstant.SNA_CMD_W) {
            trace.trace_ds("Write\n");
            processWriteCommand(buf, start, bufferLength, false);
            return PDS.OKAY_NO_OUTPUT;
        } else if (cmd == ControllerConstant.CMD_RB || cmd == ControllerConstant.SNA_CMD_RB) {
            trace.trace_ds("ReadBuffer\n");
            processReadBufferCommand(attentionID);
            return PDS.OKAY_OUTPUT;
        } else if (cmd == ControllerConstant.CMD_RM || cmd == ControllerConstant.SNA_CMD_RM) {
            trace.trace_ds("ReadModified\n");
            processReadModifiedCommand(attentionID, false);
            return PDS.OKAY_OUTPUT;
        } else if (cmd == ControllerConstant.CMD_RMA || cmd == ControllerConstant.SNA_CMD_RMA) {
            trace.trace_ds("ReadModifiedAll\n");
            processReadModifiedCommand(attentionID, true);
            return PDS.OKAY_OUTPUT;
        } else if (cmd == ControllerConstant.CMD_WSF || cmd == ControllerConstant.SNA_CMD_WSF) {
            trace.trace_ds("WriteStructuredField");
            return sf.writeStructuredField(buf, start, bufferLength);
        } else if (cmd == ControllerConstant.CMD_NOP) {
            trace.trace_ds("NoOp\n");
            return PDS.OKAY_NO_OUTPUT;
        } else {
            telnet.getEvents().showError("Unknown 3270 Data Stream command: 0x%X\n", buf[start]);
            return PDS.BAD_COMMAND;
        }
    }

    // ------------------------------------------------------------------ //
    // InsertSaAttributes helpers
    // ------------------------------------------------------------------ //
    private void insertSaAttributes(NetBuffer obptr, byte attr, byte vValue,
                                    byte[] currentp, boolean[] anyp) {
        if (vValue != currentp[0]) {
            currentp[0] = vValue;
            obptr.add(ControllerConstant.ORDER_SA);
            obptr.add(attr);
            obptr.add(vValue);
            if (anyp[0]) trace.trace_ds("'");
            trace.trace_ds(" SetAttribute(%s)", See.getEfa(attr, vValue));
            anyp[0] = false;
        }
    }

    private void insertSaAttributes(NetBuffer obptr, int baddr,
                                    byte[] currentFGp, byte[] currentGRp, byte[] currentCSp,
                                    boolean[] anyp) {
        if (replyMode == (byte) ControllerConstant.SF_SRM_CHAR) {
            boolean foundXAForeground   = false;
            boolean foundXAHighlighting = false;
            boolean foundXACharset      = false;
            for (int i = 0; i < crmnAttribute; i++) {
                if (crmAttributes[i] == See.XA_FOREGROUND)   foundXAForeground   = true;
                if (crmAttributes[i] == See.XA_HIGHLIGHTING) foundXAHighlighting = true;
                if (crmAttributes[i] == See.XA_CHARSET)      foundXACharset      = true;
            }
            if (foundXAForeground) {
                insertSaAttributes(obptr, See.XA_FOREGROUND,
                        extendedAttributes[baddr].fg, currentFGp, anyp);
            }
            if (foundXAHighlighting) {
                byte gr = extendedAttributes[baddr].gr;
                if (gr != 0) gr |= (byte) 0xf0;
                insertSaAttributes(obptr, See.XA_HIGHLIGHTING, gr, currentGRp, anyp);
            }
            if (foundXACharset) {
                byte cs = (byte) (extendedAttributes[baddr].cs & ExtendedAttribute.CS_MASK);
                if (cs != 0) cs |= (byte) 0xf0;
                insertSaAttributes(obptr, See.XA_CHARSET, cs, currentCSp, anyp);
            }
        }
    }

    // ------------------------------------------------------------------ //
    // processReadModifiedCommand
    // ------------------------------------------------------------------ //
    void processReadModifiedCommand(byte attentionIDbyte, boolean all) {
        int    baddr, sbaddr;
        boolean sendData  = true;
        boolean shortRead = false;
        byte[]  currentFG = {0x00};
        byte[]  currentGR = {0x00};
        byte[]  currentCS = {0x00};

        if (telnet.isSscp() && attentionIDbyte != AID.ENTER) return;

        trace.trace_ds("> ");
        NetBuffer obptr = new NetBuffer();

        rmDone: {
            switch (attentionIDbyte & 0xff) {
                case 0xf0: { // AID.SYS_REQ
                    obptr.add(0x01);
                    obptr.add(0x5b);
                    obptr.add(0x61);
                    obptr.add(0x02);
                    trace.trace_ds("SYSREQ");
                    break;
                }
                case 0x6c: // AID.PA1
                case 0x6e: // AID.PA2
                case 0x6b: // AID.PA3
                case 0x6d: { // AID.CLEAR
                    if (!all) { shortRead = true; sendData = false; }
                    if (!telnet.isSscp()) {
                        obptr.add(attentionIDbyte);
                        trace.trace_ds(See.getAidFromCode(attentionIDbyte));
                        if (shortRead) break rmDone;
                        Util.encodeBAddress(obptr, cursorAddress);
                        trace.trace_ds(trace.rcba(cursorAddress));
                    }
                    break;
                }
                case 0x7e: { // AID.SELECT
                    if (!all) sendData = false;
                    if (!telnet.isSscp()) {
                        obptr.add(attentionIDbyte);
                        trace.trace_ds(See.getAidFromCode(attentionIDbyte));
                        if (shortRead) break rmDone;
                        Util.encodeBAddress(obptr, cursorAddress);
                        trace.trace_ds(trace.rcba(cursorAddress));
                    }
                    break;
                }
                default: {
                    if (!telnet.isSscp()) {
                        obptr.add(attentionIDbyte);
                        trace.trace_ds(See.getAidFromCode(attentionIDbyte));
                        if (shortRead) break rmDone;
                        Util.encodeBAddress(obptr, cursorAddress);
                        trace.trace_ds(trace.rcba(cursorAddress));
                    }
                    break;
                }
            }

            baddr = 0;
            if (isFormatted) {
                // Find first field attribute
                do {
                    if (FieldAttribute.isFA(screenBuffer[baddr])) break;
                    baddr = incrementAddress(baddr);
                } while (baddr != 0);
                sbaddr = baddr;
                do {
                    if (FieldAttribute.isModified(screenBuffer[baddr])) {
                        boolean[] any = {false};
                        baddr = incrementAddress(baddr);
                        obptr.add(ControllerConstant.ORDER_SBA);
                        Util.encodeBAddress(obptr, baddr);
                        trace.trace_ds(" SetBufferAddress%s", trace.rcba(baddr));
                        while (!FieldAttribute.isFA(screenBuffer[baddr])) {
                            if (sendData && screenBuffer[baddr] != 0) {
                                insertSaAttributes(obptr, baddr, currentFG, currentGR, currentCS, any);
                                if ((extendedAttributes[baddr].cs & ControllerConstant.CS_GE) != 0) {
                                    obptr.add(ControllerConstant.ORDER_GE);
                                    if (any[0]) trace.trace_ds("'");
                                    trace.trace_ds(" GraphicEscape");
                                    any[0] = false;
                                }
                                obptr.add(Tables.CG_2_EBC[screenBuffer[baddr] & 0xff]);
                                if (!any[0]) trace.trace_ds(" '");
                                trace.trace_ds("%s", See.getEbc(Tables.CG_2_EBC[screenBuffer[baddr] & 0xff]));
                                any[0] = true;
                            }
                            baddr = incrementAddress(baddr);
                        }
                        if (any[0]) trace.trace_ds("'");
                    } else {
                        // Not modified - skip
                        do {
                            baddr = incrementAddress(baddr);
                        } while (!FieldAttribute.isFA(screenBuffer[baddr]));
                    }
                } while (baddr != sbaddr);
            } else {
                boolean[] any = {false};
                int nBytes = 0;
                if (telnet.isSscp()) baddr = sscpStart;
                do {
                    if (screenBuffer[baddr] != 0) {
                        insertSaAttributes(obptr, baddr, currentFG, currentGR, currentCS, any);
                        if ((extendedAttributes[baddr].cs & ControllerConstant.CS_GE) != 0) {
                            obptr.add(ControllerConstant.ORDER_GE);
                            if (any[0]) trace.trace_ds("' ");
                            trace.trace_ds(" GraphicEscape ");
                            any[0] = false;
                        }
                        obptr.add(Tables.CG_2_EBC[screenBuffer[baddr] & 0xff]);
                        if (!any[0]) trace.trace_ds("'");
                        trace.trace_ds(See.getEbc(Tables.CG_2_EBC[screenBuffer[baddr] & 0xff]));
                        any[0] = true;
                        nBytes++;
                    }
                    baddr = incrementAddress(baddr);
                    if (telnet.isSscp() && (nBytes >= 255 || baddr == 0)) break;
                } while (baddr != 0);
                if (any[0]) trace.trace_ds("'");
            }
        } // end rmDone

        trace.trace_ds("\n");
        telnet.output(obptr);
    }

    // ------------------------------------------------------------------ //
    // calculateFA
    // ------------------------------------------------------------------ //
    private byte calculateFA(byte fa) {
        byte r = 0x00;
        if (FieldAttribute.isProtected(fa)) r |= 0x20;
        if (FieldAttribute.isNumeric(fa))   r |= 0x10;
        if (FieldAttribute.isModified(fa))  r |= 0x01;
        r |= (byte) ((fa & ControllerConstant.FA_INTENSITY) << 2);
        return r;
    }

    // ------------------------------------------------------------------ //
    // processReadBufferCommand
    // ------------------------------------------------------------------ //
    void processReadBufferCommand(byte attentionIDbyte) {
        int     baddr;
        byte    fa;
        boolean[] any = {false};
        int     attr_count = 0;
        byte[]  currentFG = {0x00};
        byte[]  currentGR = {0x00};
        byte[]  currentCS = {0x00};

        trace.trace_ds("> ");
        NetBuffer obptr = new NetBuffer();
        obptr.add(attentionIDbyte);
        Util.encodeBAddress(obptr, cursorAddress);
        trace.trace_ds("%s%s", See.getAidFromCode(attentionIDbyte), trace.rcba(cursorAddress));

        baddr = 0;
        do {
            if (FieldAttribute.isFA(screenBuffer[baddr])) {
                if (replyMode == (byte) ControllerConstant.SF_SRM_FIELD) {
                    obptr.add(ControllerConstant.ORDER_SF);
                } else {
                    obptr.add(ControllerConstant.ORDER_SFE);
                    attr_count = obptr.getIndex();
                    obptr.add(1);
                    obptr.add(See.XA_3270);
                }
                fa = calculateFA(screenBuffer[baddr]);
                obptr.add(ControllerConstant.CODE_TABLE[fa & 0xff]);
                if (any[0]) trace.trace_ds("'");
                trace.trace_ds(" StartField%s%s%s",
                        (replyMode == (byte) ControllerConstant.SF_SRM_FIELD) ? "" : "Extended",
                        trace.rcba(baddr), See.getSeeAttribute(fa));
                if (replyMode != (byte) ControllerConstant.SF_SRM_FIELD) {
                    if (extendedAttributes[baddr].fg != 0) {
                        obptr.add(See.XA_FOREGROUND);
                        obptr.add(extendedAttributes[baddr].fg);
                        trace.trace_ds("%s", See.getEfa(See.XA_FOREGROUND, extendedAttributes[baddr].fg));
                        obptr.incrementAt(attr_count, 1);
                    }
                    if (extendedAttributes[baddr].gr != 0) {
                        obptr.add(See.XA_HIGHLIGHTING);
                        obptr.add((byte) (extendedAttributes[baddr].gr | 0xf0));
                        trace.trace_ds("%s", See.getEfa(See.XA_HIGHLIGHTING,
                                (byte) (extendedAttributes[baddr].gr | 0xf0)));
                        obptr.incrementAt(attr_count, 1);
                    }
                    if ((extendedAttributes[baddr].cs & ExtendedAttribute.CS_MASK) != 0) {
                        obptr.add(See.XA_CHARSET);
                        obptr.add((byte) ((extendedAttributes[baddr].cs & ExtendedAttribute.CS_MASK) | 0xf0));
                        trace.trace_ds("%s", See.getEfa(See.XA_CHARSET,
                                (byte) ((extendedAttributes[baddr].cs & ExtendedAttribute.CS_MASK) | 0xf0)));
                        obptr.incrementAt(attr_count, 1);
                    }
                }
                any[0] = false;
            } else {
                insertSaAttributes(obptr, baddr, currentFG, currentGR, currentCS, any);
                if ((extendedAttributes[baddr].cs & ControllerConstant.CS_GE) != 0) {
                    obptr.add(ControllerConstant.ORDER_GE);
                    if (any[0]) trace.trace_ds("'");
                    trace.trace_ds(" GraphicEscape");
                    any[0] = false;
                }
                obptr.add(Tables.CG_2_EBC[screenBuffer[baddr] & 0xff]);
                int ebcVal = Tables.CG_2_EBC[screenBuffer[baddr] & 0xff] & 0xff;
                if (ebcVal <= 0x3f || ebcVal == 0xff) {
                    if (any[0]) trace.trace_ds("'");
                    trace.trace_ds(" %s", See.getEbc(Tables.CG_2_EBC[screenBuffer[baddr] & 0xff]));
                    any[0] = false;
                } else {
                    if (!any[0]) trace.trace_ds(" '");
                    trace.trace_ds("%s", See.getEbc(Tables.CG_2_EBC[screenBuffer[baddr] & 0xff]));
                    any[0] = true;
                }
            }
            baddr = incrementAddress(baddr);
        } while (baddr != 0);
        if (any[0]) trace.trace_ds("'");
        trace.trace_ds("\n");
        telnet.output(obptr);
    }

    // ------------------------------------------------------------------ //
    // takeBufferSnapshot
    // ------------------------------------------------------------------ //
    void takeBufferSnapshot(NetBuffer obptr) {
        int  baddr = 0;
        int  attr_count;
        byte current_fg = 0x00;
        byte current_gr = 0x00;
        byte current_cs = 0x00;
        byte av;

        obptr.add(screenAlt ? ControllerConstant.CMD_EWA : ControllerConstant.CMD_EW);
        obptr.add(ControllerConstant.CODE_TABLE[0]);

        do {
            if (FieldAttribute.isFA(screenBuffer[baddr])) {
                obptr.add(ControllerConstant.ORDER_SFE);
                attr_count = obptr.getIndex();
                obptr.add(1);
                obptr.add(See.XA_3270);
                obptr.add(ControllerConstant.CODE_TABLE[calculateFA(screenBuffer[baddr]) & 0xff]);
                if (extendedAttributes[baddr].fg != 0) {
                    obptr.add(See.XA_FOREGROUND);
                    obptr.add(extendedAttributes[baddr].fg);
                    obptr.incrementAt(attr_count, 1);
                }
                if (extendedAttributes[baddr].gr != 0) {
                    obptr.add(See.XA_HIGHLIGHTING);
                    obptr.add((byte) (extendedAttributes[baddr].gr | 0xf0));
                    obptr.incrementAt(attr_count, 1);
                }
                if ((extendedAttributes[baddr].cs & ExtendedAttribute.CS_MASK) != 0) {
                    obptr.add(See.XA_CHARSET);
                    obptr.add((byte) ((extendedAttributes[baddr].cs & ExtendedAttribute.CS_MASK) | 0xf0));
                    obptr.incrementAt(attr_count, 1);
                }
            } else {
                av = extendedAttributes[baddr].fg;
                if (current_fg != av) {
                    current_fg = av;
                    obptr.add(ControllerConstant.ORDER_SA);
                    obptr.add(See.XA_FOREGROUND);
                    obptr.add(av);
                }
                av = extendedAttributes[baddr].gr;
                if (av != 0) av |= (byte) 0xf0;
                if (current_gr != av) {
                    current_gr = av;
                    obptr.add(ControllerConstant.ORDER_SA);
                    obptr.add(See.XA_HIGHLIGHTING);
                    obptr.add(av);
                }
                av = (byte) (extendedAttributes[baddr].cs & ExtendedAttribute.CS_MASK);
                if (av != 0) av |= (byte) 0xf0;
                if (current_cs != av) {
                    current_cs = av;
                    obptr.add(ControllerConstant.ORDER_SA);
                    obptr.add(See.XA_CHARSET);
                    obptr.add(av);
                }
                if ((extendedAttributes[baddr].cs & ControllerConstant.CS_GE) != 0) {
                    obptr.add(ControllerConstant.ORDER_GE);
                }
                obptr.add(Tables.CG_2_EBC[screenBuffer[baddr] & 0xff]);
            }
            baddr = incrementAddress(baddr);
        } while (baddr != 0);

        obptr.add(ControllerConstant.ORDER_SBA);
        Util.encodeBAddress(obptr, cursorAddress);
        obptr.add(ControllerConstant.ORDER_IC);
    }

    // ------------------------------------------------------------------ //
    // takeReplyModeSnapshot
    // ------------------------------------------------------------------ //
    boolean takeReplyModeSnapshot(NetBuffer obptr) {
        boolean success = false;
        if (telnet.is3270() && replyMode != (byte) ControllerConstant.SF_SRM_FIELD) {
            obptr.add(ControllerConstant.CMD_WSF);
            obptr.add(0x00);
            obptr.add(0x00);
            obptr.add(ControllerConstant.SF_SET_REPLY_MODE);
            obptr.add(0x00);
            obptr.add(replyMode);
            if (replyMode == (byte) ControllerConstant.SF_SRM_CHAR) {
                for (int i = 0; i < crmnAttribute; i++) obptr.add(crmAttributes[i]);
            }
            success = true;
        }
        return success;
    }

    // ------------------------------------------------------------------ //
    // processEraseAllUnprotectedCommand
    // ------------------------------------------------------------------ //
    void processEraseAllUnprotectedCommand() {
        int  baddr, sbaddr;
        byte fa;
        boolean f;

        telnet.getKeyboard().toggleEnterInhibitMode(false);
        onAllChanged();

        if (isFormatted) {
            baddr = 0;
            do {
                if (FieldAttribute.isFA(screenBuffer[baddr])) break;
                baddr = incrementAddress(baddr);
            } while (baddr != 0);
            sbaddr = baddr;
            f = false;
            do {
                fa = screenBuffer[baddr];
                if (!FieldAttribute.isProtected(fa)) {
                    mdtClear(screenBuffer, baddr);
                    do {
                        baddr = incrementAddress(baddr);
                        if (!f) { setCursorAddress(baddr); f = true; }
                        if (!FieldAttribute.isFA(screenBuffer[baddr])) {
                            addCharacter(baddr, CG_Null, (byte) 0);
                        }
                    } while (!FieldAttribute.isFA(screenBuffer[baddr]));
                } else {
                    do {
                        baddr = incrementAddress(baddr);
                    } while (!FieldAttribute.isFA(screenBuffer[baddr]));
                }
            } while (baddr != sbaddr);
            if (!f) setCursorAddress(0);
        } else {
            clear(true);
        }
        attentionID = AID.NONE;
        telnet.getKeyboard().resetKeyboardLock(false);
    }

    // ------------------------------------------------------------------ //
    // endText helpers
    // ------------------------------------------------------------------ //
    private void endText() {
        if (previous == PreviousEnum.TEXT) trace.trace_ds("'");
    }

    private void endText(String cmd) {
        endText();
        trace.trace_ds(" " + cmd);
    }

    private byte attributeToFA(byte attr) {
        return (byte) (ControllerConstant.FA_BASE
                | (((attr) & 0x20) != 0 ? ControllerConstant.FA_PROTECT : (byte) 0)
                | (((attr) & 0x10) != 0 ? ControllerConstant.FA_NUMERIC  : (byte) 0)
                | (((attr) & 0x01) != 0 ? ControllerConstant.FA_MODIFY   : (byte) 0)
                | (((attr) >> 2) & ControllerConstant.FA_INTENSITY));
    }

    private void startFieldWithFA(byte fa) {
        currentFaIndex = bufferAddress;
        addCharacter(bufferAddress, fa, (byte) 0);
        setForegroundColor(bufferAddress, (byte) 0);
        addGr(bufferAddress, (byte) 0);
        trace.trace_ds(See.getSeeAttribute(fa));
        isFormatted = true;
    }

    private void startField() {
        startFieldWithFA((byte) ControllerConstant.FA_BASE);
    }

    private void startFieldWithAttribute(byte attr) {
        startFieldWithFA(attributeToFA(attr));
    }

    // ------------------------------------------------------------------ //
    // processWriteCommand
    // ------------------------------------------------------------------ //
    PDS processWriteCommand(byte[] buf, int start, int length, boolean erase) {
        boolean packetwasjustresetrewrite = false;
        int     baddr;
        byte    newAttr;
        boolean lastCommand;
        boolean lastZpt;
        boolean wccKeyboardRestore;
        boolean wccSoundAlarm;
        boolean raGe;
        byte    na;
        int     anyFA;
        byte    efaFG, efaGR, efaCS;
        String  paren = "(";

        defaultFg   = 0;
        defaultGr   = 0;
        defaultCs   = 0;
        tracePrimed = true;

        trace.writeLine("::ctlr_write::" + (System.currentTimeMillis() - startTime) + " " + length + " bytes");

        if (length == 4
                && (buf[start + 0] & 0xff) == 0xf1
                && (buf[start + 1] & 0xff) == 0xc2
                && (buf[start + 2] & 0xff) == 0xff
                && (buf[start + 3] & 0xff) == 0xef) {
            trace.writeLine("****Identified packet as a reset/rewrite combination.");
            packetwasjustresetrewrite = true;
        }

        PDS rv = PDS.OKAY_NO_OUTPUT;
        telnet.getKeyboard().toggleEnterInhibitMode(false);
        if (buf.length < 2) return PDS.BAD_COMMAND;

        bufferAddress = cursorAddress;
        if (See.wccReset(buf[start + 1])) {
            if (erase) replyMode = (byte) ControllerConstant.SF_SRM_FIELD;
            trace.trace_ds("%sreset", paren);
            paren = ",";
        }
        wccSoundAlarm = See.wccSoundAlarm(buf[start + 1]);
        if (wccSoundAlarm) { trace.trace_ds("%salarm", paren); paren = ","; }

        wccKeyboardRestore = See.wccKeyboardRestore(buf[start + 1]);
        if (wccKeyboardRestore) { trace.trace_ds("%srestore", paren); paren = ","; }

        if (See.wccResetMDT(buf[start + 1])) {
            trace.trace_ds("%sresetMDT", paren);
            paren = ",";
            baddr = 0;
            if (appres.modified_sel) onAllChanged();
            do {
                if (FieldAttribute.isFA(screenBuffer[baddr])) mdtClear(screenBuffer, baddr);
                baddr = incrementAddress(baddr);
            } while (baddr != 0);
        }
        if (!paren.equals("(")) trace.trace_ds(")");

        lastCommand    = true;
        lastZpt        = false;
        currentFaIndex = getFieldAttribute(bufferAddress);

        for (int cp = 2; cp < length; cp++) {
            int b = buf[cp + start] & 0xff;
            switch (b) {
                case ControllerConstant.ORDER_SF: {
                    endText("StartField");
                    if (previous != PreviousEnum.SBA) trace.trace_ds(trace.rcba(bufferAddress));
                    previous = PreviousEnum.ORDER;
                    cp++;
                    startFieldWithAttribute(buf[cp + start]);
                    setForegroundColor(bufferAddress, (byte) 0);
                    bufferAddress = incrementAddress(bufferAddress);
                    lastCommand = true; lastZpt = false;
                    break;
                }
                case ControllerConstant.ORDER_SBA: {
                    cp += 2;
                    bufferAddress = Util.decodeBAddress(buf[cp + start - 1], buf[cp + start]);
                    endText("SetBufferAddress");
                    previous = PreviousEnum.SBA;
                    trace.trace_ds(trace.rcba(bufferAddress));
                    if (bufferAddress >= columnCount * rowCount) {
                        trace.trace_ds(" [invalid address, write command terminated]\n");
                        telnet.getEvents().runScript("ctlr_write SBA_ERROR");
                        return PDS.BAD_ADDRESS;
                    }
                    currentFaIndex = getFieldAttribute(bufferAddress);
                    lastCommand = true; lastZpt = false;
                    break;
                }
                case ControllerConstant.ORDER_IC: {
                    endText("InsertCursor");
                    if (previous != PreviousEnum.SBA) trace.trace_ds(trace.rcba(bufferAddress));
                    previous = PreviousEnum.ORDER;
                    setCursorAddress(bufferAddress);
                    lastCommand = true; lastZpt = false;
                    break;
                }
                case ControllerConstant.ORDER_PT: {
                    endText("ProgramTab");
                    previous = PreviousEnum.ORDER;
                    if (FieldAttribute.isFA(screenBuffer[bufferAddress])
                            && !FieldAttribute.isProtected(screenBuffer[bufferAddress])) {
                        bufferAddress = incrementAddress(bufferAddress);
                        lastZpt = false; lastCommand = true;
                        break;
                    }
                    baddr = getNextUnprotectedField(bufferAddress);
                    if (baddr < bufferAddress) baddr = 0;
                    if (!lastCommand || lastZpt) {
                        trace.trace_ds("(nulling)");
                        while (bufferAddress != baddr && !FieldAttribute.isFA(screenBuffer[bufferAddress])) {
                            addCharacter(bufferAddress, CG_Null, (byte) 0);
                            bufferAddress = incrementAddress(bufferAddress);
                        }
                        if (baddr == 0) lastZpt = true;
                    } else {
                        lastZpt = false;
                    }
                    bufferAddress = baddr;
                    lastCommand = true;
                    break;
                }
                case ControllerConstant.ORDER_RA: {
                    endText("RepeatToAddress");
                    cp += 2;
                    baddr = Util.decodeBAddress(buf[cp + start - 1], buf[cp + start]);
                    trace.trace_ds(trace.rcba(baddr));
                    cp++;
                    if ((buf[cp + start] & 0xff) == ControllerConstant.ORDER_GE) {
                        raGe = true;
                        trace.trace_ds("GraphicEscape");
                        cp++;
                    } else {
                        raGe = false;
                    }
                    previous = PreviousEnum.ORDER;
                    if (buf[cp + start] != 0) trace.trace_ds("'");
                    trace.trace_ds("%s", See.getEbc(buf[cp + start]));
                    if (buf[cp + start] != 0) trace.trace_ds("'");
                    if (baddr >= columnCount * rowCount) {
                        trace.trace_ds(" [invalid address, write command terminated]\n");
                        telnet.getEvents().runScript("ctlr_write baddr>COLS*ROWS");
                        return PDS.BAD_ADDRESS;
                    }
                    do {
                        if (raGe) {
                            addCharacter(bufferAddress, Tables.EBC_2_CG0[buf[cp + start] & 0xff],
                                    (byte) ControllerConstant.CS_GE);
                        } else if (defaultCs != 0) {
                            addCharacter(bufferAddress, Tables.EBC_2_CG0[buf[cp + start] & 0xff], (byte) 1);
                        } else {
                            addCharacter(bufferAddress, Tables.EBC_2_CG[buf[cp + start] & 0xff], (byte) 0);
                        }
                        setForegroundColor(bufferAddress, defaultFg);
                        addGr(bufferAddress, defaultGr);
                        bufferAddress = incrementAddress(bufferAddress);
                    } while (bufferAddress != baddr);
                    currentFaIndex = getFieldAttribute(bufferAddress);
                    lastCommand = true; lastZpt = false;
                    break;
                }
                case ControllerConstant.ORDER_EUA: {
                    cp += 2;
                    baddr = Util.decodeBAddress(buf[cp + start - 1], buf[cp + start]);
                    endText("EraseUnprotectedAll");
                    if (previous != PreviousEnum.SBA) trace.trace_ds(trace.rcba(baddr));
                    previous = PreviousEnum.ORDER;
                    if (baddr >= columnCount * rowCount) {
                        trace.trace_ds(" [invalid address, write command terminated]\n");
                        telnet.getEvents().runScript("ctlr_write baddr>COLS*ROWS#2");
                        return PDS.BAD_ADDRESS;
                    }
                    do {
                        if (FieldAttribute.isFA(screenBuffer[bufferAddress])) {
                            currentFaIndex = bufferAddress;
                        } else if (!FieldAttribute.isProtected(screenBuffer[currentFaIndex])) {
                            addCharacter(bufferAddress, CG_Null, (byte) 0);
                        }
                        bufferAddress = incrementAddress(bufferAddress);
                    } while (bufferAddress != baddr);
                    currentFaIndex = getFieldAttribute(bufferAddress);
                    lastCommand = true; lastZpt = false;
                    break;
                }
                case ControllerConstant.ORDER_GE: {
                    endText("GraphicEscape ");
                    cp++;
                    previous = PreviousEnum.ORDER;
                    if (buf[cp + start] != 0) trace.trace_ds("'");
                    trace.trace_ds("%s", See.getEbc(buf[cp + start]));
                    if (buf[cp + start] != 0) trace.trace_ds("'");
                    addCharacter(bufferAddress, Tables.EBC_2_CG0[buf[cp + start] & 0xff],
                            (byte) ControllerConstant.CS_GE);
                    setForegroundColor(bufferAddress, defaultFg);
                    addGr(bufferAddress, defaultGr);
                    bufferAddress = incrementAddress(bufferAddress);
                    currentFaIndex = getFieldAttribute(bufferAddress);
                    lastCommand = false; lastZpt = false;
                    break;
                }
                case ControllerConstant.ORDER_MF: {
                    endText("ModifyField");
                    if (previous != PreviousEnum.SBA) trace.trace_ds(trace.rcba(bufferAddress));
                    previous = PreviousEnum.ORDER;
                    cp++;
                    na = buf[cp + start];
                    if (FieldAttribute.isFA(screenBuffer[bufferAddress])) {
                        for (int i = 0; i < (na & 0xff); i++) {
                            cp++;
                            if (buf[cp + start] == See.XA_3270) {
                                trace.trace_ds(" 3270");
                                cp++;
                                newAttr = attributeToFA(buf[cp + start]);
                                addCharacter(bufferAddress, newAttr, (byte) 0);
                                trace.trace_ds(See.getSeeAttribute(newAttr));
                            } else if (buf[cp + start] == See.XA_FOREGROUND) {
                                trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                                cp++;
                                if (appres.m3279) setForegroundColor(bufferAddress, buf[cp + start]);
                            } else if (buf[cp + start] == See.XA_HIGHLIGHTING) {
                                trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                                cp++;
                                addGr(bufferAddress, (byte) (buf[cp + start] & 0x07));
                            } else if (buf[cp + start] == See.XA_CHARSET) {
                                int cs = 0;
                                trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                                cp++;
                                if ((buf[cp + start] & 0xff) == 0xf1) cs = 1;
                                addCharacter(bufferAddress, screenBuffer[bufferAddress], (byte) cs);
                            } else if (buf[cp + start] == See.XA_ALL) {
                                trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                                cp++;
                            } else {
                                trace.trace_ds("%s[unsupported]",
                                        See.getEfa(buf[cp + start], buf[cp + start + 1]));
                                cp++;
                            }
                        }
                        bufferAddress = incrementAddress(bufferAddress);
                    } else {
                        cp += (na & 0xff) * 2;
                    }
                    lastCommand = true; lastZpt = false;
                    break;
                }
                case ControllerConstant.ORDER_SFE: {
                    endText("StartFieldExtended");
                    if (previous != PreviousEnum.SBA) trace.trace_ds(trace.rcba(bufferAddress));
                    previous = PreviousEnum.ORDER;
                    cp++;
                    na = buf[cp + start];
                    anyFA = 0; efaFG = 0; efaGR = 0; efaCS = 0;
                    for (int i = 0; i < (na & 0xff); i++) {
                        cp++;
                        if (buf[cp + start] == See.XA_3270) {
                            trace.trace_ds(" 3270");
                            cp++;
                            startFieldWithAttribute(buf[cp + start]);
                            anyFA++;
                        } else if (buf[cp + start] == See.XA_FOREGROUND) {
                            trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                            cp++;
                            if (appres.m3279) efaFG = buf[cp + start];
                        } else if (buf[cp + start] == See.XA_HIGHLIGHTING) {
                            trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                            cp++;
                            efaGR = (byte) (buf[cp + start] & 0x07);
                        } else if (buf[cp + start] == See.XA_CHARSET) {
                            trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                            cp++;
                            if ((buf[cp + start] & 0xff) == 0xf1) efaCS = 1;
                        } else if (buf[cp + start] == See.XA_ALL) {
                            trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                            cp++;
                        } else {
                            trace.trace_ds("%s[unsupported]",
                                    See.getEfa(buf[cp + start], buf[cp + start + 1]));
                            cp++;
                        }
                    }
                    if (anyFA == 0) startFieldWithFA((byte) ControllerConstant.FA_BASE);
                    addCharacter(bufferAddress, screenBuffer[bufferAddress], efaCS);
                    setForegroundColor(bufferAddress, efaFG);
                    addGr(bufferAddress, efaGR);
                    bufferAddress = incrementAddress(bufferAddress);
                    lastCommand = true; lastZpt = false;
                    break;
                }
                case ControllerConstant.ORDER_SA: {
                    endText("SetAttribtue");
                    previous = PreviousEnum.ORDER;
                    cp++;
                    if (buf[cp + start] == See.XA_FOREGROUND) {
                        trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                        if (appres.m3279) defaultFg = buf[cp + start + 1];
                    } else if (buf[cp + start] == See.XA_HIGHLIGHTING) {
                        trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                        defaultGr = (byte) (buf[cp + start + 1] & 0x07);
                    } else if (buf[cp + start] == See.XA_ALL) {
                        trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                        defaultFg = 0; defaultGr = 0; defaultCs = 0;
                    } else if (buf[cp + start] == See.XA_CHARSET) {
                        trace.trace_ds("%s", See.getEfa(buf[cp + start], buf[cp + start + 1]));
                        defaultCs = ((buf[cp + start + 1] & 0xff) == 0xf1) ? (byte) 1 : (byte) 0;
                    } else {
                        trace.trace_ds("%s[unsupported]",
                                See.getEfa(buf[cp + start], buf[cp + start + 1]));
                    }
                    cp++;
                    lastCommand = true; lastZpt = false;
                    break;
                }
                case ControllerConstant.FCORDER_SUB:
                case ControllerConstant.FCORDER_DUP:
                case ControllerConstant.FCORDER_FM:
                case ControllerConstant.FCORDER_FF:
                case ControllerConstant.FCORDER_CR:
                case ControllerConstant.FCORDER_NL:
                case ControllerConstant.FCORDER_EM:
                case ControllerConstant.FCORDER_EO: {
                    endText(See.getEbc(buf[cp + start]));
                    previous = PreviousEnum.ORDER;
                    addCharacter(bufferAddress, Tables.EBC_2_CG[buf[cp + start] & 0xff], defaultCs);
                    setForegroundColor(bufferAddress, defaultFg);
                    addGr(bufferAddress, defaultGr);
                    bufferAddress = incrementAddress(bufferAddress);
                    lastCommand = true; lastZpt = false;
                    break;
                }
                case ControllerConstant.FCORDER_NULL: {
                    endText("NULL");
                    previous = PreviousEnum.NULL_CHARACTER;
                    addCharacter(bufferAddress, Tables.EBC_2_CG[buf[cp + start] & 0xff], defaultCs);
                    setForegroundColor(bufferAddress, defaultFg);
                    addGr(bufferAddress, defaultGr);
                    bufferAddress = incrementAddress(bufferAddress);
                    lastCommand = false; lastZpt = false;
                    break;
                }
                default: {
                    if (b <= 0x3F) {
                        endText("ILLEGAL_ORDER");
                        trace.trace_ds("%s", See.getEbc(buf[cp + start]));
                        lastCommand = true; lastZpt = false;
                        break;
                    }
                    if (previous != PreviousEnum.TEXT) trace.trace_ds(" '");
                    previous = PreviousEnum.TEXT;
                    trace.trace_ds("%s", See.getEbc(buf[cp + start]));
                    addCharacter(bufferAddress, Tables.EBC_2_CG[buf[cp + start] & 0xff], defaultCs);
                    setForegroundColor(bufferAddress, defaultFg);
                    addGr(bufferAddress, defaultGr);
                    bufferAddress = incrementAddress(bufferAddress);
                    lastCommand = false; lastZpt = false;
                    break;
                }
            }
        }

        setFormattedFlag();
        if (previous == PreviousEnum.TEXT) trace.trace_ds("'");
        trace.trace_ds("\n");

        if (wccKeyboardRestore) {
            attentionID = AID.NONE;
            telnet.getKeyboard().resetKeyboardLock(false);
        } else if ((telnet.getKeyboard().getKeyboardLock() & KeyboardConstants.OiaTWait) != 0) {
            telnet.getKeyboard().keyboardLockClear(KeyboardConstants.OiaTWait, "ctlr_write");
        }

        tracePrimed = false;
        processPendingInput();

        if (!packetwasjustresetrewrite) {
            telnet.getEvents().runScript("ctlr_write - end");
            try {
                notifyDataAvailable();
            } catch (Exception ignored) {}
        }
        return rv;
    }

    // ------------------------------------------------------------------ //
    // notifyDataAvailable
    // ------------------------------------------------------------------ //
    private void notifyDataAvailable() {
        synchronized (dataAvailablePadlock) {
            if (telnet != null) {
                dataAvailableCount = telnet.getStartedReceivingCount();
            } else {
                dataAvailableCount++;
            }
        }
        int rcvCnt = (telnet != null) ? telnet.getStartedReceivingCount() : 0;
        trace.trace_dsn("NotifyDataAvailable : dataReceivedCount = " + rcvCnt
                + "  dataAvailableCount = " + getDataAvailableCount() + "\n");
    }

    // ------------------------------------------------------------------ //
    // writeSspcLuData
    // ------------------------------------------------------------------ //
    void writeSspcLuData(byte[] buf, int start, int buflen) {
        int  cp = start;
        int  sRow;
        byte c;
        int  baddr;
        byte fa;

        trace.trace_ds("SSCP-LU data\n");
        for (int i = 0; i < buflen; cp++, i++) {
            int b = buf[cp] & 0xff;
            switch (b) {
                case ControllerConstant.FCORDER_NL:
                    sRow = bufferAddress / columnCount;
                    while ((bufferAddress / columnCount) == sRow) {
                        addCharacter(bufferAddress, Tables.EBC_2_CG[0], defaultCs);
                        setForegroundColor(bufferAddress, defaultFg);
                        addGr(bufferAddress, defaultGr);
                        bufferAddress = incrementAddress(bufferAddress);
                    }
                    break;
                case ControllerConstant.ORDER_SF:
                    cp++; i++;
                    fa = attributeToFA(buf[cp]);
                    trace.trace_ds(" StartField" + trace.rcba(bufferAddress)
                            + " " + See.getSeeAttribute(fa) + " [translated to space]\n");
                    addCharacter(bufferAddress, CG_Space, defaultCs);
                    setForegroundColor(bufferAddress, defaultFg);
                    addGr(bufferAddress, defaultGr);
                    bufferAddress = incrementAddress(bufferAddress);
                    break;
                case ControllerConstant.ORDER_IC:
                    trace.trace_ds(" InsertCursor%s [ignored]\n", trace.rcba(bufferAddress));
                    break;
                case ControllerConstant.ORDER_SBA:
                    baddr = Util.decodeBAddress(buf[cp + 1], buf[cp + 2]);
                    trace.trace_ds(" SetBufferAddress%s [ignored]\n", trace.rcba(baddr));
                    cp += 2; i += 2;
                    break;
                case ControllerConstant.ORDER_GE:
                    cp++;
                    if (++i >= buflen) break;
                    c = ((buf[cp] & 0xff) <= 0x40) ? CG_Space : Tables.EBC_2_CG0[buf[cp] & 0xff];
                    addCharacter(bufferAddress, c, (byte) ControllerConstant.CS_GE);
                    setForegroundColor(bufferAddress, defaultFg);
                    addGr(bufferAddress, defaultGr);
                    bufferAddress = incrementAddress(bufferAddress);
                    break;
                default:
                    if (buf[cp] == (byte) ControllerConstant.FCORDER_NULL) {
                        c = CG_Space;
                    } else if (buf[cp] == (byte) ControllerConstant.FCORDER_FM) {
                        c = CG_Asterisk;
                    } else if (buf[cp] == (byte) ControllerConstant.FCORDER_DUP) {
                        c = CG_Semicolon;
                    } else if (b < 0x40) {
                        trace.trace_ds(" X'" + buf[cp] + "') [translated to space]\n");
                        c = CG_Space;
                    } else {
                        c = Tables.EBC_2_CG[buf[cp] & 0xff];
                    }
                    addCharacter(bufferAddress, c, defaultCs);
                    setForegroundColor(bufferAddress, defaultFg);
                    addGr(bufferAddress, defaultGr);
                    bufferAddress = incrementAddress(bufferAddress);
                    break;
            }
        }
        setCursorAddress(bufferAddress);
        sscpStart = bufferAddress;
        attentionID = AID.NONE;
        telnet.getKeyboard().resetKeyboardLock(false);
        telnet.getEvents().runScript("ctlr_write_sscp_lu done");
    }

    // ------------------------------------------------------------------ //
    // processPendingInput / continueProcessing
    // ------------------------------------------------------------------ //
    void processPendingInput() {
        while (telnet.getKeyboard().runTypeAhead()) ;
        continueProcessing();
    }

    void continueProcessing() {
        synchronized (telnet) {
            SmsState ws = telnet.getWaitState();
            switch (ws) {
                case IDLE:
                    break;
                case KB_WAIT:
                    if (telnet.isKeyboardInWait()) telnet.signalWaitEvent();
                    break;
                case WAIT_ANSI:
                    if (telnet.isAnsi()) telnet.signalWaitEvent();
                    break;
                case WAIT_3270:
                    if (telnet.is3270() || telnet.isSscp()) telnet.signalWaitEvent();
                    break;
                case WAIT:
                    if (!telnet.canProceed()) break;
                    if (telnet.isPending()
                            || (telnet.isConnected()
                                && (telnet.getKeyboard().getKeyboardLock() & KeyboardConstants.AwaitingFirst) != 0))
                        break;
                    telnet.signalWaitEvent();
                    break;
                case CONNECT_WAIT:
                    if (telnet.isPending()
                            || (telnet.isConnected()
                                && (telnet.getKeyboard().getKeyboardLock() & KeyboardConstants.AwaitingFirst) != 0))
                        break;
                    telnet.signalWaitEvent();
                    break;
                default:
                    log.warn("**BUGBUG**IGNORED STATE {}", ws);
                    break;
            }
        }
    }

    // ------------------------------------------------------------------ //
    // clear / blankOutScreen
    // ------------------------------------------------------------------ //
    void clear(boolean canSnap) {
        if (streamHasData()) {
            if (canSnap && !traceSkipping && appres.toggled(Appres.ScreenTrace)) {
                // trace.trace_screen(); // TODO: trace_screen not yet ported
            }
        }
        traceSkipping = false;
        for (int i = 0; i < rowCount * columnCount; i++) {
            screenBuffer[i] = 0;
            extendedAttributes[i].clear();
        }
        onAllChanged();
        setCursorAddress(0);
        bufferAddress = 0;
        isFormatted   = false;
        defaultFg     = 0;
        defaultGr     = 0;
        sscpStart     = 0;
    }

    private void blankOutScreen() {
        for (int i = 0; i < rowCount * columnCount; i++) {
            screenBuffer[i] = CG_Space;
        }
        onAllChanged();
        setCursorAddress(0);
        bufferAddress = 0;
        isFormatted   = false;
    }

    // ------------------------------------------------------------------ //
    // addCharacter / addGr / setForegroundColor / setBackgroundColor
    // ------------------------------------------------------------------ //
    void addCharacter(int baddr, byte c, byte cs) {
        byte oc;
        if ((oc = screenBuffer[baddr]) != c || extendedAttributes[baddr].cs != cs) {
            if (tracePrimed && !isBlank(oc)) {
                if (appres.toggled(Appres.ScreenTrace)) {
                    // trace.trace_screen(); // TODO: trace_screen not yet ported
                }
                tracePrimed = false;
            }
            onOneChanged(baddr);
            screenBuffer[baddr]          = c;
            extendedAttributes[baddr].cs = cs;
        }
    }

    void addGr(int baddr, byte gr) {
        if (extendedAttributes[baddr].gr != gr) {
            onOneChanged(baddr);
            extendedAttributes[baddr].gr = gr;
        }
    }

    void setForegroundColor(int baddr, byte color) {
        if (appres.m3279) {
            if ((color & 0xf0) != 0xf0) color = 0;
            if (extendedAttributes[baddr].fg != color) {
                onOneChanged(baddr);
                extendedAttributes[baddr].fg = color;
            }
        }
    }

    void setBackgroundColor(int baddr, byte color) {
        if (appres.m3279) {
            if ((color & 0xf0) != 0xf0) color = 0;
            if (extendedAttributes[baddr].bg != color) {
                onOneChanged(baddr);
                extendedAttributes[baddr].bg = color;
            }
        }
    }

    // ------------------------------------------------------------------ //
    // copyBlock
    // ------------------------------------------------------------------ //
    void copyBlock(int fromAddress, int toAddress, int count, boolean moveExtendedAttributes) {
        boolean changed = false;
        int start, end, inc;
        if (toAddress < fromAddress || fromAddress + count < toAddress) {
            start = 0; end = count + 1; inc = 1;
        } else {
            start = count - 1; end = -1; inc = -1;
        }
        for (int i = start; i != end; i += inc) {
            if (screenBuffer[fromAddress + i] != screenBuffer[toAddress + i]) {
                screenBuffer[toAddress + i] = screenBuffer[fromAddress + i];
                changed = true;
            }
        }
        if (changed) onRegionChanged(toAddress, toAddress + count);

        if (!moveExtendedAttributes) {
            for (int i = start; i != end; i += inc) {
                if (extendedAttributes[toAddress + i].cs != extendedAttributes[fromAddress + i].cs) {
                    extendedAttributes[toAddress + i].cs = extendedAttributes[fromAddress + i].cs;
                    onRegionChanged(toAddress + i, toAddress + i + 1);
                }
            }
        }

        if (moveExtendedAttributes) {
            changed = false;
            for (int i = 0; i < count; i++) {
                ExtendedAttribute fa = extendedAttributes[fromAddress + i];
                ExtendedAttribute ta = extendedAttributes[toAddress + i];
                if (fa.fg != ta.fg || fa.bg != ta.bg || fa.gr != ta.gr || fa.cs != ta.cs) {
                    ta.fg = fa.fg; ta.bg = fa.bg; ta.gr = fa.gr; ta.cs = fa.cs;
                    changed = true;
                }
            }
            if (changed) onRegionChanged(toAddress, toAddress + count);
        }
    }

    // ------------------------------------------------------------------ //
    // eraseRegion / scrollOne / screenRegionChanged
    // ------------------------------------------------------------------ //
    void eraseRegion(int baddr, int count, boolean clearEa) {
        boolean changed = false;
        for (int i = 0; i < count; i++) {
            if (screenBuffer[baddr + i] != 0) { screenBuffer[baddr + i] = 0; changed = true; }
        }
        if (changed) onRegionChanged(baddr, baddr + count);
        if (clearEa) {
            changed = false;
            for (int i = 0; i < count; i++) {
                if (!extendedAttributes[baddr + i].isZero()) {
                    extendedAttributes[baddr + i].clear();
                    changed = true;
                }
            }
            if (changed) onRegionChanged(baddr, baddr + count);
        }
    }

    void scrollOne() {
        throw new UnsupportedOperationException("scrollOne not implemented");
    }

    void screenRegionChanged(int start, int end) {
        onRegionChanged(start, end);
    }

    // ------------------------------------------------------------------ //
    // swapAltBuffers
    // ------------------------------------------------------------------ //
    void swapAltBuffers(boolean alt) {
        if (alt != isAltBuffer) {
            byte[] stmp          = screenBuffer;
            screenBuffer         = aScreenBuffer;
            aScreenBuffer        = stmp;
            ExtendedAttribute[] etmp = extendedAttributes;
            extendedAttributes       = aExtendedAttributeBuffer;
            aExtendedAttributeBuffer = etmp;
            isAltBuffer = alt;
            onAllChanged();
        }
    }

    // ------------------------------------------------------------------ //
    // setMDT / mdtClear
    // ------------------------------------------------------------------ //
    void setMDT(byte[] data, int offset) {
        if (offset != -1) {
            if ((data[offset] & ControllerConstant.FA_MODIFY) != 0) return;
            data[offset] |= ControllerConstant.FA_MODIFY;
            if (appres.modified_sel) onAllChanged();
        }
    }

    void mdtClear(byte[] data, int offset) {
        if ((data[offset] & ControllerConstant.FA_MODIFY) == 0) return;
        data[offset] &= ControllerConstant.FA_MODIFY_MASK;
        if (appres.modified_sel) onAllChanged();
    }

    // ------------------------------------------------------------------ //
    // shrink
    // ------------------------------------------------------------------ //
    private void shrink() {
        for (int i = 0; i < rowCount * columnCount; i++) {
            screenBuffer[i] = debuggingFont ? CG_Space : CG_Null;
        }
        onAllChanged();
    }

    // ------------------------------------------------------------------ //
    // Cursor address / coordinate helpers
    // ------------------------------------------------------------------ //
    int getCursorX() { return addressToColumn(cursorAddress); }
    int getCursorY() { return addressToRow(cursorAddress); }

    void setCursorAddress(int address) {
        if (address != cursorAddress) {
            cursorAddress = address;
            fireCursorLocationChanged();
        }
    }

    int addressToRow(int address)    { return address / columnCount; }
    int addressToColumn(int address) { return address % columnCount; }
    int rowColumnToByteAddress(int row, int column) { return row * columnCount + column; }

    int incrementAddress(int address) { return (address + 1) % (columnCount * rowCount); }
    int decrementAddress(int address) {
        return (address != 0) ? (address - 1) : (columnCount * rowCount - 1);
    }

    // ------------------------------------------------------------------ //
    // Timer helpers
    // ------------------------------------------------------------------ //
    Timer addTimeout(int ms, Runnable task) {
        Timer t = new Timer(true);
        t.schedule(new TimerTask() { public void run() { task.run(); } }, ms);
        return t;
    }

    void removeTimeout(Timer t) { if (t != null) t.cancel(); }

    // ------------------------------------------------------------------ //
    // moveCursor
    // ------------------------------------------------------------------ //
    boolean moveCursor(CursorOp op, int x, int y) {
        int  bAddress, sbAddress, nbAddress;
        boolean success = false;

        switch (op) {
            case EXACT:
            case NEAREST_UNPROTECTED_FIELD:
                if (!telnet.is3270()) { x--; y--; }
                if (x < 0) x = 0;
                if (y < 0) y = 0;
                bAddress = ((y * columnCount) + x) % (rowCount * columnCount);
                if (op == CursorOp.EXACT) {
                    setCursorAddress(bAddress);
                } else {
                    setCursorAddress(getNextUnprotectedField(cursorAddress));
                }
                success = true;
                break;
            case TAB:
                if (telnet.isAnsi()) {
                    telnet.sendChar('\t');
                    return true;
                } else {
                    setCursorAddress(getNextUnprotectedField(cursorAddress));
                }
                success = true;
                break;
            case BACK_TAB:
                if (telnet.is3270()) {
                    bAddress = cursorAddress;
                    bAddress = decrementAddress(bAddress);
                    if (FieldAttribute.isFA(screenBuffer[bAddress])) {
                        bAddress = decrementAddress(bAddress);
                    }
                    sbAddress = bAddress;
                    while (true) {
                        nbAddress = incrementAddress(bAddress);
                        if (FieldAttribute.isFA(screenBuffer[bAddress])
                                && !FieldAttribute.isProtected(screenBuffer[bAddress])
                                && !FieldAttribute.isFA(screenBuffer[nbAddress])) {
                            break;
                        }
                        bAddress = decrementAddress(bAddress);
                        if (bAddress == sbAddress) {
                            setCursorAddress(0);
                            success = true;
                            break;
                        }
                    }
                    bAddress = incrementAddress(bAddress);
                    setCursorAddress(bAddress);
                    success = true;
                }
                break;
            default:
                throw new UnsupportedOperationException("cursor op '" + op + "' not implemented");
        }
        return success;
    }

    // ------------------------------------------------------------------ //
    // dumpRange
    // ------------------------------------------------------------------ //
    void dumpRange(int first, int len, boolean inAscii, byte[] buf, int relRows, int relCols) {
        boolean any = false;
        byte[]  lineBuffer = new byte[maxColumns * 3 + 1];
        int s = 0;

        for (int i = 0; i < len; i++) {
            if (i != 0 && 0 == ((first + i) % relCols)) {
                lineBuffer[s] = 0;
                telnet.getAction().actionOutput(lineBuffer, s);
                s = 0; any = false;
            }
            if (!any) any = true;
            if (inAscii) {
                byte c = Tables.CG_2_ASCII[buf[first + i] & 0xff];
                lineBuffer[s++] = (c == 0) ? (byte) ' ' : c;
            } else {
                String temp = String.format("%s%02x", i != 0 ? " " : "",
                        Tables.CG_2_EBC[buf[first + i] & 0xff] & 0xff);
                for (int tt = 0; tt < temp.length(); tt++) lineBuffer[s++] = (byte) temp.charAt(tt);
            }
        }
        if (any) {
            lineBuffer[s] = 0;
            telnet.getAction().actionOutput(lineBuffer, s);
        }
    }

    private void dumpRangeXml(int first, int length, boolean inAscii,
                               byte[] buffer, int relRows, int relCols) {
        boolean any    = false;
        byte[]  linebuf = new byte[maxColumns * 3 * 5 + 1];
        int     s = 0;
        if (!inAscii) throw new IllegalArgumentException("dumpRangeXml only valid for ascii buffer");

        for (int i = 0; i < length; i++) {
            if (i != 0 && 0 == ((first + i) % relCols)) {
                linebuf[s] = 0;
                telnet.getAction().actionOutput(linebuf, s);
                s = 0; any = false;
            }
            if (!any) any = true;
            byte c = Tables.CG_2_ASCII[buffer[first + i] & 0xff];
            if (c == 0) c = (byte) ' ';
            linebuf[s++] = c;
        }
        if (any) {
            linebuf[s] = 0;
            telnet.getAction().actionOutput(linebuf, s, true);
        }
    }

    private boolean dumpFixed(Object[] args, String name, boolean inAscii,
                               byte[] buffer, int relRows, int relColumns, int cAddress) {
        int row, col, len, rows = 0, cols = 0;
        switch (args.length) {
            case 0:
                row = 0; col = 0; len = relRows * relColumns;
                break;
            case 1:
                row = cAddress / relColumns; col = cAddress % relColumns; len = (int) args[0];
                break;
            case 3:
                row = (int) args[0]; col = (int) args[1]; len = (int) args[2];
                break;
            case 4:
                row = (int) args[0]; col = (int) args[1]; rows = (int) args[2]; cols = (int) args[3];
                len = 0;
                break;
            default:
                telnet.getEvents().showError(name + " requires 0, 1, 3 or 4 arguments");
                return false;
        }
        if ((row < 0 || row > relRows || col < 0 || col > relColumns || len < 0)
                || ((args.length < 4) && ((row * relColumns) + col + len > relRows * relColumns))
                || ((args.length == 4) && (cols < 0 || rows < 0
                    || col + cols > relColumns || row + rows > relRows))) {
            telnet.getEvents().showError(name + ": Invalid argument");
            return false;
        }
        if (args.length < 4) {
            dumpRange((row * relColumns) + col, len, inAscii, buffer, relRows, relColumns);
        } else {
            for (int i = 0; i < rows; i++) {
                dumpRange(((row + i) * relColumns) + col, cols, inAscii, buffer, relRows, relColumns);
            }
        }
        return true;
    }

    private boolean dumpField(String name, boolean inAscii) {
        int start, baddr;
        int length = 0;
        if (!isFormatted) {
            telnet.getEvents().showError(name + ": Screen is not formatted");
            return false;
        }
        int faIndex = getFieldAttribute(cursorAddress);
        start = faIndex;
        start = incrementAddress(start);
        baddr = start;
        do {
            if (FieldAttribute.isFA(screenBuffer[baddr])) break;
            length++;
            baddr = incrementAddress(baddr);
        } while (baddr != start);
        dumpRange(start, length, inAscii, screenBuffer, rowCount, columnCount);
        return true;
    }

    private int dumpFieldAsXml(int address, ExtendedAttribute ea) {
        byte fa = fakeFA;
        int  start, baddr;
        int  length = 0;

        int faIndex = getFieldAttribute(address);
        if (faIndex != -1) fa = screenBuffer[faIndex];
        start = faIndex;
        start = incrementAddress(start);
        baddr = start;

        do {
            if (FieldAttribute.isFA(screenBuffer[baddr])) {
                if (extendedAttributes[baddr].fg != 0) ea.fg = extendedAttributes[baddr].fg;
                if (extendedAttributes[baddr].bg != 0) ea.bg = extendedAttributes[baddr].bg;
                if (extendedAttributes[baddr].cs != 0) ea.cs = extendedAttributes[baddr].cs;
                if (extendedAttributes[baddr].gr != 0) ea.gr = extendedAttributes[baddr].gr;
                break;
            }
            length++;
            baddr = incrementAddress(baddr);
        } while (baddr != start);

        int columnStart  = addressToColumn(start);
        int rowStart     = addressToRow(start);
        int rowEnd       = addressToRow(baddr) + 1;
        int remainingLen = length;

        for (int r = rowStart; r < rowEnd; r++) {
            int segLength;
            if (r == rowStart) {
                segLength     = Math.min(length, columnCount - columnStart);
                remainingLen -= segLength;
            } else {
                start        = rowColumnToByteAddress(r, 0);
                segLength    = Math.min(columnCount, remainingLen);
                remainingLen -= segLength;
            }
            telnet.getAction().actionOutput("<Field>");
            telnet.getAction().actionOutput("<Location position=\"" + start
                    + "\" left=\"" + addressToColumn(start)
                    + "\" top=\""  + addressToRow(start)
                    + "\" length=\"" + segLength + "\"/>");

            StringBuilder temp = new StringBuilder("<Attributes Base=\"" + fa + "\"");
            if (FieldAttribute.isProtected(fa)) temp.append(" Protected=\"true\"");
            else                                 temp.append(" Protected=\"false\"");
            if (FieldAttribute.isZero(fa)) {
                temp.append(" FieldType=\"Hidden\"");
            } else if (FieldAttribute.isHigh(fa)) {
                temp.append(" FieldType=\"High\"");
            } else if (FieldAttribute.isIntense(fa)) {
                temp.append(" FieldType=\"Intense\"");
            } else {
                if (ea.fg != 0) temp.append(" Foreground=\"")
                        .append(See.getEfaUnformatted(See.XA_FOREGROUND, ea.fg)).append("\"");
                if (ea.bg != 0) temp.append(" Background=\"")
                        .append(See.getEfaUnformatted(See.XA_BACKGROUND, ea.bg)).append("\"");
                if (ea.gr != 0) temp.append(" Highlighting=\"")
                        .append(See.getEfaUnformatted(See.XA_HIGHLIGHTING, (byte)(ea.bg | 0xf0))).append("\"");
                if ((ea.cs & ExtendedAttribute.CS_MASK) != 0) temp.append(" Mask=\"")
                        .append(See.getEfaUnformatted(See.XA_CHARSET,
                                (byte)((ea.cs & ExtendedAttribute.CS_MASK) | 0xf0))).append("\"");
            }
            temp.append("/>");
            telnet.getAction().actionOutput(temp.toString());
            dumpRangeXml(start, segLength, true, screenBuffer, rowCount, columnCount);
            telnet.getAction().actionOutput("</Field>");
        }

        if (baddr <= address) return -1;
        return baddr;
    }

    // ------------------------------------------------------------------ //
    // dump (debug)
    // ------------------------------------------------------------------ //
    void dump() {
        log.debug("dump starting.... Cursor@{}", cursorAddress);
        for (int y = 0; y < 24; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < 80; x++) {
                byte ch = Tables.CG_2_ASCII[screenBuffer[x + y * 80] & 0xff];
                sb.append(ch == 0 ? ' ' : (char)(ch & 0xff));
            }
            log.debug("{} {}", String.format("%02d", y), sb);
        }
    }

    // ------------------------------------------------------------------ //
    // Public action methods (called from Actions dispatcher)
    // ------------------------------------------------------------------ //
    boolean asciiAction(Object... args) {
        return dumpFixed(args, "Ascii_action", true, screenBuffer, rowCount, columnCount, cursorAddress);
    }

    boolean asciiFieldAction(Object... args) {
        return dumpField("AsciiField_action", true);
    }

    boolean dumpXmlAction(Object... args) {
        int pos = 0;
        telnet.getAction().actionOutput("<?xml version=\"1.0\"?>");
        telnet.getAction().actionOutput(
                "<XMLScreen xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        telnet.getAction().actionOutput("<CX>" + columnCount + "</CX>");
        telnet.getAction().actionOutput("<CY>" + rowCount + "</CY>");
        if (isFormatted) {
            telnet.getAction().actionOutput("<Formatted>true</Formatted>");
            ExtendedAttribute ea = new ExtendedAttribute();
            int lastPos = -1;
            int cnt     = 0;
            do {
                lastPos = pos;
                pos     = dumpFieldAsXml(pos, ea);
                if (lastPos == pos) cnt++;
                else                cnt = 0;
            } while (pos != -1 && cnt < 999);
        } else {
            telnet.getAction().actionOutput("<Formatted>false</Formatted>");
        }
        telnet.getAction().actionOutput("<Unformatted>");
        for (int i = 0; i < rowCount; i++) {
            int s   = rowColumnToByteAddress(i, 0);
            int len = columnCount;
            telnet.getAction().actionOutput("<Text>");
            dumpRangeXml(s, len, true, screenBuffer, rowCount, columnCount);
            telnet.getAction().actionOutput("</Text>");
        }
        telnet.getAction().actionOutput("</Unformatted>");
        telnet.getAction().actionOutput("</XMLScreen>");
        return true;
    }
}
