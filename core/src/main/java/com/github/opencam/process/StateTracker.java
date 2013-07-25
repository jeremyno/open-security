package com.github.opencam.process;

import java.util.List;

import org.apache.commons.lang3.ObjectUtils;

import com.github.opencam.email.SendEmail;
import com.github.opencam.security.AlarmStatus;

public class StateTracker implements Runnable {
  OpenCamController proc;
  AlarmStatus lastStatus = null;
  private final SendEmail emailer;
  private final List<String> alertEmailRecipients;

  public StateTracker(final OpenCamController proc, final SendEmail emailer, final List<String> shortEmail) {
    super();
    this.proc = proc;
    this.emailer = emailer;
    this.alertEmailRecipients = shortEmail;
  }

  public void run() {
    final AlarmStatus newStatus = proc.getCurrentStatus();
    if (!ObjectUtils.equals(newStatus, lastStatus)) {
      String message = "Changed state from " + lastStatus + " to " + newStatus + "\n";
      for (final SecurityDeviceStatus i : proc.getDeviceStatus()) {
        message += i + "\n";
      }

      proc.handleStatus(lastStatus, newStatus);
      System.out.println(message);
      lastStatus = newStatus;
    }
  }
}
