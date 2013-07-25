package com.github.opencam.processing;

import java.awt.image.BufferedImage;

public interface ImageDiff<T> {
  T getImageMeta(BufferedImage img);

  public int getImageDiff(T img1, T img2, BufferedImage img, int thresh);
}
