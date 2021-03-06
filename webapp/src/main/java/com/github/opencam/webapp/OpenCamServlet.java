package com.github.opencam.webapp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.State;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.github.opencam.imagegrabber.Resource;
import com.github.opencam.process.OpenCamController;
import com.github.opencam.process.SystemConfiguration;
import com.github.opencam.util.ClassLoaderUtils;
import com.github.opencam.util.ThreadUtils;

public class OpenCamServlet implements Filter {
  OpenCamController opencam;
  String logpath = "";

  public void destroy() {
    opencam.stop();
    opencam = null;
  }

  public void doFilter(final ServletRequest arg0, final ServletResponse arg1, final FilterChain arg2) throws IOException, ServletException {
    if (arg0 instanceof HttpServletRequest && arg1 instanceof HttpServletResponse) {
      final HttpServletRequest req = (HttpServletRequest) arg0;
      final HttpServletResponse resp = (HttpServletResponse) arg1;

      final String reqPath = req.getRequestURI();
      final String conPath = req.getContextPath();
      final String path = reqPath.substring(conPath.length());

      if (path != null) {
        final ServletOutputStream outputStream = resp.getOutputStream();
        if (path.contains("system/logs")) {
          resp.setContentType("text/plain");
          FileInputStream fis = null;
          try {
            fis = new FileInputStream(logpath);
            IOUtils.copy(fis, outputStream);
            outputStream.flush();
            outputStream.close();
          } finally {
            if (fis != null) {
              IOUtils.closeQuietly(fis);
            }
          }
        } else {

          final Pattern pat = Pattern.compile("[/]images[/]?(.*).jpg");
          final Matcher matcher = pat.matcher(path);

          if (matcher.matches()) {
            final String name = matcher.group(1);
            final Resource lastImage = opencam.getLastImage(name);

            if (lastImage != null) {
              final byte[] image = lastImage.getData();
              resp.setContentType("image/jpeg");
              resp.setContentLength(image.length);
              final ServletOutputStream writer = outputStream;
              writer.write(image);
              writer.close();
              return;
            } else if (name.equals("threads")) {
              resp.setContentType("text/html");
              final PrintWriter writer = resp.getWriter();

              final Map<State, Map<Thread, StackTraceElement[]>> running = ThreadUtils.getAllStackTracesByState();
              writer.println("Threads in " + running.keySet());

              for (final Entry<State, Map<Thread, StackTraceElement[]>> state : running.entrySet()) {
                writer.println("<h1>" + state.getKey() + "</h2>");
                writer.println("<div style='padding-left:4em;'>");
                for (final Entry<Thread, StackTraceElement[]> i : state.getValue().entrySet()) {
                  writer.println("<h2>" + i.getKey().getName() + "</h2>");
                  writer.println("<ul>");
                  for (final StackTraceElement j : i.getValue()) {
                    writer.println("<li>" + j + "</li>");
                  }
                  writer.println("</ul>");
                }
                writer.println("</div>");
              }

              writer.println("No image for " + name);
              writer.close();
            } else {
              resp.setContentType("text/plain");
              final PrintWriter writer = resp.getWriter();
              writer.println("No image for " + name);
              writer.close();
              return;
            }
          }
        }
      }

      req.setAttribute("opencam", opencam);
    }

    arg2.doFilter(arg0, arg1);
  }

  public void init(final FilterConfig arg0) throws ServletException {
    final String configPath = System.getProperty("opencam.configPath");
    final SystemConfiguration config = new SystemConfiguration(configPath);
    final String controllerClazz = config.getAplicationProperties().getProperty("controller.class", "com.github.opencam.process.PoolingOpenCamController");

    opencam = ClassLoaderUtils.newObject(controllerClazz, config);
    logpath = config.getAplicationProperties().getProperty("system.logpath", "/var/log/tomcat7/catalina.out");
    opencam.start();
  }
}
