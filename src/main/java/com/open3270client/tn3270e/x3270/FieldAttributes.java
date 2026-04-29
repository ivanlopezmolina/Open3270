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
 * Value object capturing all field attribute flags for a single 3270 field byte.
 * Port of the {@code FieldAttributes} struct in {@code FieldAttribute.cs}.
 */
public class FieldAttributes {
    public boolean isModified;
    public boolean isNumeric;
    public boolean isProtected;
    public boolean isSkip;
    public boolean isZero;
    public boolean isHigh;
    public boolean isNormal;
    public boolean isSelectable;
    public boolean isIntense;

    /** Builds a {@code FieldAttributes} from a raw 3270 field-attribute byte. */
    public static FieldAttributes fromByte(byte fa) {
        FieldAttributes a = new FieldAttributes();
        a.isModified   = FieldAttribute.isModified(fa);
        a.isNumeric    = FieldAttribute.isNumeric(fa);
        a.isProtected  = FieldAttribute.isProtected(fa);
        a.isSkip       = FieldAttribute.isSkip(fa);
        a.isZero       = FieldAttribute.isZero(fa);
        a.isHigh       = FieldAttribute.isHigh(fa);
        a.isNormal     = FieldAttribute.isNormal(fa);
        a.isSelectable = FieldAttribute.isSelectable(fa);
        a.isIntense    = FieldAttribute.isIntense(fa);
        return a;
    }
}
