package com.github.opencam.process;

import java.util.ArrayList;
import java.util.List;

import com.github.opencam.security.AlarmStatus;

public class StatusEvent {
  long timestamp;
  AlarmStatus status;
  List<String> notes = new ArrayList<String>();

  public StatusEvent(final long timestamp, final AlarmStatus status, final List<String> notes) {
    super();
    this.timestamp = timestamp;
    this.status = status;
    this.notes = notes;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public AlarmStatus getStatus() {
    return status;
  }

  public List<String> getNotes() {
    return notes;
  }

}
