package io.github.agentsoz.ees.util;

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

public class TestUtils {


    public void compareLineByLine(String actualFile, String expectedFile, String expression) {
        try {
            List<String> expectedLines = getMatchingLines(expectedFile, expression);
            List<String> actualLines = getMatchingLines(actualFile, expression);
            for (int i = 0; i < expectedLines.size(); i++) {
                String expected = expectedLines.get(i);
                if (i >= actualLines.size()) {
                    String msg = "\nmatching lines:"
                            + " expected:" + expectedLines.size()
                            + " actual:" + actualLines.size() + "\n"
                            + "diff:\n"
                            + "expected: " + expected + "\n"
                            + "actual  : <EOF>\n";
                    assertTrue(msg, false);
                }
                String actual = actualLines.get(i);
                if (!actual.equals(expected)) {
                    String msg = "\ndiff:\n"
                            + "expected: " + expected + "\n"
                            + "actual  : " + actual + "\n";
                    assertTrue(msg, false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(e.getMessage(), false);
        }
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
