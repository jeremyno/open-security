package com.github.opencam.util;

public class ThreadUtils {
  public static void sleep(final long millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
