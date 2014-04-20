package com.github.opencam.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.conn.PoolingClientConnectionManager;

import com.github.opencam.devices.HttpUser;
import com.github.opencam.imagegrabber.ImageSource;
import com.github.opencam.imagegrabber.Resource;
import com.github.opencam.util.ApplicationProperties;
import com.github.opencam.util.ClassLoaderUtils;

public class ClosedLoopArchivingController implements OpenCamController {
  SystemConfiguration config;

  List<ClosedLoopImageThread> threads = new ArrayList<ClosedLoopImageThread>();
  Map<String, ClosedLoopImageThread> camMap = new HashMap<String, ClosedLoopImageThread>();
  List<String> names;
  ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();

  public ClosedLoopArchivingController(final SystemConfiguration config) {
    super();
    this.config = config;

    final PoolingClientConnectionManager cm = new PoolingClientConnectionManager();

    final Archiver archive = buildArchiver();

    for (final Entry<String, Map<String, String>> camConfig : config.getCamConfigs().entrySet()) {
      final Map<String, String> configs = camConfig.getValue();
      final ApplicationProperties camConfigAP = new ApplicationProperties(configs);
      final String name = camConfig.getKey();
      final ImageSource plugin = ClassLoaderUtils.loadObject(configs.get("class"), camConfigAP, name, cm);

      if (plugin instanceof HttpUser) {
        final HttpUser httpUser = (HttpUser) plugin;
        httpUser.setConnectionManager(cm);
      }

      final long waitTime = (long) (1000 / plugin.getCurrentFramerate());
      final ClosedLoopImageThread thread = new ClosedLoopImageThread(plugin, archive, waitTime);
      camMap.put(name, thread);
      threads.add(thread);
    }

    names = new ArrayList<String>(config.getCamConfigs().keySet());
    pool.scheduleAtFixedRate(new ArchiveJob(archive), config.getInitialDelay(), config.getMaxUploadDelay(), TimeUnit.MILLISECONDS);
  }

  public Archiver buildArchiver() {
    final Archiver archiver = config.getNewArchiver();

    // local could go here and federate

    return archiver;
  }

  public void stop() {
    for (final ClosedLoopImageThread i : threads) {
      i.doAllDone();
    }
  }

  public void start() {
    for (final ClosedLoopImageThread i : threads) {
      i.start();
    }
  }

  public Resource getLastImage(final String name) {
    final ClosedLoopImageThread thread = camMap.get(name);

    if (thread != null) {
      return thread.getLastResource();
    }

    return null;
  }

  public List<String> getCameraNames() {
    return names;
  }

  public boolean isSystemArmed() {
    return false;
  }

  public String getStatusString() {
    return "Operating";
  }

  public Collection<SecurityDeviceStatus> getDeviceStatus() {
    return new ArrayList<SecurityDeviceStatus>();
  }

  public void setSystemArmed(final boolean armed, final String who) {
    // we don't care
  }
}
