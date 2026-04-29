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

import com.open3270client.tn3270e.PDS;

import java.util.Arrays;

/**
 * Processes 3270 Write Structured Field commands and builds query replies.
 * Port of {@code SF.cs} (class {@code StructuredField}).
 */
class StructuredField {

    private final Telnet telnet;

    private static final byte[] SUPPORTED_REPLIES = {
        See.QR_SUMMARY,
        See.QR_USABLE_AREA,
        See.QR_ALPHA_PART,
        See.QR_CHARSETS,
        See.QR_COLOR,
        See.QR_HIGHLIGHTING,
        See.QR_REPLY_MODES,
        See.QR_IMP_PART
    };
    private final int NSR = SUPPORTED_REPLIES.length;

    private boolean qrInProgress = false;
    private byte    replyMode    = 0;
    private int     cgcsgid      = 0x02b90025;

    private static final String[] BIT4X4 = {
        "0000","0001","0010","0011","0100","0101","0110","0111",
        "1000","1001","1010","1011","1100","1101","1110","1111"
    };

    StructuredField(Telnet telnet) {
        this.telnet = telnet;
    }

    // ------------------------------------------------------------------ //
    // Inner code constants (mirrors C# Codes inner class)
    // ------------------------------------------------------------------ //
    private static final byte CODE_READ_PARTITION   = 0x01;
    private static final byte CODE_QUERY            = 0x02;
    private static final byte CODE_QUERY_LIST       = 0x03;
    private static final byte CODE_QCODE_LIST       = 0x00;
    private static final byte CODE_EQUIV_QCODE_LIST = 0x40;
    private static final byte CODE_ALL              = (byte) 0x80;
    private static final byte CODE_ERASE_RESET      = 0x03;
    private static final byte CODE_DEFAULT          = 0x00;
    private static final byte CODE_ALTERNATE        = (byte) 0x80;
    private static final byte CODE_SET_REPLY_MODE   = 0x09;
    private static final byte CODE_FIELD            = 0x00;
    private static final byte CODE_EXTENDED_FIELD   = 0x01;
    private static final byte CODE_CHARACTER        = 0x02;
    private static final byte CODE_CREATE_PARTITION = 0x0c;
    private static final byte CODE_OUTBOUND_DS      = 0x40;

    // ------------------------------------------------------------------ //
    // Public entry point
    // ------------------------------------------------------------------ //

    /**
     * Processes a 3270 Write Structured Field command.
     */
    PDS writeStructuredField(byte[] buffer, int start, int bufferLength) {
        int cp = start + 1;   // skip WSF command byte
        bufferLength--;
        boolean first     = true;
        PDS     rv        = PDS.OKAY_NO_OUTPUT;
        boolean badCommand = false;

        while (bufferLength > 0) {
            if (first) telnet.getTrace().trace_ds(" ");
            else       telnet.getTrace().trace_ds("< WriteStructuredField ");
            first = false;

            if (bufferLength < 2) {
                telnet.getTrace().trace_ds("error: single byte at end of message\n");
                return (rv != PDS.OKAY_NO_OUTPUT) ? rv : PDS.BAD_COMMAND;
            }

            int fieldlen = ((buffer[cp] & 0xff) << 8) + (buffer[cp + 1] & 0xff);
            if (fieldlen == 0)     fieldlen = bufferLength;
            if (fieldlen < 3)      { telnet.getTrace().trace_ds("error: field length %d too small\n", fieldlen); return (rv != PDS.OKAY_NO_OUTPUT) ? rv : PDS.BAD_COMMAND; }
            if (fieldlen > bufferLength) { telnet.getTrace().trace_ds("error: field length %d exceeds remaining message length %d\n", fieldlen, bufferLength); return (rv != PDS.OKAY_NO_OUTPUT) ? rv : PDS.BAD_COMMAND; }

            PDS rvThis;
            byte[] field = cloneBytes(buffer, cp, fieldlen);
            switch (buffer[cp + 2]) {
                case CODE_READ_PARTITION   -> { telnet.getTrace().trace_ds("ReadPartition");    rvThis = readPart(field, fieldlen); }
                case CODE_ERASE_RESET      -> { telnet.getTrace().trace_ds("EraseReset");       rvThis = eraseReset(field, fieldlen); }
                case CODE_SET_REPLY_MODE   -> { telnet.getTrace().trace_ds("SetReplyMode");     rvThis = setReplyMode(field, fieldlen); }
                case CODE_CREATE_PARTITION -> { telnet.getTrace().trace_ds("CreatePartition");  rvThis = createPartition(field, fieldlen); }
                case CODE_OUTBOUND_DS      -> { telnet.getTrace().trace_ds("OutboundDS");       rvThis = outboundDS(field, fieldlen); }
                default                    -> { telnet.getTrace().trace_ds("unsupported ID 0x%02x\n", buffer[cp + 2]); rvThis = PDS.BAD_COMMAND; }
            }

            if (rvThis.getValue() < 0) badCommand = true;
            else rv = PDS.fromValue(rv.getValue() | rvThis.getValue());

            cp           += fieldlen;
            bufferLength -= fieldlen;
        }

        if (first) telnet.getTrace().trace_ds(" (null)\n");
        return (badCommand && rv == PDS.OKAY_NO_OUTPUT) ? PDS.BAD_COMMAND : rv;
    }

    // ------------------------------------------------------------------ //
    // Field handlers
    // ------------------------------------------------------------------ //

    private PDS readPart(byte[] buf, int len) {
        if (len < 5) { telnet.getTrace().trace_ds(" error: field length %d too small\n", len); return PDS.BAD_COMMAND; }
        byte partition = buf[3];
        telnet.getTrace().trace_ds("(0x%02x)", partition);
        String comma = "";

        switch (buf[4] & 0xFF) {
            case CODE_QUERY: {
                telnet.getTrace().trace_ds(" Query");
                if ((partition & 0xff) != 0xff) { telnet.getTrace().trace_ds(" error: illegal partition\n"); return PDS.BAD_COMMAND; }
                telnet.getTrace().trace_ds("\n");
                NetBuffer obptr = queryReplyStart();
                for (byte sr : SUPPORTED_REPLIES) doQueryReply(obptr, sr);
                queryReplyEnd(obptr);
                break;
            }
            case CODE_QUERY_LIST: {
                telnet.getTrace().trace_ds(" QueryList ");
                if ((partition & 0xff) != 0xff) { telnet.getTrace().trace_ds("error: illegal partition\n"); return PDS.BAD_COMMAND; }
                if (len < 6) { telnet.getTrace().trace_ds("error: missing request type\n"); return PDS.BAD_COMMAND; }
                NetBuffer obptr = queryReplyStart();
                switch (buf[5]) {
                    case CODE_QCODE_LIST: {
                        telnet.getTrace().trace_ds("List(");
                        if (len < 7) { telnet.getTrace().trace_ds(")\n"); doQueryReply(obptr, See.QR_NULL); }
                        else {
                            for (int i = 6; i < len; i++) { telnet.getTrace().trace_ds("%s%s", comma, See.getQCodeode(buf[i])); comma = ","; }
                            telnet.getTrace().trace_ds(")\n");
                            int any = 0;
                            for (byte sr : SUPPORTED_REPLIES) {
                                boolean found = false;
                                for (int pos = 0; pos < len - 6; pos++) if (buf[pos + 6] == sr) { found = true; break; }
                                if (found) { doQueryReply(obptr, sr); any++; }
                            }
                            if (any == 0) doQueryReply(obptr, See.QR_NULL);
                        }
                        break;
                    }
                    case CODE_EQUIV_QCODE_LIST: {
                        telnet.getTrace().trace_ds("Equivalent+List(");
                        for (int i = 6; i < len; i++) { telnet.getTrace().trace_ds("%s%s", comma, See.getQCodeode(buf[i])); comma = ","; }
                        telnet.getTrace().trace_ds(")\n");
                        for (byte sr : SUPPORTED_REPLIES) doQueryReply(obptr, sr);
                        break;
                    }
                    case CODE_ALL: {
                        telnet.getTrace().trace_ds("All\n");
                        for (byte sr : SUPPORTED_REPLIES) doQueryReply(obptr, sr);
                        break;
                    }
                    default: { telnet.getTrace().trace_ds("unknown request type 0x%02x\n", buf[5]); return PDS.BAD_COMMAND; }
                }
                queryReplyEnd(obptr);
                break;
            }
            case ControllerConstant.SNA_CMD_RMA: {
                telnet.getTrace().trace_ds(" ReadModifiedAll");
                if (partition != 0x00) { telnet.getTrace().trace_ds(" error: illegal partition\n"); return PDS.BAD_COMMAND; }
                telnet.getTrace().trace_ds("\n");
                telnet.getController().processReadModifiedCommand(AID.Q_REPLY, true);
                break;
            }
            case ControllerConstant.SNA_CMD_RB: {
                telnet.getTrace().trace_ds(" ReadBuffer");
                if (partition != 0x00) { telnet.getTrace().trace_ds(" error: illegal partition\n"); return PDS.BAD_COMMAND; }
                telnet.getTrace().trace_ds("\n");
                telnet.getController().processReadBufferCommand(AID.Q_REPLY);
                break;
            }
            case ControllerConstant.SNA_CMD_RM: {
                telnet.getTrace().trace_ds(" ReadModified");
                if (partition != 0x00) { telnet.getTrace().trace_ds(" error: illegal partition\n"); return PDS.BAD_COMMAND; }
                telnet.getTrace().trace_ds("\n");
                telnet.getController().processReadModifiedCommand(AID.Q_REPLY, false);
                break;
            }
            default: { telnet.getTrace().trace_ds(" unknown type 0x%02x\n", buf[4]); return PDS.BAD_COMMAND; }
        }
        return PDS.OKAY_OUTPUT;
    }

    private PDS eraseReset(byte[] buf, int len) {
        if (len != 4) { telnet.getTrace().trace_ds(" error: wrong field length %d\n", len); return PDS.BAD_COMMAND; }
        switch (buf[3]) {
            case CODE_DEFAULT   -> { telnet.getTrace().trace_ds(" Default\n");   telnet.getController().erase(false); }
            case CODE_ALTERNATE -> { telnet.getTrace().trace_ds(" Alternate\n"); telnet.getController().erase(true); }
            default             -> { telnet.getTrace().trace_ds(" unknown type 0x%02x\n", buf[3]); return PDS.BAD_COMMAND; }
        }
        return PDS.OKAY_NO_OUTPUT;
    }

    private PDS setReplyMode(byte[] buf, int len) {
        if (len < 5) { telnet.getTrace().trace_ds(" error: wrong field length %d\n", len); return PDS.BAD_COMMAND; }
        byte partition = buf[3];
        telnet.getTrace().trace_ds("(0x%02x)", partition);
        if (partition != 0x00) { telnet.getTrace().trace_ds(" error: illegal partition\n"); return PDS.BAD_COMMAND; }
        switch (buf[4] & 0xFF) {
            case CODE_FIELD          -> telnet.getTrace().trace_ds(" Field\n");
            case CODE_EXTENDED_FIELD -> telnet.getTrace().trace_ds(" ExtendedField\n");
            case CODE_CHARACTER      -> telnet.getTrace().trace_ds(" Character");
            default -> { telnet.getTrace().trace_ds(" unknown mode 0x%02x\n", buf[4]); return PDS.BAD_COMMAND; }
        }
        replyMode = buf[4];
        if (buf[4] == CODE_CHARACTER) {
            String comma = "(";
            telnet.getController().setCrmnAttribute(len - 5);
            for (int i = 5; i < len; i++) {
                telnet.getController().getCrmAttributes()[i - 5] = buf[i];
                telnet.getTrace().trace_ds("%s%s", comma, See.getEfaOnly(buf[i]));
                comma = ",";
            }
            telnet.getTrace().trace_ds("%s\n", (telnet.getController().getCrmnAttribute() != 0) ? ")" : "");
        }
        return PDS.OKAY_NO_OUTPUT;
    }

    private PDS createPartition(byte[] buf, int len) {
        if (len > 3) {
            telnet.getTrace().trace_ds("(");
            byte pid = buf[3];
            telnet.getTrace().trace_ds("pid=0x%02x", pid);
            if (pid != 0x00) { telnet.getTrace().trace_ds(") error: illegal partition\n"); return PDS.BAD_COMMAND; }
        }
        if (len > 4) {
            byte uom = (byte) ((buf[4] & 0xf0) >> 4);
            telnet.getTrace().trace_ds(",uom=B'%s'", BIT4X4[uom & 0xf]);
            if (uom != 0x0 && uom != 0x02) { telnet.getTrace().trace_ds(") error: illegal units\n"); return PDS.BAD_COMMAND; }
            byte am = (byte) (buf[4] & 0x0f);
            telnet.getTrace().trace_ds(",am=B'%s'", BIT4X4[am & 0xf]);
            if ((am & 0xff) > 0x2) { telnet.getTrace().trace_ds(") error: illegal a-mode\n"); return PDS.BAD_COMMAND; }
        }
        telnet.getTrace().trace_ds(")\n");
        telnet.getController().setCursorAddress(0);
        telnet.getController().setBufferAddress(0);
        return PDS.OKAY_NO_OUTPUT;
    }

    private PDS outboundDS(byte[] buf, int len) {
        if (len < 5) { telnet.getTrace().trace_ds(" error: field length %d too short\n", len); return PDS.BAD_COMMAND; }
        telnet.getTrace().trace_ds("(0x%02x)", buf[3]);
        if (buf[3] != 0x00) { telnet.getTrace().trace_ds(" error: illegal partition 0x%0x\n", buf[3]); return PDS.BAD_COMMAND; }
        switch (buf[4] & 0xFF) {
            case ControllerConstant.SNA_CMD_W: {
                telnet.getTrace().trace_ds(" Write");
                if (len > 5) telnet.getController().processWriteCommand(buf, 4, len - 4, false);
                else         telnet.getTrace().trace_ds("\n");
                break;
            }
            case ControllerConstant.SNA_CMD_EW: {
                telnet.getTrace().trace_ds(" EraseWrite");
                telnet.getController().erase(telnet.getController().isScreenAlt());
                if (len > 5) telnet.getController().processWriteCommand(buf, 4, len - 4, true);
                else         telnet.getTrace().trace_ds("\n");
                break;
            }
            case ControllerConstant.SNA_CMD_EWA: {
                telnet.getTrace().trace_ds(" EraseWriteAlternate");
                telnet.getController().erase(telnet.getController().isScreenAlt());
                if (len > 5) telnet.getController().processWriteCommand(buf, 4, len - 4, true);
                else         telnet.getTrace().trace_ds("\n");
                break;
            }
            case ControllerConstant.SNA_CMD_EAU: {
                telnet.getTrace().trace_ds(" EraseAllUnprotected\n");
                telnet.getController().processEraseAllUnprotectedCommand();
                break;
            }
            default: { telnet.getTrace().trace_ds(" unknown type 0x%02x\n", buf[4]); return PDS.BAD_COMMAND; }
        }
        return PDS.OKAY_NO_OUTPUT;
    }

    // ------------------------------------------------------------------ //
    // Query-reply helpers
    // ------------------------------------------------------------------ //

    private NetBuffer queryReplyStart() {
        NetBuffer buf = new NetBuffer();
        buf.add(AID.SF);
        qrInProgress = true;
        return buf;
    }

    private void doQueryReply(NetBuffer obptr, byte code) {
        if (qrInProgress) {
            telnet.getTrace().trace_ds("> StructuredField\n");
            qrInProgress = false;
        }
        int obptr0 = obptr.getIndex();
        obptr.add16(0);
        obptr.add(See.SFID_QREPLY);
        obptr.add(code);

        Controller ctrl = telnet.getController();
        String comma = "";
        switch (code) {
            case (byte) 0x85 /* QR_CHARSETS */: {
                telnet.getTrace().trace_ds("> QueryReply(CharacterSets)\n");
                obptr.add(0x82); obptr.add(0x00); obptr.add(7); obptr.add(7);
                obptr.add(0x00); obptr.add(0x00); obptr.add(0x00); obptr.add(0x00);
                obptr.add(0x07); obptr.add(0x00); obptr.add(0x10); obptr.add(0x00);
                obptr.add32(cgcsgid);
                break;
            }
            case (byte) 0xa6 /* QR_IMP_PART */: {
                telnet.getTrace().trace_ds("> QueryReply(ImplicitPartition)\n");
                obptr.add(0x0); obptr.add(0x0); obptr.add(0x0b); obptr.add(0x01); obptr.add(0x00);
                obptr.add16(80); obptr.add16(24);
                obptr.add16(ctrl.getMaxColumns()); obptr.add16(ctrl.getMaxRows());
                break;
            }
            case (byte) 0xff /* QR_NULL */: {
                telnet.getTrace().trace_ds("> QueryReply(Null)\n");
                break;
            }
            case (byte) 0x80 /* QR_SUMMARY */: {
                telnet.getTrace().trace_ds("> QueryReply(Summary(");
                for (byte sr : SUPPORTED_REPLIES) {
                    telnet.getTrace().trace_ds("%s%s", comma, See.getQCodeode(sr)); comma = ",";
                    obptr.add(sr);
                }
                telnet.getTrace().trace_ds("))\n");
                break;
            }
            case (byte) 0x81 /* QR_USABLE_AREA */: {
                telnet.getTrace().trace_ds("> QueryReply(UsableArea)\n");
                obptr.add(0x01); obptr.add(0x00);
                obptr.add16(ctrl.getMaxColumns()); obptr.add16(ctrl.getMaxRows());
                obptr.add(0x01);
                obptr.add16(100); obptr.add16(1);
                obptr.add16(100); obptr.add16(1);
                obptr.add(7); obptr.add(7);
                obptr.add16(ctrl.getMaxColumns() * ctrl.getMaxRows());
                break;
            }
            case (byte) 0x86 /* QR_COLOR */: {
                telnet.getTrace().trace_ds("> QueryReply(Color)\n");
                boolean color8 = telnet.getAppres().color8;
                obptr.add(0x00); obptr.add(color8 ? 8 : 16);
                obptr.add(0x00); obptr.add(0xf0 + See.COLOR_GREEN);
                for (int i = 0xf1; i <= (color8 ? 0xf8 : 0xff); i++) {
                    obptr.add(i);
                    obptr.add(telnet.getAppres().m3279 ? i : 0x00);
                }
                break;
            }
            case (byte) 0x87 /* QR_HIGHLIGHTING */: {
                telnet.getTrace().trace_ds("> QueryReply(Highlighting)\n");
                obptr.add(5);
                obptr.add(See.XAH_DEFAULT); obptr.add(See.XAH_NORMAL);
                obptr.add(See.XAH_BLINK);   obptr.add(See.XAH_BLINK);
                obptr.add(See.XAH_REVERSE);  obptr.add(See.XAH_REVERSE);
                obptr.add(See.XAH_UNDERSCORE); obptr.add(See.XAH_UNDERSCORE);
                obptr.add(See.XAH_INTENSIFY);  obptr.add(See.XAH_INTENSIFY);
                break;
            }
            case (byte) 0x88 /* QR_REPLY_MODES */: {
                telnet.getTrace().trace_ds("> QueryReply(ReplyModes)\n");
                obptr.add(CODE_FIELD); obptr.add(CODE_EXTENDED_FIELD); obptr.add(CODE_CHARACTER);
                break;
            }
            case (byte) 0x84 /* QR_ALPHA_PART */: {
                telnet.getTrace().trace_ds("> QueryReply(AlphanumericPartitions)\n");
                obptr.add(0);
                obptr.add16(ctrl.getMaxRows() * ctrl.getMaxColumns());
                obptr.add(0);
                break;
            }
            default: return;
        }
        obptr.add16At(obptr0, obptr.getIndex() - obptr0);
    }

    private void queryReplyEnd(NetBuffer obptr) {
        telnet.output(obptr);
        telnet.getKeyboard().toggleEnterInhibitMode(true);
    }

    // ------------------------------------------------------------------ //
    // Helpers
    // ------------------------------------------------------------------ //

    private static byte[] cloneBytes(byte[] data, int start, int length) {
        return Arrays.copyOfRange(data, start, start + length);
    }

    private static int get16(byte[] buf, int offset) {
        return ((buf[offset] & 0xff) << 8) | (buf[offset + 1] & 0xff);
    }
}
