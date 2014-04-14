package com.github.opencam.processing;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class NoDiffer implements ImageDiff<List<Double>> {

  public List<Double> getImageMeta(final BufferedImage img) {
    return Arrays.asList(0.0);
  }

  public int getImageDiff(final List<Double> img1, final List<Double> img2, final BufferedImage img, final int thresh) {
    return 0;
  }

}
