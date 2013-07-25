package com.github.opencam.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.PredicateUtils;

import com.github.opencam.security.AlarmStatus;

public class CleanDisk implements Runnable, Predicate<File> {
  File directory;
  double freeSpacePercent;
  int deleteAndCheck;

  public CleanDisk(final String directory, final double freeSpacePercent, final int deleteAndCheck) {
    super();
    this.directory = new File(directory);
    this.freeSpacePercent = freeSpacePercent;
    this.deleteAndCheck = deleteAndCheck;
  }

  public void run() {
    final List<File> allFiles = new ArrayList<File>(15000);
    addFilesRecursively(directory, allFiles, PredicateUtils.allPredicate(this, new NominalStatusOnly()));
    reverseSortAge(allFiles);
    deleteFilesTillFreeSpaceHit(allFiles);

    final double percentFree = getPercentFree();
    if (percentFree < freeSpacePercent) {
      allFiles.clear();
      addFilesRecursively(directory, allFiles, this);
      reverseSortAge(allFiles);
      deleteFilesTillFreeSpaceHit(allFiles);
    }
  }

  private void reverseSortAge(final List<File> allFiles) {
    Collections.sort(allFiles, new Comparator<File>() {
      public int compare(final File o1, final File o2) {
        return (int) (o1.lastModified() - o2.lastModified());
      }
    });
  }

  private double getPercentFree() {
    final double total = directory.getTotalSpace();
    final double free = directory.getFreeSpace();
    return free / total;
  }

  private void deleteFilesTillFreeSpaceHit(final List<File> files) {
    int i = 0;

    if (getPercentFree() < freeSpacePercent) {
      for (final File f : files) {
        if (i >= deleteAndCheck) {
          i = 0;
          if (getPercentFree() > freeSpacePercent) {
            return;
          }
        }

        f.delete();
        i++;
      }
    }
  }

  private void addFilesRecursively(final File dir, final List<File> files, final Predicate<File> acceptFile) {
    for (final File f : dir.listFiles()) {
      if (f.isDirectory()) {
        addFilesRecursively(f, files, acceptFile);
      } else {
        if (acceptFile.evaluate(f)) {
          files.add(f);
        }
      }
    }
  }

  public boolean evaluate(final File arg0) {
    return arg0.getPath().endsWith(".jpg");
  }

  private class NominalStatusOnly implements Predicate<File> {
    public boolean evaluate(final File arg0) {
      return arg0.getPath().contains(AlarmStatus.Nominal.toString());
    }
  }
}
