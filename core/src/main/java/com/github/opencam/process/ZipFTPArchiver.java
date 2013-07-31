package com.github.opencam.process;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;

import com.github.opencam.encryption.EncryptionUtils;
import com.github.opencam.imagegrabber.Resource;
import com.github.opencam.imagewriter.Filestore;

public class ZipFTPArchiver implements Archiver {
  Filestore store;
  int trailingEntries = 0;
  LinkedList<String> entries = new LinkedList<String>();
  long lastUpload = 0;
  private ZipFile file;
  private final String password;

  public ZipFTPArchiver(final Filestore store, final int trailingEntries, final String password) {
    super();
    this.store = store;
    this.trailingEntries = trailingEntries;
    this.lastUpload = System.currentTimeMillis();
    this.password = password;
    file = new ZipFile();
  }

  /* (non-Javadoc)
   * @see com.github.opencam.process.Archiver#processImage(com.github.opencam.imagegrabber.Resource)
   */
  public void processImage(final Resource resource) {
    final byte[] data = resource.getData();
    final ZipEntry entry = file.buildEntry(resource.getFileName(), data);
    synchronized (this) {
      file.saveFile(entry, data);
    }
  }

  /* (non-Javadoc)
   * @see com.github.opencam.process.Archiver#doUpload()
   */
  public void doUpload() {
    System.out.println("Uploading");
    final ZipFile oldFile;
    synchronized (this) {
      oldFile = file;
      file = new ZipFile();
      lastUpload = System.currentTimeMillis();
    }

    final StopWatch watch = new StopWatch();
    watch.start();
    byte[] zipFile = oldFile.closeAndReturnBytes();
    watch.stop();
    // TimingUtils.printNanoTime("closeAndReturn", watch);
    if (zipFile == null) {
      return;
    }
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_kkmmss");
    final String base = sdf.format(new Date());

    watch.reset();
    watch.start();
    zipFile = EncryptionUtils.encrypt(zipFile, password);
    watch.stop();
    // TimingUtils.printNanoTime("encryption", watch);
    final InputStream is = new ByteArrayInputStream(zipFile);
    final String file = "upload_" + base + ".zip";
    watch.reset();
    watch.start();
    store.saveFile(file, is);
    watch.stop();
    // TimingUtils.printNanoTime("FTP upload", watch);
    IOUtils.closeQuietly(is);
    System.out.println("done uploading");
  }
}
