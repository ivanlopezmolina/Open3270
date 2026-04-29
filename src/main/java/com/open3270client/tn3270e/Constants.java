/*
 * Open3270Client - Java / Spring Boot port of Open3270
 * Original C# library: https://github.com/Open3270/Open3270
 * Original authors: Mike Warriner, Francois Botha
 * Original license: MIT
 *
 * Java port copyright (c) 2026 Ivanlopezmolina
 * Released under the MIT License.
 */
package com.open3270client.tn3270e;

import java.util.*;

/**
 * Static constants shared across the TN3270(E) protocol layer, including the
 * function-key lookup table.
 */
public final class Constants {

    private Constants() {}

    public static final TnKey[] FUNCTION_KEYS = {
        TnKey.F1, TnKey.F2, TnKey.F3, TnKey.F4, TnKey.F5, TnKey.F6,
        TnKey.F7, TnKey.F8, TnKey.F9, TnKey.F10, TnKey.F11, TnKey.F12
    };

    public static final TnKey[] A_KEYS = {
        TnKey.PA1, TnKey.PA2, TnKey.PA3, TnKey.PA4, TnKey.PA5, TnKey.PA6,
        TnKey.PA7, TnKey.PA8, TnKey.PA9, TnKey.PA10, TnKey.PA11, TnKey.PA12
    };

    /**
     * Maps a function key or PA key to its numeric component (1-12).
     */
    public static final Map<TnKey, Integer> FUNCTION_KEY_INT_LUT;

    static {
        Map<TnKey, Integer> lut = new EnumMap<>(TnKey.class);
        lut.put(TnKey.F1, 1);  lut.put(TnKey.F2, 2);   lut.put(TnKey.F3, 3);
        lut.put(TnKey.F4, 4);  lut.put(TnKey.F5, 5);   lut.put(TnKey.F6, 6);
        lut.put(TnKey.F7, 7);  lut.put(TnKey.F8, 8);   lut.put(TnKey.F9, 9);
        lut.put(TnKey.F10, 10); lut.put(TnKey.F11, 11); lut.put(TnKey.F12, 12);
        lut.put(TnKey.PA1, 1);  lut.put(TnKey.PA2, 2);  lut.put(TnKey.PA3, 3);
        lut.put(TnKey.PA4, 4);  lut.put(TnKey.PA5, 5);  lut.put(TnKey.PA6, 6);
        lut.put(TnKey.PA7, 7);  lut.put(TnKey.PA8, 8);  lut.put(TnKey.PA9, 9);
        lut.put(TnKey.PA10, 10); lut.put(TnKey.PA11, 11); lut.put(TnKey.PA12, 12);
        FUNCTION_KEY_INT_LUT = Collections.unmodifiableMap(lut);
    }

    /** Returns {@code true} if {@code key} is one of F1–F12. */
    public static boolean isFunctionKey(TnKey key) {
        return FUNCTION_KEY_INT_LUT.containsKey(key) &&
               Arrays.asList(FUNCTION_KEYS).contains(key);
    }

    /** Returns {@code true} if {@code key} is one of PA1–PA12. */
    public static boolean isAKey(TnKey key) {
        return Arrays.asList(A_KEYS).contains(key);
    }
}
