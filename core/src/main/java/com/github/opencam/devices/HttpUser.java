package com.github.opencam.devices;

import org.apache.http.conn.ClientConnectionManager;

public interface HttpUser {

  void setConnectionManager(ClientConnectionManager cm);

}
