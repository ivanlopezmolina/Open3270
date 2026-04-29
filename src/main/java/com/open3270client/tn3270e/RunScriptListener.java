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

/**
 * Callback invoked when the library needs to run a script at a given location.
 * Equivalent to the C# {@code RunScriptDelegate}.
 */
@FunctionalInterface
public interface RunScriptListener {
    void runScript(String where);
}
