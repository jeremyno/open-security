package com.github.opencam.process;

import com.github.opencam.imagegrabber.ImageSource;
import com.github.opencam.imagegrabber.Resource;

public class ClosedLoopImageThread extends Thread {
  ImageSource src;
  Archiver archiver;
  Resource lastResource;

  long lastTime;
  long lastStart;

  boolean run = true;

  public ClosedLoopImageThread(final ImageSource src, final Archiver archiver) {
    super();
    this.src = src;
    this.archiver = archiver;
  }

  @Override
  public void run() {
    while (run) {
      lastResource = src.getImage();
      archiver.processImage(lastResource);
      lastTime = System.currentTimeMillis() - lastStart;
      lastStart = System.currentTimeMillis();
      lastResource.addNotes("Last process time: " + lastTime + " ms");
    }
  }

  public void doAllDone() {
    run = false;
  }

  public Resource getLastResource() {
    return lastResource;
  }

  public long getLastProcessTime() {
    return lastTime;
  }

  public ImageSource getSrc() {
    return src;
  }
}
