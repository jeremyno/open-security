package com.github.opencam.process;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.opencam.imagegrabber.ImageSource;
import com.github.opencam.imagegrabber.Resource;
import com.github.opencam.util.ExceptionUtils;
import com.github.opencam.util.ThreadUtils;

public class ClosedLoopImageThread extends Thread {
  ImageSource src;
  Archiver archiver;
  Resource lastResource;

  long lastTime;
  long lastStart;
  long targetTime;
  long lastWait;

  boolean run = true;

  Logger log = Logger.getLogger(getClass().getCanonicalName());

  public ClosedLoopImageThread(final ImageSource src, final Archiver archiver, final long targetTime) {
    super();
    this.src = src;
    this.archiver = archiver;
    this.targetTime = targetTime;
  }

  @Override
  public void run() {
    while (run) {
      try {
        lastResource = src.getImage();

        if (lastResource != null) {
          archiver.processImage(lastResource);
          lastTime = System.currentTimeMillis() - lastStart;
          lastStart = System.currentTimeMillis();
          lastResource.addNotes("Last process time: " + lastTime + " ms");
        }
        lastWait = targetTime - lastTime;

        lastWait = Math.max(lastWait, 100);

        Thread.sleep(lastWait);
      } catch (final Exception e) {
        log.log(Level.WARNING, "Problem getting image for " + src.getName(), e);
        ThreadUtils.sleep(5000);
        if (ExceptionUtils.isMemoryException(e)) {
          ThreadUtils.sleep(5000);
        }
      }
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

  public long getLastWait() {
    return lastWait;
  }
}
