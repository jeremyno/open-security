package com.github.opencam.processing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.github.opencam.ui.Configurable;
import com.github.opencam.util.ApplicationProperties;

public class RgbGrid implements ImageDiff<List<Double>>, Configurable {
  int xwidth;
  int ywidth;
  int step;

  public RgbGrid() {
    super();
  }

  public RgbGrid(final int xwidth, final int ywidth, final int step) {
    super();
    this.xwidth = xwidth;
    this.ywidth = ywidth;
    this.step = step;
  }

  public List<Double> getImageMeta(final BufferedImage img) {
    final int width = img.getWidth();
    final int height = img.getHeight();

    final int xboxes = width / xwidth;
    final int yboxes = height / ywidth;
    final int len = xboxes * yboxes;
    final double[] grid = new double[xboxes * yboxes * 3];
    for (int i = 0; i < width; i += step) {
      final int xbox = i / xwidth;
      final int offset = xbox * yboxes;

      for (int j = 0; j < height; j += step) {
        final int ybox = j / ywidth;
        final int index = offset + ybox;
        final int rgb = img.getRGB(i, j);
        final int red = rgb >> 16 & 0xFF;
        final int green = rgb >> 8 & 0xFF;
        final int blue = rgb & 0xFF;
        grid[index] += red;
        grid[index + len] = green;
        grid[index + 2 * len] = blue;
      }
    }

    final List<Double> out = new ArrayList<Double>(grid.length);

    for (final double element : grid) {
      out.add(element * 10000 / xwidth / ywidth);
    }

    return out;
  }

  public int getImageDiff(final List<Double> img1, final List<Double> img2, final BufferedImage img, final int thresh) {
    double maxDiff = 0;

    final int halfLength = img1.size() / 3;

    Graphics g = null;
    int width = 1;
    if (img != null) {
      g = img.getGraphics();
      g.setColor(Color.red);
      width = img.getHeight() / this.ywidth;
    }

    for (int i = 0; i < img1.size(); i++) {
      final double one = img1.get(i);
      final double two = img2.get(i);

      final double diff = Math.abs(one - two);
      if (diff > maxDiff) {
        maxDiff = diff;
      }

      if (g != null && diff > thresh) {
        final int effective = i >= halfLength ? i - halfLength : i;

        final int x = effective / width;
        final int y = effective % width;
        g.drawRect(x * xwidth, y * ywidth, xwidth, ywidth);
      }
    }

    return (int) Math.round(maxDiff);
  }

  @Override
  public String toString() {
    return "RgbGrid [xwidth=" + xwidth + ", ywidth=" + ywidth + ", step=" + step + "]";
  }

  public void configure(final ApplicationProperties props) {
    xwidth = props.getInteger("x");
    ywidth = props.getInteger("y");
    step = props.getInteger("step");
  }
}
