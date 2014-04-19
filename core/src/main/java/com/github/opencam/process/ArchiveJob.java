package com.github.opencam.process;

import java.util.logging.Level;
import java.util.logging.Logger;

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
    } catch (final Exception e) {
      log.log(Level.WARNING, "Unable to do upload", e);
    }
  }
}