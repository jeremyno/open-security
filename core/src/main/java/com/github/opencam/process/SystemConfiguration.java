package com.github.opencam.process;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.opencam.imagewriter.FTPFileSink;
import com.github.opencam.imagewriter.Filestore;
import com.github.opencam.imagewriter.RoundRobinFilestorePool;
import com.github.opencam.util.ApplicationProperties;

public class SystemConfiguration {
  Pattern cameraPat = Pattern.compile("cameras[.]([^.]+)[.](.+)");
  Logger log = Logger.getLogger(getClass().getCanonicalName());
  final ApplicationProperties ap;
  private final Map<String, Map<String, String>> camConfigs;
  final Map<String, ApplicationProperties> motionConfigs;
  long initialDelay;
  long maxUploadDelay;

  public long getInitialDelay() {
    return initialDelay;
  }

  public long getMaxUploadDelay() {
    return maxUploadDelay;
  }

  public SystemConfiguration(final String path) {
    log.info("Loading config from " + path);
    final Properties props = new Properties();
    try {
      props.load(new FileInputStream(path));
    } catch (final Exception e) {
      throw new RuntimeException("Cannot load properties from " + path, e);
    }
    ap = new ApplicationProperties(props);

    camConfigs = new HashMap<String, Map<String, String>>();
    for (final Object keyOb : props.keySet()) {
      final String key = keyOb.toString();
      final Matcher match = cameraPat.matcher(key);
      if (match.find()) {
        final String value = props.getProperty(key);
        final String name = match.group(1);
        final String newKey = match.group(2);

        Map<String, String> camProps = camConfigs.get(name);

        if (camProps == null) {
          camProps = new HashMap<String, String>();
          camConfigs.put(name, camProps);
        }

        camProps.put(newKey, value);
      }
    }

    motionConfigs = ap.getSubPropertyMap("motion");
    initialDelay = (long) (ap.getDouble("initialDelaySeconds") * 1000);
    maxUploadDelay = (long) (ap.getDouble("offsite.maxTime") * 1000);
  }

  public Map<String, ApplicationProperties> getMotionConfig() {
    return motionConfigs;
  }

  public ApplicationProperties getAplicationProperties() {
    return ap;
  }

  public Map<String, Map<String, String>> getCamConfigs() {
    return camConfigs;
  }

  public Archiver getNewArchiver() {
    Archiver archive;

    final List<Filestore> sinkSource = new ArrayList<Filestore>();
    final int ftpCount = ap.getInteger("offsite.ftp.connectionCount", 3);
    if (ftpCount > 0) {
      for (int i = 0; i < ftpCount; i++) {
        final FTPFileSink sink = new FTPFileSink(ap.getProperty("offsite.ftp.hostname"), ap.getInteger("offsite.ftp.port"), ap.getProperty("offsite.ftp.username"), ap.getProperty("offsite.ftp.password"));
        sinkSource.add(sink);
      }
      archive = new ZipFTPArchiver(RoundRobinFilestorePool.fromList(sinkSource), ap.getProperty("offsite.passphrase"));
    } else {
      archive = new NoOpArchiver();
    }

    return archive;
  }

  public Archiver getLocalCacheArchiver() {
    return new LocalCacheArchiver(ap.getProperty("system.camera.localcache"));
  }

}
