package com.github.opencam.imagegrabber;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamUtils {
  public static final int DEFAULT_JPG_BUFFER_SIZE = 69632;

  public static String readLine(final InputStream stream) {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream(DEFAULT_JPG_BUFFER_SIZE);
    int in;

    try {
      while ((in = stream.read()) >= 0) {
        bos.write(in);
        if (in == '\n') {
          break;
        }
      }

      final String readLine = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bos.toByteArray()))).readLine();
      return readLine;
    } catch (final IOException e) {
      throw new RuntimeException("Error reading line", e);
    }
  }
}
