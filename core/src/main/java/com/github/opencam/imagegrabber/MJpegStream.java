package com.github.opencam.imagegrabber;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

public class MJpegStream {
  private final char[] boundry;
  private final char[] end;
  private final InputStream stream;

  public MJpegStream(final String boundry, final InputStream stream) {
    super();
    this.boundry = ("--" + boundry).toCharArray();
    this.end = ("--" + boundry + "--").toCharArray();
    this.stream = new BufferedInputStream(stream, StreamUtils.DEFAULT_JPG_BUFFER_SIZE);
  }

  private boolean matches(final char[] boundryTrack, final int boundryI, final char[] carr) {
    int start = boundryI - carr.length;

    if (start < 0) {
      start += boundryTrack.length;
    }

    for (int i = 0; i < carr.length; i++) {
      if (carr[i] != boundryTrack[(start + i) % boundryTrack.length]) {
        return false;
      }
    }

    return true;
  }

  @SuppressWarnings("resource")
  public Resource getNextEntity() {
    final ByteArrayOutputStream outstream = new ByteArrayOutputStream(StreamUtils.DEFAULT_JPG_BUFFER_SIZE);

    int boundryI = 0;
    final char[] boundryTrack = new char[end.length];

    int in;

    try {
      while ((in = stream.read()) >= 0) {
        boundryI = (boundryI + 1) % boundryTrack.length;
        boundryTrack[boundryI] = (char) in;
        outstream.write(in);

        if (matches(boundryTrack, boundryI, this.boundry)) {
          final Resource processStream = processStream(outstream, boundry.length);
          IOUtils.closeQuietly(outstream);
          return processStream;
        }

        if (matches(boundryTrack, boundryI, this.end)) {
          final Resource out = processStream(outstream, end.length);
          IOUtils.closeQuietly(stream);
          IOUtils.closeQuietly(outstream);
          return out;
        }
      }
    } catch (final IOException e) {
      throw new RuntimeException("Problem reading stream", e);
    }

    return null;
  }

  private Resource processStream(final ByteArrayOutputStream outstream, final int matchLength) {
    byte[] arr = outstream.toByteArray();
    arr = Arrays.copyOf(arr, arr.length - matchLength);
    final ByteArrayInputStream read = new ByteArrayInputStream(arr);

    String r;
    boolean firstLine = true;
    String contentType = null;

    while ((r = StreamUtils.readLine(read)) != null) {
      if (!firstLine && r.length() == 0) {
        break;
      } else if (r.startsWith("Content-Type: ")) {
        contentType = r.substring(14);
      }

      firstLine = false;
    }

    final ByteArrayOutputStream body = new ByteArrayOutputStream(arr.length);
    try {
      IOUtils.copy(read, body);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

    byte[] bodyArr = body.toByteArray();
    if (bodyArr.length > 2 && bodyArr[bodyArr.length - 2] == '\r' && bodyArr[bodyArr.length - 1] == '\n') {
      bodyArr = Arrays.copyOf(bodyArr, bodyArr.length - 2);
    }

    return new BeanResource(contentType, bodyArr, "image.jpg", "", System.currentTimeMillis(), null);
  }

  public boolean available() {
    try {
      return stream.available() > 0;
    } catch (final IOException e) {
      throw new RuntimeException();
    }
  }
}
