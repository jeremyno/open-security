package com.github.opencam.process;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.opencam.util.ApplicationProperties;

public class SystemConfiguration {
  Pattern cameraPat = Pattern.compile("cameras[.]([^.]+)[.](.+)");
  Logger log = Logger.getLogger(getClass().getCanonicalName());
  final ApplicationProperties ap;
  private final Map<String, Map<String, String>> camConfigs;
  final Map<String, ApplicationProperties> motionConfigs;

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

}
