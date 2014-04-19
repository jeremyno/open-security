package com.github.opencam.process;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SystemStatusWriter {
  PoolingOpenCamController controller;

  public SystemStatusWriter(final PoolingOpenCamController controller) {
    super();
    this.controller = controller;
  }

  public void writeImageStatus(final PrintWriter out) {
    out.println("<ul>");
    long lastTimestamp = 0;
    long firstTimestamp = Long.MAX_VALUE;
    for (final SecurityDeviceStatus i : controller.getDeviceStatus()) {
      out.println("<li>" + i + "</li>");
      final long timestamp = i.getChekinTimestamp();
      if (lastTimestamp < timestamp) {
        lastTimestamp = timestamp;
      }

      if (firstTimestamp > timestamp) {
        firstTimestamp = timestamp;
      }
    }

    out.println();
    out.println("<li>Processing is running " + (float) (System.currentTimeMillis() - lastTimestamp) / 1000 + " to " + (float) (System.currentTimeMillis() - firstTimestamp) / 1000 + " seconds behind.</li>");
    out.println("<li>Current System time is " + new java.util.Date() + "</li>");
    out.println("</ul>");
  }

  public static void writeStatus(final PoolingOpenCamController control, final PrintWriter writer) {
    final SystemStatusWriter i = new SystemStatusWriter(control);
    i.writeImageStatus(writer);
  }

  public static String getStatus(final PoolingOpenCamController control) {
    final StringWriter out = new StringWriter();
    writeStatus(control, new PrintWriter(out));
    return out.toString();
  }
}
