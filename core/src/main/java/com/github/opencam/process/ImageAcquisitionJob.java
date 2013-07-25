package com.github.opencam.process;

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
  OpenCamController controller;

  public ImageAcquisitionJob(final ImageSource src, final String localcache, final OpenCamController controller) {
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
      final Resource image = src.getImage();
      controller.processRawImage(name, image);

      lastImage = image;
    } catch (final Exception e) {
      log.log(Level.FINE, "Problem reading from image stream.", e);
    }
  }

  public Resource getLastImage() {
    return lastImage;
  }

}
