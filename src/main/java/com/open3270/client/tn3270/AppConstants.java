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
package com.open3270.client.tn3270;

import java.util.HashMap;
import java.util.Map;

public final class AppConstants {
    private AppConstants() {}

    public static final TnKey[] FunctionKeys = {
        TnKey.F1, TnKey.F2, TnKey.F3, TnKey.F4, TnKey.F5, TnKey.F6,
        TnKey.F7, TnKey.F8, TnKey.F9, TnKey.F10, TnKey.F11, TnKey.F12
    };

    public static final TnKey[] AKeys = {
        TnKey.PA1, TnKey.PA2, TnKey.PA3, TnKey.PA4, TnKey.PA5, TnKey.PA6,
        TnKey.PA7, TnKey.PA8, TnKey.PA9, TnKey.PA10, TnKey.PA11, TnKey.PA12
    };

    public static final Map<TnKey, Integer> FunctionKeyIntLUT = new HashMap<>();

    static {
        FunctionKeyIntLUT.put(TnKey.F1, 1);
        FunctionKeyIntLUT.put(TnKey.F2, 2);
        FunctionKeyIntLUT.put(TnKey.F3, 3);
        FunctionKeyIntLUT.put(TnKey.F4, 4);
        FunctionKeyIntLUT.put(TnKey.F5, 5);
        FunctionKeyIntLUT.put(TnKey.F6, 6);
        FunctionKeyIntLUT.put(TnKey.F7, 7);
        FunctionKeyIntLUT.put(TnKey.F8, 8);
        FunctionKeyIntLUT.put(TnKey.F9, 9);
        FunctionKeyIntLUT.put(TnKey.F10, 10);
        FunctionKeyIntLUT.put(TnKey.F11, 11);
        FunctionKeyIntLUT.put(TnKey.F12, 12);
        FunctionKeyIntLUT.put(TnKey.PA1, 1);
        FunctionKeyIntLUT.put(TnKey.PA2, 2);
        FunctionKeyIntLUT.put(TnKey.PA3, 3);
        FunctionKeyIntLUT.put(TnKey.PA4, 4);
        FunctionKeyIntLUT.put(TnKey.PA5, 5);
        FunctionKeyIntLUT.put(TnKey.PA6, 6);
        FunctionKeyIntLUT.put(TnKey.PA7, 7);
        FunctionKeyIntLUT.put(TnKey.PA8, 8);
        FunctionKeyIntLUT.put(TnKey.PA9, 9);
        FunctionKeyIntLUT.put(TnKey.PA10, 10);
        FunctionKeyIntLUT.put(TnKey.PA11, 11);
        FunctionKeyIntLUT.put(TnKey.PA12, 12);
    }
}
