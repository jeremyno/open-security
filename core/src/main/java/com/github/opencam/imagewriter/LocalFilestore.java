package com.github.opencam.imagewriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class LocalFilestore implements Filestore {
  String prefix;

  public LocalFilestore(final String prefix) {
    super();
    this.prefix = prefix;
    if (prefix.length() > 0 && !prefix.endsWith("/")) {
      throw new IllegalArgumentException("All prefixes must end with /");
    }
  }

  public void saveFile(final String file, final InputStream stream) {
    final String effective = prefix + file;

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(new File(effective));
      IOUtils.copy(stream, fos);
      stream.close();
    } catch (final Exception e) {
      throw new RuntimeException("Problem saving file:effective", e);
    } finally {
      try {
        fos.close();
      } catch (final IOException e) {
        // nothing
      }
    }

  }

  public void deleteFile(final String file) {
    final String effective = prefix + file;

    final File f = new File(effective);
    if (!f.delete()) {
      throw new RuntimeException("Problem deleting " + file + " (" + effective + ")");
    }
  }
}
