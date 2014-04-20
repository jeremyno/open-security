package com.github.opencam.process;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.opencam.util.ExceptionUtils;

final class ArchiveJob implements Runnable {
  /**
   * 
   */
  Archiver archive;
  Logger log = Logger.getLogger(getClass().getCanonicalName());

  public ArchiveJob(final Archiver archive) {
    super();
    this.archive = archive;
  }

  public void run() {
    try {
      archive.doUpload();
    } catch (final Throwable e) {
      log.log(Level.WARNING, "Unable to do upload", e);
      if (e instanceof Error && !ExceptionUtils.isMemoryException(e)) {
        throw (Error)e;
      }
    }
  }
}