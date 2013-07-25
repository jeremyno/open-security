package com.github.opencam.process;

public class ProcessImageJob implements Runnable {

  private final String source;
  private final OpenCamController controller;

  public ProcessImageJob(final String source, final OpenCamController controller) {
    this.source = source;
    this.controller = controller;
  }

  public void run() {
    controller.processImage(source);
  }

  @Override
  public String toString() {
    return "ProcessImageJob [source=" + source + "]";
  }
}
