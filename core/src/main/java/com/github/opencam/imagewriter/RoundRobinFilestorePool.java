package com.github.opencam.imagewriter;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class RoundRobinFilestorePool implements Filestore {
  AtomicLong number = new AtomicLong();
  Filestore[] base;

  public RoundRobinFilestorePool(final Filestore[] base) {
    super();
    this.base = base;
  }

  public void saveFile(final String file, final InputStream stream) {
    get().saveFile(file, stream);
  }

  public void deleteFile(final String file) {
    get().deleteFile(file);
  }

  private Filestore get() {
    long num = number.incrementAndGet();
    num %= base.length;
    return base[(int) num];
  }

  public static RoundRobinFilestorePool fromList(final List<Filestore> base) {
    return new RoundRobinFilestorePool(base.toArray(new Filestore[0]));
  }
}
