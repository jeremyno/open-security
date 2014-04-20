package com.github.opencam.process;

import java.text.SimpleDateFormat;
import java.util.Date;
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
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        final Date before = new Date();
        final Resource r = src.getImage();
        final Date after = new Date();
        if (r != null) {
          lastResource = r;
          archiver.processImage(lastResource);
          lastTime = System.currentTimeMillis() - lastStart;
          lastStart = System.currentTimeMillis();
          lastWait = targetTime - lastTime;
          lastWait = Math.max(lastWait, 100);
          lastResource.addNotes("Processing: " + lastTime + "ms, Sleep: " + lastWait + "ms");
          lastResource.addNotes("Acquired between " + sdf.format(before) + " & " + sdf.format(after));
        }

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
