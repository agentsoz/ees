package io.github.agentsoz.ees.util;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2020 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
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
