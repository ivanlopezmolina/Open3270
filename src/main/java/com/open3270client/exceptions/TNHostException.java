/*
 * Open3270Client - Java / Spring Boot port of Open3270
 * Original C# library: https://github.com/Open3270/Open3270
 * Original authors: Mike Warriner, Francois Botha
 * Original license: MIT
 *
 * Java port copyright (c) 2026 Ivanlopezmolina
 * Released under the MIT License.
 */
package com.open3270client.exceptions;

/**
 * Thrown when an error occurs communicating with the TN3270 host.
 */
public class TNHostException extends RuntimeException {

    private final String reason;
    private String auditLog;

    public TNHostException(String message, String reason, String auditLog) {
        super(message);
        this.reason = reason;
        this.auditLog = auditLog;
    }

    /** The detailed reason for the failure. */
    public String getReason() {
        return reason;
    }

    /** The audit log captured up to the point of this exception. */
    public String getAuditLog() {
        return auditLog;
    }

    public void setAuditLog(String auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    public String toString() {
        return "TNHostException '" + getMessage() + "' " + reason;
    }
}
