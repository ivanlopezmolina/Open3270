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
 * C-style printf-like formatter used by the trace layer.
 * Supports: {@code %c %f %d %s %u %x %X %02x}.
 * Port of {@code TraceFormatter.cs}.
 */
class TraceFormatter {

    private TraceFormatter() {}

    static String format(String fmt, Object... args) {
        if (fmt == null) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int argIdx = 0;
        while (i < fmt.length()) {
            if (fmt.charAt(i) == '%') {
                if (i + 1 >= fmt.length()) { sb.append('%'); i++; continue; }
                char spec = fmt.charAt(i + 1);
                // %02x
                if (spec == '0' && fmt.startsWith("%02x", i)) {
                    try {
                        int v = ((Number) args[argIdx]).intValue();
                        sb.append(String.format("%02X", v));
                    } catch (Exception e) {
                        sb.append("??");
                    }
                    i += 4; argIdx++;
                    continue;
                }
                switch (spec) {
                    case 'c' -> { sb.append((char) args[argIdx]); i += 2; argIdx++; }
                    case 'f' -> { sb.append(args[argIdx]); i += 2; argIdx++; }
                    case 'd', 's', 'u' -> {
                        sb.append(args[argIdx] == null ? "(null)" : args[argIdx].toString());
                        i += 2; argIdx++;
                    }
                    case 'x' -> { sb.append(String.format("%x", args[argIdx])); i += 2; argIdx++; }
                    case 'X' -> { sb.append(String.format("%X", args[argIdx])); i += 2; argIdx++; }
                    default  -> { sb.append('%'); i++; }
                }
            } else {
                sb.append(fmt.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }
}
