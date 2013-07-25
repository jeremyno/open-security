package com.github.opencam.imagewriter;

import java.io.InputStream;

public interface Filestore {
  public void saveFile(String file, InputStream stream);

  public void deleteFile(String file);
}
