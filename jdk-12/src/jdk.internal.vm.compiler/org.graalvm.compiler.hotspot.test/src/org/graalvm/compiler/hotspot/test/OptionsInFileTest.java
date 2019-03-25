/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */


package org.graalvm.compiler.hotspot.test;

import static org.graalvm.compiler.debug.DebugOptions.Dump;
import static org.graalvm.compiler.debug.DebugOptions.MethodFilter;
import static org.graalvm.compiler.debug.DebugOptions.PrintGraph;
import static org.graalvm.compiler.test.SubprocessUtil.getVMCommandLine;
import static org.graalvm.compiler.test.SubprocessUtil.withoutDebuggerArguments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.graalvm.compiler.core.test.GraalCompilerTest;
import org.graalvm.compiler.test.SubprocessUtil;
import org.graalvm.compiler.test.SubprocessUtil.Subprocess;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests reading options from a file specified by the {@code graal.options.file}.
 */
public class OptionsInFileTest extends GraalCompilerTest {
    @Test
    public void test() throws IOException, InterruptedException {
        String methodFilterValue = "a very unlikely method name";
        String debugFilterValue = "a very unlikely debug scope";
        File optionsFile = File.createTempFile("options", ".properties").getAbsoluteFile();
        try {
            Assert.assertFalse(methodFilterValue.equals(MethodFilter.getDefaultValue()));
            Assert.assertFalse(debugFilterValue.equals(Dump.getDefaultValue()));
            Assert.assertTrue(PrintGraph.getDefaultValue());

            try (PrintStream out = new PrintStream(new FileOutputStream(optionsFile))) {
                out.println(MethodFilter.getName() + "=" + methodFilterValue);
                out.println(Dump.getName() + "=" + debugFilterValue);
                out.println(PrintGraph.getName() + " = false");
            }

            List<String> vmArgs = withoutDebuggerArguments(getVMCommandLine());
            vmArgs.removeIf(a -> a.startsWith("-Dgraal."));
            vmArgs.add("-Dgraal.options.file=" + optionsFile);
            vmArgs.add("-XX:+JVMCIPrintProperties");
            Subprocess proc = SubprocessUtil.java(vmArgs);
            String[] expected = {
                            "graal.MethodFilter := \"a very unlikely method name\"",
                            "graal.Dump := \"a very unlikely debug scope\"",
                            "graal.PrintGraph := false"};
            for (String line : proc.output) {
                for (int i = 0; i < expected.length; i++) {
                    if (expected[i] != null && line.contains(expected[i])) {
                        expected[i] = null;
                    }
                }
            }

            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != null) {
                    Assert.fail(String.format("Did not find '%s' in output of command:%n%s", expected[i], proc));
                }
            }
        } finally {
            optionsFile.delete();
        }
    }
}