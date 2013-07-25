package com.github.opencam.ui;

import java.util.Map;

public interface SecurityPlugin extends Named {
  public void configure(Map<String, String> config);
}
