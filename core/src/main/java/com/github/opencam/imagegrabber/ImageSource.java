package com.github.opencam.imagegrabber;

import com.github.opencam.security.AlarmStatus;
import com.github.opencam.ui.Named;

public interface ImageSource extends Named {
  Resource getImage();

  public double suggestFrameRate(double suggestion);

  public double getCurrentFramerate();

  public double setFramerate(AlarmStatus status);
}
