package com.github.opencam.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.github.opencam.imagegrabber.Resource;

public class LocalCacheArchiver implements Archiver {
  String localcache;
  final Logger log = Logger.getLogger(getClass().getCanonicalName());

  public LocalCacheArchiver(final String localcache) {
    super();
    this.localcache = localcache;
  }

  public void processImage(final Resource resource) {
    final String name = resource.getSourceName();
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_kkmmss");
    final String base = sdf.format(new Date(resource.getTimestamp()));
    String filePath = "/" + name + "/" + base;

    filePath += "_" + resource.getSourceStatus() + ".jpg";

    final String localFile = localcache + filePath;
    log.fine("Saving to " + localFile);
    try {
      FileUtils.forceMkdir(new File(localcache + "/" + name));
      IOUtils.write(resource.getData(), new FileOutputStream(localFile));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void doUpload() {

  }

}
