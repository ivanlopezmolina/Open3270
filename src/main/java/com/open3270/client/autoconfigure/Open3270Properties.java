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
package com.open3270.client.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Open3270 Spring Boot autoconfiguration.
 *
 * <pre>{@code
 * open3270:
 *   host: mainframe.example.com
 *   port: 23
 *   term-type: IBM-3278-2-E
 *   use-ssl: false
 *   connect-timeout-ms: 5000
 * }</pre>
 */
@ConfigurationProperties(prefix = "open3270")
public class Open3270Properties {

    /** Hostname or IP of the TN3270 server. */
    private String host;

    /** TCP port of the TN3270 server. */
    private int port = 23;

    /** Terminal type string (TN3270E DEVICE-TYPE). */
    private String termType = "IBM-3278-2-E";

    /** Whether to use SSL/TLS for the connection. */
    private boolean useSsl = false;

    /** Optional LU name for TN3270E LU-LU sessions. */
    private String lu;

    /** Connection timeout in milliseconds. */
    private int connectTimeoutMs = 5000;

    /** Whether to refuse TN3270E negotiation (fall back to standard TN3270). */
    private boolean refuseTn3270e = false;

    // ── getters / setters ─────────────────────────────────────────────────────

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getTermType() { return termType; }
    public void setTermType(String termType) { this.termType = termType; }

    public boolean isUseSsl() { return useSsl; }
    public void setUseSsl(boolean useSsl) { this.useSsl = useSsl; }

    public String getLu() { return lu; }
    public void setLu(String lu) { this.lu = lu; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public boolean isRefuseTn3270e() { return refuseTn3270e; }
    public void setRefuseTn3270e(boolean refuseTn3270e) { this.refuseTn3270e = refuseTn3270e; }
}
