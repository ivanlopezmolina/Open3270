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

public class FieldAttributes {
    private boolean modified;
    private boolean numeric;
    private boolean isProtected;
    private boolean skip;
    private boolean zero;
    private boolean high;
    private boolean normal;
    private boolean selectable;
    private boolean intense;

    public boolean isModified() { return modified; }
    public void setModified(boolean modified) { this.modified = modified; }

    public boolean isNumeric() { return numeric; }
    public void setNumeric(boolean numeric) { this.numeric = numeric; }

    public boolean isProtected() { return isProtected; }
    public void setProtected(boolean isProtected) { this.isProtected = isProtected; }

    public boolean isSkip() { return skip; }
    public void setSkip(boolean skip) { this.skip = skip; }

    public boolean isZero() { return zero; }
    public void setZero(boolean zero) { this.zero = zero; }

    public boolean isHigh() { return high; }
    public void setHigh(boolean high) { this.high = high; }

    public boolean isNormal() { return normal; }
    public void setNormal(boolean normal) { this.normal = normal; }

    public boolean isSelectable() { return selectable; }
    public void setSelectable(boolean selectable) { this.selectable = selectable; }

    public boolean isIntense() { return intense; }
    public void setIntense(boolean intense) { this.intense = intense; }

    public static FieldAttributes fromByte(byte fa) {
        FieldAttributes attrs = new FieldAttributes();
        attrs.modified   = FieldAttribute.isModified(fa);
        attrs.numeric    = FieldAttribute.isNumeric(fa);
        attrs.isProtected= FieldAttribute.isProtected(fa);
        attrs.skip       = FieldAttribute.isSkip(fa);
        attrs.zero       = FieldAttribute.isZero(fa);
        attrs.high       = FieldAttribute.isHigh(fa);
        attrs.normal     = FieldAttribute.isNormal(fa);
        attrs.selectable = FieldAttribute.isSelectable(fa);
        attrs.intense    = FieldAttribute.isIntense(fa);
        return attrs;
    }
}
