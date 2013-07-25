package com.github.opencam.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApplicationProperties {
  Map<String, String> props;

  public ApplicationProperties(final Map<String, String> props) {
    super();
    this.props = props;
  }

  public ApplicationProperties(final Properties props2) {
    props = new HashMap<String, String>();
    for (final Object key : props2.keySet()) {
      final String sKey = key.toString();
      props.put(sKey, props2.getProperty(sKey));
    }
  }

  public String getProperty(final String property) {
    final String out = props.get(property);
    if (out == null) {
      throw new RuntimeException("Unable to find property " + property);
    }
    return out;
  }

  public double getDouble(final String property) {
    return Double.parseDouble(getProperty(property));
  }

  public List<String> getList(final String property) {
    final String property2 = getProperty(property);
    final String[] split = property2.split(",");
    return Arrays.asList(split);
  }

  public int getInteger(final String string) {
    return Integer.parseInt(getProperty(string));
  }

  public Integer getInteger(final String string, final Integer object) {
    final String prop = props.get(string);
    if (prop == null) {
      return object;
    }
    return Integer.parseInt(prop);
  }

  public boolean getBoolean(final String string, final boolean b) {
    final String prop = props.get(string);
    if (prop == null) {
      return b;
    }
    return Boolean.parseBoolean(prop);
  }

  public String getProperty(final String property, final String defaultValue) {
    final String prop = props.get(property);

    if (prop == null) {
      return defaultValue;
    }

    return prop;
  }

  public Map<String, ApplicationProperties> getSubPropertyMap(final String property) {
    final Map<String, Map<String, String>> out = new HashMap<String, Map<String, String>>();
    final Pattern p = Pattern.compile(property + "[.]([^.]*)[.](.*)");

    for (final Entry<String, String> i : props.entrySet()) {
      final Matcher match = p.matcher(i.getKey());
      if (match.matches()) {
        final String key1 = match.group(1);
        final String key2 = match.group(2);

        Map<String, String> sub = out.get(key1);

        if (sub == null) {
          sub = new HashMap<String, String>();
          out.put(key1, sub);
        }

        sub.put(key2, i.getValue());
      }
    }

    final Map<String, ApplicationProperties> aout = new HashMap<String, ApplicationProperties>();
    for (final Entry<String, Map<String, String>> i : out.entrySet()) {
      aout.put(i.getKey(), new ApplicationProperties(i.getValue()));
    }

    return aout;
  }

  public double getDouble(final String string, final double d) {
    final String prop = props.get(string);
    if (prop == null) {
      return d;
    }

    return Double.parseDouble(prop);
  }

  public boolean getBoolean(final String string) {
    return Boolean.parseBoolean(getProperty(string));
  }
}
