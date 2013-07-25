package com.github.opencam.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import com.github.opencam.security.AlarmStatus;

public class SecurityLog {
  PrintStream out;

  public SecurityLog(final String path) throws FileNotFoundException {
    out = new PrintStream(new FileOutputStream(path), true);
  }

  public synchronized void changeStatus(final AlarmStatus status, final String who) {
    final SimpleDateFormat df = new SimpleDateFormat();
    out.println(df.format(new Date()) + " Alarm Status changed to " + status + " by " + who);
  }

  public synchronized void changeArmed(final boolean armed, final String who) {
    final SimpleDateFormat df = new SimpleDateFormat();
    out.println(df.format(new Date()) + " alarm is " + (armed ? "armed" : "disarmed") + " by " + who);
  }

  public static boolean getLastArmedStatus(final String path) throws FileNotFoundException, IOException {
    boolean armed = false;

    for (final String s : IOUtils.readLines(new FileInputStream(path))) {
      if (s.contains("armed")) {
        armed = !s.contains("disarmed");
      }
    }

    return armed;
  }

  public static String getPath(final String basePath, final boolean exist) {
    String lastFound = null;
    int i = 0;

    while (new File(basePath + i).exists()) {
      lastFound = basePath + i;
      i++;
    }

    if (exist) {
      return lastFound;
    } else {
      return basePath + i;
    }
  }
}
