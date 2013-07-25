package com.github.opencam.util;

import org.apache.commons.lang3.time.StopWatch;

public class TimingUtils {
  public static void printNanoTime(final String task, final long nanos) {
    System.out.println(task + " took " + (float) nanos / 1000000 + "ms.");
  }

  public static void printNanoTime(final String task, final StopWatch watch) {
    System.out.println(task + " took " + (float) watch.getNanoTime() / 1000000 + "ms.");
  }

}
