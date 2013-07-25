package com.github.opencam.processing;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import com.github.opencam.imagegrabber.Resource;
import com.github.opencam.ui.Configurable;
import com.github.opencam.util.ApplicationProperties;

@SuppressWarnings("rawtypes")
public class StatefulDiffer implements Configurable {
  ImageDiff differ;
  Object lastDiff;
  int thresh;
  BufferedImage img;
  Resource lastResource = null;

  public StatefulDiffer(final ImageDiff differ) {
    super();
    this.differ = differ;
  }

  public int getImageDiff(final Resource original) {
    if (lastResource == original) {
      return 0;
    }

    final ByteArrayInputStream bis = new ByteArrayInputStream(original.getData());
    BufferedImage img;
    try {
      img = ImageIO.read(bis);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    IOUtils.closeQuietly(bis);

    final Object meta = differ.getImageMeta(img);

    int out = 0;
    if (lastDiff == null) {
      lastDiff = meta;
    } else {
      out = differ.getImageDiff(lastDiff, meta, img, thresh);
      lastDiff = meta;
    }

    original.addNotes("ImageDIff: " + out);
    original.addNotes("Last Checkin: " + new Date(original.getTimestamp()));

    this.img = img;
    this.lastResource = original;
    return out;
  }

  public int getThresh() {
    return thresh;
  }

  public BufferedImage getLastImage() {
    return img;
  }

  public void configure(final ApplicationProperties props) {
    thresh = props.getInteger("threshold");
  }
}
