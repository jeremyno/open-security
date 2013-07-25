package com.github.opencam.process;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingRunnable implements Runnable {
  Runnable job;

  public LoggingRunnable(final Runnable job) {
    super();
    this.job = job;
  }

  public void run() {
    try {
      job.run();
    } catch (final Exception e) {
      final Logger log = Logger.getLogger(this.getClass().getCanonicalName());
      log.log(Level.WARNING, "Problem running job " + job, e);
    }
  }

  @Override
  public String toString() {
    return job.toString() + " (log wrapped)";
  }
}
