package com.github.opencam.util;

import java.lang.Thread.State;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ThreadUtils {
  public static void sleep(final long millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<State, Map<Thread, StackTraceElement[]>> getAllStackTracesByState() {
    final Map<Thread, StackTraceElement[]> t = Thread.getAllStackTraces();

    final Map<State, Map<Thread, StackTraceElement[]>> running = new HashMap<Thread.State, Map<Thread, StackTraceElement[]>>();

    for (final Entry<Thread, StackTraceElement[]> i : t.entrySet()) {
      final State state = i.getKey().getState();
      if (running.get(state) == null) {
        running.put(state, new HashMap<Thread, StackTraceElement[]>());
      }
      StackTraceElement[] stackTrace = i.getValue();
      if (stackTrace == null) {
        stackTrace = new StackTraceElement[0];
      }

      running.get(state).put(i.getKey(), stackTrace);
    }

    return running;
  }

}
