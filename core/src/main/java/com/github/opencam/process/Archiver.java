package com.github.opencam.process;

import com.github.opencam.imagegrabber.Resource;

public interface Archiver {

  public abstract void processImage(Resource resource);

  public abstract void doUpload();

}