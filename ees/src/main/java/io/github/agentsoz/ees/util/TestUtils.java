package io.github.agentsoz.ees.util;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 EES code contributors.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.matsim.core.utils.misc.Time.parseTime;

public class TestUtils {

    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

    public void compareLineByLine(String actualFile, String expectedFile, String expression, int slack) {
        try {
            List<String> expectedLines = getMatchingLines(expectedFile, expression);
            List<String> actualLines = getMatchingLines(actualFile, expression);
            java.util.Collections.sort(expectedLines);
            java.util.Collections.sort(actualLines);
            log.info("\n===< comparing >===\n"
                    + "expected: " + expectedFile + ", " + expectedLines.size() + " lines\n"
                    + "actual  : " + actualFile + ", " + actualLines.size() + " lines");
            for (int i = 0; i < expectedLines.size(); i++) {
                String expected = expectedLines.get(i);
                if (i >= actualLines.size()) {
                    String msg = "\ndiff:\n"
                            + "expected: " + expected + "\n"
                            + "actual  : <EOF>\n";
                    assertTrue(msg, false);
                }
                String actual = actualLines.get(i);
                if (!actual.equals(expected)) {
                    boolean isDifferent = true;
                    String[] exp = expected.split("\\|",3);
                    String[] act = actual.split("\\|",3);
                    // check if all but the first column (time) is the same
                    if (exp[2].equals(act[2])) {
                        // if so, and the time difference is within slack then accept
                        double expTime = Double.valueOf(exp[0]);
                        double actTime = Double.valueOf(act[0]);
                        if ((actTime+slack<=expTime) || (actTime-slack>=expTime)) {
                            String msg = "\naccepting diff (with time slack "+slack+"):\n"
                                    + "expected: " + expected + "\n"
                                    + "actual  : " + actual + "\n";
                            log.info(msg);
                            isDifferent = false;
                        }

                    }
                    if (isDifferent) {
                        String msg = "\ndiff:\n"
                                + "expected: " + expected + "\n"
                                + "actual  : " + actual + "\n";
                        assertTrue(msg, false);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(e.getMessage(), false);
        }
        log.info("\n===< ok >===");

    }

    private List<String> getMatchingLines(String file, String expression) throws IOException {
        Pattern pattern = Pattern.compile(expression);
        Matcher matcher = pattern.matcher("");
        List<String> lines = new ArrayList<>();
        BufferedReader rd = new BufferedReader(new FileReader(file));
        String line = null;
        while ((line = rd.readLine()) != null) {
            matcher.reset(line);
            if (matcher.find()) {
                lines.add(line);
            }
        }
        return lines;
    }
}
