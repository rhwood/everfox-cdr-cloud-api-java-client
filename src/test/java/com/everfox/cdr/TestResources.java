/*
 * Copyright 2026 Everfox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.everfox.cdr;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Loads the sample files shared by the tests of every API, which are in {@code src/test/resources/com/everfox/cdr}.
 *
 * <p>Most were saved from Microsoft Office. {@code test.odt} was generated with macOS {@code textutil}.
 */
public final class TestResources {

    /** Not instantiable. */
    private TestResources() {}

    /**
     * Loads a sample file.
     *
     * @param name the file name, for example {@code test.pdf}
     * @return the contents of the file
     * @throws IllegalStateException if the file is not found
     * @throws UncheckedIOException if the file cannot be read
     */
    public static byte[] load(String name) {
        try (InputStream in = TestResources.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Test resource not found: " + name);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read test resource: " + name, e);
        }
    }
}
