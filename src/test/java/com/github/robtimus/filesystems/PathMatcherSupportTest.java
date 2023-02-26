/*
 * PathMatcherSupportTest.java
 * Copyright 2016 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.filesystems;

import static com.github.robtimus.junit.support.ThrowableAssertions.assertChainEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class PathMatcherSupportTest {

    @Test
    void testToPattern() {
        assertEquals("^[^/]*$", PathMatcherSupport.toPattern("glob:*").pattern());
        assertEquals("^$", PathMatcherSupport.toPattern("glob:").pattern());
        assertEquals(".*", PathMatcherSupport.toPattern("regex:.*").pattern());
        assertEquals("", PathMatcherSupport.toPattern("regex:").pattern());
    }

    @Test
    void testToPatternNoSyntax() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> PathMatcherSupport.toPattern(".*"));
        assertChainEquals(Messages.pathMatcher().syntaxNotFound(".*"), exception);
    }

    @Test
    void testToPatternInvalidSyntax() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> PathMatcherSupport.toPattern("java:.*"));
        assertChainEquals(Messages.pathMatcher().unsupportedPathMatcherSyntax("java"), exception);
    }

    @Test
    void testToGlobPattern() {
        // examples taken from FileSystem.getPathMatcher javadoc
        testToGlobPattern("*.java", "^[^/]*\\.java$");
        testToGlobPattern("*.*", "^[^/]*\\.[^/]*$");
        testToGlobPattern("*.{java,class}", "^[^/]*\\.(?:(?:java)|(?:class))$");
        testToGlobPattern("foo.?", "^foo\\.[^/]$");
        testToGlobPattern("/home/*/*", "^/home/[^/]*/[^/]*$", "/home/gus/data");
        testToGlobPattern("/home/**", "^/home/.*$", "/home/gus", "/home/gus/data");
        testToGlobPattern("C:\\\\*", "^C:\\\\[^/]*$", "C:\\foo", "C:\\bar");

        // more complex
        // + does not mean anything in globs
        testToGlobPattern("{[a-z]+,[1-3]+}", "^(?:(?:[[a-z]&&[^/]]\\+)|(?:[[1-3]&&[^/]]\\+))$");
        testToGlobPattern("[!a-z][^a-z][!-a-z]", "^[[^a-z]&&[^/]][[\\^a-z]&&[^/]][[^-a-z]&&[^/]]$", "1^1", "1a1");

        // failures
        testInvalidGlobPattern("*.{java,class", 13);
        testInvalidGlobPattern("*.{java,{class}}", 8);
        testInvalidGlobPattern("*.java}", 6);
        testInvalidGlobPattern("[a-z/1-3]", 4);
        testInvalidGlobPattern("[a-z[1-3]", 4);
        testInvalidGlobPattern("[a-z", 4);
        testInvalidGlobPattern("foo]", 3);
        testInvalidGlobPattern("*.\\foo", 2);
    }

    private void testToGlobPattern(String glob, String regex, String... examples) {
        Pattern pattern = PathMatcherSupport.toGlobPattern(glob);
        assertEquals(regex, pattern.pattern());

        for (String example : examples) {
            assertTrue(pattern.matcher(example).matches(), example + " must match glob " + glob);
        }
    }

    private void testInvalidGlobPattern(String glob, int index) {
        PatternSyntaxException exception = assertThrows(PatternSyntaxException.class, () -> PathMatcherSupport.toGlobPattern(glob));
        assertEquals(glob, exception.getPattern());
        assertEquals(index, exception.getIndex());
    }
}
