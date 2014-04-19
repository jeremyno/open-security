package com.github.opencam.process;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.impl.conn.PoolingClientConnectionManager;

import com.github.opencam.devices.HttpUser;
import com.github.opencam.email.SendEmail;
import com.github.opencam.email.SendEmailTLSImpl;
import com.github.opencam.imagegrabber.BeanResource;
import com.github.opencam.imagegrabber.ImageSource;
import com.github.opencam.imagegrabber.Resource;
import com.github.opencam.imagewriter.FTPFileSink;
import com.github.opencam.imagewriter.Filestore;
import com.github.opencam.imagewriter.RoundRobinFilestorePool;
import com.github.opencam.processing.ImageDiff;
import com.github.opencam.processing.StatefulDiffer;
import com.github.opencam.security.AlarmStatus;
import com.github.opencam.security.SecurityDevice;
import com.github.opencam.ui.Configurable;
import com.github.opencam.ui.SecurityPlugin;
import com.github.opencam.util.ApplicationProperties;

/**
 * This is where the magic happens. We grab all of the inputs, and immediately locally cache them
 * and upload an ecrypted version. Then we zip up all of the images over 15 minutes, encrypt the
 * zip, upload the images and delete the individual counderparts afterwards.
 * 
 * 
 * 
 * @author jnorman
 * 
 */
public class PoolingOpenCamController implements OpenCamController {
  private final class StatusJob implements Runnable {
    private final AlarmStatus lastStatus;
    private final AlarmStatus newStatus;

    private StatusJob(final AlarmStatus lastStatus, final AlarmStatus newStatus) {
      this.lastStatus = lastStatus;
      this.newStatus = newStatus;
    }

    public void run() {
      if (PoolingOpenCamController.this.isSystemArmed()) {
        try {
          String message = "<h2>Changed state from " + lastStatus + " to " + newStatus + " (" + PoolingOpenCamController.this.getStatusString() + ")</h2>\n";

          message += SystemStatusWriter.getStatus(PoolingOpenCamController.this);

          message += "<h2>Images:</h2>";
          // prepare all images
          int i = 0;
          final List<Resource> resources = new ArrayList<Resource>(50);
          for (final String s : getCameraNames()) {
            message += "<h3>" + s + "</h3><ul>";
            for (final Resource image : getTrailingImages(s)) {
              resources.add(image);
              message += "<li><img width=\"320\" height=\"240\" src=\"cid:attach-" + i + "\" /><br/>" + image.getFileName() + "</li>";
              i++;
            }
            message += "</ul>";
          }

          emailer.sendAlertEmail(PoolingOpenCamController.this.abridgedEmail, "Security System Status Change to " + newStatus, message, resources);
        } catch (final Exception e) {
          log.log(Level.WARNING, "Problem sending alert email", e);
        }
      }
    }
  }

  private final class ImageArchivingJob implements Runnable {
    String source;
    private final Resource resource;

    private ImageArchivingJob(final String source, final Resource resource) {
      this.resource = resource;
      this.source = source;
    }

    public void run() {
      trailingImages.addImage(source, resource);
      archive.processImage(resource);
    }
  }

  private final class ArchiveJob implements Runnable {
    public void run() {
      try {
        archive.doUpload();
      } catch (final Exception e) {
        log.log(Level.WARNING, "Unable to do upload", e);
      }
    }
  }

  ApplicationProperties props;
  List<ImageSource> imageSources = new ArrayList<ImageSource>();
  Map<String, SecurityDevice> securityDevices = new HashMap<String, SecurityDevice>();

  TrailingImages trailingImages;
  LastTimestamp lastAlarm = new LastTimestamp();
  private final long initialDelay;
  private final long alarmCheck;
  protected LastTimestamp lastNotConnectedAlarm = new LastTimestamp();
  private final long latentDisconnect;
  private final long latentAlarm;
  private final long stateTrackerDelay;
  private final long cameraDelay;
  private final long permanentDisconnectDelay;
  List<AlarmCheckJob> alarmCheckJobs = new ArrayList<AlarmCheckJob>();
  String localcache;
  boolean running = false;
  private Thread me;
  Logger log = Logger.getLogger(getClass().getCanonicalName());
  private ScheduledExecutorService service;
  Map<String, ImageAcquisitionJob> imageJobs = new HashMap<String, ImageAcquisitionJob>();
  List<String> cameraNames = new ArrayList<String>();
  private final long cleanDiskEvery;
  private final double cleanDiskGoal;
  private final int deleteAndCheck;
  private final Archiver archive;
  List<String> abridgedEmail;
  private final SendEmail emailer;
  private long emailDelay;
  private boolean systemArmed = false;
  private final SecurityLog securitylog;
  int threadCount = 0;
  private DebugScheduledExecutorService cameraThreads;
  Map<String, StatefulDiffer> differs = new HashMap<String, StatefulDiffer>();
  Map<String, StatusEvent> statusEvents = new ConcurrentHashMap<String, StatusEvent>();
  private final long processImagesEvery;
  SystemConfiguration config;

  public PoolingOpenCamController(final SystemConfiguration config) {
    this.config = config;

    final PoolingClientConnectionManager cm = new PoolingClientConnectionManager();

    for (final Entry<String, Map<String, String>> camConfig : config.getCamConfigs().entrySet()) {
      final Map<String, String> configs = camConfig.getValue();
      final String name = camConfig.getKey();
      try {
        final Class<?> clazz = Class.forName(configs.get("class"));
        final Object pluginOb = clazz.newInstance();
        final SecurityPlugin plugin = (SecurityPlugin) pluginOb;
        plugin.setName(name);
        plugin.configure(configs);
        if (plugin instanceof SecurityDevice) {
          securityDevices.put(name, (SecurityDevice) plugin);
        }

        if (plugin instanceof ImageSource) {
          imageSources.add((ImageSource) plugin);
        }

        if (plugin instanceof HttpUser) {
          final HttpUser httpUser = (HttpUser) plugin;
          httpUser.setConnectionManager(cm);
        }
      } catch (final Exception e) {
        throw new RuntimeException("Problem configuring plugin: " + name, e);
      }
    }

    this.cameraNames = new ArrayList<String>(config.getCamConfigs().keySet());

    this.props = config.getAplicationProperties();
    alarmCheck = (long) (Double.parseDouble(getProp("alarm.checkSeconds")) * 1000);
    initialDelay = Integer.parseInt(getProp("initialDelaySeconds")) * 1000;
    latentDisconnect = (long) (Double.parseDouble(getProp("alarm.latentDisconnectSeconds")) * 1000);
    latentAlarm = (long) (Double.parseDouble(getProp("alarm.latentAlarmSeconds")) * 1000);
    stateTrackerDelay = (long) (Double.parseDouble(getProp("system.stateTrackerDelaySeconds")) * 1000);
    cameraDelay = (long) (Double.parseDouble(getProp("system.cameraDelaySeconds")) * 1000);
    permanentDisconnectDelay = (long) (Double.parseDouble(getProp("alarm.permanentlyDisconnectedAfterSeconds")) * 1000);
    cleanDiskEvery = (long) (Double.parseDouble(getProp("system.cleanDisk.checkEveryHours")) * 1000 * 60 * 60);
    cleanDiskGoal = Double.parseDouble(getProp("system.cleanDisk.Goal"));
    localcache = getProp("system.camera.localcache");
    deleteAndCheck = Integer.parseInt(getProp("system.cleanDisk.deleteAndCheck"));
    final List<Filestore> sinkSource = new ArrayList<Filestore>();
    final int ftpCount = props.getInteger("offsite.ftp.connectionCount", 3);
    if (ftpCount > 0) {
      for (int i = 0; i < ftpCount; i++) {
        final FTPFileSink sink = new FTPFileSink(getProp("offsite.ftp.hostname"), Integer.parseInt(getProp("offsite.ftp.port")), getProp("offsite.ftp.username"), getProp("offsite.ftp.password"));
        sinkSource.add(sink);
      }
      archive = new ZipFTPArchiver(RoundRobinFilestorePool.fromList(sinkSource), getIProp("offsite.trailingEntries"), getProp("offsite.passphrase"));
    } else {
      archive = new NoOpArchiver();
    }

    abridgedEmail = props.getList("email.alertRecipients");
    emailer = new SendEmailTLSImpl(props.getProperty("email.send.host"), props.getInteger("email.send.port"), props.getProperty("email.send.username"), props.getProperty("email.send.password"), props.getProperty("email.send.from"));
    this.trailingImages = new TrailingImages(props.getInteger("email.images.trailing.each"), props.getInteger("system.images.processed", 1));
    final String logBasePath = props.getProperty("system.log");
    threadCount = props.getInteger("system.threads", Runtime.getRuntime().availableProcessors() * 4);
    processImagesEvery = (long) (props.getDouble("system.processImagesEvery") * 1000);

    final String lastPath = SecurityLog.getPath(logBasePath, true);
    if (lastPath != null) {
      try {
        systemArmed = SecurityLog.getLastArmedStatus(lastPath);
      } catch (final Exception e) {
        throw new RuntimeException("Unable to find last armed status", e);
      }
    }

    for (final Entry<String, ApplicationProperties> motion : config.getMotionConfig().entrySet()) {
      final ApplicationProperties mprops = motion.getValue();
      final String sourceName = motion.getKey();
      try {
        final Class<?> differClazz = Class.forName(mprops.getProperty("class"));
        final Object differ = differClazz.newInstance();
        if (differ instanceof Configurable) {
          final Configurable configurable = (Configurable) differ;
          configurable.configure(mprops);
        }
        final StatefulDiffer value = new StatefulDiffer((ImageDiff<?>) differ);
        value.configure(mprops);
        differs.put(sourceName, value);
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    try {
      securitylog = new SecurityLog(SecurityLog.getPath(logBasePath, false));
    } catch (final FileNotFoundException e) {
      throw new RuntimeException("Problem opening security log", e);
    }

    log.info("Security Devices: " + securityDevices);
    log.info("Cameras: " + imageSources);
    log.info("Thread Count: " + threadCount);
  }

  private int getIProp(final String s) {
    return Integer.parseInt(getProp(s));
  }

  private String getProp(final String s) {
    return props.getProperty(s);
  }

  public void processRawImage(final String source, final Resource resource) {
    service.submit(new PersistImageJob(resource, localcache));
    service.submit(new ImageArchivingJob(source, resource));
  }

  public void runOpencam() {
    final String prefix = props.getProperty("system.threads.name", "Executor-");
    final ScheduledExecutorService baseService = Executors.newScheduledThreadPool(threadCount, new ThreadFactory() {
      int number = 0;

      public synchronized Thread newThread(final Runnable r) {
        final Thread t = new Thread(r);
        t.setName(prefix + number++);
        return t;
      }
    });

    service = new DebugScheduledExecutorService(baseService, props.getBoolean("system.pool.debug"), props.getDouble("system.pool.debugFilter"));
    cameraThreads = new DebugScheduledExecutorService(Executors.newScheduledThreadPool(imageSources.size()), props.getBoolean("system.cameraPool.debug"), props.getDouble("system.cameraPool.debugFilter"));

    for (final SecurityDevice dev : securityDevices.values()) {
      final AlarmCheckJob job = new AlarmCheckJob(dev, lastAlarm, lastNotConnectedAlarm, permanentDisconnectDelay, this);
      service.scheduleWithFixedDelay(job, initialDelay, alarmCheck, TimeUnit.MILLISECONDS);
      this.alarmCheckJobs.add(job);
    }

    for (final ImageSource src : imageSources) {
      final ImageAcquisitionJob job = new ImageAcquisitionJob(src, localcache, this);
      cameraThreads.scheduleWithFixedDelay(job, initialDelay, cameraDelay, TimeUnit.MILLISECONDS);
      this.imageJobs.put(src.getName(), job);
      service.scheduleWithFixedDelay(new ProcessImageJob(src.getName(), this), initialDelay, processImagesEvery, TimeUnit.MILLISECONDS);
    }

    final StateTracker tracker = new StateTracker(this, emailer, abridgedEmail);
    service.scheduleAtFixedRate(tracker, initialDelay, stateTrackerDelay, TimeUnit.MILLISECONDS);

    final CleanDisk disk = new CleanDisk(localcache, cleanDiskGoal, deleteAndCheck);
    service.scheduleWithFixedDelay(disk, initialDelay, cleanDiskEvery, TimeUnit.MILLISECONDS);

    service.scheduleWithFixedDelay(new ArchiveJob(), initialDelay, getIProp("offsite.maxTime") * 1000, TimeUnit.MILLISECONDS);

    try {
      while (running) {
        service.awaitTermination(10000, TimeUnit.NANOSECONDS);
      }
      service.shutdownNow();
      cameraThreads.shutdownNow();
      service.awaitTermination(30, TimeUnit.SECONDS);
      cameraThreads.awaitTermination(30, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      service.shutdownNow();
      cameraThreads.shutdownNow();
      throw new RuntimeException("Not so awesome", e);
    }
  }

  public synchronized void start() {
    if (me == null) {
      running = true;
      me = new Thread(new Runnable() {

        public void run() {
          PoolingOpenCamController.this.runOpencam();
        }

      });

      me.start();
    } else {
      throw new RuntimeException("Can only run the controller once");
    }
  }

  List<Resource> getTrailingImages(final String cameraName) {
    return trailingImages.getTrailingImages(cameraName);
  }

  public void stop() {
    running = false;
    service.shutdown();
    try {
      service.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch (final InterruptedException e) {
      log.log(Level.WARNING, "Problem while shutting down executor service", e);
    }
    log.info("Shut down the controller service");
  }

  public AlarmStatus getCurrentStatus() {
    final long millisSinceLastCheckin = lastAlarm.getMillisSinceLastCheckin();
    if (millisSinceLastCheckin >= 0 && latentAlarm > millisSinceLastCheckin) {
      return AlarmStatus.AlarmDetected;
    }

    final long disconnect = lastNotConnectedAlarm.getMillisSinceLastCheckin();
    if (disconnect >= 0 && latentDisconnect > disconnect) {
      return AlarmStatus.NotConnected;
    }

    return AlarmStatus.Nominal;
  }

  public String getStatusString() {
    final AlarmStatus status = getCurrentStatus();

    switch (status) {
    case AlarmDetected:
      return "Alarm Detected by " + lastAlarm.getWho() + " at " + new Date(lastAlarm.getLatestCheckin());
    case NotConnected:
      return "Not Connected to " + lastNotConnectedAlarm.getWho() + " at " + new Date(lastNotConnectedAlarm.getLatestCheckin());
    }

    return "" + status;
  }

  public List<SecurityDeviceStatus> getDeviceStatus() {
    final List<SecurityDeviceStatus> status = new ArrayList<SecurityDeviceStatus>();

    for (final Entry<String, StatusEvent> i : statusEvents.entrySet()) {
      final SecurityDevice device = securityDevices.get(i.getKey());
      final StatusEvent event = i.getValue();
      status.add(new SecurityDeviceStatus(event.getTimestamp(), device, event.getStatus(), event.getNotes()));
    }

    return status;
  }

  public boolean isRunning() {
    return running;
  }

  public Resource getLastImage(final String name) {
    final List<ProcessedImageHolder> trail = trailingImages.getTrailingProcessedImages(name);
    if (trail == null || trail.size() < 1) {
      return null;
    }

    final ProcessedImageHolder out = trail.get(0);
    final byte[] processedImage = out.getProcessedImage();
    if (processedImage == null) {
      return null;
    }
    return new BeanResource("image/jpeg", processedImage, "", name, 0, AlarmStatus.Nominal);
  }

  public List<String> getCameraNames() {
    return cameraNames;
  }

  public void handleStatus(final AlarmStatus lastStatus, final AlarmStatus newStatus) {
    service.schedule(new StatusJob(lastStatus, newStatus), emailDelay, TimeUnit.MILLISECONDS);

    for (final ImageSource src : imageSources) {
      try {
        src.setFramerate(newStatus);
      } catch (final Exception e) {
        log.log(Level.WARNING, "Problem setting new frame rate on " + src, e);
      }
    }
  }

  public boolean isSystemArmed() {
    return systemArmed;
  }

  public void setSystemArmed(final boolean systemArmed, final String who) {
    this.systemArmed = systemArmed;
    securitylog.changeArmed(systemArmed, who);
  }

  public void processAlarmMessage(final String source, final AlarmStatus status, final long timestamp, final List<String> notes) {
    this.statusEvents.put(source, new StatusEvent(timestamp, status, notes));
    if (status.equals(AlarmStatus.AlarmDetected)) {
      lastAlarm.doCheckin(source);
    }

    if (status.equals(AlarmStatus.NotConnected)) {
      lastNotConnectedAlarm.doCheckin(source);
    }
  }

  public void processImage(final String source) {
    final StatefulDiffer differ = differs.get(source);
    if (differ == null) {
      throw new RuntimeException("Differ not configured for " + source);
    }
    final Resource last = trailingImages.getLastImage(source);
    final int diff = differ.getImageDiff(last);

    final long timestamp = last != null ? last.getTimestamp() : 0;
    final List<String> notes = last != null ? last.getNotes() : new ArrayList<String>();

    if (diff >= differ.getThresh()) {
      processAlarmMessage(source, AlarmStatus.AlarmDetected, timestamp, notes);
    } else {
      processAlarmMessage(source, AlarmStatus.Nominal, timestamp, notes);
    }

    trailingImages.addProcessedImage(source, last, differ.getLastImage());
  }

  public void configure(final SystemConfiguration config) {

  }
}
