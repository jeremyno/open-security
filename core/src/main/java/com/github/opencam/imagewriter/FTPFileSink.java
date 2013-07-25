package com.github.opencam.imagewriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FTPFileSink implements Filestore {
  FTPClient client;
  String hostname;
  int port;

  String username;
  String password;
  final Logger log = Logger.getLogger(getClass().getCanonicalName());

  public FTPFileSink(final String hostname, final int port, final String username, final String password) {
    super();
    this.hostname = hostname;
    this.port = port;
    this.username = username;
    this.password = password;
  }

  private void saveFileUse(final String file, final InputStream stream) throws IOException {
    final FTPClient client = getClient();

    final String[] dirParts = file.split("/");
    final StringBuilder dirName = new StringBuilder();
    for (int i = 0; i < dirParts.length - 1; i++) {
      dirName.append("/").append(dirParts[i]);

      if (!client.makeDirectory(dirName.toString())) {
        if (!client.getReplyString().contains("File exists")) {
          throw new RuntimeException("Unable to create necessary directory: " + dirName);
        }
      }
    }

    if (!client.storeFile(file, stream)) {
      throw new RuntimeException("Problem saving file.");
    }
  }

  private void deleteFileUse(final String file) {
    final FTPClient client = getClient();
    try {
      if (!client.deleteFile(file)) {
        throw new RuntimeException("problem deleting file " + file);
      }
    } catch (final IOException e) {
      throw new RuntimeException("Problem deleting file " + file, e);
    }
  }

  private FTPClient getClient() {
    if (client == null) {
      client = new FTPClient();
    }

    if (!client.isConnected() || !client.isAvailable()) {
      try {
        // client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
        client.connect(hostname, port);
        client.feat();
        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
          throw new RuntimeException("Unable to connect");
        }

        if (!client.login(username, password)) {
          throw new RuntimeException("Unable to log in");
        }

        client.enterLocalPassiveMode();
      } catch (final Exception e) {
        if (client.isConnected()) {
          try {
            client.disconnect();
          } catch (final IOException f) {
            log.log(Level.FINE, "Exception while disconnecting after failure.", f);
          }
        }
        throw new RuntimeException("Unable to connect to ftps server", e);
      }
    }

    return client;
  }

  public void saveFile(final String filename, final InputStream stream) {
    final Callable<?> r = new Callable<Object>() {
      public Object call() throws Exception {
        saveFileUse(filename, stream);
        return null;
      }
    };

    execute(r);
  }

  private <T> T execute(final Callable<T> run) {
    try {
      return run.call();
    } catch (final Exception e) {
      if (client != null && client.isConnected()) {
        try {
          client.disconnect();
        } catch (final IOException f) {
          log.log(Level.FINE, "Exception while disconnecting after failure.", f);
        }
      }

      client = null;
    }

    try {
      return run.call();
    } catch (final Exception e) {
      throw new RuntimeException("Unable to save file", e);
    }
  }

  public void deleteFile(final String file) {
    final Callable<?> r = new Callable<Object>() {
      public Object call() throws Exception {
        deleteFileUse(file);
        return null;
      }
    };

    execute(r);
  }
}
