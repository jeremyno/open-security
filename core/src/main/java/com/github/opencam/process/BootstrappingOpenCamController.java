package com.github.opencam.process;

public class BootstrappingOpenCamController {
  SystemConfiguration config;

  public BootstrappingOpenCamController(final String path) {
    config = new SystemConfiguration(path);
  }

}
