package com.github.opencam.process;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import com.github.opencam.imagegrabber.StreamUtils;

public class ZipFile {
  ByteArrayOutputStream stream;
  ZipOutputStream zos;
  int count = 0;

  public ZipFile() {
    stream = new ByteArrayOutputStream(30 * StreamUtils.DEFAULT_JPG_BUFFER_SIZE);
    zos = new ZipOutputStream(stream);
  }

  public ZipEntry buildEntry(final String name, final byte[] data) {
    final ZipEntry entry = new ZipEntry(count + "_" + name);
    entry.setMethod(ZipEntry.STORED);
    entry.setCompressedSize(data.length);
    entry.setSize(data.length);
    final CRC32 crc = new CRC32();
    crc.update(data);
    entry.setCrc(crc.getValue());
    return entry;
  }

  public void saveFile(final ZipEntry entry, final byte[] data) {
    try {
      count++;
      zos.putNextEntry(entry);
      zos.write(data);
      zos.closeEntry();
    } catch (final IOException e) {
      throw new RuntimeException("Problem writing to zip file", e);
    }
  }

  public byte[] closeAndReturnBytes() {
    if (count == 0) {
      return null;
    }

    IOUtils.closeQuietly(zos);
    zos = null;
    IOUtils.closeQuietly(stream);
    final byte[] out = stream.toByteArray();
    stream = null;
    return out;
  }
}
