package com.github.opencam.process;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import com.github.opencam.imagegrabber.Resource;
import com.github.opencam.imagegrabber.StreamUtils;

public class ProcessedImageHolder {
  Resource original;
  byte[] processedImage = null;
  BufferedImage processed;

  public ProcessedImageHolder(final Resource original, final BufferedImage processed) {
    super();
    this.original = original;
    this.processed = processed;
  }

  public Resource getOriginal() {
    return original;
  }

  public byte[] getProcessedImage() {
    if (processedImage == null) {
      synchronized (this) {
        if (processedImage == null && processed != null) {
          final ByteArrayOutputStream out = new ByteArrayOutputStream(StreamUtils.DEFAULT_JPG_BUFFER_SIZE);
          try {
            ImageIO.write(processed, "jpeg", out);
            IOUtils.closeQuietly(out);
          } catch (final IOException e) {
            throw new RuntimeException(e);
          }
          processedImage = out.toByteArray();
        }
      }
    }

    return processedImage;
  }

  public BufferedImage getProcessed() {
    return processed;
  }

}
