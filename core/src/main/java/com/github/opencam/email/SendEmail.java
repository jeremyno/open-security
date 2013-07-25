package com.github.opencam.email;

import java.util.List;

import com.github.opencam.imagegrabber.Resource;

public interface SendEmail {
  public void sendAlertEmail(List<String> recipients, String subject, final String message, final List<Resource> images);
}
