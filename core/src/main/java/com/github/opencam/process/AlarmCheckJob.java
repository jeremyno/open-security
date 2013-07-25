package com.github.opencam.process;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ObjectUtils;

import com.github.opencam.security.AlarmStatus;
import com.github.opencam.security.SecurityDevice;
import com.github.opencam.ui.Named;

public class AlarmCheckJob implements Runnable {
  SecurityDevice device;
  LastTimestamp alarmStamp;
  LastTimestamp disconnectStamp;
  Long firstDisconnect;
  AlarmStatus lastStatus;
  Logger log = Logger.getLogger(getClass().getCanonicalName());
  final long permenentDisconnectDelay;
  long lastCheckin = 0;
  private String name;
  private final OpenCamController controller;

  public AlarmCheckJob(final SecurityDevice device, final LastTimestamp alarmStamp, final LastTimestamp disconnectStamp, final long permanetDisconnectDelay, final OpenCamController controller) {
    super();
    this.device = device;
    this.alarmStamp = alarmStamp;
    this.disconnectStamp = disconnectStamp;
    this.permenentDisconnectDelay = permanetDisconnectDelay;
    this.controller = controller;
    if (device instanceof Named) {
      final Named named = (Named) device;
      name = named.getName();
    }
  }

  public void run() {
    try {
      AlarmStatus status = device.getAlarmStatus(false);
      lastCheckin = System.currentTimeMillis();

      if (ObjectUtils.notEqual(lastStatus, status)) {
        if (status.equals(AlarmStatus.NotConnected)) {
          firstDisconnect = System.currentTimeMillis();
        }

        lastStatus = status;
      }

      if (status.equals(AlarmStatus.NotConnected) && permenentDisconnectDelay < System.currentTimeMillis() - firstDisconnect) {
        status = AlarmStatus.PermanentlyDisconnected;
      }

      final String name = this.name != null ? this.name : device.toString();
      switch (status) {
      case AlarmDetected:
        alarmStamp.doCheckin(name);
        break;
      case NotConnected:
        disconnectStamp.doCheckin(name);
        break;
      default:
        // nothing
      }
      controller.processAlarmMessage(name, status, System.currentTimeMillis(), Arrays.asList("Checkin"));
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Problem checking on " + device + " status", e);
    }
  }

  // public AlarmStatus getLastStatus() {
  // return lastStatus;
  // }
  //
  // public long getLastCheckin() {
  // return System.currentTimeMillis();
  // }

  public SecurityDevice getDevice() {
    return this.device;
  }
}
