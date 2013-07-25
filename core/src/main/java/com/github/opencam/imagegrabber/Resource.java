package com.github.opencam.imagegrabber;

import java.util.List;

import com.github.opencam.security.AlarmStatus;

public interface Resource {
  String getMimeType();

  String getFileName();

  byte[] getData();

  long getTimestamp();

  String getSourceName();

  AlarmStatus getSourceStatus();

  List<String> getNotes();

  void addNotes(String notes);
}
