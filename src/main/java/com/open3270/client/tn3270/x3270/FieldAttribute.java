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

public final class FieldAttribute {
    private FieldAttribute() {}

    public static boolean isFA(byte c) {
        return (c & ControllerConstant.FA_MASK) == ControllerConstant.FA_BASE;
    }

    public static boolean isModified(byte c) {
        return (c & ControllerConstant.FA_MODIFY) != 0;
    }

    public static boolean isNumeric(byte c) {
        return (c & ControllerConstant.FA_NUMERIC) != 0;
    }

    public static boolean isProtected(byte c) {
        return (c & ControllerConstant.FA_PROTECT) != 0;
    }

    public static boolean isSkip(byte c) {
        return isNumeric(c) && isProtected(c);
    }

    public static boolean isZero(byte c) {
        return (c & ControllerConstant.FA_INTENSITY) == ControllerConstant.FA_INT_ZERO_NSEL;
    }

    public static boolean isHigh(byte c) {
        return (c & ControllerConstant.FA_INTENSITY) == ControllerConstant.FA_INT_HIGH_SEL;
    }

    public static boolean isNormal(byte c) {
        int intensity = c & ControllerConstant.FA_INTENSITY;
        return intensity == ControllerConstant.FA_INT_NORM_NSEL || intensity == ControllerConstant.FA_INT_NORM_SEL;
    }

    public static boolean isSelectable(byte c) {
        int intensity = c & ControllerConstant.FA_INTENSITY;
        return intensity == ControllerConstant.FA_INT_NORM_SEL || intensity == ControllerConstant.FA_INT_HIGH_SEL;
    }

    public static boolean isIntense(byte c) {
        return (c & ControllerConstant.FA_INT_HIGH_SEL) == ControllerConstant.FA_INT_HIGH_SEL;
    }
}
