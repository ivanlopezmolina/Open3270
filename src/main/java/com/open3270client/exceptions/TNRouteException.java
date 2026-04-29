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
 * Thrown when automatic screen navigation fails while routing between two
 * named screens.
 */
public class TNRouteException extends RuntimeException {

    private final String from;
    private final String to;
    private final String text;

    public TNRouteException(String from, String to, String text) {
        super("TNRouteException from screen '" + from + "' to '" + to + "'. " + text + ".");
        this.from = from;
        this.to = to;
        this.text = text;
    }

    /** The screen we were on when the error occurred. */
    public String getFrom() {
        return from;
    }

    /** The screen we were attempting to navigate to. */
    public String getTo() {
        return to;
    }

    /** A description of the error. */
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "TNRouteException from screen '" + from + "' to '" + to + "'. " + text + ".";
    }
}
