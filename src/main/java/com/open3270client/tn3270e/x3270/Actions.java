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

import com.open3270client.exceptions.TNHostException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Action dispatcher: maps action names to keyboard / controller / print methods.
 * Port of {@code Actions.cs}.
 */
class Actions {

    private final Telnet telnet;
    private final List<XtActionRec> actions;
    private final Map<String, XtActionRec> actionLookup = new HashMap<>();

    private List<byte[]> dataCapture       = null;
    private List<String> dataStringCapture = null;

    Actions(Telnet tn) {
        this.telnet = tn;
        actions = buildActions();
    }

    private List<XtActionRec> buildActions() {
        Keyboard  kb   = telnet.getKeyboard();
        Controller ctrl = telnet.getController();
        List<XtActionRec> list = new ArrayList<>();
        // name, causesSubmit, handler
        list.add(new XtActionRec("printtext",      false, args -> telnet.getPrint().printTextAction(args)));
        list.add(new XtActionRec("flip",           false, args -> kb.flipAction(args)));
        list.add(new XtActionRec("ascii",          false, args -> ctrl.asciiAction(args)));
        list.add(new XtActionRec("dumpxml",        false, args -> ctrl.dumpXmlAction(args)));
        list.add(new XtActionRec("asciifield",     false, args -> ctrl.asciiFieldAction(args)));
        list.add(new XtActionRec("attn",           true,  args -> kb.attnAction(args)));
        list.add(new XtActionRec("backspace",      false, args -> kb.backSpaceAction(args)));
        list.add(new XtActionRec("backtab",        false, args -> kb.backTabAction(args)));
        list.add(new XtActionRec("circumnot",      false, args -> kb.circumNotAction(args)));
        list.add(new XtActionRec("clear",          true,  args -> kb.clearAction(args)));
        list.add(new XtActionRec("cursorselect",   false, args -> kb.cursorSelectAction(args)));
        list.add(new XtActionRec("delete",         false, args -> kb.deleteAction(args)));
        list.add(new XtActionRec("deletefield",    false, args -> kb.deleteFieldAction(args)));
        list.add(new XtActionRec("deleteword",     false, args -> kb.deleteWordAction(args)));
        list.add(new XtActionRec("down",           false, args -> kb.moveCursorDown(args)));
        list.add(new XtActionRec("dup",            false, args -> kb.dupAction(args)));
        list.add(new XtActionRec("emulateinput",   true,  args -> kb.emulateInputAction(args)));
        list.add(new XtActionRec("enter",          true,  args -> kb.enterAction(args)));
        list.add(new XtActionRec("erase",          false, args -> kb.eraseAction(args)));
        list.add(new XtActionRec("eraseeof",       false, args -> kb.eraseEndOfFieldAction(args)));
        list.add(new XtActionRec("eraseinput",     false, args -> kb.eraseInputAction(args)));
        list.add(new XtActionRec("fieldend",       false, args -> kb.fieldEndAction(args)));
        list.add(new XtActionRec("fields",         false, args -> kb.fieldsAction(args)));
        list.add(new XtActionRec("fieldget",       false, args -> kb.fieldGetAction(args)));
        list.add(new XtActionRec("fieldset",       false, args -> kb.fieldSetAction(args)));
        list.add(new XtActionRec("fieldmark",      false, args -> kb.fieldMarkAction(args)));
        list.add(new XtActionRec("fieldexit",      false, args -> kb.fieldExitAction(args)));
        list.add(new XtActionRec("hexstring",      false, args -> kb.hexStringAction(args)));
        list.add(new XtActionRec("home",           false, args -> kb.homeAction(args)));
        list.add(new XtActionRec("insert",         false, args -> kb.insertAction(args)));
        list.add(new XtActionRec("interrupt",      true,  args -> kb.interruptAction(args)));
        list.add(new XtActionRec("key",            false, args -> kb.sendKeyAction(args)));
        list.add(new XtActionRec("left",           false, args -> kb.leftAction(args)));
        list.add(new XtActionRec("left2",          false, args -> kb.moveCursorLeft2Positions(args)));
        list.add(new XtActionRec("monocase",       false, args -> kb.monoCaseAction(args)));
        list.add(new XtActionRec("movecursor",     false, args -> kb.moveCursorAction(args)));
        list.add(new XtActionRec("newline",        false, args -> kb.moveCursorToNewLine(args)));
        list.add(new XtActionRec("nextword",       false, args -> kb.moveCursorToNextUnprotectedWord(args)));
        list.add(new XtActionRec("pa",             true,  args -> kb.paAction(args)));
        list.add(new XtActionRec("pf",             true,  args -> kb.pfAction(args)));
        list.add(new XtActionRec("previousword",   false, args -> kb.previousWordAction(args)));
        list.add(new XtActionRec("reset",          true,  args -> kb.resetAction(args)));
        list.add(new XtActionRec("right",          false, args -> kb.moveRight(args)));
        list.add(new XtActionRec("right2",         false, args -> kb.moveCursorRight2Positions(args)));
        list.add(new XtActionRec("string",         true,  args -> kb.sendStringAction(args)));
        list.add(new XtActionRec("sysreq",         true,  args -> kb.systemRequestAction(args)));
        list.add(new XtActionRec("tab",            false, args -> kb.tabForwardAction(args)));
        list.add(new XtActionRec("toggleinsert",   false, args -> kb.toggleInsertAction(args)));
        list.add(new XtActionRec("togglereverse",  false, args -> kb.toggleReverseAction(args)));
        list.add(new XtActionRec("up",             false, args -> kb.moveCursorUp(args)));
        return list;
    }

    // ------------------------------------------------------------------ //
    // Output capture
    // ------------------------------------------------------------------ //

    void actionOutput(String data) { actionOutput(data, false); }

    void actionOutput(String data, boolean encode) {
        if (dataCapture       == null) dataCapture       = new ArrayList<>();
        if (dataStringCapture == null) dataStringCapture = new ArrayList<>();
        dataCapture.add(data.getBytes(StandardCharsets.US_ASCII));
        dataStringCapture.add(encode ? encodeXml(data) : data);
    }

    void actionOutput(byte[] data, int length) { actionOutput(data, length, false); }

    void actionOutput(byte[] data, int length, boolean encode) {
        if (dataCapture       == null) dataCapture       = new ArrayList<>();
        if (dataStringCapture == null) dataStringCapture = new ArrayList<>();
        byte[] temp = new byte[length];
        System.arraycopy(data, 0, temp, 0, length);
        dataCapture.add(temp);
        String s = new String(temp, StandardCharsets.US_ASCII);
        dataStringCapture.add(encode ? encodeXml(s) : s);
    }

    String getStringData(int index) {
        if (dataStringCapture == null || index < 0 || index >= dataStringCapture.size()) return null;
        return dataStringCapture.get(index);
    }

    byte[] getByteData(int index) {
        if (dataCapture == null || index < 0 || index >= dataCapture.size()) return null;
        return dataCapture.get(index);
    }

    // ------------------------------------------------------------------ //
    // Dispatch
    // ------------------------------------------------------------------ //

    boolean keyboardCommandCausesSubmit(String name) {
        String key = name.toLowerCase();
        XtActionRec rec = actionLookup.get(key);
        if (rec != null) return rec.causesSubmit;
        for (XtActionRec a : actions) {
            if (a.name.equals(key)) { actionLookup.put(key, a); return a.causesSubmit; }
        }
        throw new RuntimeException("Sorry, action '" + name + "' is not known");
    }

    boolean execute(boolean submit, String name, Object... args) {
        telnet.getEvents().clear();
        if (!telnet.isConnected())
            throw new TNHostException("TN3270 Host is not connected", telnet.getDisconnectReason(), null);
        dataCapture       = null;
        dataStringCapture = null;
        String key = name.toLowerCase();
        XtActionRec rec = actionLookup.get(key);
        if (rec != null) { rec.proc.accept(args); return true; }
        for (XtActionRec a : actions) {
            if (a.name.equals(key)) { actionLookup.put(key, a); a.proc.accept(args); return true; }
        }
        throw new RuntimeException("Sorry, action '" + name + "' is not known");
    }

    // ------------------------------------------------------------------ //
    // Helpers
    // ------------------------------------------------------------------ //

    private static String encodeXml(String data) {
        return data.replace("&", "&amp;").replace("<", "&lt;");
    }

    // ------------------------------------------------------------------ //
    // Inner record
    // ------------------------------------------------------------------ //

    static class XtActionRec {
        final String name;
        final boolean causesSubmit;
        final java.util.function.Consumer<Object[]> proc;

        XtActionRec(String name, boolean causesSubmit, java.util.function.Consumer<Object[]> proc) {
            this.name         = name.toLowerCase();
            this.causesSubmit = causesSubmit;
            this.proc         = proc;
        }
    }
}
