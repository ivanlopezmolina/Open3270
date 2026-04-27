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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.open3270.client.interfaces.IAudit;

public class TNTrace {
    private static final Logger log = LoggerFactory.getLogger(TNTrace.class);

    public boolean optionTraceAnsi        = false;
    public boolean optionTraceDS          = false;
    public boolean optionTraceDSN         = false;
    public boolean optionTraceEvent       = false;
    public boolean optionTraceNetworkData = false;

    private IAudit audit;

    public TNTrace(IAudit audit) {
        this.audit = audit;
    }

    public void write(String text) {
        if (audit != null) audit.write(text);
        else log.debug(text);
    }

    public void writeLine(String text) {
        if (audit != null) audit.writeLine(text);
        else log.debug(text);
    }

    public void println(String text) {
        writeLine(text);
    }
}
