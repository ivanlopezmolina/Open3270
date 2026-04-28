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

public class TnHeader {

    public static final int SIZE = 5;

    public static final class Ops {
        private Ops() {}
        public static final byte Associate  = 0;
        public static final byte Connect    = 1;
        public static final byte DeviceType = 2;
        public static final byte Functions  = 3;
        public static final byte Is         = 4;
        public static final byte Reason     = 5;
        public static final byte Reject     = 6;
        public static final byte Request    = 7;
        public static final byte Send       = 8;
    }

    public static final class DataType {
        private DataType() {}
        public static final byte Data3270   = 0;
        public static final byte DataScs    = 1;
        public static final byte Response   = 2;
        public static final byte BindImage  = 3;
        public static final byte Unbind     = 4;
        public static final byte NvtData    = 5;
        public static final byte Request    = 6;
        public static final byte SscpLuData = 7;
        public static final byte PrintEoj   = 8;
    }

    public static final class ResponseFlags {
        private ResponseFlags() {}
        public static final byte NoResponse       = 0x00;
        public static final byte ErrorResponse    = 0x01;
        public static final byte AlwaysResponse   = 0x02;
        public static final byte PositiveResponse = 0x00;
        public static final byte NegativeResponse = 0x01;
    }

    public static final class NegotiationReasonCodes {
        private NegotiationReasonCodes() {}
        public static final int ConnPartner    = 0;
        public static final int DeviceInUse    = 1;
        public static final int InvAssociate   = 2;
        public static final int InvDeviceName  = 3;
        public static final int InvDeviceType  = 4;
        public static final int TypeNameError  = 5;
        public static final int UnknownError   = 6;
        public static final int UnsupportedReq = 7;
    }

    public byte dataType;
    public byte requestFlag;
    public byte responseFlag;
    public byte seqNo1;
    public byte seqNo2;

    public TnHeader() {}

    public TnHeader(byte[] buf) {
        dataType     = buf[0];
        requestFlag  = buf[1];
        responseFlag = buf[2];
        seqNo1       = buf[3];
        seqNo2       = buf[4];
    }

    public void writeTo(byte[] buf) {
        buf[0] = dataType;
        buf[1] = requestFlag;
        buf[2] = responseFlag;
        buf[3] = seqNo1;
        buf[4] = seqNo2;
    }
}
