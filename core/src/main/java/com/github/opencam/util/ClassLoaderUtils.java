package com.github.opencam.util;

import java.lang.reflect.Constructor;

import org.apache.http.conn.ClientConnectionManager;

import com.github.opencam.devices.HttpUser;
import com.github.opencam.ui.Configurable;
import com.github.opencam.ui.SecurityPlugin;

public class ClassLoaderUtils {
  public static <T> T newObject(final String clazz, final Object... params) {
    try {
      final Class<?> clazzOb = Class.forName(clazz);
      final Constructor<?>[] constructors = clazzOb.getConstructors();

      for (final Constructor<?> c : constructors) {
        final Class<?>[] types = c.getParameterTypes();

        if (types.length == params.length) {
          boolean match = true;
          for (int i = 0; i < types.length; i++) {
            if (params[i] != null) {
              match &= types[i].isAssignableFrom(params[i].getClass());
            }
          }

          if (match) {
            final Object pluginOb = c.newInstance(params);

            return (T) pluginOb;
          }
        }
      }

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    throw new RuntimeException("Unable to find a suitable constructor");
  }

  public static <T> T loadObject(final String clazz, final ApplicationProperties config) {
    try {
      final Class<?> clazzOb = Class.forName(clazz);
      final Object pluginOb = clazzOb.newInstance();

      if (config != null && pluginOb instanceof Configurable) {
        final Configurable configurable = (Configurable) pluginOb;
        configurable.configure(config);
      }

      return (T) pluginOb;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T loadObject(final String clazz, final ApplicationProperties config, final String name, final ClientConnectionManager cm) {
    try {
      final T ob = loadObject(clazz, config);

      if (ob instanceof SecurityPlugin) {
        final SecurityPlugin plugin = (SecurityPlugin) ob;
        plugin.setName(name);
        plugin.configure(config.asMap());
      }

      if (ob instanceof HttpUser) {
        final HttpUser httpUser = (HttpUser) ob;
        httpUser.setConnectionManager(cm);
      }

      return ob;
    } catch (final Exception e) {
      throw new RuntimeException("Problem configuring plugin: " + name, e);
    }
  }
}
