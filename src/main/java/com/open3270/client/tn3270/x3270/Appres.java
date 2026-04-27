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

public class Appres {
    public boolean mono           = false;
    public boolean m3278          = false;
    public boolean extended       = true;
    public boolean modifiedSel    = false;
    public boolean once           = false;
    public boolean scripted       = false;
    public boolean numericLock    = false;
    public boolean secure         = false;
    public boolean noOther        = false;
    public boolean doConfirms     = false;
    public boolean reconnect      = false;
    public boolean modelSpecified = false;
    public String  model          = "4";
    public String  hostsFile      = null;
    public String  charset        = "bracket";
    public String  sbcsFontName   = null;
    public String  dbcsFontName   = null;
    public String  terminalName   = "IBM-3278-4-E";
    public String  loginMacro     = null;
    public String  loginString    = null;
    public boolean debug3270      = false;
    public int     bufferSize     = 4096;
    public boolean ftWaitForStartAck = false;
    public boolean altCursor      = false;
    public boolean cursorBlink    = false;
    public boolean monoCase       = false;
    public boolean blankFill      = false;
    public boolean scrollBar      = false;
    public boolean trilinear      = false;
    public boolean highlight      = false;
    public boolean visibleControl = false;
    public boolean ftForceAscii   = false;
}
