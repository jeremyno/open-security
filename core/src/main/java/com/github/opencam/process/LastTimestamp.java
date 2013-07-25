package com.github.opencam.process;

public class LastTimestamp {
  long lastCheckin = 0;
  String who;

  public long getLatestCheckin() {
    return lastCheckin;
  }

  public long getMillisSinceLastCheckin() {
    final long check = lastCheckin;
    if (check == 0) {
      return -1;
    }
    return System.currentTimeMillis() - check;
  }

  public void doCheckin(final String who) {
    final long myTime = System.currentTimeMillis();

    if (myTime > lastCheckin) {
      synchronized (this) {
        lastCheckin = myTime;
        this.who = who;
      }
    }
  }

  public String getWho() {
    return who;
  }
}
