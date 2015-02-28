package com.github.opencam.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

public class ProcessUtils {
  public static String exec(final String... cmdarray) {
    final StringBuilder out = new StringBuilder();
    try {
      final Process p = Runtime.getRuntime().exec(cmdarray);
      final BufferedReader rd = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;

      while ((line = rd.readLine()) != null) {
        out.append(line).append("\n");
      }
    } catch (final Exception e) {
      throw new RuntimeException("Unable to execute " + Arrays.toString(cmdarray), e);
    }

    return out.toString();
  }
}
