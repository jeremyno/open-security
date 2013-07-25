package com.github.opencam.security;

public interface SecurityDevice {
  AlarmStatus getAlarmStatus(boolean useCache);
}
