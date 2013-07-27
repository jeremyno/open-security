package com.github.opencam.encryption;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

public class Aes128Encyption implements EncryptionPlugin {
  String passphrase;
  int approxSaltRepeats = 50;
  AesKey lastKey;

  public void setPassphrase(final String passphrase) {
    this.passphrase = passphrase;
  }

  public byte[] encrypt(final byte[] original) {
    final AesKey key = getKey();
    return encrypt(original, key);
  }

  private AesKey getKey() {
    if (lastKey == null) {
      synchronized (this) {
        lastKey = new AesKey(passphrase);
        return lastKey;
      }
    }

    if (lastKey.incrementAndGet() % approxSaltRepeats == 0) {
      lastKey = new AesKey(passphrase);
    }

    return lastKey;
  }

  private class AesKey {
    AtomicInteger count = new AtomicInteger();
    final byte[] salt = new byte[saltLength];
    final SecretKey secret;

    public AesKey(final String passphrase) {
      r.nextBytes(salt);
      SecretKeyFactory factory;
      try {
        factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        final KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, ENC_ITERATIONS, KEY_SIZE);
        final SecretKey tmp = factory.generateSecret(spec);

        secret = new SecretKeySpec(tmp.getEncoded(), "AES");
      } catch (final Exception e) {
        throw new RuntimeException("Problem creating key");
      }
    }

    public byte[] getSalt() {
      return salt;
    }

    public SecretKey getSecret() {
      return secret;
    }

    public int incrementAndGet() {
      return count.incrementAndGet();
    }

  }

  public byte[] decrypt(final byte[] encd) {
    return decrypt(encd, passphrase);
  }

  private static final int KEY_SIZE = 128;
  private static final int ENC_ITERATIONS = 65536;
  private static SecureRandom r = new SecureRandom();
  private final static int saltLength = 16;
  private final static int ivLength = 16;

  private static byte[] encrypt(final byte[] data, final AesKey key) {
    try {
      final ByteArrayInputStream in = new ByteArrayInputStream(data);
      final ByteArrayOutputStream base = new ByteArrayOutputStream(data.length + saltLength + ivLength);
      final OutputStream stream = getEncryptStream(base, key);
      IOUtils.copy(in, stream);
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(stream);
      IOUtils.closeQuietly(base);

      return base.toByteArray();
    } catch (final Exception e) {
      throw new RuntimeException("Problem encrypting data", e);
    }
  }

  public static byte[] decrypt(final byte[] data, final String password) {
    try {
      final ByteArrayInputStream in = new ByteArrayInputStream(data);
      final ByteArrayOutputStream base = new ByteArrayOutputStream(data.length + saltLength + ivLength);
      final InputStream stream = getDecryptStream(in, password);
      IOUtils.copy(stream, base);
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(stream);
      IOUtils.closeQuietly(base);

      return base.toByteArray();
    } catch (final Exception e) {
      throw new RuntimeException("Problem encrypting data", e);
    }
  }

  public static InputStream getDecryptStream(final InputStream stream, final String password) {
    final byte[] salt = new byte[saltLength];
    final byte[] iv = new byte[ivLength];

    try {
      IOUtils.readFully(stream, salt);
      IOUtils.readFully(stream, iv);

      final SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      final KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ENC_ITERATIONS, KEY_SIZE);
      final SecretKey tmp = factory.generateSecret(spec);
      final SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
      final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
      return new CipherInputStream(stream, cipher);
    } catch (final Exception e) {
      throw new RuntimeException("Problem creating cipher for decrypt", e);
    }
  }

  public static OutputStream getEncryptStream(final OutputStream ostream, final AesKey key) {

    try {
      /* Derive the key, given password and salt. */
      /* Encrypt the message. */
      final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, key.getSecret());
      final AlgorithmParameters params = cipher.getParameters();
      final byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();

      if (iv.length != ivLength) {
        throw new RuntimeException("IV was unexpected length");
      }

      final byte[] salt = key.getSalt();

      ostream.write(salt);
      ostream.write(iv);

      final CipherOutputStream cipherOutputStream = new CipherOutputStream(ostream, cipher);

      return cipherOutputStream;
    } catch (final Exception e) {
      throw new RuntimeException("problem creating encryption stream", e);
    }
  }

}
