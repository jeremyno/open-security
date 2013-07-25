package com.github.opencam.imagegrabber;

import java.util.ArrayList;
import java.util.List;

import com.github.opencam.security.AlarmStatus;

public class BeanResource implements Resource {
  String mimeType;
  byte[] data;
  String filename;
  String sourceName;
  long timestamp;
  AlarmStatus status;
  private final List<String> notes = new ArrayList<String>();

  public BeanResource(final String mimeType, final byte[] data, final String filename, final String sourceName, final long timestamp, final AlarmStatus status) {
    super();
    this.mimeType = mimeType;
    this.data = data;
    this.filename = filename;
    this.sourceName = sourceName;
    this.timestamp = timestamp;
    this.status = status;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getSourceName() {
    return sourceName;
  }

  public String getMimeType() {
    return mimeType;
  }

  public byte[] getData() {
    return data;
  }

  public String getFileName() {
    return filename;
  }

  public AlarmStatus getSourceStatus() {
    return status;
  }

  public List<String> getNotes() {
    return notes;
  }

  public void addNotes(final String note) {
    notes.add(note);
  }
}
