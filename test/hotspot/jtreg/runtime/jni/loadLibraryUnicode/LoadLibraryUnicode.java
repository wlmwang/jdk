/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, JetBrains s.r.o.. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.Platform;
import jdk.test.lib.Asserts;

public class LoadLibraryUnicode {

    static native int giveANumber();

    private static final String NON_LATIN_PATH_NAME = "ka-\u1889-omega-\u03c9";

    public static void verifySystemLoad() throws Exception {
        String osDependentLibraryFileName = null;
        if (Platform.isLinux()) {
            osDependentLibraryFileName = "libLoadLibraryUnicode.so";
        } else if (Platform.isOSX()) {
            osDependentLibraryFileName = "libLoadLibraryUnicode.dylib";
        } else if (Platform.isWindows()) {
            osDependentLibraryFileName = "LoadLibraryUnicode.dll";
        } else {
            throw new Error("Unsupported OS");
        }

        String testNativePath = LoadLibraryUnicodeTest.getSystemProperty("test.nativepath");
        Path origLibraryPath = Paths.get(testNativePath).resolve(osDependentLibraryFileName);

        Path currentDirPath = Paths.get(".").toAbsolutePath();
        Path newLibraryPath = currentDirPath.resolve(NON_LATIN_PATH_NAME);
        Files.createDirectory(newLibraryPath);
        newLibraryPath = newLibraryPath.resolve(osDependentLibraryFileName);

        System.out.println(String.format("Copying '%s' to '%s'", origLibraryPath, newLibraryPath));
        Files.copy(origLibraryPath, newLibraryPath);

        System.out.println(String.format("Loading '%s'", newLibraryPath));
        System.load(newLibraryPath.toString());

        final int retval = giveANumber();
        Asserts.assertEquals(retval, 42);
    }

    public static void verifyExceptionMessage() throws Exception {
        Path currentDirPath = Paths.get(".").toAbsolutePath();
        Path newLibraryPath = currentDirPath.resolve(NON_LATIN_PATH_NAME).resolve("non-existent-library");

        System.out.println(String.format("Loading '%s'", newLibraryPath));
        try {
            System.load(newLibraryPath.toString());
        } catch(UnsatisfiedLinkError e) {
            // The name of the library may have been corrupted by encoding/decoding it improperly.
            // Verify that it is still the same.
            Asserts.assertTrue(e.getMessage().contains(NON_LATIN_PATH_NAME));
        }
    }

    public static void main(String[] args) throws Exception {
        verifySystemLoad();
        verifyExceptionMessage();
    }
}
