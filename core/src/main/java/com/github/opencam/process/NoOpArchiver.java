package com.github.opencam.process;

import com.github.opencam.imagegrabber.Resource;

public class NoOpArchiver implements Archiver {
  public void processImage(final Resource resource) {
    // No op
  }

  public void doUpload() {
    // No op
  }
}
