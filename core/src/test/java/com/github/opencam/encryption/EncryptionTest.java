package com.github.opencam.encryption;

import java.util.Random;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;

import com.github.opencam.imagegrabber.StreamUtils;

public class EncryptionTest {
  @Test
  public void testEncryption() {
    final String passphrase = "uhnjiugbnjuygvbnjuygbasdhefdvbcxzhygdzbxnmkiujmkiujhnmkjuytgbhytfvgtrfdcvgtredsxcfrdxc";
    final byte[] stream = new byte[StreamUtils.DEFAULT_JPG_BUFFER_SIZE * 15 * 5 * 3];
    final Random r = new Random();
    r.nextBytes(stream);

    final StopWatch watch = new StopWatch();
    watch.start();
    final byte[] enc = EncryptionUtils.encrypt(stream, passphrase);
    watch.stop();
    System.out.println("Took " + (float) watch.getNanoTime() / 1000000 + " ms");
    Assert.assertArrayEquals(stream, EncryptionUtils.decrypt(enc, passphrase));
  }
}
