package com.github.opencam.devices;

import java.util.Collection;
import java.util.List;

import com.github.opencam.imagegrabber.ImageSource;
import com.github.opencam.security.SecurityDevice;

public interface LogicalDeviceRead {
  Collection<ImageSource> getImageSources();

  Collection<SecurityDevice> getDevices();

  Collection<String> getNamedDevices();

  public List<String> getStatus(String name);
}
