package com.github.opencam.process;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.opencam.imagegrabber.ImageSource;
import com.github.opencam.imagegrabber.Resource;
import com.github.opencam.security.SecurityDevice;

public class ImageAcquisitionJob implements Runnable {
  private final ImageSource src;
  SecurityDevice d = null;
  String name;
  final Logger log = Logger.getLogger(getClass().getCanonicalName());
  String localcache;
  Resource lastImage;
  PoolingOpenCamController controller;

  public ImageAcquisitionJob(final ImageSource src, final String localcache, final PoolingOpenCamController controller) {
    this.src = src;
    this.localcache = localcache;
    name = src.getName();
    if (src instanceof SecurityDevice) {
      d = (SecurityDevice) src;
    }
    this.controller = controller;
  }

  public void run() {
    try {
      final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      final Date before = new Date();
      final long start = System.currentTimeMillis();
      final Resource image = src.getImage();
      final Date after = new Date();
      image.addNotes("Acquired between " + sdf.format(before) + " & " + sdf.format(after));
      controller.processRawImage(name, image);
      final long took = System.currentTimeMillis() - start;
      image.addNotes("Processing " + took + "ms.");

      lastImage = image;
    } catch (final Exception e) {
      log.log(Level.FINE, "Problem reading from image stream.", e);
    }
  }

  public Resource getLastImage() {
    return lastImage;
  }

  @Override
  public String toString() {
    return "ImageAquisitionJob [source=" + d + "]";
  }
}
