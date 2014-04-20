package com.github.opencam.devices;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.github.opencam.imagegrabber.BeanResource;
import com.github.opencam.imagegrabber.ImageSource;
import com.github.opencam.imagegrabber.MJpegStream;
import com.github.opencam.imagegrabber.Resource;
import com.github.opencam.imagegrabber.StreamUtils;
import com.github.opencam.security.AlarmStatus;
import com.github.opencam.security.SecurityDevice;
import com.github.opencam.ui.SecurityPlugin;
import com.github.opencam.util.ApplicationProperties;
import com.github.opencam.util.ExceptionUtils;
import com.github.opencam.util.MapUtils;

public class LoftekCxs2200 implements ImageSource, SecurityDevice, SecurityPlugin, HttpUser {
  public enum FrameRate {
    Full(0, Double.MAX_VALUE), Fps20(1, 20), Fps15(3, 15), Fps10(6, 10), Fps5(11, 5), Fps4(12, 4), Fps3(13, 3), Fps2(14, 2), Fps1(15, 1), Spf2(17, 1. / 2), Spf3(19, 1. / 3), Spf4(21, 1. / 4), Spf5(23, 1. / 5);
    int value;
    double hz;

    FrameRate(final int value, final double hz) {
      this.value = value;
      this.hz = hz;
    }

    public int getRateNumber() {
      return value;
    }

    public double getHz() {
      return hz;
    }

    public static FrameRate getLowestFramerate() {
      FrameRate r = FrameRate.Full;

      for (final FrameRate t : FrameRate.values()) {
        if (t.getHz() < r.getHz()) {
          r = t;
        }
      }

      return r;
    }
  }

  public enum Strategy {
    Stream, Snapshot
  }

  DefaultHttpClient client;
  String host;
  String password;
  int port;
  String name;
  HttpGet request;
  HttpGet snapshotRequest;
  HttpGet statusRequest;
  MJpegStream stream;
  String username;
  FrameRate rate;
  double alarmFramerate;
  double nominalFramerate;
  double disconnectFramerate;
  final Logger log = Logger.getLogger(getClass().getCanonicalName());
  private InputStream content;
  private AlarmStatus lastStatus;
  private Strategy strategy;
  boolean checkAlarm;
  private boolean disconnectOverride;

  public LoftekCxs2200(final String username, final String password, final String host, final int port, final double framerate) {
    init(username, password, host, port, framerate);
  }

  public LoftekCxs2200() {
    // use the configure option with this
  }

  private void init(final String username, final String password, final String host, final int port, final double framerate) {
    this.username = username;
    this.password = password;
    this.host = host;
    this.port = port;
    statusRequest = new HttpGet("http://" + host + ":" + port + "/get_status.cgi");
    snapshotRequest = new HttpGet("http://" + host + ":" + port + "/snapshot.cgi");
    suggestFrameRate(framerate);
  }

  public void configure(final Map<String, String> config) {
    final ApplicationProperties props = new ApplicationProperties(config);
    final String username = MapUtils.getRequired(config, "username");
    final String password = MapUtils.getRequired(config, "password");
    final String host = MapUtils.getRequired(config, "hostname");
    final String portStr = MapUtils.getRequired(config, "port");
    nominalFramerate = props.getDouble("framerate");
    alarmFramerate = props.getDouble("alarmFramerate");
    disconnectFramerate = props.getDouble("systemDisconnectFramerate");
    final int port = Integer.parseInt(portStr);
    strategy = Strategy.valueOf(props.getProperty("strategy", "Stream"));
    System.out.println(getName() + " strategy is " + strategy);

    checkAlarm = props.getBoolean("alarmCheck", true);
    init(username, password, host, port, nominalFramerate);
  }

  public double suggestFrameRate(final double suggestion) {
    FrameRate rate = FrameRate.Spf5;
    double miss = Double.MAX_VALUE;

    for (final FrameRate test : FrameRate.values()) {
      final double testMiss = Math.abs(test.getHz() - suggestion);
      if (testMiss < miss) {
        rate = test;
        miss = testMiss;
      }
    }

    if (suggestion <= 0.04) {
      rate = FrameRate.Full;
    }

    if (!ObjectUtils.equals(this.rate, rate)) {
      this.rate = rate;
      final String uri = "http://" + host + ":" + port + "/videostream.cgi?rate=" + rate.getRateNumber() + "&resolution=32";
      log.info("Setting URI for " + this + " to " + uri);
      request = new HttpGet(uri);
    }

    return this.rate.getHz();
  }

  public Resource getImage() {
    try {
      final Resource out = getResource();
      this.disconnectOverride = false;
      return out;
    } catch (final RuntimeException e) {
      log.log(Level.FINE, "Problem getting image", e);

      try {
        Thread.sleep(3000);
      } catch (final InterruptedException e1) {
        throw new RuntimeException(e1);
      }
      try {
        resetStream();
        final Resource out = getResource();
        this.disconnectOverride = false;
        return out;
      } catch (final Exception f) {
        disconnectOverride = true;
        if (ExceptionUtils.isConnectionException(f)) {
          return null;
        }
        throw new RuntimeException(f);
      }
    }
  }

  private Resource getResource() {
    switch (strategy) {
    case Snapshot:
      return getSnapshotResource();
    case Stream:
      return getStreamResource();
    default:
      throw new RuntimeException("Unsupported strategy: " + strategy);
    }
  }

  private Resource getSnapshotResource() {
    try {
      final HttpResponse response = client.execute(snapshotRequest);
      final HttpEntity entity = response.getEntity();
      final InputStream content = new BufferedInputStream(entity.getContent(), StreamUtils.DEFAULT_JPG_BUFFER_SIZE);

      final ByteArrayOutputStream bos = new ByteArrayOutputStream(StreamUtils.DEFAULT_JPG_BUFFER_SIZE);
      IOUtils.copy(content, bos);
      IOUtils.closeQuietly(content);
      IOUtils.closeQuietly(bos);
      if (bos.size() > StreamUtils.DEFAULT_JPG_BUFFER_SIZE) {
        System.out.println("Size: " + bos.size() + " " + (double) bos.size() * 100 / StreamUtils.DEFAULT_JPG_BUFFER_SIZE + "% of default");
      }

      final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_kkmmss_SSS");
      final String filename = sdf.format(new Date()) + "_" + lastStatus + ".jpg";

      final byte[] data = bos.toByteArray();
      final BeanResource out = new BeanResource(entity.getContentType().getValue(), data, filename, name, System.currentTimeMillis(), getAlarmStatus(true));
      return out;
    } catch (final Exception e) {
      throw new RuntimeException("Problem fetching snapshot", e);
    }
  }

  private Resource getStreamResource() {
    final Resource entity = getJpegStream().getNextEntity();

    final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_kkmmss_SSS");
    final String filename = sdf.format(new Date()) + "_" + lastStatus + ".jpg";

    final byte[] data = entity.getData();
    final BeanResource out = new BeanResource(entity.getMimeType(), data, filename, name, System.currentTimeMillis(), getAlarmStatus(true));
    return out;
  }

  private void resetStream() {
    if (content != null) {
      IOUtils.closeQuietly(content);
      content = null;
    }
    stream = null;
  }

  private MJpegStream getJpegStream() {
    MJpegStream check = stream;
    if (check == null) {
      try {
        final HttpResponse response = client.execute(request);
        final HttpEntity entity = response.getEntity();
        content = entity.getContent();
        final MJpegStream newStream = new MJpegStream("ipcamera", content);
        newStream.getNextEntity(); // toss the first one
        stream = newStream;
        check = newStream;
      } catch (final Exception e) {
        throw new RuntimeException("Unable to load image", e);
      }
    }

    return check;
  }

  public AlarmStatus getAlarmStatus(final boolean useCache) {

    if (useCache && lastStatus != null) {
      return lastStatus;
    }

    AlarmStatus out = null;
    try {
      out = getAlarmStatusUse();

      if (out == null) {
        throw new RuntimeException("No status found");
      }

    } catch (final Exception e) {
      try {
        out = getAlarmStatusUse();

        if (out == null) {
          throw new RuntimeException("No status found");
        }

      } catch (final Exception e1) {
        log.log(Level.FINE, "Problem getting status", e);
        out = AlarmStatus.NotConnected;
      }
    }

    lastStatus = out;
    return out;
  }

  public AlarmStatus getAlarmStatusUse() throws Exception {
    if (checkAlarm) {
      final HttpResponse response = client.execute(statusRequest);
      final InputStream content = response.getEntity().getContent();
      final List<String> lines = IOUtils.readLines(content);
      IOUtils.closeQuietly(content);
      for (final String l : lines) {
        if (l.contains("alarm_status")) {
          if (l.equals("var alarm_status=0;")) {
            if (disconnectOverride) {
              return AlarmStatus.NotConnected;
            }
            return AlarmStatus.Nominal;
          } else {
            return AlarmStatus.AlarmDetected;
          }
        }
      }
      return null;
    } else {
      return AlarmStatus.Nominal;
    }
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public double getCurrentFramerate() {
    return rate.getHz();
  }

  @Override
  public String toString() {
    return "Loftek CXS2200 \"" + name + "\"";
  }

  public double setFramerate(final AlarmStatus status) {
    switch (status) {
    case AlarmDetected:
      return suggestFrameRate(alarmFramerate);
    case NotConnected:
      return suggestFrameRate(disconnectFramerate);
    default:
      log.warning("Unknown state: " + status + ", assuming nominal");
    case Nominal:

      return suggestFrameRate(nominalFramerate);
    }
  }

  public void setConnectionManager(final ClientConnectionManager cm) {
    final HttpParams httpParams = new BasicHttpParams();
    final int maxTimeout = (int) (1000. / FrameRate.getLowestFramerate().getHz() * 3);
    HttpConnectionParams.setConnectionTimeout(httpParams, maxTimeout);
    log.info("Max timout for " + this + " is " + maxTimeout);
    HttpConnectionParams.setSoTimeout(httpParams, maxTimeout);
    client = new DefaultHttpClient(cm, httpParams);
    client.getCredentialsProvider().setCredentials(new AuthScope(host, port), new UsernamePasswordCredentials(username, password));
  }

}
