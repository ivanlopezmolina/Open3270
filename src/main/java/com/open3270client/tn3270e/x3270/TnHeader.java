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

import com.open3270client.tn3270e.DataType3270;

/**
 * TN3270E protocol header (5 bytes).
 * Port of {@code Tn3270eHeader.cs} / {@code TnHeader}.
 */
public class TnHeader {

    public static final int EH_SIZE = 5;

    // Request flag
    public static final byte RQF_ERROR_CONDITION_CLEARED = 0x00;

    // Response data codes
    public static final class HeaderResponseData {
        public static final byte POS_DEVICE_END             = 0x00;
        public static final byte NEG_COMMAND_REJECT         = 0x00;
        public static final byte NEG_INTERVENTION_REQUIRED  = 0x01;
        public static final byte NEG_OPERATION_CHECK        = 0x02;
        public static final byte NEG_COMPONENT_DISCONNECTED = 0x03;
    }

    // Response flag codes
    public static final class HeaderResponseFlags {
        public static final byte NO_RESPONSE       = 0x00;
        public static final byte ERROR_RESPONSE    = 0x01;
        public static final byte ALWAYS_RESPONSE   = 0x02;
        public static final byte POSITIVE_RESPONSE = 0x00;
        public static final byte NEGATIVE_RESPONSE = 0x01;
    }

    // Negotiation operation codes
    public static final class Ops {
        public static final int ASSOCIATE   = 0;
        public static final int CONNECT     = 1;
        public static final int DEVICE_TYPE = 2;
        public static final int FUNCTIONS   = 3;
        public static final int IS          = 4;
        public static final int REASON      = 5;
        public static final int REJECT      = 6;
        public static final int REQUEST     = 7;
        public static final int SEND        = 8;
    }

    // Negotiation rejection reason codes
    public static final class NegotiationReasonCodes {
        public static final int CONN_PARTNER    = 0;
        public static final int DEVICE_IN_USE   = 1;
        public static final int INV_ASSOCIATE   = 2;
        public static final int INV_DEVICE_NAME = 3;
        public static final int INV_DEVICE_TYPE = 4;
        public static final int TYPE_NAME_ERROR = 5;
        public static final int UNKNOWN_ERROR   = 6;
        public static final int UNSUPPORTED_REQ = 7;
    }

    // ------------------------------------------------------------------
    // Instance fields (one header per TN3270E PDU)
    // ------------------------------------------------------------------

    private DataType3270 dataType;
    private byte requestFlag;
    private byte responseFlag;
    private final byte[] sequenceNumber = new byte[2];

    public TnHeader() {}

    public TnHeader(DataType3270 dataType, byte requestFlag, byte responseFlag, byte seq0, byte seq1) {
        this.dataType     = dataType;
        this.requestFlag  = requestFlag;
        this.responseFlag = responseFlag;
        this.sequenceNumber[0] = seq0;
        this.sequenceNumber[1] = seq1;
    }

    public DataType3270 getDataType()   { return dataType; }
    public void setDataType(DataType3270 d) { this.dataType = d; }

    public byte getRequestFlag()  { return requestFlag; }
    public void setRequestFlag(byte b) { this.requestFlag = b; }

    public byte getResponseFlag() { return responseFlag; }
    public void setResponseFlag(byte b) { this.responseFlag = b; }

    public byte[] getSequenceNumber() { return sequenceNumber; }

    /**
     * Serialises this header into a 5-byte array.
     */
    public byte[] toBytes() {
        byte[] out = new byte[EH_SIZE];
        out[0] = (byte) dataType.ordinal();
        out[1] = requestFlag;
        out[2] = responseFlag;
        out[3] = sequenceNumber[0];
        out[4] = sequenceNumber[1];
        return out;
    }

    /**
     * Reads a header from the given byte array at the given offset.
     */
    public static TnHeader fromBytes(byte[] data, int offset) {
        TnHeader h = new TnHeader();
        h.dataType     = DataType3270.values()[data[offset] & 0xff];
        h.requestFlag  = data[offset + 1];
        h.responseFlag = data[offset + 2];
        h.sequenceNumber[0] = data[offset + 3];
        h.sequenceNumber[1] = data[offset + 4];
        return h;
    }
}
