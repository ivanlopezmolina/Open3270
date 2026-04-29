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

/**
 * Field attribute utility methods.
 * Port of {@code FieldAttribute.cs}.
 */
public final class FieldAttribute {

    private FieldAttribute() {}

    public static boolean isFA(byte c) {
        return ((c & ControllerConstant.FA_MASK) == ControllerConstant.FA_BASE);
    }
    public static boolean isModified(byte c)   { return (c & ControllerConstant.FA_MODIFY) != 0; }
    public static boolean isNumeric(byte c)    { return (c & ControllerConstant.FA_NUMERIC) != 0; }
    public static boolean isProtected(byte c)  { return (c & ControllerConstant.FA_PROTECT) != 0; }
    public static boolean isProtectedAt(byte[] buf, int idx) { return (buf[idx] & ControllerConstant.FA_PROTECT) != 0; }
    public static boolean isSkip(byte c)       { return isNumeric(c) && isProtected(c); }
    public static boolean isZero(byte c)       { return (c & ControllerConstant.FA_INTENSITY) == ControllerConstant.FA_INT_ZERO_NSEL; }
    public static boolean isHigh(byte c)       { return (c & ControllerConstant.FA_INTENSITY) == ControllerConstant.FA_INT_HIGH_SEL; }
    public static boolean isNormal(byte c) {
        return (c & ControllerConstant.FA_INTENSITY) == ControllerConstant.FA_INT_NORM_NSEL
            || (c & ControllerConstant.FA_INTENSITY) == ControllerConstant.FA_INT_NORM_SEL;
    }
    public static boolean isSelectable(byte c) {
        return (c & ControllerConstant.FA_INTENSITY) == ControllerConstant.FA_INT_NORM_SEL
            || (c & ControllerConstant.FA_INTENSITY) == ControllerConstant.FA_INT_HIGH_SEL;
    }
    public static boolean isIntense(byte c) {
        return (c & ControllerConstant.FA_INT_HIGH_SEL) == ControllerConstant.FA_INT_HIGH_SEL;
    }
}
