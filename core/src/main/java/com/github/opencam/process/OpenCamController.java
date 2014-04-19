package com.github.opencam.process;

import java.util.List;

import com.github.opencam.imagegrabber.Resource;

public interface OpenCamController {
  public void stop();

  public void start();

  public Resource getLastImage(String name);

  public List<String> getCameraNames();
}
