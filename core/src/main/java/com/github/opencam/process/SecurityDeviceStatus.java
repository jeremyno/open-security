package com.github.opencam.process;

import java.util.List;

import com.github.opencam.security.AlarmStatus;
import com.github.opencam.security.SecurityDevice;

public class SecurityDeviceStatus {
  long chekinTimestamp;
  SecurityDevice device;
  AlarmStatus status;
  private final List<String> notes;

  public SecurityDeviceStatus(final long chekinTimestamp, final SecurityDevice device, final AlarmStatus status, final List<String> notes) {
    super();
    this.chekinTimestamp = chekinTimestamp;
    this.device = device;
    this.status = status;
    this.notes = notes;
  }

  public AlarmStatus getStatus() {
    return status;
  }

  public long getChekinTimestamp() {
    return chekinTimestamp;
  }

  public SecurityDevice getDevice() {
    return device;
  }

  @Override
  public String toString() {
    return device + " = " + status + " " + notes;
  }

  public List<String> getNotes() {
    return notes;
  }
}
