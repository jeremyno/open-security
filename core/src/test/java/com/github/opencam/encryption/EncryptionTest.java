package com.github.opencam.encryption;

import java.util.Random;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;

import com.github.opencam.imagegrabber.StreamUtils;
import com.github.opencam.util.TimingUtils;

public class EncryptionTest {
  @Test
  public void testEncryption() {
    final String passphrase = "uhnjiugbnjuygvbnjuygbasdhefdvbcxzhygdzbxnmkiujmkiujhnmkjuytgbhytfvgtrfdcvgtredsxcfrdxc";
    final byte[] stream = new byte[StreamUtils.DEFAULT_JPG_BUFFER_SIZE * 15 * 5 * 3];
    final Random r = new Random();
    r.nextBytes(stream);

    final EncryptionPlugin plugin = new Aes128Encyption();

    testEncryption(passphrase, stream, plugin);
  }

  private void testEncryption(final String passphrase, final byte[] stream, final EncryptionPlugin plugin) {
    final int reps = 100;
    final StopWatch watch = new StopWatch();
    byte[] enc = null;
    watch.start();
    plugin.setPassphrase(passphrase);
    for (int i = 0; i < reps; i++) {
      enc = plugin.encrypt(stream);
    }
    watch.stop();
    TimingUtils.printNanoTime("Aerage timing test over " + reps + " of " + plugin, watch.getNanoTime() / reps);
    Assert.assertArrayEquals(stream, plugin.decrypt(enc));
  }
}
