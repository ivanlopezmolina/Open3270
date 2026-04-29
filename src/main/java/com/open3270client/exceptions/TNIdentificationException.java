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

import com.open3270client.interfaces.IXMLScreen;

/**
 * Thrown when the screen identification engine cannot match the current screen
 * to any known screen definition.
 */
public class TNIdentificationException extends RuntimeException {

    private final String page;
    private final String dump;

    public TNIdentificationException(String page, IXMLScreen screen) {
        super("Screen identification failed for page: " + page);
        this.page = page;
        this.dump = (screen != null) ? screen.dump() : null;
    }

    /** The name of the screen we were coming from (not the unrecognised screen). */
    public String getPage() {
        return page;
    }

    /** A text dump of the unrecognised screen. */
    public String getDump() {
        return dump;
    }

    @Override
    public String toString() {
        return "TNIdentificationException current screen='" + page + "'. Dump is \n\n" + dump + "\n\n";
    }
}
