package com.github.opencam.util;

import java.net.ConnectException;

public class ExceptionUtils {
  public static boolean isConnectionException(final Throwable e) {
    if (e != null && (e instanceof ConnectException || isConnectionException(e.getCause()))) {
      return true;
    }

    return false;
  }

  public static boolean isMemoryException(final Throwable e) {
    if (e != null && (e instanceof OutOfMemoryError || isMemoryException(e.getCause()))) {
      return true;
    }

    return false;
  }
}
