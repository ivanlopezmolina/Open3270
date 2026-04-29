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

import java.io.PrintWriter;

/**
 * Screen-print utility.
 * Port of {@code Print.cs}.
 */
class Print implements AutoCloseable {

    private final Telnet telnet;

    Print(Telnet telnet) {
        this.telnet = telnet;
    }

    /**
     * Print the ASCII-fied contents of the screen onto a writer.
     *
     * @return {@code true} if anything was printed.
     */
    boolean printFormattedScreen(PrintWriter f, boolean evenIfEmpty) {
        int ns = 0, nr = 0;
        boolean any = false;
        Controller ctrl = telnet.getController();
        byte fa = ctrl.getFakeFA();
        int faIndex = ctrl.getFieldAttribute(0);
        if (faIndex != -1) fa = ctrl.getScreenBuffer()[faIndex];

        for (int i = 0; i < ctrl.getRowCount() * ctrl.getColumnCount(); i++) {
            if (i != 0 && (i % ctrl.getColumnCount()) == 0) { nr++; ns = 0; }
            byte e = ctrl.getScreenBuffer()[i];
            if (FieldAttribute.isFA(e)) { fa = e; }
            byte ch;
            if (FieldAttribute.isZero(fa)) ch = (byte) ' ';
            else                           ch = Tables.CG_2_ASCII[e & 0xff];
            if (ch == (byte) ' ') {
                ns++;
            } else {
                any = true;
                while (nr-- > 0) f.println();
                nr = 0;
                while (ns-- > 0) f.print(' ');
                ns = 0;
                f.print((char) (ch & 0xff));
            }
        }
        nr++;
        if (!any && !evenIfEmpty) return false;
        while (nr-- > 0) f.println();
        return true;
    }

    /**
     * Print the screen as text (takes a {@link PrintWriter} as first arg).
     */
    boolean printTextAction(Object... args) {
        if (args.length != 1) {
            telnet.getEvents().showError("PrintText_action: requires PrintWriter parameter");
            return false;
        }
        PrintWriter f = (PrintWriter) args[0];
        printFormattedScreen(f, true);
        return true;
    }

    @Override
    public void close() { /* nothing to release */ }
}
